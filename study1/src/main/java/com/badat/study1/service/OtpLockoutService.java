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
public class OtpLockoutService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityEventService securityEventService;

    @Value("${security.rate-limit.otp-max-attempts:5}")
    private int maxOtpAttempts;

    @Value("${security.rate-limit.forgot-password-otp-max-attempts:10}")
    private int forgotPasswordOtpMaxAttempts;

    @Value("${security.rate-limit.otp-lockout-minutes:60}") // 1 hour lockout
    private int otpLockoutMinutes;

    private static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";
    private static final String OTP_LOCKED_PREFIX = "otp_locked:";

    // Key format: otp_attempts:{purpose}:{email}:{ipAddress}
    private String getAttemptsKey(String email, String ipAddress, String purpose) {
        return OTP_ATTEMPTS_PREFIX + purpose + ":" + email + ":" + ipAddress;
    }

    // Key format: otp_locked:{purpose}:{email}:{ipAddress}
    private String getLockKey(String email, String ipAddress, String purpose) {
        return OTP_LOCKED_PREFIX + purpose + ":" + email + ":" + ipAddress;
    }

    private int getMaxAttemptsForPurpose(String purpose) {
        return "forgot_password".equals(purpose) ? forgotPasswordOtpMaxAttempts : maxOtpAttempts;
    }

    public boolean isLocked(String email, String ipAddress, String purpose) {
        String lockKey = getLockKey(email, ipAddress, purpose);
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    public void recordFailedAttempt(String email, String ipAddress, String purpose) {
        String attemptsKey = getAttemptsKey(email, ipAddress, purpose);
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, otpLockoutMinutes, TimeUnit.MINUTES); // Attempts expire after lockout duration

        log.warn("Failed OTP attempt for email: {} from IP: {}, purpose: {}, attempt: {}",
                email, ipAddress, purpose, attempts);

        int maxAttempts = getMaxAttemptsForPurpose(purpose);
        if (attempts != null && attempts >= maxAttempts) {
            lock(email, ipAddress, purpose, "Excessive failed OTP attempts");
        }
    }

    private void lock(String email, String ipAddress, String purpose, String reason) {
        String lockKey = getLockKey(email, ipAddress, purpose);
        redisTemplate.opsForValue().set(lockKey, "locked", otpLockoutMinutes, TimeUnit.MINUTES);
        log.error("OTP LOCKED: Email: {}, IP: {}, Purpose: {} for {} minutes. Reason: {}",
                email, ipAddress, purpose, otpLockoutMinutes, reason);
        
        // Log OTP lockout to security events
        securityEventService.logSecurityEvent(
            SecurityEvent.EventType.OTP_LOCKED,
            ipAddress,
            email,
            "OTP locked for " + purpose + " - " + reason + " (locked for " + otpLockoutMinutes + " minutes)"
        );
    }

    public void clearAttempts(String email, String ipAddress, String purpose) {
        String attemptsKey = getAttemptsKey(email, ipAddress, purpose);
        String lockKey = getLockKey(email, ipAddress, purpose);
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(lockKey); // Clear lock if any
        log.info("OTP attempts and lock cleared for email: {} from IP: {}, purpose: {}", email, ipAddress, purpose);
    }

    public Long getRemainingAttempts(String email, String ipAddress, String purpose) {
        String attemptsKey = getAttemptsKey(email, ipAddress, purpose);
        Long attempts = (Long) redisTemplate.opsForValue().get(attemptsKey);
        int maxAttempts = getMaxAttemptsForPurpose(purpose);
        return Math.max(0, maxAttempts - (attempts != null ? attempts : 0L));
    }

    public Long getLockoutTimeRemaining(String email, String ipAddress, String purpose) {
        String lockKey = getLockKey(email, ipAddress, purpose);
        return redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
    }
}