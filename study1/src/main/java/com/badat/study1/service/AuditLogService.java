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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void logLoginAttempt(User user, String ipAddress, boolean success, String failureReason, String deviceInfo) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user != null ? user.getId() : null)
                    .action("LOGIN")
                    .details(success ? "Đăng nhập thành công" : "Đăng nhập thất bại")
                    .ipAddress(ipAddress)
                    .success(success)
                    .failureReason(failureReason)
                    .deviceInfo(deviceInfo != null && !deviceInfo.isBlank() ? deviceInfo : "Unknown Device")
                    .build();
            
            auditLogRepository.save(auditLog);
            log.info("Audit log created for login attempt: user={}, ip={}, success={}, device={}", 
                    user != null ? user.getUsername() : "unknown", ipAddress, success, deviceInfo);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    public void logAccountLocked(User user, String ipAddress, String reason) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .action("ACCOUNT_LOCKED")
                    .details("Tài khoản bị khóa: " + reason)
                    .ipAddress(ipAddress)
                    .success(false)
                    .failureReason(reason)
                    .build();
            
            auditLogRepository.save(auditLog);
            log.info("Audit log created for account lock: user={}, ip={}, reason={}", 
                    user.getUsername(), ipAddress, reason);
        } catch (Exception e) {
            log.error("Failed to create audit log for account lock: {}", e.getMessage());
        }
    }

    public void logAccountUnlocked(User user, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .action("ACCOUNT_UNLOCKED")
                    .details("Tài khoản được mở khóa")
                    .ipAddress(ipAddress)
                    .success(true)
                    .build();
            
            auditLogRepository.save(auditLog);
            log.info("Audit log created for account unlock: user={}, ip={}", user.getUsername(), ipAddress);
        } catch (Exception e) {
            log.error("Failed to create audit log for account unlock: {}", e.getMessage());
        }
    }
    
    public void logProfileUpdate(User user, String ipAddress, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .action("PROFILE_UPDATE")
                    .details(details)
                    .ipAddress(ipAddress)
                    .success(true)
                    .deviceInfo("Unknown Device")
                    .build();
            
            auditLogRepository.save(auditLog);
            log.info("Audit log created for profile update: user={}, ip={}", user.getUsername(), ipAddress);
        } catch (Exception e) {
            log.error("Failed to create audit log for profile update: {}", e.getMessage());
        }
    }
    
    public void logLogout(User user, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .action("LOGOUT")
                    .details("Đăng xuất thành công")
                    .ipAddress(ipAddress)
                    .success(true)
                    .deviceInfo("Unknown Device")
                    .build();
            
            auditLogRepository.save(auditLog);
            log.info("Audit log created for logout: user={}, ip={}", user.getUsername(), ipAddress);
        } catch (Exception e) {
            log.error("Failed to create audit log for logout: {}", e.getMessage());
        }
    }

    public Page<AuditLogResponse> getUserAuditLogs(Long userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page - 1, size);
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
            Page<AuditLog> auditLogs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            
            return auditLogs.getContent().stream()
                    .map(AuditLogResponse::fromAuditLog)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get recent user audit logs: {}", e.getMessage());
            return List.of();
        }
    }

    public Page<AuditLogResponse> getUserAuditLogsWithFilters(Long userId, int page, int size, 
                                                           String action, Boolean success, 
                                                           String fromDateStr, String toDateStr) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                log.info("Attempting to get audit logs for user {} (attempt {})", userId, retryCount + 1);
                
                Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
                
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
                    auditLogs = auditLogRepository.findByUserIdWithFilters(
                            userId, action, success, fromDate, toDate, pageable);
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
}
