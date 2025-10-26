package com.badat.study1.controller;

import com.badat.study1.dto.request.ForgotPasswordRequest;
import com.badat.study1.dto.request.ResetPasswordRequest;
import com.badat.study1.dto.response.ApiResponse;
import com.badat.study1.service.UserService;
import com.badat.study1.service.OtpService;
import com.badat.study1.service.RateLimitService;
import com.badat.study1.service.SecurityEventService;
import com.badat.study1.service.AuditLogService;
import com.badat.study1.model.SecurityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;
    private final OtpService otpService;
    private final RateLimitService rateLimitService;
    private final SecurityEventService securityEventService;
    private final AuditLogService auditLogService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> sendForgotPasswordOtp(
            @RequestBody ForgotPasswordRequest request, 
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String email = request.getEmail();
            
            // 🔒 1. Rate limiting check
            if (rateLimitService.isEmailRateLimited(email)) {
                log.warn("Rate limited forgot password request for email: {} from IP: {}", email, ipAddress);
                securityEventService.logRateLimited(ipAddress, email, "Too many forgot password requests");
                return ResponseEntity.status(429).body(
                    ApiResponse.error("Quá nhiều yêu cầu. Vui lòng thử lại sau 1 giờ."));
            }
            
            // 🔒 2. Check if user exists and provider type
            try {
                userService.sendForgotPasswordOtp(email);
            } catch (RuntimeException e) {
                // Check if it's a Google account error
                if (e.getMessage().contains("Google")) {
                    log.info("Forgot password request for Google account: {} from IP: {}", email, ipAddress);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error("Tài khoản này được đăng ký bằng Google. Vui lòng sử dụng chức năng đăng nhập bằng Google."));
                }
                // For other errors (like non-existent email), still send success response to prevent email enumeration
                log.info("Forgot password request for non-existent email: {} from IP: {}", email, ipAddress);
                return ResponseEntity.ok(ApiResponse.success("Nếu email tồn tại, mã OTP đã được gửi", null));
            } catch (Exception e) {
                // For unexpected errors, still send success response to prevent email enumeration
                log.info("Forgot password request for non-existent email: {} from IP: {}", email, ipAddress);
                return ResponseEntity.ok(ApiResponse.success("Nếu email tồn tại, mã OTP đã được gửi", null));
            }
            
            // 🔒 3. Log security event
            securityEventService.logForgotPasswordRequest(ipAddress, email);
            
            // Log audit event
            try {
                auditLogService.logPasswordResetRequested(email, ipAddress, 
                    httpRequest.getRequestURI(), httpRequest.getMethod());
            } catch (Exception e) {
                log.error("Error logging password reset request: {}", e.getMessage());
            }
            
            return ResponseEntity.ok(ApiResponse.success("Nếu email tồn tại, mã OTP đã được gửi", null));
            
        } catch (Exception e) {
            log.error("Error sending forgot password OTP: {}", e.getMessage());
            return ResponseEntity.status(500).body(
                ApiResponse.error("Có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyOtp(
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String email = request.getEmail();
            String otp = request.getOtp();
            
            // 🔒 1. Rate limiting check
            if (rateLimitService.isOtpRateLimited(email)) {
                log.warn("Rate limited OTP verification for email: {} from IP: {}", email, ipAddress);
                securityEventService.logRateLimited(ipAddress, email, "Too many OTP attempts");
                return ResponseEntity.status(429).body(
                    ApiResponse.error("Quá nhiều lần nhập sai OTP. Vui lòng thử lại sau 1 giờ."));
            }
            
            // 🔒 2. Verify OTP
            try {
                userService.verifyForgotPasswordOtp(email, otp);
            } catch (RuntimeException e) {
                // Check if it's a Google account error
                if (e.getMessage().contains("Google")) {
                    log.info("OTP verification attempt for Google account: {} from IP: {}", email, ipAddress);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error("Tài khoản này được đăng ký bằng Google. Vui lòng sử dụng chức năng đăng nhập bằng Google."));
                }
                // For OTP validation errors
                securityEventService.logOtpFailed(ipAddress, email, "forgot_password", 
                        rateLimitService.getOtpAttemptCount(email).intValue());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Mã OTP không hợp lệ hoặc đã hết hạn"));
            }
            
            // 🔒 3. Generate reset token (temporary token for password reset)
            String resetToken = otpService.generateResetToken(email);
            
            securityEventService.logOtpVerified(ipAddress, email, "forgot_password");
            
            return ResponseEntity.ok(ApiResponse.success("Mã OTP hợp lệ", 
                    Map.of("resetToken", resetToken)));
            
        } catch (Exception e) {
            log.error("Error verifying OTP: {}", e.getMessage());
            return ResponseEntity.status(500).body(
                ApiResponse.error("Có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String email = request.getEmail();
            String resetToken = request.getResetToken();
            String newPassword = request.getNewPassword();
            
            // 🔒 1. Validate reset token
            if (!otpService.validateResetToken(resetToken, email)) {
                log.warn("Invalid reset token for email: {} from IP: {}", email, ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.SECURITY_ALERT, ipAddress, 
                        "Invalid reset token attempt for email: " + email);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Token không hợp lệ hoặc đã hết hạn"));
            }
            
            // 🔒 2. Reset password
            try {
                userService.resetPassword(email, resetToken, newPassword);
            } catch (RuntimeException e) {
                // Check if it's a Google account error
                if (e.getMessage().contains("Google")) {
                    log.info("Password reset attempt for Google account: {} from IP: {}", email, ipAddress);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error("Tài khoản này được đăng ký bằng Google. Vui lòng sử dụng chức năng đăng nhập bằng Google."));
                }
                // For other errors (password validation, etc.)
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage()));
            }
            
            // 🔒 3. Invalidate reset token
            otpService.invalidateResetToken(resetToken, email);
            
            // 🔒 4. Log security event
            securityEventService.logPasswordReset(ipAddress, email);
            
            return ResponseEntity.ok(ApiResponse.success("Mật khẩu đã được đặt lại thành công", null));
            
        } catch (Exception e) {
            log.error("Error resetting password: {}", e.getMessage());
            return ResponseEntity.status(500).body(
                ApiResponse.error("Có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
