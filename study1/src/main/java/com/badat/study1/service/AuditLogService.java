package com.badat.study1.service;

import com.badat.study1.dto.response.AuditLogResponse;
import com.badat.study1.model.AuditLog;
import com.badat.study1.model.User;
import com.badat.study1.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final Logger fileLogger = LoggerFactory.getLogger("com.badat.study1.audit");

    @Async("auditExecutor")
    public void logRoleChange(User adminUser, User targetUser, User.Role oldRole, User.Role newRole, String reason) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(adminUser.getId())
                    .action("ROLE_CHANGE")
                    .category(AuditLog.Category.SYSTEM_EVENT)
                    .details(String.format("Admin %s đã thay đổi vai trò của user %s từ %s thành %s. Lý do: %s", 
                        adminUser.getUsername(), targetUser.getUsername(), oldRole.name(), newRole.name(), 
                        reason != null ? reason : "Không có lý do"))
                    .ipAddress("127.0.0.1") // You might want to get real IP
                    .success(true)
                    .failureReason(null)
                    .deviceInfo("Admin Panel")
                    .build();
            
            auditLogRepository.save(auditLog);
            
            // Log to file
            fileLogger.info("ROLE_CHANGE: Admin {} changed user {} role from {} to {}. Reason: {}", 
                adminUser.getUsername(), targetUser.getUsername(), oldRole.name(), newRole.name(), reason);
                
        } catch (Exception e) {
            log.error("Error logging role change: {}", e.getMessage(), e);
        }
    }

    @Async("auditExecutor")
    public void logUserEdit(User adminUser, User targetUser, Map<String, String> changes) {
        try {
            StringBuilder details = new StringBuilder();
            details.append("Admin ").append(adminUser.getUsername())
                   .append(" đã chỉnh sửa thông tin của user ").append(targetUser.getUsername())
                   .append(". Các thay đổi: ");
            
            boolean hasChanges = false;
            for (Map.Entry<String, String> entry : changes.entrySet()) {
                if (!entry.getKey().equals("userId") && entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    if (hasChanges) details.append(", ");
                    details.append(entry.getKey()).append("=").append(entry.getValue());
                    hasChanges = true;
                }
            }
            
            if (!hasChanges) {
                details.append("Không có thay đổi nào");
            }
            
            AuditLog auditLog = AuditLog.builder()
                    .userId(adminUser.getId())
                    .action("USER_EDIT")
                    .category(AuditLog.Category.ADMIN_ACTION)
                    .details(details.toString())
                    .ipAddress("127.0.0.1") // You might want to get real IP
                    .success(true)
                    .failureReason(null)
                    .deviceInfo("Admin Panel")
                    .build();
            
            auditLogRepository.save(auditLog);
            
            // Log to file
            fileLogger.info("USER_EDIT: Admin {} edited user {} information. Changes: {}", 
                adminUser.getUsername(), targetUser.getUsername(), details.toString());
                
        } catch (Exception e) {
            log.error("Error logging user edit: {}", e.getMessage(), e);
        }
    }

    @Async("auditExecutor")
    public void logUserCreation(User adminUser, User newUser) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(adminUser.getId())
                    .action("USER_CREATE")
                    .category(AuditLog.Category.ADMIN_ACTION)
                    .details(String.format("Admin %s đã tạo tài khoản mới cho user %s (Email: %s)", 
                        adminUser.getUsername(), newUser.getUsername(), newUser.getEmail()))
                    .ipAddress("127.0.0.1") // You might want to get real IP
                    .success(true)
                    .failureReason(null)
                    .deviceInfo("Admin Panel")
                    .build();
            
            auditLogRepository.save(auditLog);
            
            // Log to file
            fileLogger.info("USER_CREATE: Admin {} created new user {} (Email: {})", 
                adminUser.getUsername(), newUser.getUsername(), newUser.getEmail());
                
        } catch (Exception e) {
            log.error("Error logging user creation: {}", e.getMessage(), e);
        }
    }

    @Async("auditExecutor")
    public void logLoginAttempt(User user, String ipAddress, boolean success, String failureReason, String deviceInfo) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user != null ? user.getId() : null)
                    .action("LOGIN")
                    .category(AuditLog.Category.USER_ACTION)
                    .details(success ? 
                        "User đăng nhập thành công từ IP: " + ipAddress + 
                        (deviceInfo != null ? " với thiết bị: " + deviceInfo : "") :
                        "User đăng nhập thất bại từ IP: " + ipAddress + 
                        (failureReason != null ? " - Lý do: " + failureReason : "") +
                        (deviceInfo != null ? " với thiết bị: " + deviceInfo : ""))
                    .ipAddress(ipAddress)
                    .success(success)
                    .failureReason(failureReason)
                    .deviceInfo(deviceInfo != null && !deviceInfo.isBlank() ? deviceInfo : "Unknown Device")
                    .build();
            
            auditLogRepository.save(auditLog);
            fileLogger.info("LOGIN userId={} ip={} success={} ua=\"{}\" reason={}",
                    user != null ? user.getId() : null, ipAddress, success, deviceInfo, failureReason);
            log.info("Audit log created for login attempt: user={}, ip={}, success={}, device={}", 
                    user != null ? user.getUsername() : "unknown", ipAddress, success, deviceInfo);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    @Async("auditExecutor")
    public void logAccountLocked(User user, String ipAddress, String reason) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .action("ACCOUNT_LOCKED")
                    .category(AuditLog.Category.SECURITY_EVENT)
                    .details("SECURITY EVENT: Tài khoản " + user.getUsername() + " bị khóa từ IP: " + ipAddress + " - Lý do: " + reason)
                    .ipAddress(ipAddress)
                    .success(false)
                    .failureReason(reason)
                    .build();
            
            auditLogRepository.save(auditLog);
            fileLogger.info("ACCOUNT_LOCKED userId={} ip={} reason=\"{}\"", user.getId(), ipAddress, reason);
            log.info("Audit log created for account lock: user={}, ip={}, reason={}", 
                    user.getUsername(), ipAddress, reason);
        } catch (Exception e) {
            log.error("Failed to create audit log for account lock: {}", e.getMessage());
        }
    }

    @Async("auditExecutor")
    public void logAccountUnlocked(User user, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .action("ACCOUNT_UNLOCKED")
                    .category(AuditLog.Category.SECURITY_EVENT)
                    .details("SECURITY EVENT: Tài khoản " + user.getUsername() + " được mở khóa từ IP: " + ipAddress)
                    .ipAddress(ipAddress)
                    .success(true)
                    .build();
            
            auditLogRepository.save(auditLog);
            fileLogger.info("ACCOUNT_UNLOCKED userId={} ip={}", user.getId(), ipAddress);
            log.info("Audit log created for account unlock: user={}, ip={}", user.getUsername(), ipAddress);
        } catch (Exception e) {
            log.error("Failed to create audit log for account unlock: {}", e.getMessage());
        }
    }
    
    @Async("auditExecutor")
    public void logProfileUpdate(User user, String ipAddress, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .action("PROFILE_UPDATE")
                    .category(AuditLog.Category.USER_ACTION)
                    .details("USER ACTION: " + user.getUsername() + " cập nhật thông tin cá nhân từ IP: " + ipAddress + " - Chi tiết: " + details)
                    .ipAddress(ipAddress)
                    .success(true)
                    .deviceInfo("Unknown Device")
                    .build();
            
            auditLogRepository.save(auditLog);
            fileLogger.info("PROFILE_UPDATE userId={} ip={} details=\"{}\"", user.getId(), ipAddress, details);
            log.info("Audit log created for profile update: user={}, ip={}", user.getUsername(), ipAddress);
        } catch (Exception e) {
            log.error("Failed to create audit log for profile update: {}", e.getMessage());
        }
    }
    
    @Async("auditExecutor")
    public void logLogout(User user, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .action("LOGOUT")
                    .category(AuditLog.Category.USER_ACTION)
                    .details("USER ACTION: " + user.getUsername() + " đăng xuất thành công từ IP: " + ipAddress)
                    .ipAddress(ipAddress)
                    .success(true)
                    .deviceInfo("Unknown Device")
                    .build();
            
            auditLogRepository.save(auditLog);
            log.info("Audit log created for logout: user={}, ip={}", user.getUsername(), ipAddress);
            fileLogger.info("LOGOUT userId={} ip={} ua=\"{}\"", user.getId(), ipAddress, "Unknown Device");
        } catch (Exception e) {
            log.error("Failed to create audit log for logout: {}", e.getMessage());
        }
    }

    @Async("auditExecutor")
    public void logAction(User user, String action, String details, String ipAddress, boolean success, String failureReason, String deviceInfo) {
        logAction(user, action, details, ipAddress, success, failureReason, deviceInfo, AuditLog.Category.USER_ACTION);
    }
    
    @Async("auditExecutor")
    public void logAction(User user, String action, String details, String ipAddress, boolean success, String failureReason, String deviceInfo, AuditLog.Category category) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user != null ? user.getId() : null)
                    .action(action)
                    .category(category)
                    .details(details)
                    .ipAddress(ipAddress)
                    .success(success)
                    .failureReason(failureReason)
                    .deviceInfo(deviceInfo != null && !deviceInfo.isBlank() ? deviceInfo : "Unknown Device")
                    .build();

            auditLogRepository.save(auditLog);
            fileLogger.info("{} userId={} ip={} success={} details=\"{}\" reason={} ua=\"{}\" category={}",
                    action, auditLog.getUserId(), ipAddress, success, details, failureReason, auditLog.getDeviceInfo(), category);
        } catch (Exception e) {
            log.error("Failed to create generic audit log: {}", e.getMessage());
        }
    }

    public Page<AuditLogResponse> getUserAuditLogs(Long userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<AuditLog> auditLogs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            
            return auditLogs.map(AuditLogResponse::fromAuditLog);
        } catch (Exception e) {
            log.error("Failed to get user audit logs: {}", e.getMessage());
            return Page.empty();
        }
    }

    public List<AuditLogResponse> getRecentUserAuditLogs(Long userId, int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            // Chỉ lấy USER_ACTION cho user thường
            Page<AuditLog> auditLogs = auditLogRepository.findUserViewWithFilters(
                    userId, null, null, null, null, pageable);
            
            return auditLogs.getContent().stream()
                    .map(AuditLogResponse::fromAuditLog)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get recent user audit logs: {}", e.getMessage());
            return List.of();
        }
    }
    
    // Method cho admin - xem tất cả categories
    public List<AuditLogResponse> getRecentUserAuditLogsForAdmin(Long userId, int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            Page<AuditLog> auditLogs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            
            return auditLogs.getContent().stream()
                    .map(AuditLogResponse::fromAuditLog)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get recent user audit logs for admin: {}", e.getMessage());
            return List.of();
        }
    }

    public Page<AuditLogResponse> getUserAuditLogsWithFilters(Long userId, int page, int size, 
                                                           String action, Boolean success, 
                                                           String fromDateStr, String toDateStr, String category) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                log.info("Attempting to get audit logs for user {} (attempt {})", userId, retryCount + 1);
                
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                
                LocalDateTime fromDate = null;
                LocalDateTime toDate = null;
                
                if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
                    fromDate = LocalDateTime.parse(fromDateStr + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
                
                if (toDateStr != null && !toDateStr.trim().isEmpty()) {
                    toDate = LocalDateTime.parse(toDateStr + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
                
                // Database query with timeout handling
                Page<AuditLog> auditLogs;
                try {
                    if (category != null && !category.trim().isEmpty()) {
                        // Filter by specific category
                        AuditLog.Category categoryEnum = AuditLog.Category.valueOf(category.toUpperCase());
                        auditLogs = auditLogRepository.findByUserIdAndCategoryWithFilters(
                                userId, categoryEnum, action, success, fromDate, toDate, pageable);
                    } else {
                        // Default: show only USER_ACTION category for regular users
                        auditLogs = auditLogRepository.findUserViewWithFilters(
                                userId, action, success, fromDate, toDate, pageable);
                    }
                    log.info("Successfully retrieved {} audit logs for user {}", auditLogs.getTotalElements(), userId);
                } catch (Exception dbException) {
                    log.error("Database error getting audit logs for user {} (attempt {}): {}", userId, retryCount + 1, dbException.getMessage());
                    if (retryCount == maxRetries - 1) {
                        return Page.empty();
                    }
                    retryCount++;
                    Thread.sleep(1000); // Wait 1 second before retry
                    continue;
                }
                
                // Map with error handling for each item
                return auditLogs.map(auditLog -> {
                    try {
                        return AuditLogResponse.fromAuditLog(auditLog);
                    } catch (Exception mappingException) {
                        log.error("Error mapping audit log {}: {}", auditLog.getId(), mappingException.getMessage());
                        return null;
                    }
                });
                
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted while getting audit logs for user {}", userId);
                return Page.empty();
            } catch (Exception e) {
                log.error("Failed to get user audit logs with filters for user {} (attempt {}): {}", userId, retryCount + 1, e.getMessage());
                if (retryCount == maxRetries - 1) {
                    log.error("All retry attempts failed for user {}", userId);
                    return Page.empty();
                }
                retryCount++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Page.empty();
                }
            }
        }
        
        return Page.empty();
    }

    public Page<AuditLogResponse> getAdminAuditLogsWithFilters(Long userId, int page, int size,
                                                               String action, Boolean success,
                                                               String fromDateStr, String toDateStr) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            LocalDateTime fromDate = null;
            LocalDateTime toDate = null;

            if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
                fromDate = LocalDateTime.parse(fromDateStr + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            if (toDateStr != null && !toDateStr.trim().isEmpty()) {
                toDate = LocalDateTime.parse(toDateStr + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            Page<AuditLog> auditLogs = auditLogRepository.findAdminViewWithFilters(
                    userId, action, success, fromDate, toDate, pageable);

            return auditLogs.map(AuditLogResponse::fromAuditLog);
        } catch (Exception e) {
            log.error("Failed to get admin audit logs with filters for user {}: {}", userId, e.getMessage());
            return Page.empty();
        }
    }
    
    public List<AuditLog.Category> getAvailableCategories(Long userId) {
        try {
            return auditLogRepository.findDistinctCategoriesByUserId(userId);
        } catch (Exception e) {
            log.error("Failed to get available categories for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }
    
    @Async("auditExecutor")
    public void logPasswordChange(User user, String ipAddress, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .action("PASSWORD_CHANGE")
                    .category(AuditLog.Category.USER_ACTION)
                    .details(details)
                    .ipAddress(ipAddress)
                    .success(true)
                    .failureReason(null)
                    .deviceInfo("Web Browser")
                    .build();

            auditLogRepository.save(auditLog);

            // Log to file
            fileLogger.info("PASSWORD_CHANGE: User {} changed password successfully. IP: {}", 
                user.getUsername(), ipAddress);

        } catch (Exception e) {
            log.error("Error logging password change: {}", e.getMessage(), e);
        }
    }
}
