package com.badat.study1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final JavaMailSender mailSender;
    
    @Value("${app.mail.from:noreply@mmomarket.com}")
    private String fromEmail;
    
    // Store OTPs temporarily (in production, use Redis or database)
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final int OTP_COOLDOWN_MINUTES = 1; // Cooldown between OTP requests
    
    /**
     * Generate and send OTP to email
     */
    public boolean sendOtp(String email, String purpose) {
        try {
            // Check cooldown period
            String cooldownKey = email + "_" + purpose + "_cooldown";
            if (otpStorage.containsKey(cooldownKey)) {
                OtpData cooldownData = otpStorage.get(cooldownKey);
                if (cooldownData.getExpiryTime().isAfter(LocalDateTime.now())) {
                    log.warn("OTP request too frequent for email: {} purpose: {}", email, purpose);
                    return false;
                }
            }
            
            // Check existing OTP attempts
            String otpKey = email + "_" + purpose;
            if (otpStorage.containsKey(otpKey)) {
                OtpData existingOtp = otpStorage.get(otpKey);
                if (existingOtp.getAttempts() >= MAX_OTP_ATTEMPTS) {
                    log.warn("Too many OTP attempts for email: {} purpose: {}", email, purpose);
                    return false;
                }
            }
            
            // Generate OTP
            String otp = generateOtp();
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
            
            // Store OTP
            OtpData otpData = new OtpData(otp, expiryTime, 0);
            otpStorage.put(otpKey, otpData);
            
            // Set cooldown
            OtpData cooldownData = new OtpData("", LocalDateTime.now().plusMinutes(OTP_COOLDOWN_MINUTES), 0);
            otpStorage.put(cooldownKey, cooldownData);
            
            // Send email
            sendOtpEmail(email, otp, purpose);
            
            log.info("OTP sent successfully to email: {} for purpose: {}", email, purpose);
            return true;
            
        } catch (Exception e) {
            log.error("Error sending OTP to email: {} purpose: {}", email, purpose, e);
            return false;
        }
    }
    
    /**
     * Verify OTP
     */
    public boolean verifyOtp(String email, String purpose, String otp) {
        try {
            String otpKey = email + "_" + purpose;
            OtpData otpData = otpStorage.get(otpKey);
            
            if (otpData == null) {
                log.warn("No OTP found for email: {} purpose: {}", email, purpose);
                return false;
            }
            
            // Check expiry
            if (otpData.getExpiryTime().isBefore(LocalDateTime.now())) {
                log.warn("OTP expired for email: {} purpose: {}", email, purpose);
                otpStorage.remove(otpKey);
                return false;
            }
            
            // Check attempts
            if (otpData.getAttempts() >= MAX_OTP_ATTEMPTS) {
                log.warn("Too many OTP attempts for email: {} purpose: {}", email, purpose);
                otpStorage.remove(otpKey);
                return false;
            }
            
            // Increment attempts
            otpData.incrementAttempts();
            
            // Verify OTP
            if (otpData.getOtp().equals(otp)) {
                // Remove OTP after successful verification
                otpStorage.remove(otpKey);
                log.info("OTP verified successfully for email: {} purpose: {}", email, purpose);
                return true;
            } else {
                log.warn("Invalid OTP for email: {} purpose: {}", email, purpose);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error verifying OTP for email: {} purpose: {}", email, purpose, e);
            return false;
        }
    }
    
    /**
     * Clean expired OTPs
     */
    public void cleanExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpStorage.entrySet().removeIf(entry -> 
            entry.getValue().getExpiryTime().isBefore(now));
    }
    
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        
        return otp.toString();
    }
    
    private void sendOtpEmail(String email, String otp, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Mã OTP xác thực - MMO Market");
            
            String emailBody = buildEmailBody(otp, purpose);
            message.setText(emailBody);
            
            mailSender.send(message);
            log.info("OTP email sent to: {}", email);
            
        } catch (Exception e) {
            log.error("Error sending OTP email to: {}", email, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
    
    private String buildEmailBody(String otp, String purpose) {
        return String.format("""
            Chào bạn,
            
            Bạn đã yêu cầu xác thực cho: %s
            
            Mã OTP của bạn là: %s
            
            Mã OTP này có hiệu lực trong 5 phút.
            Vui lòng không chia sẻ mã này với bất kỳ ai.
            
            Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.
            
            Trân trọng,
            Đội ngũ MMO Market
            """, purpose, otp);
    }
    
    /**
     * OTP Data class
     */
    private static class OtpData {
        private final String otp;
        private final LocalDateTime expiryTime;
        private int attempts;
        
        public OtpData(String otp, LocalDateTime expiryTime, int attempts) {
            this.otp = otp;
            this.expiryTime = expiryTime;
            this.attempts = attempts;
        }
        
        public String getOtp() { return otp; }
        public LocalDateTime getExpiryTime() { return expiryTime; }
        public int getAttempts() { return attempts; }
        public void incrementAttempts() { this.attempts++; }
    }
}
