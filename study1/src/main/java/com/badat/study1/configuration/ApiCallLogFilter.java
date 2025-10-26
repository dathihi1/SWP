package com.badat.study1.configuration;

import com.badat.study1.model.User;
import com.badat.study1.service.ApiCallLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiCallLogFilter extends OncePerRequestFilter {
    
    private final ApiCallLogService apiCallLogService;
    
    private static final String START_TIME_ATTRIBUTE = "apiCallStartTime";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Record start time for duration calculation
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        
        // Extract user ID before processing
        Long userId = getCurrentUserId();
        
        // Process the request
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // Log failed requests that don't pass through filter chain
            logFailedRequest(request, response, userId, e);
            throw e;
        } finally {
            // Log successful requests that passed through filter chain
            logSuccessfulRequest(request, response, userId);
        }
    }
    
    private void logSuccessfulRequest(HttpServletRequest request, HttpServletResponse response, Long userId) {
        try {
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            if (startTime == null) {
                log.warn("Start time not found for request: {}", request.getRequestURI());
                return;
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            
            // Log the API call asynchronously
            apiCallLogService.logApiCall(userId, request, response, durationMs);
            
        } catch (Exception e) {
            log.error("Error logging successful API call: {}", e.getMessage());
        }
    }
    
    private void logFailedRequest(HttpServletRequest request, HttpServletResponse response, Long userId, Exception error) {
        try {
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            if (startTime == null) {
                log.warn("Start time not found for failed request: {}", request.getRequestURI());
                return;
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            
            // Set error status on the response
            response.setStatus(500);
            
            // Log the failed API call
            apiCallLogService.logApiCall(userId, request, response, durationMs);
            
        } catch (Exception e) {
            log.error("Error logging failed API call: {}", e.getMessage());
        }
    }
    
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return null;
            }
            
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return ((User) principal).getId();
            }
            
            return null;
        } catch (Exception e) {
            log.warn("Error extracting user ID from SecurityContext: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip static resources
        if (path.startsWith("/css/") || 
            path.startsWith("/js/") || 
            path.startsWith("/images/") || 
            path.startsWith("/favicon.ico")) {
            return true;
        }
        
        // Skip health check endpoints
        if (path.equals("/actuator/health") || path.equals("/health")) {
            return true;
        }
        
        return false;
    }
    
}
