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
public class CaptchaRateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityEventService securityEventService;
    
    @Value("${security.captcha.max-attempts-per-ip:5}")
    private int maxCaptchaAttempts;
    
    @Value("${security.captcha.lockout-minutes:15}")
    private int captchaLockoutMinutes;
    
    private static final String CAPTCHA_ATTEMPTS_PREFIX = "captcha_attempts:";
    private static final String CAPTCHA_LOCKED_PREFIX = "captcha_locked:";
    
    public boolean isCaptchaRateLimited(String ipAddress) {
        String lockKey = CAPTCHA_LOCKED_PREFIX + ipAddress;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
    
    public void recordFailedCaptchaAttempt(String ipAddress) {
        String attemptsKey = CAPTCHA_ATTEMPTS_PREFIX + ipAddress;
        
        // Increment failed captcha attempts
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, captchaLockoutMinutes, TimeUnit.MINUTES);
        
        log.warn("Failed captcha attempt from IP: {}, attempt: {}", ipAddress, attempts);
        
        // Lock IP if max attempts reached
        if (attempts >= maxCaptchaAttempts) {
            lockIpForCaptchaFailures(ipAddress, attempts);
        }
    }
    
    public void clearCaptchaAttempts(String ipAddress) {
        // Clear failed attempts on successful login
        String attemptsKey = CAPTCHA_ATTEMPTS_PREFIX + ipAddress;
        redisTemplate.delete(attemptsKey);
        
        // Unlock IP if it was locked for captcha failures
        String lockKey = CAPTCHA_LOCKED_PREFIX + ipAddress;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            redisTemplate.delete(lockKey);
            log.info("Captcha rate limit cleared for IP: {}", ipAddress);
        }
    }
    
    private void lockIpForCaptchaFailures(String ipAddress, Long attempts) {
        try {
            // Lock in Redis
            String lockKey = CAPTCHA_LOCKED_PREFIX + ipAddress;
            redisTemplate.opsForValue().set(lockKey, "locked", captchaLockoutMinutes, TimeUnit.MINUTES);
            
            // Log security event
            securityEventService.logSecurityEvent(
                com.badat.study1.model.SecurityEvent.EventType.CAPTCHA_FAILED, 
                ipAddress, 
                "IP locked due to excessive captcha failures: " + attempts + " attempts"
            );
            
            log.error("IP LOCKED for captcha failures: {} for {} minutes. Attempts: {}", 
                    ipAddress, captchaLockoutMinutes, attempts);
            
        } catch (Exception e) {
            log.error("Failed to lock IP for captcha failures {}: {}", ipAddress, e.getMessage());
        }
    }
    
    public Long getCaptchaAttemptCount(String ipAddress) {
        String attemptsKey = CAPTCHA_ATTEMPTS_PREFIX + ipAddress;
        Long attempts = (Long) redisTemplate.opsForValue().get(attemptsKey);
        return attempts != null ? attempts : 0L;
    }
    
    public Long getRemainingLockoutTime(String ipAddress) {
        String lockKey = CAPTCHA_LOCKED_PREFIX + ipAddress;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0L;
    }
}
























