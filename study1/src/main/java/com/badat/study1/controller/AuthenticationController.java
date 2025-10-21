package com.badat.study1.controller;

import com.badat.study1.dto.request.LoginRequest;
import com.badat.study1.dto.response.LoginResponse;
import com.badat.study1.service.AuthenticationService;
import com.badat.study1.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AuditLogService auditLogService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            log.info("Login attempt for username: {}", loginRequest.getUsername());
            
            // Get client IP address
            String ipAddress = getClientIpAddress(request);
            log.info("Login attempt from IP: {}", ipAddress);
            // Get simple device info from User-Agent
            String userAgent = request.getHeader("User-Agent");
    
            LoginResponse loginResponse = authenticationService.login(loginRequest, ipAddress, userAgent);
            log.info("Login successful for username: {}", loginRequest.getUsername());

            // Set HttpOnly access token cookie for browser navigation
            boolean secure = request.isSecure();
            ResponseCookie accessCookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                    .httpOnly(true)
                    .secure(secure)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(60L * 60L) // 1 hour, align with token expiry
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(loginResponse);
            
        } catch (Exception e) {
            log.error("Login failed for username: {}, error: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Tên đăng nhập hoặc mật khẩu không đúng"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader, HttpServletRequest request) {
        try {
            // Prefer header; if missing, try cookie via JwtAuthenticationFilter, but here we only need header token to blacklist
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // Still clear cookie even if header missing
                boolean secure = request.isSecure();
                ResponseCookie clearCookie = ResponseCookie.from("accessToken", "")
                        .httpOnly(true)
                        .secure(secure)
                        .path("/")
                        .sameSite("Lax")
                        .maxAge(0)
                        .build();
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                        .body(Map.of("message", "Đăng xuất thành công"));
            }

            String token = authHeader.replace("Bearer ", "");
            
            // Get user info before logout for audit logging
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                Object principal = auth.getPrincipal();
                if (principal instanceof com.badat.study1.model.User) {
                    com.badat.study1.model.User user = (com.badat.study1.model.User) principal;
                    String ipAddress = getClientIpAddress(request);
                    auditLogService.logLogout(user, ipAddress);
                }
            }
            
            authenticationService.logout(token);
            
            log.info("Logout successful");
            // Clear access token cookie
            boolean secure = request.isSecure();
            ResponseCookie clearCookie = ResponseCookie.from("accessToken", "")
                    .httpOnly(true)
                    .secure(secure)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(0)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(Map.of("message", "Đăng xuất thành công"));
            
        } catch (ParseException e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Token không hợp lệ"));
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Có lỗi xảy ra khi đăng xuất"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Chưa đăng nhập"));
            }
            
            // Get User object from authentication principal
            Object principal = auth.getPrincipal();
            if (principal instanceof com.badat.study1.model.User) {
                com.badat.study1.model.User user = (com.badat.study1.model.User) principal;
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("email", user.getEmail());
                userInfo.put("role", user.getRole().name());
                userInfo.put("status", user.getStatus().name());
                userInfo.put("authorities", auth.getAuthorities());
                
                return ResponseEntity.ok(userInfo);
            } else {
                // Fallback for other types of principals
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("username", auth.getName());
                userInfo.put("authorities", auth.getAuthorities());
                
                return ResponseEntity.ok(userInfo);
            }
            
        } catch (Exception e) {
            log.error("Get current user failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Có lỗi xảy ra khi lấy thông tin người dùng"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Authorization header không hợp lệ"));
            }
            
            String refreshToken = authHeader.replace("Bearer ", "");
            LoginResponse loginResponse = authenticationService.refreshToken(refreshToken);
            
            log.info("Token refresh successful");
            return ResponseEntity.ok(loginResponse);
            
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Refresh token không hợp lệ hoặc đã hết hạn"));
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

