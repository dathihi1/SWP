package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "api_call_log", indexes = {
    @Index(name = "idx_api_call_user_id", columnList = "userId"),
    @Index(name = "idx_api_call_endpoint", columnList = "endpoint"),
    @Index(name = "idx_api_call_status_code", columnList = "statusCode"),
    @Index(name = "idx_api_call_created_at", columnList = "createdAt"),
    @Index(name = "idx_api_call_user_created", columnList = "userId, createdAt"),
    @Index(name = "idx_api_call_endpoint_created", columnList = "endpoint, createdAt"),
    @Index(name = "idx_api_call_status_created", columnList = "statusCode, createdAt")
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiCallLog extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "user_id")
    Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;
    
    @Column(name = "endpoint", nullable = false, length = 255)
    String endpoint;
    
    @Column(name = "method", nullable = false, length = 10)
    String method;
    
    @Column(name = "status_code", nullable = false)
    Integer statusCode;
    
    @Column(name = "request_params", columnDefinition = "TEXT")
    String requestParams;
    
    @Column(name = "request_body", columnDefinition = "TEXT")
    String requestBody;
    
    @Column(name = "response_status", length = 20)
    String responseStatus;
    
    @Column(name = "duration_ms")
    Integer durationMs;
    
    @Column(name = "ip_address", length = 45)
    String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    String userAgent;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    String errorMessage;
}





