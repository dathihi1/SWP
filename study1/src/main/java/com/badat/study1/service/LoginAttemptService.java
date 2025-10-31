package com.badat.study1.service;

import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 1; // Reduced to 1 minute for testing
    private static final String LOGIN_ATTEMPTS_KEY_PREFIX = "login_attempts:";
    private static final String ACCOUNT_LOCKED_KEY_PREFIX = "account_locked:";

    public boolean isAccountLocked(String username) {
        String lockKey = ACCOUNT_LOCKED_KEY_PREFIX + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    public void recordFailedLoginAttempt(String username, String ipAddress) {
        // If account is already locked, log a blocked attempt audit entry
        if (isAccountLocked(username)) {
            try {
                auditLogService.logAction(
                        null,
                        "ACCOUNT_BLOCKED_LOGIN_LOCK_ACTIVE",
                        "Blocked login attempt while account locked for username=" + username,
                        ipAddress,
                        false,
                        "ACCOUNT_LOCK_ACTIVE",
                        "System",
                        "/api/auth/login",
                        "POST",
                        com.badat.study1.model.AuditLog.Category.SECURITY_EVENT
                );
            } catch (Exception ignore) {}
        }
        String attemptsKey = LOGIN_ATTEMPTS_KEY_PREFIX + username;
        
        // Increment failed attempts
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
        
        log.info("Failed login attempt recorded for user: {}, attempt: {}", username, attempts);
        
        // Log failed login attempt to audit log
        try {
            auditLogService.logFailedLoginAttempt(username, ipAddress, "Mật khẩu không đúng", 
                "POST /api/auth/login", "POST");
        } catch (Exception e) {
            log.error("Error logging failed login attempt: {}", e.getMessage());
        }
        
        // If max attempts reached, lock the account
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            lockAccount(username, ipAddress);
        }
    }

    public void recordSuccessfulLogin(String username, String ipAddress) {
        // Clear failed attempts on successful login
        String attemptsKey = LOGIN_ATTEMPTS_KEY_PREFIX + username;
        redisTemplate.delete(attemptsKey);
        
        // Unlock account if it was locked
        String lockKey = ACCOUNT_LOCKED_KEY_PREFIX + username;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            redisTemplate.delete(lockKey);
            log.info("Account unlocked after successful login: {}", username);
        }
        
        log.info("Successful login recorded for user: {}", username);
    }

    private void lockAccount(String username, String ipAddress) {
        try {
            // Find user and update status
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setStatus(User.Status.LOCKED);
                userRepository.save(user);
                
                // Set lock in Redis
                String lockKey = ACCOUNT_LOCKED_KEY_PREFIX + username;
                redisTemplate.opsForValue().set(lockKey, LocalDateTime.now().toString());
                redisTemplate.expire(lockKey, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
                
                // Log the event
                auditLogService.logAccountLocked(user, ipAddress, 
                    "Quá nhiều lần đăng nhập sai (" + MAX_LOGIN_ATTEMPTS + " lần)", "/api/auth/login", "POST");
                
                log.warn("Account locked due to too many failed login attempts: {}", username);
            }
        } catch (Exception e) {
            log.error("Failed to lock account for user: {}, error: {}", username, e.getMessage());
        }
    }

    public void unlockAccount(String username) {
        try {
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setStatus(User.Status.ACTIVE);
                userRepository.save(user);
                
                // Remove lock from Redis
                String lockKey = ACCOUNT_LOCKED_KEY_PREFIX + username;
                redisTemplate.delete(lockKey);
                
                // Clear failed attempts
                String attemptsKey = LOGIN_ATTEMPTS_KEY_PREFIX + username;
                redisTemplate.delete(attemptsKey);
                
                log.info("Account unlocked manually: {}", username);
            }
        } catch (Exception e) {
            log.error("Failed to unlock account for user: {}, error: {}", username, e.getMessage());
        }
    }

    public long getRemainingLockoutTime(String username) {
        String lockKey = ACCOUNT_LOCKED_KEY_PREFIX + username;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    public int getFailedAttempts(String username) {
        String attemptsKey = LOGIN_ATTEMPTS_KEY_PREFIX + username;
        Object attempts = redisTemplate.opsForValue().get(attemptsKey);
        return attempts != null ? (Integer) attempts : 0;
    }
}
