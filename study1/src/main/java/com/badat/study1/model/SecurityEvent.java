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
@Table(name = "security_events", indexes = {
    @Index(name = "idx_security_events_ip_address", columnList = "ip_address"),
    @Index(name = "idx_security_events_email", columnList = "email"),
    @Index(name = "idx_security_events_created_at", columnList = "created_at"),
    @Index(name = "idx_security_events_event_type", columnList = "event_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SecurityEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(length = 255)
    private String email;
    
    @Column(length = 100)
    private String username;
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    @Column(length = 500)
    private String userAgent;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum EventType {
        LOGIN_ATTEMPT,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        IP_LOCKED,
        CAPTCHA_REQUIRED,
        CAPTCHA_FAILED,
        FORGOT_PASSWORD_REQUEST,
        OTP_SENT,
        OTP_VERIFIED,
        OTP_FAILED,
        OTP_VERIFY_ATTEMPT,
        OTP_VERIFY_SUCCESS,
        OTP_VERIFY_FAILED,
        PASSWORD_RESET,
        INVALID_OTP_ATTEMPT,
        RATE_LIMITED,
        SECURITY_ALERT,
        REGISTER_ATTEMPT,
        REGISTER_SUCCESS,
        REGISTER_VERIFIED,
        EMAIL_ENUMERATION_ATTEMPT,
        TIMING_ATTACK_ATTEMPT
    }
}

