package com.badat.study1.controller;

import com.badat.study1.dto.request.LoginRequest;
import com.badat.study1.dto.response.LoginResponse;
import com.badat.study1.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for username: {}", loginRequest.getUsername());
            
    
            LoginResponse loginResponse = authenticationService.login(loginRequest);
            log.info("Login successful for username: {}", loginRequest.getUsername());
            
            return ResponseEntity.ok(loginResponse);
            
        } catch (Exception e) {
            log.error("Login failed for username: {}, error: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Tên đăng nhập hoặc mật khẩu không đúng"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Authorization header không hợp lệ"));
            }
            
            String token = authHeader.replace("Bearer ", "");
            authenticationService.logout(token);
            
            log.info("Logout successful");
            return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
            
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
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", auth.getName());
            userInfo.put("authorities", auth.getAuthorities());
            
            return ResponseEntity.ok(userInfo);
            
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

    @GetMapping("/debug")
    public ResponseEntity<?> debugAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("authenticated", auth != null && auth.isAuthenticated());
        debugInfo.put("username", auth != null ? auth.getName() : "null");
        debugInfo.put("authorities", auth != null ? auth.getAuthorities() : "null");
        debugInfo.put("principal", auth != null ? auth.getPrincipal().getClass().getSimpleName() : "null");
        
        return ResponseEntity.ok(debugInfo);
    }
}
