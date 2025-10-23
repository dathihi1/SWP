package com.badat.study1.service;

import com.badat.study1.model.SecurityEvent;
import com.badat.study1.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityEventService {
    
    private final SecurityEventRepository securityEventRepository;
    
    public void logSecurityEvent(SecurityEvent.EventType eventType, String ipAddress, String details) {
        logSecurityEvent(eventType, ipAddress, null, null, details, null);
    }
    
    public void logSecurityEvent(SecurityEvent.EventType eventType, String ipAddress, String email, String details) {
        logSecurityEvent(eventType, ipAddress, email, null, details, null);
    }
    
    public void logSecurityEvent(SecurityEvent.EventType eventType, String ipAddress, String email, String username, String details, String userAgent) {
        try {
            SecurityEvent securityEvent = SecurityEvent.builder()
                    .eventType(eventType)
                    .ipAddress(ipAddress)
                    .email(email)
                    .username(username)
                    .details(details)
                    .userAgent(userAgent)
                    .build();
            
            securityEventRepository.save(securityEvent);
            
            log.info("Security event logged: {} from IP: {} - {}", eventType, ipAddress, details);
        } catch (Exception e) {
            log.error("Failed to log security event: {}", e.getMessage());
        }
    }
    
    public void logIpLockout(String ipAddress, String reason, int attemptCount) {
        logSecurityEvent(SecurityEvent.EventType.IP_LOCKED, ipAddress, null, null, 
                "IP locked due to excessive failed attempts. Reason: " + reason + ", Attempts: " + attemptCount, null);
    }
    
    public void logLoginAttempt(String ipAddress, String username, boolean success, String details, String userAgent) {
        SecurityEvent.EventType eventType = success ? SecurityEvent.EventType.LOGIN_SUCCESS : SecurityEvent.EventType.LOGIN_FAILED;
        logSecurityEvent(eventType, ipAddress, null, username, details, userAgent);
    }
    
    public void logCaptchaRequired(String ipAddress, String username) {
        logSecurityEvent(SecurityEvent.EventType.CAPTCHA_REQUIRED, ipAddress, null, username, 
                "Captcha required due to multiple failed attempts", null);
    }
    
    public void logCaptchaFailed(String ipAddress, String username) {
        logSecurityEvent(SecurityEvent.EventType.CAPTCHA_FAILED, ipAddress, null, username, 
                "Captcha verification failed", null);
    }
    
    public void logForgotPasswordRequest(String ipAddress, String email) {
        logSecurityEvent(SecurityEvent.EventType.FORGOT_PASSWORD_REQUEST, ipAddress, email, null, 
                "Forgot password request", null);
    }
    
    public void logOtpSent(String ipAddress, String email, String purpose) {
        logSecurityEvent(SecurityEvent.EventType.OTP_SENT, ipAddress, email, null, 
                "OTP sent for " + purpose, null);
    }
    
    public void logOtpVerified(String ipAddress, String email, String purpose) {
        logSecurityEvent(SecurityEvent.EventType.OTP_VERIFIED, ipAddress, email, null, 
                "OTP verified for " + purpose, null);
    }
    
    public void logOtpFailed(String ipAddress, String email, String purpose, int attempts) {
        logSecurityEvent(SecurityEvent.EventType.OTP_FAILED, ipAddress, email, null, 
                "OTP verification failed for " + purpose + " (attempt " + attempts + ")", null);
    }
    
    public void logPasswordReset(String ipAddress, String email) {
        logSecurityEvent(SecurityEvent.EventType.PASSWORD_RESET, ipAddress, email, null, 
                "Password reset successful", null);
    }
    
    public void logRateLimited(String ipAddress, String email, String reason) {
        logSecurityEvent(SecurityEvent.EventType.RATE_LIMITED, ipAddress, email, null, 
                "Rate limited: " + reason, null);
    }
    
    public void logSecurityAlert(String ipAddress, String details) {
        logSecurityEvent(SecurityEvent.EventType.SECURITY_ALERT, ipAddress, null, null, 
                "SECURITY ALERT: " + details, null);
    }
}
