package com.badat.study1.service;

import com.badat.study1.dto.OtpData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final OtpLockoutService otpLockoutService;
    private final AuditLogService auditLogService;
    
    @Value("${security.rate-limit.otp-expire-minutes:10}")
    private int otpExpireMinutes;
    
    @Value("${security.rate-limit.otp-max-attempts:5}")
    private int maxOtpAttempts;
    
    private static final String OTP_PREFIX = "otp:";
    private static final String RESET_TOKEN_PREFIX = "reset_token:";
    private static final String OTP_VERIFIED_PREFIX = "otp_verified:";
    private static final String OTP_EMAIL_SENT_PREFIX = "otp_email_sent:";
    
    public void sendOtp(String email, String purpose) {
        // Check if email was already sent recently (10 minutes rate limit)
        String emailSentKey = OTP_EMAIL_SENT_PREFIX + purpose + ":" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(emailSentKey))) {
            // Get TTL to show remaining time
            Long ttl = redisTemplate.getExpire(emailSentKey, TimeUnit.SECONDS);
            long remainingMinutes = ttl != null && ttl > 0 ? (ttl / 60) + 1 : 10;
            log.warn("Email OTP already sent recently for {} (purpose: {}). Remaining time: {} minutes", 
                    email, purpose, remainingMinutes);
            throw new RuntimeException("Email đã được gửi. Vui lòng đợi " + remainingMinutes + " phút trước khi yêu cầu lại.");
        }
        
        // Check rate limiting for all purposes (including forgot_password)
        // Use separate limit for forgot_password to allow legitimate users to recover their password
        if (rateLimitService.isEmailRateLimited(email, purpose)) {
            try {
                auditLogService.logAction(
                        null,
                        "EMAIL_BLOCKED_RATE_LIMIT_ACTIVE",
                        "Blocked email send due to active rate limit for " + email + " (purpose: " + purpose + ")",
                        null,
                        false,
                        "RATE_LIMIT_ACTIVE",
                        "System",
                        "/api/otp/send",
                        "POST",
                        com.badat.study1.model.AuditLog.Category.SECURITY_EVENT
                );
            } catch (Exception ignore) {}
            throw new RuntimeException("Quá nhiều yêu cầu. Vui lòng thử lại sau 1 giờ.");
        }
        
        // Mark email as sent with 10 minutes TTL (rate limit)
        redisTemplate.opsForValue().set(emailSentKey, "sent", 10, TimeUnit.MINUTES);
        log.info("Email sent marker stored in Redis - key: {}, expire: 10 minutes", emailSentKey);
        
        // Generate OTP
        String otp = generateOtp();
        String otpKey = OTP_PREFIX + purpose + ":" + email;
        log.info("Generated OTP for {}: {}", email, otp);
        
        // Store OTP in Redis
        OtpData otpData = OtpData.builder()
                .otp(otp)
                .email(email)
                .purpose(purpose)
                .createdAt(LocalDateTime.now())
                .attempts(0)
                .build();
        
        redisTemplate.opsForValue().set(otpKey, otpData, otpExpireMinutes, TimeUnit.MINUTES);
        log.info("OTP stored in Redis - key: {}, expire: {} minutes", otpKey, otpExpireMinutes);
        
        // Send HTML email based on purpose
        try {
            String htmlContent;
            String subject;
            
            if ("register".equals(purpose)) {
                htmlContent = emailTemplateService.generateRegistrationOtpEmail(otp, otpExpireMinutes, email);
                subject = "Mã OTP Đăng Ký - MMO Market";
            } else if ("forgot_password".equals(purpose)) {
                htmlContent = emailTemplateService.generateForgotPasswordOtpEmail(otp, otpExpireMinutes, email);
                subject = "Mã OTP Khôi Phục Mật Khẩu - MMO Market";
            } else {
                // Fallback to plain text for other purposes
                htmlContent = String.format("""
                    <html><body>
                    <h2>Mã OTP xác thực</h2>
                    <p>Mã OTP của bạn là: <strong>%s</strong></p>
                    <p>Mã này có hiệu lực trong %d phút.</p>
                    <p>Không chia sẻ mã này với bất kỳ ai.</p>
                    </body></html>
                    """, otp, otpExpireMinutes);
                subject = "Mã OTP xác thực - MMO Market";
            }
            
            emailService.sendHtmlEmail(email, subject, htmlContent);
            log.info("HTML OTP email sent successfully to: {} for purpose: {}", email, purpose);
            
        } catch (Exception e) {
            log.error("Failed to send HTML OTP email, falling back to plain text: {}", e.getMessage());
            // Fallback to plain text email
            String subject = "Mã OTP xác thực";
            String body = String.format("""
                Mã OTP của bạn là: %s
                Mã này có hiệu lực trong %d phút.
                Không chia sẻ mã này với bất kỳ ai.
                """, otp, otpExpireMinutes);
            
            emailService.sendEmail(email, subject, body);
        }
        
        // Record request with purpose
        rateLimitService.recordEmailRequest(email, purpose);
        
        log.info("OTP sent to email: {} for purpose: {}", email, purpose);
    }
    
    public boolean verifyOtp(String email, String otp, String purpose, String ipAddress) {
        // Check lockout TRƯỚC
        if (otpLockoutService.isLocked(email, ipAddress, purpose)) {
            Long remainingSeconds = otpLockoutService.getLockoutTimeRemaining(email, ipAddress, purpose);
            long minutes = remainingSeconds / 60;
            int maxAttempts = "forgot_password".equals(purpose) ? 10 : 5;
            throw new RuntimeException("Bạn đã nhập sai OTP quá " + maxAttempts + " lần. Vui lòng thử lại sau " + minutes + " phút");
        }
        
        String otpKey = OTP_PREFIX + purpose + ":" + email;
        
        log.info("Verifying OTP - email: {}, purpose: {}, otp: {}, key: {}", email, purpose, otp, otpKey);
        
        Object rawData = redisTemplate.opsForValue().get(otpKey);
        
        if (rawData == null) {
            log.warn("OTP not found or expired for email: {}, purpose: {}, key: {}", email, purpose, otpKey);
            return false;
        }
        
        OtpData otpData = convertToOtpData(rawData);
        if (otpData == null) {
            log.error("Failed to convert OTP data for email: {}", email);
            return false;
        }
        
        log.info("OTP data found - email: {}, storedOtp: {}, inputOtp: {}, attempts: {}", 
            email, otpData.getOtp(), otp, otpData.getAttempts());
        
        // Check attempts
        if (otpData.getAttempts() >= maxOtpAttempts) {
            log.warn("OTP max attempts exceeded for email: {}", email);
            redisTemplate.delete(otpKey);
            return false;
        }
        
        // Increment attempts
        otpData.setAttempts(otpData.getAttempts() + 1);
        redisTemplate.opsForValue().set(otpKey, otpData, otpExpireMinutes, TimeUnit.MINUTES);
        
        // Verify OTP
        boolean isValid = otp.equals(otpData.getOtp());
        
        log.info("OTP comparison - input: '{}' vs stored: '{}' -> {}", otp, otpData.getOtp(), isValid);
        
        if (isValid) {
            // Clear OTP và clear lockout attempts
            redisTemplate.delete(otpKey);
            otpLockoutService.clearAttempts(email, ipAddress, purpose);
            rateLimitService.recordOtpAttempt(email, true);
            log.info("OTP verified successfully for email: {}", email);
        } else {
            // Record failed attempt trong OtpLockoutService
            otpLockoutService.recordFailedAttempt(email, ipAddress, purpose);
            rateLimitService.recordOtpAttempt(email, false);
            log.warn("OTP verification failed for email: {}, attempt: {}", email, otpData.getAttempts());
        }
        
        return isValid;
    }
    
    public String generateResetToken(String email) {
        // CRITICAL SECURITY: Verify that OTP was successfully verified before generating reset token
        String verifiedKey = OTP_VERIFIED_PREFIX + "forgot_password:" + email;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey))) {
            log.error("SECURITY ALERT: Attempt to generate reset token without OTP verification for email: {}", email);
            throw new RuntimeException("OTP phải được xác minh trước khi tạo reset token");
        }
        
        String resetToken = UUID.randomUUID().toString();
        String tokenKey = RESET_TOKEN_PREFIX + email;
        
        // Store reset token in Redis with 30 minutes expiry
        redisTemplate.opsForValue().set(tokenKey, resetToken, 30, TimeUnit.MINUTES);
        
        log.info("Reset token generated for email: {} (OTP verified)", email);
        return resetToken;
    }
    
    /**
     * Mark OTP as verified for forgot_password flow
     * This must be called after successful OTP verification
     */
    public void markOtpVerified(String email, String purpose) {
        if (!"forgot_password".equals(purpose)) {
            // Only track for forgot_password purpose
            return;
        }
        String verifiedKey = OTP_VERIFIED_PREFIX + purpose + ":" + email;
        // Store verification state with same expiry as reset token (30 minutes)
        redisTemplate.opsForValue().set(verifiedKey, "verified", 30, TimeUnit.MINUTES);
        log.info("OTP verification state marked for email: {}, purpose: {}", email, purpose);
    }
    
    /**
     * Check if OTP has been verified for the given email and purpose
     */
    public boolean isOtpVerified(String email, String purpose) {
        String verifiedKey = OTP_VERIFIED_PREFIX + purpose + ":" + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey));
    }
    
    /**
     * Clear OTP verification state (called after successful password reset)
     */
    public void clearOtpVerificationState(String email, String purpose) {
        String verifiedKey = OTP_VERIFIED_PREFIX + purpose + ":" + email;
        redisTemplate.delete(verifiedKey);
        log.info("OTP verification state cleared for email: {}, purpose: {}", email, purpose);
    }
    
    public boolean validateResetToken(String resetToken, String email) {
        // CRITICAL SECURITY: Check if OTP was verified before allowing reset token validation
        String verifiedKey = OTP_VERIFIED_PREFIX + "forgot_password:" + email;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey))) {
            log.error("SECURITY ALERT: Attempt to validate reset token without OTP verification for email: {}", email);
            return false;
        }
        
        String tokenKey = RESET_TOKEN_PREFIX + email;
        String storedToken = (String) redisTemplate.opsForValue().get(tokenKey);
        
        if (storedToken == null) {
            log.warn("Reset token not found or expired for email: {}", email);
            return false;
        }
        
        boolean isValid = resetToken.equals(storedToken);
        
        if (isValid) {
            log.info("Reset token validated successfully for email: {} (OTP was verified)", email);
        } else {
            log.warn("Reset token validation failed for email: {} - Token mismatch", email);
        }
        
        return isValid;
    }
    
    public void invalidateResetToken(String resetToken, String email) {
        String tokenKey = RESET_TOKEN_PREFIX + email;
        String storedToken = (String) redisTemplate.opsForValue().get(tokenKey);
        
        if (resetToken.equals(storedToken)) {
            redisTemplate.delete(tokenKey);
            // Also clear OTP verification state after token is used
            clearOtpVerificationState(email, "forgot_password");
            log.info("Reset token invalidated for email: {} (OTP verification state also cleared)", email);
        }
    }
    
    private String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
    
    public boolean isOtpValid(String email, String purpose) {
        String otpKey = OTP_PREFIX + purpose + ":" + email;
        return redisTemplate.hasKey(otpKey);
    }
    
    public int getRemainingAttempts(String email, String purpose) {
        String otpKey = OTP_PREFIX + purpose + ":" + email;
        Object rawData = redisTemplate.opsForValue().get(otpKey);
        
        if (rawData == null) {
            return 0;
        }
        
        OtpData otpData = convertToOtpData(rawData);
        if (otpData == null) {
            return 0;
        }
        
        return Math.max(0, maxOtpAttempts - otpData.getAttempts());
    }
    
    private OtpData convertToOtpData(Object rawData) {
        try {
            if (rawData instanceof OtpData) {
                return (OtpData) rawData;
            }
            
            if (rawData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) rawData;
                
                // Handle type conversion issues manually
                OtpData otpData = new OtpData();
                otpData.setOtp((String) map.get("otp"));
                otpData.setEmail((String) map.get("email"));
                otpData.setPurpose((String) map.get("purpose"));
                
                // Handle attempts field - convert Integer/Long/BigInteger to int
                Object attempts = map.get("attempts");
                if (attempts instanceof Integer) {
                    otpData.setAttempts((Integer) attempts);
                } else if (attempts instanceof Long) {
                    otpData.setAttempts(((Long) attempts).intValue());
                } else if (attempts instanceof java.math.BigInteger) {
                    otpData.setAttempts(((java.math.BigInteger) attempts).intValue());
                } else if (attempts instanceof Number) {
                    otpData.setAttempts(((Number) attempts).intValue());
                } else {
                    otpData.setAttempts(0);
                }
                
                // Handle createdAt field
                Object createdAt = map.get("createdAt");
                if (createdAt instanceof String) {
                    otpData.setCreatedAt(LocalDateTime.parse((String) createdAt));
                } else if (createdAt instanceof LocalDateTime) {
                    otpData.setCreatedAt((LocalDateTime) createdAt);
                } else {
                    otpData.setCreatedAt(LocalDateTime.now());
                }
                
                return otpData;
            }
            
            // Try direct conversion
            return objectMapper.convertValue(rawData, OtpData.class);
        } catch (Exception e) {
            log.error("Failed to convert raw data to OtpData: {}", e.getMessage());
            log.error("Raw data type: {}", rawData != null ? rawData.getClass().getName() : "null");
            log.error("Raw data content: {}", rawData);
            return null;
        }
    }
}
