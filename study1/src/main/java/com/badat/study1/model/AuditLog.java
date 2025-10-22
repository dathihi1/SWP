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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;
    
    @Column(name = "action", nullable = false)
    String action;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    Category category = Category.USER_ACTION;
    
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
    
    public enum Category {
        USER_ACTION,    // Các hoạt động mà user có thể nhìn thấy (login, logout, profile update, etc.)
        API_CALL,       // Các API calls (internal system calls, background processes)
        SYSTEM_EVENT,   // Các sự kiện hệ thống (scheduled tasks, system maintenance)
        SECURITY_EVENT,  // Các sự kiện bảo mật (failed login attempts, suspicious activities)
        ADMIN_ACTION  // Các hành động của admin (thêm, sửa, xóa user, thêm, sửa, xóa sản phẩm, etc.)
    }
}
