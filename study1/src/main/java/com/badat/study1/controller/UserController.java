package com.badat.study1.controller;

import com.badat.study1.annotation.UserActivity;
import com.badat.study1.dto.request.ChangePasswordRequest;
import com.badat.study1.dto.request.UpdateProfileRequest;
import com.badat.study1.dto.response.AuditLogResponse;
import com.badat.study1.model.User;
import com.badat.study1.model.UserActivityLog;
import com.badat.study1.service.AuditLogService;
import com.badat.study1.service.UserActivityLogService;
import com.badat.study1.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final UserActivityLogService userActivityLogService;
    private final AuditLogService auditLogService;

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

            User updatedUser = userService.updateProfile(currentUser.getId(), request);
            userActivityLogService.logProfileUpdate(updatedUser, "Cập nhật thông tin cá nhân", ipAddress, userAgent,
                    httpRequest.getRequestURI(), httpRequest.getMethod(), true, null);

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

    @PostMapping("/password")
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

            userService.changePassword(currentUser.getId(), request.getCurrentPassword(), request.getNewPassword());
            userActivityLogService.logPasswordChange(currentUser, ipAddress, userAgent,
                    httpRequest.getRequestURI(), httpRequest.getMethod(), true, null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đổi mật khẩu thành công"
            ));
        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Có lỗi xảy ra khi đổi mật khẩu: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            log.info("Avatar upload request for user: {}, IP: {}", currentUser.getUsername(), ipAddress);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống"));
            }
            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không được vượt quá 2MB"));
            }
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") &&
                !contentType.equals("image/png") && !contentType.equals("image/gif"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file JPG, PNG, GIF"));
            }

            userService.uploadAvatar(currentUser.getId(), file);
            userActivityLogService.logProfileUpdate(currentUser, "Cập nhật avatar", ipAddress, userAgent,
                    request.getRequestURI(), request.getMethod(), true, null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Avatar đã được cập nhật thành công"
            ));
        } catch (IOException e) {
            log.error("Error uploading avatar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Có lỗi xảy ra khi upload avatar: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error uploading avatar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Có lỗi xảy ra khi upload avatar: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            log.info("Avatar delete request for user: {}, IP: {}", currentUser.getUsername(), ipAddress);

            userService.deleteAvatar(currentUser.getId());
            userActivityLogService.logProfileUpdate(currentUser, "Xóa avatar", ipAddress, userAgent,
                    request.getRequestURI(), request.getMethod(), true, null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Avatar đã được xóa thành công"
            ));
        } catch (Exception e) {
            log.error("Error deleting avatar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Có lỗi xảy ra khi xóa avatar: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        try {
            byte[] avatarData = userService.getAvatar(userId);
            if (avatarData != null && avatarData.length > 0) {
                HttpHeaders headers = new HttpHeaders();
                String contentType = detectContentType(avatarData);
                headers.setContentType(MediaType.valueOf(contentType));
                headers.setContentLength(avatarData.length);
                headers.setCacheControl("no-cache, no-store, must-revalidate");
                return ResponseEntity.ok().headers(headers).body(avatarData);
            } else {
                return getDefaultAvatarResponse();
            }
        } catch (Exception e) {
            log.error("Error getting avatar for user {}: {}", userId, e.getMessage());
            return getDefaultAvatarResponse();
        }
    }

    @GetMapping("/avatar/me")
    public ResponseEntity<byte[]> getMyAvatar() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
                return getDefaultAvatarResponse();
            }
            User currentUser = (User) authentication.getPrincipal();
            byte[] avatarData = userService.getAvatar(currentUser.getId());
            if (avatarData != null && avatarData.length > 0) {
                HttpHeaders headers = new HttpHeaders();
                String contentType = detectContentType(avatarData);
                headers.setContentType(MediaType.valueOf(contentType));
                headers.setContentLength(avatarData.length);
                headers.setCacheControl("no-cache, no-store, must-revalidate");
                return ResponseEntity.ok().headers(headers).body(avatarData);
            }
            return getDefaultAvatarResponse();
        } catch (Exception e) {
            log.error("Error getting current user's avatar: {}", e.getMessage());
            return getDefaultAvatarResponse();
        }
    }

    @GetMapping("/audit-logs/me")
    public ResponseEntity<Page<AuditLogResponse>> getMyAuditLogs(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size,
                                                                 @RequestParam(required = false) String action,
                                                                 @RequestParam(required = false) Boolean success,
                                                                 @RequestParam(required = false, name = "from") String fromDate,
                                                                 @RequestParam(required = false, name = "to") String toDate,
                                                                 @RequestParam(required = false) String category) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            return ResponseEntity.status(401).build();
        }
        User user = (User) auth.getPrincipal();
        Page<AuditLogResponse> logs = auditLogService.getUserAuditLogsWithFilters(user.getId(), page, size, action, success, fromDate, toDate, category);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/audit-logs/categories")
    public ResponseEntity<java.util.List<String>> getAvailableCategories() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            return ResponseEntity.status(401).build();
        }
        User user = (User) auth.getPrincipal();
        java.util.List<com.badat.study1.model.AuditLog.Category> categories = auditLogService.getAvailableCategories(user.getId());
        java.util.List<String> categoryNames = categories.stream().map(Enum::name).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(categoryNames);
    }

    @GetMapping("/activity-logs")
    public ResponseEntity<?> getMyActivityLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) Boolean success,
        @RequestParam(required = false) String fromDate,
        @RequestParam(required = false) String toDate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User currentUser = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        LocalDate from = null;
        LocalDate to = null;
        if (fromDate != null && !fromDate.isEmpty()) {
            from = LocalDate.parse(fromDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (toDate != null && !toDate.isEmpty()) {
            to = LocalDate.parse(toDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        }
        String normalizedAction = (action != null && action.trim().isEmpty()) ? null : action;
        Page<UserActivityLog> logs = userActivityLogService.getUserActivities(
            currentUser.getId(), normalizedAction, success, from, to, pageable);
        Page<com.badat.study1.dto.response.UserActivityLogResponse> response = logs.map(com.badat.study1.dto.response.UserActivityLogResponse::fromEntity);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("content", response.getContent());
        result.put("totalElements", response.getTotalElements());
        result.put("totalPages", response.getTotalPages());
        result.put("currentPage", response.getNumber());
        result.put("size", response.getSize());
        result.put("first", response.isFirst());
        result.put("last", response.isLast());
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<byte[]> getDefaultAvatarResponse() {
        try {
            byte[] defaultAvatar = userService.getDefaultAvatar();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/svg+xml"));
            headers.setContentLength(defaultAvatar.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            return ResponseEntity.ok().headers(headers).body(defaultAvatar);
        } catch (Exception e) {
            log.error("Error getting default avatar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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

    private String detectContentType(byte[] data) {
        if (data.length < 4) {
            return "image/svg+xml";
        }
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (data[0] == (byte) 0x89 && data[1] == (byte) 0x50 && data[2] == (byte) 0x4E && data[3] == (byte) 0x47) {
            return "image/png";
        }
        if (data[0] == (byte) 0x47 && data[1] == (byte) 0x49 && data[2] == (byte) 0x46 && data[3] == (byte) 0x38) {
            return "image/gif";
        }
        String content = new String(data, 0, Math.min(100, data.length));
        if (content.contains("<svg") || content.contains("<?xml")) {
            return "image/svg+xml";
        }
        return "image/png";
    }
}