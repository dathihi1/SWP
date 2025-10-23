package com.badat.study1.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Slf4j
@Service
public class EmailTemplateService {
    
    private static final String TEMPLATE_PATH = "static/email-templates/";
    
    /**
     * Load HTML template from resources
     * @param templateName Name of the template file (e.g., "otp-registration.html")
     * @return HTML content as string
     */
    public String loadTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH + templateName);
            if (!resource.exists()) {
                log.error("Template not found: {}", templateName);
                throw new RuntimeException("Template not found: " + templateName);
            }
            
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("Template loaded successfully: {}", templateName);
            return content;
            
        } catch (IOException e) {
            log.error("Failed to load template: {}", templateName, e);
            throw new RuntimeException("Failed to load template: " + templateName, e);
        }
    }
    
    /**
     * Populate OTP email template with dynamic values
     * @param templateName Name of the template file
     * @param otpCode The 6-digit OTP code
     * @param validityMinutes Validity period in minutes
     * @param purpose Purpose of the OTP (register, forgot_password)
     * @param userEmail User's email address
     * @return Populated HTML content
     */
    public String generateOtpEmail(String templateName, String otpCode, int validityMinutes, String purpose, String userEmail) {
        try {
            String template = loadTemplate(templateName);
            
            // Replace placeholders
            String htmlContent = template
                .replace("{{OTP_CODE}}", otpCode)
                .replace("{{VALIDITY_MINUTES}}", String.valueOf(validityMinutes))
                .replace("{{PURPOSE}}", getPurposeText(purpose))
                .replace("{{USER_EMAIL}}", userEmail != null ? userEmail : "")
                .replace("{{CURRENT_YEAR}}", String.valueOf(LocalDate.now().getYear()))
                .replace("{{SUPPORT_EMAIL}}", "support@mmomarket.com");
            
            log.info("OTP email template populated for purpose: {} with email: {}", purpose, userEmail);
            return htmlContent;
            
        } catch (Exception e) {
            log.error("Failed to generate OTP email for purpose: {}", purpose, e);
            throw new RuntimeException("Failed to generate OTP email", e);
        }
    }
    
    /**
     * Get user-friendly purpose text
     * @param purpose Technical purpose (register, forgot_password)
     * @return User-friendly text
     */
    private String getPurposeText(String purpose) {
        return switch (purpose) {
            case "register" -> "Đăng ký tài khoản";
            case "forgot_password" -> "Khôi phục mật khẩu";
            default -> "Xác thực tài khoản";
        };
    }
    
    /**
     * Generate registration OTP email
     * @param otpCode The 6-digit OTP code
     * @param validityMinutes Validity period in minutes
     * @param userEmail User's email address
     * @return Populated HTML content
     */
    public String generateRegistrationOtpEmail(String otpCode, int validityMinutes, String userEmail) {
        return generateOtpEmail("otp-registration.html", otpCode, validityMinutes, "register", userEmail);
    }
    
    /**
     * Generate forgot password OTP email
     * @param otpCode The 6-digit OTP code
     * @param validityMinutes Validity period in minutes
     * @param userEmail User's email address
     * @return Populated HTML content
     */
    public String generateForgotPasswordOtpEmail(String otpCode, int validityMinutes, String userEmail) {
        return generateOtpEmail("otp-forgot-password.html", otpCode, validityMinutes, "forgot_password", userEmail);
    }
}
