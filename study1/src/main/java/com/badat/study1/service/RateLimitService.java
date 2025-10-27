package com.badat.study1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${security.rate-limit.email-max-requests-per-hour:3}")
    private int emailMaxRequestsPerHour;
    
    @Value("${security.rate-limit.otp-max-attempts:5}")
    private int otpMaxAttempts;
    
    @Value("${security.rate-limit.register-max-requests-per-hour:5}")
    private int registerMaxRequestsPerHour;
    
    private static final String EMAIL_RATE_LIMIT_PREFIX = "rate_limit:forgot_password:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";
    private static final String REGISTER_RATE_LIMIT_PREFIX = "rate_limit:register:";
    
    public boolean isEmailRateLimited(String email) {
        String key = EMAIL_RATE_LIMIT_PREFIX + email;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        return attempts != null && attempts >= emailMaxRequestsPerHour;
    }
    
    public void recordEmailRequest(String email) {
        String key = EMAIL_RATE_LIMIT_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
        
        if (attempts == 1) {
            log.info("First forgot password request for email: {}", email);
        } else {
            log.warn("Forgot password request #{} for email: {}", attempts, email);
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
        
        if (attempts == 1) {
            log.info("First {} request from IP: {}", type, ipAddress);
        } else {
            log.warn("{} request #{} from IP: {}", type, attempts, ipAddress);
        }
    }
    
    public void clearIpRateLimit(String ipAddress) {
        String key = REGISTER_RATE_LIMIT_PREFIX + ipAddress;
        redisTemplate.delete(key);
        log.info("IP rate limit cleared for: {}", ipAddress);
    }
}

