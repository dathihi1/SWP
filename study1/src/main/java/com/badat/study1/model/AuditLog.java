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
}
