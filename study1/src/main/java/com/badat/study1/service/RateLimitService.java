package com.badat.study1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.badat.study1.model.SecurityEvent;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityEventService securityEventService;
    private final AuditLogService auditLogService;
    
    @Value("${security.rate-limit.email-max-requests-per-hour:3}")
    private int emailMaxRequestsPerHour;
    
    @Value("${security.rate-limit.otp-max-attempts:5}")
    private int otpMaxAttempts;
    
    @Value("${security.rate-limit.register-max-requests-per-hour:5}")
    private int registerMaxRequestsPerHour;
    
    @Value("${security.rate-limit.forgot-password-ip-max-attempts:30}")
    private int forgotPasswordIpMaxAttempts;
    
    @Value("${security.rate-limit.ip-lockout-minutes:60}")
    private int ipLockoutMinutes;
    
    private static final String EMAIL_RATE_LIMIT_PREFIX = "rate_limit:forgot_password:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";
    private static final String REGISTER_RATE_LIMIT_PREFIX = "rate_limit:register:";
    private static final String FORGOT_PASSWORD_IP_PREFIX = "forgot_password_ip:";
    
    public boolean isEmailRateLimited(String email) {
        String key = EMAIL_RATE_LIMIT_PREFIX + email;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        return attempts != null && attempts >= emailMaxRequestsPerHour;
    }
    
    public void recordEmailRequest(String email) {
        String key = EMAIL_RATE_LIMIT_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
        
        if (attempts != null && attempts == 1) {
            log.info("First forgot password request for email: {}", email);
        } else if (attempts != null) {
            log.warn("Forgot password request #{} for email: {}", attempts, email);
            
            // Log email rate limit when approaching or reaching limit
            if (attempts >= emailMaxRequestsPerHour) {
                String details = "Email rate limited for forgot password (attempts: " + attempts + "/" + emailMaxRequestsPerHour + ")";
                securityEventService.logSecurityEvent(
                    SecurityEvent.EventType.EMAIL_RATE_LIMIT,
                    null,
                    email,
                    details
                );
                try {
                    auditLogService.logAction(
                        null,
                        "ACCOUNT_BLOCKED_EMAIL_RATE_LIMIT",
                        details,
                        null,
                        false,
                        "RATE_LIMIT_REACHED",
                        "System",
                        "/api/otp/send",
                        "POST",
                        com.badat.study1.model.AuditLog.Category.SECURITY_EVENT
                    );
                } catch (Exception ignore) {
                }
            }
        }
    }
    
    public boolean isOtpRateLimited(String email) {
        String key = OTP_ATTEMPTS_PREFIX + email;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        return attempts != null && attempts >= otpMaxAttempts;
    }
    
    public void recordOtpAttempt(String email, boolean success) {
        String key = OTP_ATTEMPTS_PREFIX + email;
        
        if (success) {
            // Clear attempts on successful verification
            redisTemplate.delete(key);
            log.info("OTP verification successful for email: {}, attempts cleared", email);
        } else {
            // Increment failed attempts
            Long attempts = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
            
            log.warn("Failed OTP attempt #{} for email: {}", attempts, email);
        }
    }
    
    public Long getEmailRequestCount(String email) {
        String key = EMAIL_RATE_LIMIT_PREFIX + email;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        return attempts != null ? attempts : 0L;
    }
    
    public Long getOtpAttemptCount(String email) {
        String key = OTP_ATTEMPTS_PREFIX + email;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        return attempts != null ? attempts : 0L;
    }
    
    public void clearEmailRateLimit(String email) {
        String key = EMAIL_RATE_LIMIT_PREFIX + email;
        redisTemplate.delete(key);
        log.info("Email rate limit cleared for: {}", email);
    }
    
    public void clearOtpRateLimit(String email) {
        String key = OTP_ATTEMPTS_PREFIX + email;
        redisTemplate.delete(key);
        log.info("OTP rate limit cleared for: {}", email);
    }
    
    public boolean isIpRateLimited(String ipAddress, String type) {
        String key = REGISTER_RATE_LIMIT_PREFIX + ipAddress;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        return attempts != null && attempts >= registerMaxRequestsPerHour;
    }
    
    public void recordIpRequest(String ipAddress, String type) {
        String key = REGISTER_RATE_LIMIT_PREFIX + ipAddress;
        Long attempts = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
        
        if (attempts != null && attempts == 1) {
            log.info("First {} request from IP: {}", type, ipAddress);
        } else if (attempts != null) {
            log.warn("{} request #{} from IP: {}", type, attempts, ipAddress);
        }
    }
    
    public void clearIpRateLimit(String ipAddress) {
        String key = REGISTER_RATE_LIMIT_PREFIX + ipAddress;
        redisTemplate.delete(key);
        log.info("IP rate limit cleared for: {}", ipAddress);
    }
    
    // Methods riêng cho forgot password IP rate limiting
    public boolean isIpRateLimitedForForgotPassword(String ipAddress) {
        String key = FORGOT_PASSWORD_IP_PREFIX + ipAddress;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        return attempts != null && attempts >= forgotPasswordIpMaxAttempts;
    }

    public void recordForgotPasswordIpRequest(String ipAddress) {
        String key = FORGOT_PASSWORD_IP_PREFIX + ipAddress;
        Long attempts = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, ipLockoutMinutes, TimeUnit.MINUTES);
        log.info("Forgot password IP request recorded for: {}, attempt: {}", ipAddress, attempts);
    }
}

