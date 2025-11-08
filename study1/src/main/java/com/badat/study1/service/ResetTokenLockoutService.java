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
public class ResetTokenLockoutService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityEventService securityEventService;

    @Value("${security.rate-limit.reset-token-max-attempts:5}")
    private int maxResetTokenAttempts;

    @Value("${security.rate-limit.reset-token-lockout-minutes:60}")
    private int resetTokenLockoutMinutes;

    private static final String RESET_TOKEN_ATTEMPTS_PREFIX = "reset_token_attempts:";
    private static final String RESET_TOKEN_LOCKED_PREFIX = "reset_token_locked:";

    // Key format: reset_token_attempts:{email}:{ipAddress}
    private String getAttemptsKey(String email, String ipAddress) {
        return RESET_TOKEN_ATTEMPTS_PREFIX + email + ":" + ipAddress;
    }

    // Key format: reset_token_locked:{email}:{ipAddress}
    private String getLockKey(String email, String ipAddress) {
        return RESET_TOKEN_LOCKED_PREFIX + email + ":" + ipAddress;
    }

    public boolean isLocked(String email, String ipAddress) {
        String lockKey = getLockKey(email, ipAddress);
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    public void recordFailedAttempt(String email, String ipAddress) {
        String attemptsKey = getAttemptsKey(email, ipAddress);
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, resetTokenLockoutMinutes, TimeUnit.MINUTES);

        log.warn("Failed reset token attempt for email: {} from IP: {}, attempt: {}",
                email, ipAddress, attempts);

        if (attempts != null && attempts >= maxResetTokenAttempts) {
            lock(email, ipAddress, "Excessive failed reset token attempts");
        }
    }

    private void lock(String email, String ipAddress, String reason) {
        String lockKey = getLockKey(email, ipAddress);
        redisTemplate.opsForValue().set(lockKey, "locked", resetTokenLockoutMinutes, TimeUnit.MINUTES);
        log.error("RESET TOKEN LOCKED: Email: {}, IP: {} for {} minutes. Reason: {}",
                email, ipAddress, resetTokenLockoutMinutes, reason);

        // Log reset token lockout to security events
        securityEventService.logSecurityEvent(
            SecurityEvent.EventType.RESET_TOKEN_LOCKED,
            ipAddress,
            email,
            "Reset token locked - " + reason + " (locked for " + resetTokenLockoutMinutes + " minutes)"
        );
    }

    public void clearAttempts(String email, String ipAddress) {
        String attemptsKey = getAttemptsKey(email, ipAddress);
        String lockKey = getLockKey(email, ipAddress);
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(lockKey);
        log.info("Reset token attempts and lock cleared for email: {} from IP: {}", email, ipAddress);
    }

    public Long getLockoutTimeRemaining(String email, String ipAddress) {
        String lockKey = getLockKey(email, ipAddress);
        return redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
    }

    public Long getRemainingAttempts(String email, String ipAddress) {
        String attemptsKey = getAttemptsKey(email, ipAddress);
        Long attempts = (Long) redisTemplate.opsForValue().get(attemptsKey);
        return Math.max(0, maxResetTokenAttempts - (attempts != null ? attempts : 0L));
    }
}
