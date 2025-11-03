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
import org.springframework.lang.NonNull;
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
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        // Record start time for duration calculation
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        
        // Process the request and log exactly once (success OR fail)
        boolean hasException = false;
        Exception captured = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            hasException = true;
            captured = e;
        } finally {
            Long userId = getCurrentUserId();
            int status = response.getStatus();
            if (hasException || status >= 400) {
                logFailedRequest(request, response, userId, captured);
            } else {
                logSuccessfulRequest(request, response, userId);
            }
            if (hasException) {
                if (captured instanceof ServletException) {
                    throw (ServletException) captured;
                } else if (captured instanceof IOException) {
                    throw (IOException) captured;
                } else {
                    throw new RuntimeException(captured);
                }
            }
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
            
            // Extract data BEFORE async call to avoid request recycling issues
            String endpoint = request.getRequestURI();
            String method = request.getMethod();
            int statusCode = response.getStatus();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            // Log the API call asynchronously with extracted data
            apiCallLogService.logApiCall(userId, endpoint, method, statusCode, ipAddress, userAgent, durationMs);
            
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
            
            // Extract data BEFORE async call to avoid request recycling issues
            String endpoint = request.getRequestURI();
            String method = request.getMethod();
            int statusCode = response.getStatus();
            if (statusCode < 400) {
                statusCode = 500; // default to 500 if not set to an error
            }
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            // Ensure response reflects the error status (do not override non-error statuses)
            try {
                response.setStatus(statusCode);
            } catch (Exception ignore) {}
            
            // Log the failed API call asynchronously with extracted data
            apiCallLogService.logApiCall(userId, endpoint, method, statusCode, ipAddress, userAgent, durationMs);
            
        } catch (Exception e) {
            log.error("Error logging failed API call: {}", e.getMessage());
        }
    }
    
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                log.debug("No authenticated user found for API call");
                return null;
            }
            
            Object principal = authentication.getPrincipal();
            log.debug("Principal type: {}, Principal: {}", principal.getClass().getSimpleName(), principal);
            
            // Now both Google and Local users use User object as principal
            if (principal instanceof User) {
                User user = (User) principal;
                Long userId = user.getId();
                log.debug("Extracted user ID: {} for user: {}", userId, user.getUsername());
                return userId;
            }
            
            log.debug("Principal is not a User instance: {}", principal.getClass().getName());
            return null;
        } catch (Exception e) {
            log.warn("Error extracting user ID from SecurityContext: {}", e.getMessage());
            return null;
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
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
