package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "auditlog")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "user_id")
    Long userId;
    
    @Column(name = "action", nullable = false)
    String action;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    Category category = Category.ADMIN_ACTION;
    
    @Column(name = "details", columnDefinition = "TEXT")
    String details;
    
    @Column(name = "ip_address", length = 50)
    String ipAddress;
    
    @Column(name = "success", nullable = false)
    @Builder.Default
    Boolean success = true;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    String failureReason;
    
    @Column(name = "device_info", length = 500)
    String deviceInfo;
    
    @Column(name = "endpoint", length = 255)
    String endpoint;
    
    @Column(name = "method", length = 10)
    String method;
    
    public enum Category {
        SECURITY_EVENT,  // Các sự kiện bảo mật (failed login attempts, suspicious activities, account locked/unlocked)
        ADMIN_ACTION,    // Các hành động của admin (thêm, sửa, xóa user, thay đổi role, etc.)
        API_CALL,        // Các lời gọi API (API requests, responses)
        SYSTEM_EVENT,    // Các sự kiện hệ thống (system events, maintenance, etc.)
        USER_ACTION      // Các hành động của user (user actions, profile updates, etc.)
    }
}
