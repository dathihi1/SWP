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
    
    @Value("${security.rate-limit.otp-expire-minutes:10}")
    private int otpExpireMinutes;
    
    @Value("${security.rate-limit.otp-max-attempts:5}")
    private int maxOtpAttempts;
    
    private static final String OTP_PREFIX = "otp:";
    private static final String RESET_TOKEN_PREFIX = "reset_token:";
    
    public void sendOtp(String email, String purpose) {
        // Check rate limiting (skip for forgot password)
        if (!"forgot_password".equals(purpose) && rateLimitService.isEmailRateLimited(email)) {
            throw new RuntimeException("Quá nhiều yêu cầu. Vui lòng thử lại sau 1 giờ.");
        }
        
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
        
        // Record request
        rateLimitService.recordEmailRequest(email);
        
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
        String resetToken = UUID.randomUUID().toString();
        String tokenKey = RESET_TOKEN_PREFIX + email;
        
        // Store reset token in Redis with 30 minutes expiry
        redisTemplate.opsForValue().set(tokenKey, resetToken, 30, TimeUnit.MINUTES);
        
        log.info("Reset token generated for email: {}", email);
        return resetToken;
    }
    
    public boolean validateResetToken(String resetToken, String email) {
        String tokenKey = RESET_TOKEN_PREFIX + email;
        String storedToken = (String) redisTemplate.opsForValue().get(tokenKey);
        
        if (storedToken == null) {
            log.warn("Reset token not found or expired for email: {}", email);
            return false;
        }
        
        boolean isValid = resetToken.equals(storedToken);
        
        if (isValid) {
            log.info("Reset token validated successfully for email: {}", email);
        } else {
            log.warn("Reset token validation failed for email: {}", email);
        }
        
        return isValid;
    }
    
    public void invalidateResetToken(String resetToken, String email) {
        String tokenKey = RESET_TOKEN_PREFIX + email;
        String storedToken = (String) redisTemplate.opsForValue().get(tokenKey);
        
        if (resetToken.equals(storedToken)) {
            redisTemplate.delete(tokenKey);
            log.info("Reset token invalidated for email: {}", email);
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
