package com.badat.study1.service;

import com.badat.study1.model.ApiCallLog;
import com.badat.study1.repository.ApiCallLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiCallLogService {
    
    private final ApiCallLogRepository apiCallLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // New method with extracted data to avoid request recycling
    @Async("apiLogExecutor")
    public void logApiCall(Long userId, String endpoint, String method, int statusCode, String ipAddress, String userAgent, long durationMs) {
        try {
            // Skip logging for certain endpoints
            if (shouldSkipLogging(endpoint)) {
                return;
            }
            
            log.debug("Logging API call: {} {} - UserId: {} - Status: {}", method, endpoint, userId, statusCode);
            
            // Determine response status
            String responseStatus = determineResponseStatus(statusCode);
            
            // Extract error message if any
            String errorMessage = statusCode >= 400 ? "HTTP " + statusCode : null;
            
            ApiCallLog apiLog = ApiCallLog.builder()
                    .userId(userId)
                    .endpoint(endpoint)
                    .method(method)
                    .statusCode(statusCode)
                    .requestParams(null) // Can't extract in async context
                    .requestBody(null)   // Can't extract in async context
                    .responseStatus(responseStatus)
                    .durationMs((int) durationMs)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .errorMessage(errorMessage)
                    .build();
            
            apiCallLogRepository.save(apiLog);
            log.info("API call logged: {} {} - {}ms - Status: {}", method, endpoint, durationMs, statusCode);
            
        } catch (Exception e) {
            log.error("Error logging API call: {}", e.getMessage());
        }
    }
    
    // Legacy method for backward compatibility (deprecated)
    @Async("apiLogExecutor")
    @Deprecated
    public void logApiCall(Long userId, HttpServletRequest request, HttpServletResponse response, long durationMs) {
        try {
            // Extract data BEFORE async call to avoid request recycling issues
            String endpoint = request.getRequestURI();
            String method = request.getMethod();
            int statusCode = response.getStatus();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            // Call new method with extracted data
            logApiCall(userId, endpoint, method, statusCode, ipAddress, userAgent, durationMs);
        } catch (Exception e) {
            log.error("Error logging API call (legacy): {}", e.getMessage());
        }
    }
    
    private boolean shouldSkipLogging(String endpoint) {
        // Skip static resources
        if (endpoint.startsWith("/css/") || 
            endpoint.startsWith("/js/") || 
            endpoint.startsWith("/images/") || 
            endpoint.startsWith("/favicon.ico")) {
            return true;
        }
        
        // Skip health check endpoints
        if (endpoint.equals("/actuator/health") || endpoint.equals("/health")) {
            return true;
        }
        
        // Skip sensitive endpoints (optional - you might want to log these for security)
        if (endpoint.contains("/password") || 
            endpoint.contains("/auth/login") || 
            endpoint.contains("/auth/register")) {
            return true;
        }
        
        return false;
    }
    
    private String extractRequestParams(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            Enumeration<String> paramNames = request.getParameterNames();
            
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);
                
                // Don't log sensitive parameters
                if (!isSensitiveParam(paramName)) {
                    params.put(paramName, paramValue);
                }
            }
            
            return params.isEmpty() ? null : objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            log.warn("Error serializing request parameters: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractRequestBody(HttpServletRequest request) {
        // Only log request body for POST/PUT requests
        if (!"POST".equals(request.getMethod()) && !"PUT".equals(request.getMethod())) {
            return null;
        }
        
        try {
            // Note: This is a simplified approach. In production, you might want to use
            // a more sophisticated approach to capture request body without consuming it
            return "Request body captured (implementation needed)";
        } catch (Exception e) {
            log.warn("Error extracting request body: {}", e.getMessage());
            return null;
        }
    }
    
    private boolean isSensitiveParam(String paramName) {
        String lowerParamName = paramName.toLowerCase();
        return lowerParamName.contains("password") || 
               lowerParamName.contains("token") || 
               lowerParamName.contains("secret") ||
               lowerParamName.contains("key");
    }
    
    private String determineResponseStatus(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return "SUCCESS";
        } else if (statusCode >= 300 && statusCode < 400) {
            return "REDIRECT";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "CLIENT_ERROR";
        } else if (statusCode >= 500) {
            return "SERVER_ERROR";
        } else {
            return "UNKNOWN";
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
}
