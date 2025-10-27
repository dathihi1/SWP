package com.badat.study1.controller;

import com.badat.study1.annotation.UserActivity;
import com.badat.study1.model.UserActivityLog;
import com.badat.study1.dto.request.UpdateProfileRequest;
import com.badat.study1.dto.request.ChangePasswordRequest;
import com.badat.study1.model.User;
import com.badat.study1.service.UserActivityLogService;
import com.badat.study1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class ProfileController {
    
    private final UserService userService;
    private final UserActivityLogService userActivityLogService;
    
    @PutMapping("/profile")
    @UserActivity(action = "PROFILE_UPDATE", category = UserActivityLog.Category.ACCOUNT)
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request, 
                                        HttpServletRequest httpRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            User currentUser = (User) authentication.getPrincipal();
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            log.info("Profile update request for user: {}, IP: {}", currentUser.getUsername(), ipAddress);
            
            // Update user profile
            User updatedUser = userService.updateProfile(currentUser.getId(), request);
            
            // Log the profile update
            userActivityLogService.logProfileUpdate(updatedUser, "Cập nhật thông tin cá nhân", ipAddress, userAgent, httpRequest.getRequestURI(), httpRequest.getMethod(), true, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật thông tin thành công");
            response.put("data", Map.of(
                "id", updatedUser.getId(),
                "username", updatedUser.getUsername(),
                "email", updatedUser.getEmail(),
                "fullName", updatedUser.getFullName(),
                "phone", updatedUser.getPhone(),
                "role", updatedUser.getRole().name(),
                "status", updatedUser.getStatus().name()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Có lỗi xảy ra khi cập nhật thông tin: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/profile/change-password")
    @UserActivity(action = "PASSWORD_CHANGE", category = UserActivityLog.Category.ACCOUNT)
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, 
                                          HttpServletRequest httpRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            User currentUser = (User) authentication.getPrincipal();
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            log.info("Password change request for user: {}, IP: {}", currentUser.getUsername(), ipAddress);
            
            // Change password
            userService.changePassword(currentUser.getId(), request.getCurrentPassword(), request.getNewPassword());
            
            // Log the password change
            userActivityLogService.logPasswordChange(currentUser, ipAddress, userAgent, httpRequest.getRequestURI(), httpRequest.getMethod(), true, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đổi mật khẩu thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Có lỗi xảy ra khi đổi mật khẩu: " + e.getMessage()
            ));
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