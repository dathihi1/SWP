package com.badat.study1.configuration;

import com.badat.study1.service.IpLockoutService;
import com.badat.study1.service.SecurityEventService;
import com.badat.study1.model.SecurityEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter để chặn request từ IP bị khóa TRƯỚC KHI request đến controller.
 * Điều này giúp tiết kiệm tài nguyên server vì không cần xử lý request ở controller layer.
 * 
 * Filter này chạy ở servlet layer, trước cả DispatcherServlet và controller.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpBlockingFilter extends OncePerRequestFilter {

    private final IpLockoutService ipLockoutService;
    private final SecurityEventService securityEventService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Các endpoint cần check IP blocking
     * Chỉ áp dụng cho các endpoint authentication để tránh ảnh hưởng đến các API khác
     */
    private static final String[] BLOCKED_ENDPOINTS = {
        "/api/auth/login",
        "/api/auth/register", 
        "/api/auth/forgot-password",
        "/api/auth/verify-otp",
        "/api/auth/verify-register-otp",
        "/api/auth/verify-forgot-password-otp",
        "/api/auth/reset-password"
    };
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        // Chỉ check cho POST requests đến các endpoint authentication
        if (shouldCheckIp(requestPath, method)) {
            String ipAddress = getClientIpAddress(request);
            
            // Check IP lockout từ Redis (rất nhanh)
            if (ipLockoutService.isIpLocked(ipAddress)) {
                log.warn("🚫 IP Blocked at Filter layer - IP: {}, Path: {}, Method: {}", 
                        ipAddress, requestPath, method);
                
                // Log security event
                try {
                    securityEventService.logSecurityEvent(
                        SecurityEvent.EventType.IP_LOCKED, 
                        ipAddress, 
                        "Request blocked at filter layer: " + method + " " + requestPath
                    );
                } catch (Exception e) {
                    log.error("Failed to log security event: {}", e.getMessage());
                }
                
                // Trả về response ngay lập tức, không cần đi đến controller
                sendBlockedResponse(response, ipAddress);
                return; // Dừng filter chain, không cho request tiếp tục
            }
        }
        
        // IP không bị lock hoặc không phải endpoint cần check, cho phép tiếp tục
        filterChain.doFilter(request, response);
    }
    
    /**
     * Kiểm tra xem request có cần check IP blocking không
     */
    private boolean shouldCheckIp(String path, String method) {
        // Chỉ check POST requests
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        
        // Check xem path có trong danh sách endpoint cần block không
        for (String endpoint : BLOCKED_ENDPOINTS) {
            if (path.equals(endpoint)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gửi response khi IP bị block
     */
    private void sendBlockedResponse(HttpServletResponse response, String ipAddress) throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "IP đã bị khóa do quá nhiều lần đăng nhập sai");
        errorResponse.put("lockedUntil", "30 phút");
        errorResponse.put("ipAddress", ipAddress);
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
    
    /**
     * Lấy IP address từ request (hỗ trợ proxy/load balancer)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check X-Forwarded-For header (dùng khi có proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For có thể chứa nhiều IP, lấy IP đầu tiên
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }
    
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip static resources (không cần check IP cho static files)
        if (path.startsWith("/static/") || 
            path.startsWith("/css/") || 
            path.startsWith("/js/") || 
            path.startsWith("/images/") || 
            path.equals("/favicon.ico")) {
            return true;
        }
        
        return false;
    }
}

