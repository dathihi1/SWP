package com.badat.study1.controller;

import com.badat.study1.dto.request.UpdateProfileRequest;
import com.badat.study1.model.User;
import com.badat.study1.service.AuditLogService;
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
    private final AuditLogService auditLogService;
    
    @PutMapping("/profile")
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
            
            log.info("Profile update request for user: {}, IP: {}", currentUser.getUsername(), ipAddress);
            
            // Update user profile
            User updatedUser = userService.updateProfile(currentUser.getId(), request);
            
            // Log the profile update
            auditLogService.logProfileUpdate(updatedUser, ipAddress, "Cập nhật thông tin cá nhân");
            
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