package com.badat.study1.service;

import com.badat.study1.model.IpLockout;
import com.badat.study1.repository.IpLockoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpLockoutService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final IpLockoutRepository ipLockoutRepository;
    private final SecurityEventService securityEventService;
    
    @Value("${security.rate-limit.ip-max-attempts:30}")
    private int maxAttemptsPerIp;
    
    @Value("${security.rate-limit.ip-lockout-minutes:30}")
    private int ipLockoutMinutes;
    
    @Value("${security.rate-limit.captcha-required-attempts:3}")
    private int captchaRequiredAttempts;
    
    private static final String IP_ATTEMPTS_PREFIX = "ip_attempts:";
    private static final String IP_LOCKED_PREFIX = "ip_locked:";
    private static final String CAPTCHA_LOCKED_PREFIX = "captcha_locked:";
    private static final String CAPTCHA_ATTEMPTS_PREFIX = "captcha_attempts:";
    private static final String FORGOT_PASSWORD_IP_PREFIX = "forgot_password_ip:";
    
    public boolean isIpLocked(String ipAddress) {
        // Check Redis first for quick response
        String lockKey = IP_LOCKED_PREFIX + ipAddress;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            return true;
        }
        
        // Check database for active lockouts
        Optional<IpLockout> activeLockout = ipLockoutRepository.findActiveLockoutByIpAddress(ipAddress, LocalDateTime.now());
        if (activeLockout.isPresent()) {
            // Update Redis cache
            redisTemplate.opsForValue().set(lockKey, "locked", ipLockoutMinutes, TimeUnit.MINUTES);
            return true;
        }
        
        return false;
    }
    
    public boolean requiresCaptcha(String ipAddress) {
        String attemptsKey = IP_ATTEMPTS_PREFIX + ipAddress;
        Long attempts = (Long) redisTemplate.opsForValue().get(attemptsKey);
        return attempts != null && attempts >= captchaRequiredAttempts;
    }
    
    public void recordFailedAttempt(String ipAddress, String username) {
        String attemptsKey = IP_ATTEMPTS_PREFIX + ipAddress;
        
        // Increment failed attempts for this IP
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, ipLockoutMinutes, TimeUnit.MINUTES);
        
        log.warn("Failed login attempt from IP: {}, attempt: {}, username: {}", 
                ipAddress, attempts, username);
        
        // Lock IP if max attempts reached
        if (attempts >= maxAttemptsPerIp) {
            lockIp(ipAddress, "Excessive failed login attempts: " + attempts);
        }
    }
    
    public void recordSuccessfulAttempt(String ipAddress) {
        // Clear failed attempts on successful login
        String attemptsKey = IP_ATTEMPTS_PREFIX + ipAddress;
        redisTemplate.delete(attemptsKey);
        
        // Clear any active lockouts for this IP
        unlockIp(ipAddress);
        
        log.info("Successful login from IP: {}, attempts cleared", ipAddress);
    }
    
    public void lockIp(String ipAddress, String reason) {
        try {
            // Lock in Redis for immediate effect
            String lockKey = IP_LOCKED_PREFIX + ipAddress;
            redisTemplate.opsForValue().set(lockKey, "locked", ipLockoutMinutes, TimeUnit.MINUTES);
            
            // Save to database for audit trail
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(ipLockoutMinutes);
            IpLockout ipLockout = IpLockout.builder()
                    .ipAddress(ipAddress)
                    .reason(reason)
                    .attemptCount(maxAttemptsPerIp)
                    .isActive(true)
                    .lockedUntil(lockedUntil)
                    .build();
            
            ipLockoutRepository.save(ipLockout);
            
            // Log security event
            securityEventService.logIpLockout(ipAddress, reason, maxAttemptsPerIp);
            
            log.error("IP LOCKED: {} for {} minutes. Reason: {}", ipAddress, ipLockoutMinutes, reason);
            
        } catch (Exception e) {
            log.error("Failed to lock IP {}: {}", ipAddress, e.getMessage());
        }
    }
    
    private void unlockIp(String ipAddress) {
        try {
            // Remove from Redis
            String lockKey = IP_LOCKED_PREFIX + ipAddress;
            redisTemplate.delete(lockKey);
            
            // Deactivate in database
            ipLockoutRepository.deactivateLockoutsByIpAddress(ipAddress, LocalDateTime.now());
            
            log.info("IP unlocked: {}", ipAddress);
            
        } catch (Exception e) {
            log.error("Failed to unlock IP {}: {}", ipAddress, e.getMessage());
        }
    }

    /**
     * Admin unlocks an IP: clear all related Redis keys (ip, captcha, attempts) and deactivate DB lockouts.
     */
    public void adminUnlockIp(String ipAddress) {
        try {
            // Core ip lock
            redisTemplate.delete(IP_LOCKED_PREFIX + ipAddress);
            redisTemplate.delete(IP_ATTEMPTS_PREFIX + ipAddress);
            // Captcha lock & attempts
            redisTemplate.delete(CAPTCHA_LOCKED_PREFIX + ipAddress);
            redisTemplate.delete(CAPTCHA_ATTEMPTS_PREFIX + ipAddress);
            // Forgot password IP rate limit
            redisTemplate.delete(FORGOT_PASSWORD_IP_PREFIX + ipAddress);
            // Deactivate DB lockouts
            ipLockoutRepository.deactivateLockoutsByIpAddress(ipAddress, LocalDateTime.now());
            log.info("Admin unlocked IP and cleared rate-limit keys: {}", ipAddress);
        } catch (Exception e) {
            log.error("Failed adminUnlockIp for {}: {}", ipAddress, e.getMessage());
        }
    }

    /**
     * Persist a lockout record for captcha failures so Admin can see active lockouts.
     */
    public void createCaptchaLockoutRecord(String ipAddress, String reason) {
        try {
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(ipLockoutMinutes);
            IpLockout ipLockout = IpLockout.builder()
                    .ipAddress(ipAddress)
                    .reason(reason)
                    .attemptCount(maxAttemptsPerIp)
                    .isActive(true)
                    .lockedUntil(lockedUntil)
                    .build();
            ipLockoutRepository.save(ipLockout);
            log.info("Created captcha lockout record for IP: {}", ipAddress);
        } catch (Exception e) {
            log.error("Failed to create captcha lockout record for {}: {}", ipAddress, e.getMessage());
        }
    }
    
    public Long getAttemptCount(String ipAddress) {
        String attemptsKey = IP_ATTEMPTS_PREFIX + ipAddress;
        Long attempts = (Long) redisTemplate.opsForValue().get(attemptsKey);
        return attempts != null ? attempts : 0L;
    }
    
    public void clearAttempts(String ipAddress) {
        String attemptsKey = IP_ATTEMPTS_PREFIX + ipAddress;
        redisTemplate.delete(attemptsKey);
        log.info("Attempts cleared for IP: {}", ipAddress);
    }
    
    public void cleanupExpiredLockouts() {
        try {
            // Find expired lockouts in database
            var expiredLockouts = ipLockoutRepository.findExpiredLockouts(LocalDateTime.now());
            
            for (IpLockout lockout : expiredLockouts) {
                unlockIp(lockout.getIpAddress());
            }
            
            if (!expiredLockouts.isEmpty()) {
                log.info("Cleaned up {} expired IP lockouts", expiredLockouts.size());
            }
            
        } catch (Exception e) {
            log.error("Failed to cleanup expired lockouts: {}", e.getMessage());
        }
    }
}

