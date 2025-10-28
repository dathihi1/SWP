package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "user_activity_log", indexes = {
    @Index(name = "idx_user_activity_user_id", columnList = "userId"),
    @Index(name = "idx_user_activity_action", columnList = "action"),
    @Index(name = "idx_user_activity_category", columnList = "category"),
    @Index(name = "idx_user_activity_created_at", columnList = "createdAt"),
    @Index(name = "idx_user_activity_user_created", columnList = "userId, createdAt")
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLog extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "user_id", nullable = false)
    Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;
    
    @Column(name = "action", nullable = false, length = 100)
    String action;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    Category category = Category.SHOPPING;
    
    @Column(name = "entity_type", length = 50)
    String entityType;
    
    @Column(name = "entity_id")
    Long entityId;
    
    @Column(name = "details", columnDefinition = "TEXT")
    String details;
    
    @Column(name = "ip_address", length = 45)
    String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    String userAgent;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    String metadata;
    
    @Column(name = "endpoint", length = 255)
    String endpoint;
    
    @Column(name = "method", length = 10)
    String method;
    
    @Column(name = "success", nullable = false)
    @Builder.Default
    Boolean success = true;
    
    @Column(name = "failure_reason", length = 500)
    String failureReason;
    
    // Computed properties for template display
    public String getActionDisplayName() {
        if (action == null) return "Unknown";
        switch (action) {
            case "VIEW_PRODUCT": return "Xem sản phẩm";
            case "ADD_TO_CART": return "Thêm vào giỏ";
            case "REMOVE_FROM_CART": return "Xóa khỏi giỏ";
            case "CREATE_ORDER": return "Tạo đơn hàng";
            case "CANCEL_ORDER": return "Hủy đơn hàng";
            case "PAYMENT_INITIATED": return "Bắt đầu thanh toán";
            case "PAYMENT_SUCCESS": return "Thanh toán thành công";
            case "PAYMENT_FAILED": return "Thanh toán thất bại";
            case "CREATE_REVIEW": return "Tạo đánh giá";
            case "UPDATE_REVIEW": return "Cập nhật đánh giá";
            case "LOGIN": return "Đăng nhập";
            case "LOGOUT": return "Đăng xuất";
            case "REGISTER": return "Đăng ký";
            case "UPDATE_PROFILE": return "Cập nhật hồ sơ";
            case "CHANGE_PASSWORD": return "Đổi mật khẩu";
            case "UPLOAD_AVATAR": return "Tải lên avatar";
            case "DELETE_AVATAR": return "Xóa avatar";
            default: return action.replace("_", " ").toLowerCase();
        }
    }
    
    public String getSuccessIcon() {
        if (success == null) return "fa-question";
        return success ? "fa-check" : "fa-times";
    }
    
    public enum Category {
        SHOPPING,   // View product, add to cart, etc.
        ORDER,      // Create order, cancel order, etc.
        PAYMENT,    // Payment initiated, success, failed
        REVIEW,     // Create review, update review, etc.
        ACCOUNT     // Profile update, password change, login, logout, register, etc.
    }
}




