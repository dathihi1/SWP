package com.badat.study1.dto.response;

import com.badat.study1.model.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String action;
    private String details;
    private String ipAddress;
    private Boolean success;
    private String failureReason;
    private String deviceInfo;
    private LocalDateTime createdAt;
    private String actionDisplayName;
    private String successIcon;
    private String successColor;

    public static AuditLogResponse fromAuditLog(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .details(auditLog.getDetails())
                .ipAddress(auditLog.getIpAddress())
                .success(auditLog.getSuccess())
                .failureReason(auditLog.getFailureReason())
                .deviceInfo(auditLog.getDeviceInfo())
                .createdAt(auditLog.getCreatedAt())
                .actionDisplayName(getActionDisplayName(auditLog.getAction()))
                .successIcon(auditLog.getSuccess() ? "fa-check-circle" : "fa-times-circle")
                .successColor(auditLog.getSuccess() ? "text-success" : "text-danger")
                .build();
    }

    private static String getActionDisplayName(String action) {
        switch (action) {
            case "LOGIN":
                return "Đăng nhập";
            case "LOGOUT":
                return "Đăng xuất";
            case "ACCOUNT_LOCKED":
                return "Khóa tài khoản";
            case "ACCOUNT_UNLOCKED":
                return "Mở khóa tài khoản";
            case "PASSWORD_CHANGE":
                return "Đổi mật khẩu";
            case "PROFILE_UPDATE":
                return "Cập nhật thông tin";
            case "REGISTER":
                return "Đăng ký tài khoản";
            case "OTP_VERIFY":
                return "Xác minh OTP";
            default:
                return action;
        }
    }
}
