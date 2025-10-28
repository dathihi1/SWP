package com.badat.study1.controller;

import com.badat.study1.annotation.UserActivity;
import com.badat.study1.model.UserActivityLog;
import com.badat.study1.dto.request.LoginRequest;
import com.badat.study1.dto.response.LoginResponse;
import com.badat.study1.dto.response.CaptchaResponse;
import com.badat.study1.dto.request.UserCreateRequest;
import com.badat.study1.dto.request.VerifyRequest;
import com.badat.study1.dto.request.ForgotPasswordRequest;
import com.badat.study1.dto.response.ApiResponse;
import com.badat.study1.service.UserService;
import com.badat.study1.service.AuthenticationService;
import com.badat.study1.service.CaptchaService;
import com.badat.study1.service.IpLockoutService;
import com.badat.study1.service.OtpLockoutService;
import com.badat.study1.service.OtpService;
import com.badat.study1.service.SecurityEventService;
import com.badat.study1.service.CaptchaRateLimitService;
import com.badat.study1.service.RateLimitService;
import com.badat.study1.model.SecurityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final CaptchaService captchaService;
    private final IpLockoutService ipLockoutService;
    private final SecurityEventService securityEventService;
    private final CaptchaRateLimitService captchaRateLimitService;
    private final RateLimitService rateLimitService;
    private final OtpLockoutService otpLockoutService;
    private final OtpService otpService;

    @PostMapping("/login")
    @UserActivity(action = "LOGIN", category = UserActivityLog.Category.ACCOUNT)
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String username = loginRequest.getUsername();
            
            log.info("Login attempt for username: {} from IP: {}", username, ipAddress);
            
            // 🔒 1. Check IP lockout
            if (ipLockoutService.isIpLocked(ipAddress)) {
                log.warn("Login blocked - IP locked: {}", ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.IP_LOCKED, ipAddress, 
                        "Login attempt from locked IP");
                return ResponseEntity.status(429).body(Map.of(
                    "error", "IP đã bị khóa do quá nhiều lần đăng nhập sai",
                    "lockedUntil", "30 phút"
                ));
            }
            
            // 🔒 1.5. Check if IP is rate limited for captcha failures
            if (captchaRateLimitService.isCaptchaRateLimited(ipAddress)) {
                log.warn("Captcha rate limited - IP: {}", ipAddress);
                return ResponseEntity.status(429).body(Map.of(
                    "error", "Quá nhiều lần nhập sai captcha. Vui lòng thử lại sau 15 phút",
                    "captchaRateLimited", true
                ));
            }
            
            // 🔒 2. Check captcha FIRST (validate correctness)
            String captchaCode = loginRequest.getCaptchaCode();
            String captchaId = loginRequest.getCaptchaId();
            
            // If captchaCode is null, try to get from simple captcha field (for frontend compatibility)
            if (captchaCode == null || captchaCode.trim().isEmpty()) {
                captchaCode = loginRequest.getCaptcha();
            }
            
            // Always require captcha input (not empty)
            if (captchaCode == null || captchaCode.trim().isEmpty()) {
                securityEventService.logCaptchaRequired(ipAddress, username);
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Vui lòng nhập mã xác thực",
                    "message", "captcha required",
                    "captchaRequired", true
                ));
            }
            
            // Validate captcha correctness
            boolean captchaValid = false;
            if (captchaId != null && !captchaId.trim().isEmpty()) {
                if (captchaId.startsWith("frontend-")) {
                    // Frontend captcha not allowed for security reasons
                    log.warn("Frontend captcha not allowed from IP: {}", ipAddress);
                    securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_FAILED, ipAddress, 
                            "Frontend captcha attempt blocked");
                    return ResponseEntity.status(400).body(Map.of(
                        "error", "Captcha không hợp lệ, vui lòng làm mới trang",
                        "message", "frontend captcha not allowed",
                        "captchaRequired", true
                    ));
                } else {
                    // Backend captcha with Redis validation
                    captchaValid = captchaService.validateSimpleCaptcha(captchaId, captchaCode);
                    log.info("Backend captcha validation: {}", captchaValid ? "valid" : "invalid");
                }
            } else {
                // No captcha ID provided
                log.warn("Login attempt without captchaId from IP: {}", ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_FAILED, ipAddress, 
                        "Login attempt without captcha ID");
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Mã xác thực không hợp lệ, vui lòng làm mới trang",
                    "message", "invalid captcha",
                    "captchaRequired", true
                ));
            }
            
            // If captcha is invalid, return immediately without checking credentials
            if (!captchaValid) {
                captchaRateLimitService.recordFailedCaptchaAttempt(ipAddress);
                securityEventService.logCaptchaRequired(ipAddress, username);
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Mã xác thực không đúng, vui lòng nhập lại",
                    "message", "captcha incorrect",
                    "captchaRequired", true,
                    "captcha", captchaService.generateSimpleCaptcha()
                ));
            }
            
            // 🔒 3. Only if captcha is valid, then check username/password
            try {
                LoginResponse loginResponse = authenticationService.login(loginRequest, ipAddress, userAgent);
                
                // Clear IP attempts on successful login
                ipLockoutService.recordSuccessfulAttempt(ipAddress);
                captchaRateLimitService.clearCaptchaAttempts(ipAddress);
                securityEventService.logLoginAttempt(ipAddress, username, true, "Login successful", userAgent);
                
                // Set HttpOnly access token cookie for browser navigation
                boolean secure = request.isSecure();
                ResponseCookie accessCookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                        .httpOnly(true)
                        .secure(secure)
                        .path("/")
                        .sameSite("Lax")
                        .maxAge(60L * 60L) // 1 hour, align with token expiry
                        .build();

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                        .body(loginResponse);
                        
            } catch (Exception e) {
                // Record failed attempt
                ipLockoutService.recordFailedAttempt(ipAddress, username);
                securityEventService.logLoginAttempt(ipAddress, username, false, e.getMessage(), userAgent);
                
                Map<String, Object> response = new HashMap<>();
                
                // Determine error type based on exception message
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("User not found")) {
                    response.put("error", "Tên đăng nhập không đúng");
                } else if (errorMessage != null && errorMessage.contains("Invalid password")) {
                    response.put("error", "Mật khẩu không đúng");
                } else {
                    response.put("error", "Tên đăng nhập hoặc mật khẩu không đúng");
                }
                
                // Always generate new captcha after failed attempt
                response.put("captchaRequired", true);
                response.put("message", "captcha required");
                response.put("captcha", captchaService.generateSimpleCaptcha());
                
                log.info("Generated new captcha after failed login attempt for user: {} from IP: {}", username, ipAddress);
                
                return ResponseEntity.status(401).body(response);
            }
            
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi hệ thống"));
        }
    }
    
    @GetMapping("/captcha/simple")
    public ResponseEntity<?> getSimpleCaptcha() {
        try {
            log.info("Generating simple captcha...");
            Map<String, String> captchaData = captchaService.generateSimpleCaptcha();
            log.info("Simple captcha generated successfully: {}", captchaData.get("captchaId"));
            return ResponseEntity.ok(captchaData);
        } catch (Exception e) {
            log.error("Error generating simple captcha: {}", e.getMessage(), e);
            // Return a fallback captcha without Redis
            Map<String, String> fallbackCaptcha = new HashMap<>();
            fallbackCaptcha.put("captchaId", "fallback-" + System.currentTimeMillis());
            fallbackCaptcha.put("captchaText", "FALLBACK");
            return ResponseEntity.ok(fallbackCaptcha);
        }
    }

    @GetMapping("/captcha")
    public ResponseEntity<?> getCaptcha() {
        try {
            CaptchaResponse captcha = captchaService.generateCaptcha();
            return ResponseEntity.ok(captcha);
        } catch (Exception e) {
            log.error("Failed to generate captcha: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Không thể tạo captcha"));
        }
    }

    // Registration flow: FE validates, BE validates again, send OTP async, immediately respond with nextUrl
    @PostMapping("/register")
    @UserActivity(action = "REGISTER", category = UserActivityLog.Category.ACCOUNT)
    public ResponseEntity<?> register(@RequestBody @Valid UserCreateRequest request, HttpServletRequest http) {
        try {
            String ipAddress = getClientIpAddress(http);
            
            // Log security event
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.REGISTER_ATTEMPT, 
                ipAddress, 
                "Registration attempt for: " + request.getEmail()
            );
            
            // Check IP lockout
            if (ipLockoutService.isIpLocked(ipAddress)) {
                log.warn("Registration blocked - IP locked: {}", ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.IP_LOCKED, ipAddress, 
                        "Registration attempt from locked IP");
                return ResponseEntity.status(429).body(Map.of(
                    "error", "IP đã bị khóa do quá nhiều lần đăng nhập sai",
                    "lockedUntil", "30 phút"
                ));
            }
            
            // Check IP rate limiting for registration
            if (rateLimitService.isIpRateLimited(ipAddress, "register")) {
                log.warn("Registration rate limited - IP: {}", ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.RATE_LIMITED, ipAddress, 
                        "Registration rate limited");
                return ResponseEntity.status(429).body(Map.of(
                    "error", "Quá nhiều yêu cầu đăng ký. Vui lòng thử lại sau 1 giờ",
                    "rateLimited", true
                ));
            }
            
            // Validate captcha
            if (request.getCaptchaCode() == null || request.getCaptchaCode().trim().isEmpty()) {
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_REQUIRED, ipAddress, 
                        "Registration without captcha");
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Vui lòng nhập mã xác thực",
                    "captchaRequired", true
                ));
            }
            
            if (request.getCaptchaId() == null || request.getCaptchaId().trim().isEmpty()) {
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_FAILED, ipAddress, 
                        "Registration without captcha ID");
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Mã xác thực không hợp lệ, vui lòng làm mới trang",
                    "captchaRequired", true
                ));
            }
            
            // Validate captcha correctness
            boolean captchaValid = captchaService.validateSimpleCaptcha(request.getCaptchaId(), request.getCaptchaCode());
            if (!captchaValid) {
                captchaRateLimitService.recordFailedCaptchaAttempt(ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_FAILED, ipAddress, 
                        "Registration with invalid captcha");
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Mã xác thực không đúng, vui lòng nhập lại",
                    "captchaRequired", true,
                    "captcha", captchaService.generateSimpleCaptcha()
                ));
            }
            
            // Add delay to prevent timing attacks
            Thread.sleep(500 + new Random().nextInt(500));
            
            // Call userService.register() - CATCH specific exceptions
            try {
                userService.register(request);
            } catch (RuntimeException e) {
                if ("EMAIL_EXISTS".equals(e.getMessage())) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Email đã được sử dụng",
                        "field", "email"
                    ));
                } else if ("USERNAME_EXISTS".equals(e.getMessage())) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Tên đăng nhập đã tồn tại",
                        "field", "username"
                    ));
                }
                throw e; // Re-throw other exceptions
            }
            
            // Record IP request
            rateLimitService.recordIpRequest(ipAddress, "register");
            
            // Log successful registration attempt
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.REGISTER_SUCCESS, 
                ipAddress, 
                "OTP sent to: " + request.getEmail()
            );
            
            // Always return success to prevent email enumeration
            return ResponseEntity.ok(ApiResponse.success(
                "Nếu email hợp lệ, chúng tôi đã gửi mã OTP",
                Map.of("nextUrl", "/verify-otp?email=" + request.getEmail())
            ));
            
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            // Don't leak information
            return ResponseEntity.ok(ApiResponse.success(
                "Nếu email hợp lệ, chúng tôi đã gửi mã OTP",
                Map.of("nextUrl", "/verify-otp?email=" + request.getEmail())
            ));
        }
    }

    @PostMapping("/verify-register-otp")
    @UserActivity(action = "OTP_VERIFY", category = UserActivityLog.Category.ACCOUNT)
    public ResponseEntity<?> verifyRegisterOtp(@RequestBody VerifyRequest request, HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            
            // Validate email format
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                log.warn("OTP verification attempt with empty email from IP: {}", ipAddress);
                return ResponseEntity.badRequest().body(ApiResponse.error("Email không được để trống"));
            }
            
            // Validate OTP format
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                log.warn("OTP verification attempt with empty OTP from IP: {}", ipAddress);
                return ResponseEntity.badRequest().body(ApiResponse.error("Mã OTP không được để trống"));
            }
            
            // Log security event
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.OTP_VERIFY_ATTEMPT, 
                ipAddress, 
                "OTP verification attempt for: " + request.getEmail()
            );
            
            // Call userService.verify() với ipAddress
            try {
                userService.verify(request.getEmail(), request.getOtp(), ipAddress);
            } catch (RuntimeException e) {
                // Check if lockout error
                if (e.getMessage().contains("nhập sai OTP quá 5 lần")) {
                    return ResponseEntity.status(429).body(Map.of(
                        "error", e.getMessage(),
                        "locked", true
                    ));
                }
                throw e; // Re-throw other errors
            }
            
            // Log successful verification
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.OTP_VERIFY_SUCCESS, 
                ipAddress, 
                "OTP verified successfully for: " + request.getEmail()
            );
            
            // On success, redirect to login page
            return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công",
                    Map.of("nextUrl", "/login")));
                    
        } catch (Exception e) {
            log.error("OTP verification failed for email: {} from IP: {}, error: {}", 
                request.getEmail(), getClientIpAddress(httpRequest), e.getMessage());
            
            // Log failed verification
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.OTP_VERIFY_FAILED, 
                getClientIpAddress(httpRequest), 
                "OTP verification failed for: " + request.getEmail() + ", reason: " + e.getMessage()
            );
            
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/check-otp-lockout")
    public ResponseEntity<?> checkOtpLockout(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String email = request.get("email");
            String ipAddress = getClientIpAddress(httpRequest);
            String purpose = request.getOrDefault("purpose", "register");
            
            boolean isLocked = otpLockoutService.isLocked(email, ipAddress, purpose);
            
            if (isLocked) {
                Long remainingSeconds = otpLockoutService.getLockoutTimeRemaining(email, ipAddress, purpose);
                return ResponseEntity.ok(Map.of(
                    "locked", true,
                    "remainingSeconds", remainingSeconds,
                    "message", "Bạn đã nhập sai OTP quá 5 lần. Vui lòng thử lại sau " + (remainingSeconds / 60) + " phút"
                ));
            }
            
            Long remainingAttempts = otpLockoutService.getRemainingAttempts(email, ipAddress, purpose);
            return ResponseEntity.ok(Map.of(
                "locked", false,
                "remainingAttempts", remainingAttempts
            ));
            
        } catch (Exception e) {
            log.error("Error checking OTP lockout: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            
            // Log security event
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.FORGOT_PASSWORD_REQUEST, 
                ipAddress, 
                "Forgot password attempt for: " + request.getEmail()
            );
            
            // Check IP lockout
            if (ipLockoutService.isIpLocked(ipAddress)) {
                log.warn("Forgot password blocked - IP locked: {}", ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.IP_LOCKED, ipAddress, 
                        "Forgot password attempt from locked IP");
                return ResponseEntity.status(429).body(Map.of(
                    "error", "IP đã bị khóa do quá nhiều lần đăng nhập sai",
                    "lockedUntil", "30 phút"
                ));
            }
            
            // Check IP rate limiting với limit riêng cho forgot password
            if (rateLimitService.isIpRateLimitedForForgotPassword(ipAddress)) {
                log.warn("Forgot password rate limited - IP: {}", ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.RATE_LIMITED, ipAddress, 
                        "Forgot password rate limited");
                return ResponseEntity.status(429).body(Map.of(
                    "error", "Quá nhiều yêu cầu từ IP này. Vui lòng thử lại sau 1 giờ",
                    "rateLimited", true
                ));
            }
            
            // Validate captcha
            if (request.getCaptchaCode() == null || request.getCaptchaCode().trim().isEmpty()) {
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_REQUIRED, ipAddress, 
                        "Forgot password without captcha");
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Vui lòng nhập mã xác thực",
                    "captchaRequired", true
                ));
            }
            
            if (request.getCaptchaId() == null || request.getCaptchaId().trim().isEmpty()) {
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_FAILED, ipAddress, 
                        "Forgot password without captcha ID");
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Mã xác thực không hợp lệ, vui lòng làm mới trang",
                    "captchaRequired", true
                ));
            }
            
            // Validate captcha correctness
            log.info("Forgot password captcha validation - ID: {}, User input: {}", request.getCaptchaId(), request.getCaptchaCode());
            boolean captchaValid = captchaService.validateSimpleCaptcha(request.getCaptchaId(), request.getCaptchaCode());
            log.info("Forgot password captcha validation result: {}", captchaValid);
            if (!captchaValid) {
                captchaRateLimitService.recordFailedCaptchaAttempt(ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_FAILED, ipAddress, 
                        "Forgot password with invalid captcha");
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Mã xác thực không đúng, vui lòng nhập lại",
                    "captchaRequired", true,
                    "captcha", captchaService.generateSimpleCaptcha()
                ));
            }
            
            // Add delay to prevent timing attacks
            Thread.sleep(500 + new Random().nextInt(500));
            
            userService.forgotPassword(request.getEmail());
            
            // Record IP request
            rateLimitService.recordForgotPasswordIpRequest(ipAddress);
            
            // Log successful request
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.FORGOT_PASSWORD_REQUEST, 
                ipAddress, 
                "Password reset email sent to: " + request.getEmail()
            );
            
            // Always return success to prevent email enumeration
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mã OTP đã được gửi đến email của bạn",
                "nextUrl", "/verify-otp?email=" + request.getEmail() + "&type=forgot_password"
            ));
            
        } catch (Exception e) {
            log.error("Forgot password error: {}", e.getMessage(), e);
            // Always return success to prevent email enumeration
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Nếu email tồn tại, chúng tôi đã gửi hướng dẫn khôi phục mật khẩu",
                "nextUrl", "/verify-otp?email=" + request.getEmail() + "&type=forgot_password"
            ));
        }
    }

    @PostMapping("/verify-forgot-password-otp")
    @UserActivity(action = "OTP_VERIFY", category = UserActivityLog.Category.ACCOUNT)
    public ResponseEntity<?> verifyForgotPasswordOtp(@RequestBody VerifyRequest request, HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            
            // Validate email format
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                log.warn("Forgot password OTP verification attempt with empty email from IP: {}", ipAddress);
                return ResponseEntity.badRequest().body(ApiResponse.error("Email không được để trống"));
            }
            
            // Validate OTP format
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                log.warn("Forgot password OTP verification attempt with empty OTP from IP: {}", ipAddress);
                return ResponseEntity.badRequest().body(ApiResponse.error("Mã OTP không được để trống"));
            }
            
            // Log security event
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.OTP_VERIFY_ATTEMPT, 
                ipAddress, 
                "Forgot password OTP verification attempt for: " + request.getEmail()
            );
            
            // Call userService.verifyForgotPasswordOtp() với ipAddress
            try {
                userService.verifyForgotPasswordOtp(request.getEmail(), request.getOtp(), ipAddress);
            } catch (RuntimeException e) {
                // Check if lockout error
                if (e.getMessage().contains("nhập sai OTP quá 5 lần")) {
                    return ResponseEntity.status(429).body(Map.of(
                        "error", e.getMessage(),
                        "locked", true
                    ));
                }
                throw e; // Re-throw other errors
            }
            
            // Generate resetToken and save to Redis
            String resetToken = otpService.generateResetToken(request.getEmail());
            
            // Log successful verification
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.OTP_VERIFY_SUCCESS, 
                ipAddress, 
                "Forgot password OTP verified successfully for: " + request.getEmail()
            );
            
            // Return nextUrl to reset-password page
            return ResponseEntity.ok(ApiResponse.success("Mã OTP hợp lệ",
                    Map.of("nextUrl", "/reset-password?email=" + request.getEmail() + "&token=" + resetToken)));
                    
        } catch (Exception e) {
            log.error("Forgot password OTP verification failed for email: {} from IP: {}, error: {}", 
                request.getEmail(), getClientIpAddress(httpRequest), e.getMessage());
            
            // Log failed verification
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.OTP_VERIFY_FAILED, 
                getClientIpAddress(httpRequest), 
                "Forgot password OTP verification failed for: " + request.getEmail() + ", reason: " + e.getMessage()
            );
            
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @UserActivity(action = "RESET_PASSWORD", category = UserActivityLog.Category.ACCOUNT)
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String email = request.get("email");
            String resetToken = request.get("resetToken");
            String newPassword = request.get("newPassword");
            String repassword = request.get("repassword");
            
            // Validate inputs
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Email không được để trống"));
            }
            
            if (resetToken == null || resetToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Token không hợp lệ"));
            }
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu mới không được để trống"));
            }
            
            if (repassword == null || repassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Xác nhận mật khẩu không được để trống"));
            }
            
            if (!newPassword.equals(repassword)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu mới và xác nhận mật khẩu không khớp"));
            }
            
            if (newPassword.length() < 6 || newPassword.length() > 100) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu phải từ 6-100 ký tự"));
            }
            
            // Log security event
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.PASSWORD_RESET, 
                ipAddress, 
                "Reset password attempt for: " + email
            );
            
            // Validate resetToken and reset password
            userService.resetPasswordWithToken(email, resetToken, newPassword);
            
            // Log successful reset
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.PASSWORD_RESET, 
                ipAddress, 
                "Password reset successfully for: " + email
            );
            
            return ResponseEntity.ok(ApiResponse.success("Mật khẩu đã được đặt lại thành công"));
                    
        } catch (Exception e) {
            log.error("Reset password failed for email: {} from IP: {}, error: {}", 
                request.get("email"), getClientIpAddress(httpRequest), e.getMessage());
            
            // Log failed reset
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.PASSWORD_RESET, 
                getClientIpAddress(httpRequest), 
                "Reset password failed for: " + request.get("email") + ", reason: " + e.getMessage()
            );
            
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @UserActivity(action = "LOGOUT", category = UserActivityLog.Category.ACCOUNT)
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader, HttpServletRequest request) {
        try {
            // Prefer header; if missing, try cookie via JwtAuthenticationFilter, but here we only need header token to blacklist
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // Still clear cookie even if header missing
                boolean secure = request.isSecure();
                ResponseCookie clearCookie = ResponseCookie.from("accessToken", "")
                        .httpOnly(true)
                        .secure(secure)
                        .path("/")
                        .sameSite("Lax")
                        .maxAge(0)
                        .build();
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                        .body(Map.of("message", "Đăng xuất thành công"));
            }

            String token = authHeader.replace("Bearer ", "");
            
            // Get user info before logout for audit logging
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                // Logout will be handled by UserActivityAspect
            }
            
            authenticationService.logout(token);
            
            log.info("Logout successful");
            // Clear access token cookie
            boolean secure = request.isSecure();
            ResponseCookie clearCookie = ResponseCookie.from("accessToken", "")
                    .httpOnly(true)
                    .secure(secure)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(0)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(Map.of("message", "Đăng xuất thành công"));
            
        } catch (ParseException e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Token không hợp lệ"));
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Có lỗi xảy ra khi đăng xuất"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Chưa đăng nhập"));
            }
            
            // Get User object from authentication principal
            Object principal = auth.getPrincipal();
            if (principal instanceof com.badat.study1.model.User) {
                com.badat.study1.model.User user = (com.badat.study1.model.User) principal;
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("email", user.getEmail());
                userInfo.put("role", user.getRole().name());
                userInfo.put("status", user.getStatus().name());
                userInfo.put("authorities", auth.getAuthorities());
                
                return ResponseEntity.ok(userInfo);
            } else {
                // Fallback for other types of principals
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("username", auth.getName());
                userInfo.put("authorities", auth.getAuthorities());
                
                return ResponseEntity.ok(userInfo);
            }
            
        } catch (Exception e) {
            log.error("Get current user failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Có lỗi xảy ra khi lấy thông tin người dùng"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Authorization header không hợp lệ"));
            }
            
            String refreshToken = authHeader.replace("Bearer ", "");
            LoginResponse loginResponse = authenticationService.refreshToken(refreshToken);
            
            log.info("Token refresh successful");
            return ResponseEntity.ok(loginResponse);
            
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Refresh token không hợp lệ hoặc đã hết hạn"));
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

