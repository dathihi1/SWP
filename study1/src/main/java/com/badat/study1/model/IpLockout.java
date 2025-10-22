package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ip_lockouts", indexes = {
    @Index(name = "idx_ip_lockouts_ip_address", columnList = "ip_address"),
    @Index(name = "idx_ip_lockouts_created_at", columnList = "created_at"),
    @Index(name = "idx_ip_lockouts_is_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class IpLockout {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ip_address", length = 45, nullable = false)
    private String ipAddress;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Column(name = "attempt_count")
    private Integer attemptCount;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;
}

