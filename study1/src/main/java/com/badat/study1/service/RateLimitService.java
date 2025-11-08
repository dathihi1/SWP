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
    
    @Value("${security.rate-limit.forgot-password-email-max-requests-per-hour:5}")
    private int forgotPasswordEmailMaxRequestsPerHour;
    
    @Value("${security.rate-limit.otp-max-attempts:5}")
    private int otpMaxAttempts;
    
    @Value("${security.rate-limit.register-max-requests-per-hour:5}")
    private int registerMaxRequestsPerHour;
    
    @Value("${security.rate-limit.forgot-password-ip-max-attempts:30}")
    private int forgotPasswordIpMaxAttempts;
    
    @Value("${security.rate-limit.ip-lockout-minutes:60}")
    private int ipLockoutMinutes;
    
    @Value("${security.rate-limit.verify-otp-email-max-requests-per-hour:10}")
    private int verifyOtpEmailMaxRequestsPerHour;
    
    @Value("${security.rate-limit.verify-otp-forgot-password-email-max-requests-per-hour:15}")
    private int verifyOtpForgotPasswordEmailMaxRequestsPerHour;
    
    @Value("${security.rate-limit.verify-otp-ip-max-requests-per-hour:20}")
    private int verifyOtpIpMaxRequestsPerHour;
    
    @Value("${security.rate-limit.verify-otp-forgot-password-ip-max-requests-per-hour:30}")
    private int verifyOtpForgotPasswordIpMaxRequestsPerHour;
    
    private static final String EMAIL_RATE_LIMIT_PREFIX = "rate_limit:email:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";
    private static final String REGISTER_RATE_LIMIT_PREFIX = "rate_limit:register:";
    private static final String FORGOT_PASSWORD_IP_PREFIX = "forgot_password_ip:";
    private static final String VERIFY_OTP_EMAIL_PREFIX = "rate_limit:verify_otp:email:";
    private static final String VERIFY_OTP_IP_PREFIX = "rate_limit:verify_otp:ip:";
    
    /**
     * Check email rate limiting with purpose-specific limits
     * @param email Email address
     * @param purpose Purpose: "register" or "forgot_password"
     * @return true if rate limited
     */
    public boolean isEmailRateLimited(String email, String purpose) {
        String key = EMAIL_RATE_LIMIT_PREFIX + purpose + ":" + email;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        int maxRequests = "forgot_password".equals(purpose) ? forgotPasswordEmailMaxRequestsPerHour : emailMaxRequestsPerHour;
        return attempts != null && attempts >= maxRequests;
    }
    
    /**
     * Check email rate limiting (backward compatibility - uses default limit)
     * @param email Email address
     * @return true if rate limited
     */
    public boolean isEmailRateLimited(String email) {
        // Default to register purpose for backward compatibility
        return isEmailRateLimited(email, "register");
    }
    
    public void recordEmailRequest(String email) {
        // Default to register purpose for backward compatibility
        recordEmailRequest(email, "register");
    }
    
    /**
     * Record email request with purpose-specific tracking
     * @param email Email address
     * @param purpose Purpose: "register" or "forgot_password"
     */
    public void recordEmailRequest(String email, String purpose) {
        String key = EMAIL_RATE_LIMIT_PREFIX + purpose + ":" + email;
        Long attempts = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
        
        int maxRequests = "forgot_password".equals(purpose) ? forgotPasswordEmailMaxRequestsPerHour : emailMaxRequestsPerHour;
        
        if (attempts != null && attempts == 1) {
            log.info("First {} request for email: {}", purpose, email);
        } else if (attempts != null) {
            log.warn("{} request #{} for email: {}", purpose, attempts, email);
            
            // Log email rate limit when approaching or reaching limit
            if (attempts >= maxRequests) {
                String details = "Email rate limited for " + purpose + " (attempts: " + attempts + "/" + maxRequests + ")";
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
        // Default to register purpose for backward compatibility
        return getEmailRequestCount(email, "register");
    }
    
    public Long getEmailRequestCount(String email, String purpose) {
        String key = EMAIL_RATE_LIMIT_PREFIX + purpose + ":" + email;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        return attempts != null ? attempts : 0L;
    }
    
    public Long getOtpAttemptCount(String email) {
        String key = OTP_ATTEMPTS_PREFIX + email;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        return attempts != null ? attempts : 0L;
    }
    
    public void clearEmailRateLimit(String email) {
        // Clear both register and forgot_password limits
        redisTemplate.delete(EMAIL_RATE_LIMIT_PREFIX + "register:" + email);
        redisTemplate.delete(EMAIL_RATE_LIMIT_PREFIX + "forgot_password:" + email);
        log.info("Email rate limit cleared for: {}", email);
    }
    
    public void clearEmailRateLimit(String email, String purpose) {
        String key = EMAIL_RATE_LIMIT_PREFIX + purpose + ":" + email;
        redisTemplate.delete(key);
        log.info("Email rate limit cleared for: {} (purpose: {})", email, purpose);
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
    
    // Methods cho verify OTP rate limiting
    /**
     * Check if email is rate limited for verify OTP
     * @param email Email address
     * @param purpose Purpose: "register" or "forgot_password"
     * @return true if rate limited
     */
    public boolean isVerifyOtpEmailRateLimited(String email, String purpose) {
        String key = VERIFY_OTP_EMAIL_PREFIX + purpose + ":" + email;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        int maxRequests = "forgot_password".equals(purpose) ? verifyOtpForgotPasswordEmailMaxRequestsPerHour : verifyOtpEmailMaxRequestsPerHour;
        return attempts != null && attempts >= maxRequests;
    }
    
    /**
     * Check if IP is rate limited for verify OTP
     * @param ipAddress IP address
     * @param purpose Purpose: "register" or "forgot_password"
     * @return true if rate limited
     */
    public boolean isVerifyOtpIpRateLimited(String ipAddress, String purpose) {
        String key = VERIFY_OTP_IP_PREFIX + purpose + ":" + ipAddress;
        Long attempts = (Long) redisTemplate.opsForValue().get(key);
        int maxRequests = "forgot_password".equals(purpose) ? verifyOtpForgotPasswordIpMaxRequestsPerHour : verifyOtpIpMaxRequestsPerHour;
        return attempts != null && attempts >= maxRequests;
    }
    
    /**
     * Record verify OTP request (both email and IP)
     * @param email Email address
     * @param ipAddress IP address
     * @param purpose Purpose: "register" or "forgot_password"
     */
    public void recordVerifyOtpRequest(String email, String ipAddress, String purpose) {
        // Record email rate limit
        String emailKey = VERIFY_OTP_EMAIL_PREFIX + purpose + ":" + email;
        Long emailAttempts = redisTemplate.opsForValue().increment(emailKey);
        redisTemplate.expire(emailKey, 1, TimeUnit.HOURS);
        
        // Record IP rate limit
        String ipKey = VERIFY_OTP_IP_PREFIX + purpose + ":" + ipAddress;
        Long ipAttempts = redisTemplate.opsForValue().increment(ipKey);
        redisTemplate.expire(ipKey, 1, TimeUnit.HOURS);
        
        int emailMaxRequests = "forgot_password".equals(purpose) ? verifyOtpForgotPasswordEmailMaxRequestsPerHour : verifyOtpEmailMaxRequestsPerHour;
        int ipMaxRequests = "forgot_password".equals(purpose) ? verifyOtpForgotPasswordIpMaxRequestsPerHour : verifyOtpIpMaxRequestsPerHour;
        
        log.info("Verify OTP request recorded - email: {}, IP: {}, purpose: {}, email attempts: {}/{}, IP attempts: {}/{}", 
                email, ipAddress, purpose, emailAttempts, emailMaxRequests, ipAttempts, ipMaxRequests);
        
        // Log security event if approaching or reaching limits
        if (emailAttempts != null && emailAttempts >= emailMaxRequests) {
            String details = "Email rate limited for verify OTP " + purpose + " (attempts: " + emailAttempts + "/" + emailMaxRequests + ")";
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.RATE_LIMITED,
                ipAddress,
                email,
                details
            );
        }
        
        if (ipAttempts != null && ipAttempts >= ipMaxRequests) {
            String details = "IP rate limited for verify OTP " + purpose + " (attempts: " + ipAttempts + "/" + ipMaxRequests + ")";
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.RATE_LIMITED,
                ipAddress,
                email,
                details
            );
        }
    }
    
    /**
     * Get remaining time for verify OTP rate limit (in minutes)
     * @param email Email address
     * @param ipAddress IP address
     * @param purpose Purpose: "register" or "forgot_password"
     * @return Remaining minutes, or 0 if not rate limited
     */
    public Long getVerifyOtpRateLimitRemainingMinutes(String email, String ipAddress, String purpose) {
        // Check email rate limit
        if (isVerifyOtpEmailRateLimited(email, purpose)) {
            String emailKey = VERIFY_OTP_EMAIL_PREFIX + purpose + ":" + email;
            Long remainingSeconds = redisTemplate.getExpire(emailKey, TimeUnit.SECONDS);
            return remainingSeconds != null && remainingSeconds > 0 ? remainingSeconds / 60 : 0L;
        }
        
        // Check IP rate limit
        if (isVerifyOtpIpRateLimited(ipAddress, purpose)) {
            String ipKey = VERIFY_OTP_IP_PREFIX + purpose + ":" + ipAddress;
            Long remainingSeconds = redisTemplate.getExpire(ipKey, TimeUnit.SECONDS);
            return remainingSeconds != null && remainingSeconds > 0 ? remainingSeconds / 60 : 0L;
        }
        
        return 0L;
    }
    
    /**
     * Clear verify OTP rate limit for email and IP
     * @param email Email address
     * @param ipAddress IP address
     * @param purpose Purpose: "register" or "forgot_password"
     */
    public void clearVerifyOtpRateLimit(String email, String ipAddress, String purpose) {
        String emailKey = VERIFY_OTP_EMAIL_PREFIX + purpose + ":" + email;
        String ipKey = VERIFY_OTP_IP_PREFIX + purpose + ":" + ipAddress;
        redisTemplate.delete(emailKey);
        redisTemplate.delete(ipKey);
        log.info("Verify OTP rate limit cleared for email: {}, IP: {}, purpose: {}", email, ipAddress, purpose);
    }
}

