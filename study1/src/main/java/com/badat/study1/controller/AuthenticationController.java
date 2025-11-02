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
import com.badat.study1.service.ResetTokenLockoutService;
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
import jakarta.servlet.http.HttpServletResponse;
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
    private final ResetTokenLockoutService resetTokenLockoutService;
    private final com.badat.study1.service.UserActivityLogService userActivityLogService;
    private final com.badat.study1.repository.UserRepository userRepository;

    @PostMapping("/login")
    @UserActivity(action = "LOGIN", category = UserActivityLog.Category.ACCOUNT)
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String username = loginRequest.getUsername();
            
            log.info("Login attempt for username: {} from IP: {}", username, ipAddress);
            
            // IP lockout check is now handled by IpBlockingFilter (removed duplicate check here)
            
            //  1. Check if IP is rate limited for captcha failures
            if (captchaRateLimitService.isCaptchaRateLimited(ipAddress)) {
                log.warn("Captcha rate limited - IP: {}", ipAddress);
                return ResponseEntity.status(429).body(Map.of(
                    "error", "Quá nhiều lần nhập sai captcha. Vui lòng thử lại sau 15 phút",
                    "captchaRateLimited", true
                ));
            }
            
            //  2. Check captcha FIRST (validate correctness) - captchaId from HttpOnly cookie
            String captchaCode = loginRequest.getCaptchaCode();
            
            // Fallback to simple captcha field for backward compatibility
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
            
            // Validate captcha using ID from HttpOnly cookie (IMAGE captcha)
            boolean captchaValid = captchaService.validateCaptcha(request, captchaCode);
            log.info("Captcha validation (from cookie): {}", captchaValid ? "valid" : "invalid");
            
            // If captcha is invalid, return immediately without checking credentials
            if (!captchaValid) {
                captchaRateLimitService.recordFailedCaptchaAttempt(ipAddress);
                securityEventService.logCaptchaRequired(ipAddress, username);
                // Generate new captcha with new cookie
                Map.Entry<CaptchaResponse, String> newCaptchaResult = captchaService.generateCaptchaWithCookie();
                ResponseCookie clearCookie = ResponseCookie.from("captcha_id", "")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();
                return ResponseEntity.status(400)
                        .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                        .header(HttpHeaders.SET_COOKIE, newCaptchaResult.getValue())
                        .body(Map.of(
                            "error", "Mã xác thực không đúng, vui lòng nhập lại",
                            "message", "captcha incorrect",
                            "captchaRequired", true,
                            "captcha", newCaptchaResult.getKey()
                        ));
            }
            
            // Clear cookie after successful validation (one-time use)
            ResponseCookie clearCookie = ResponseCookie.from("captcha_id", "")
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            
            // 🔒 3. Only if captcha is valid, then check username/password
            try {
                LoginResponse loginResponse = authenticationService.login(loginRequest, ipAddress, userAgent);
                
                // Clear IP attempts on successful login
                ipLockoutService.recordSuccessfulAttempt(ipAddress);
                captchaRateLimitService.clearCaptchaAttempts(ipAddress);
                securityEventService.logLoginAttempt(ipAddress, username, true, "Login successful", userAgent);

                // Explicit user activity log for LOGIN success (ensure it's recorded even before security context is set)
                try {
                    userRepository.findByUsername(username).ifPresent(u -> {
                        userActivityLogService.logLogin(u, ipAddress, userAgent, request.getRequestURI(), request.getMethod(), true, null);
                    });
                } catch (Exception ignore) {}
                
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
                        .header(HttpHeaders.SET_COOKIE, clearCookie.toString()) // Clear captcha cookie
                        .header(HttpHeaders.SET_COOKIE, accessCookie.toString()) // Set access token cookie
                        .body(loginResponse);
                        
            } catch (Exception e) {
                // Record failed attempt
                ipLockoutService.recordFailedAttempt(ipAddress, username);
                securityEventService.logLoginAttempt(ipAddress, username, false, e.getMessage(), userAgent);
                
                Map<String, Object> errorResponse = new HashMap<>();
                
                // Determine error type based on exception message
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("User not found")) {
                    errorResponse.put("error", "Tên đăng nhập không đúng");
                } else if (errorMessage != null && errorMessage.contains("Invalid password")) {
                    errorResponse.put("error", "Mật khẩu không đúng");
                } else {
                    errorResponse.put("error", "Tên đăng nhập hoặc mật khẩu không đúng");
                }
                
                // Always generate new captcha (image) after failed attempt
                Map.Entry<CaptchaResponse, String> newCaptchaResult = captchaService.generateCaptchaWithCookie();
                errorResponse.put("captchaRequired", true);
                errorResponse.put("message", "captcha required");
                errorResponse.put("captcha", newCaptchaResult.getKey());
                
                log.info("Generated new captcha after failed login attempt for user: {} from IP: {}", username, ipAddress);
                
                // Optionally record failed login in audit/security logs already handled above

                return ResponseEntity.status(401)
                        .header(HttpHeaders.SET_COOKIE, newCaptchaResult.getValue())
                        .body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi hệ thống"));
        }
    }
    
    @GetMapping("/captcha")
    public ResponseEntity<?> getCaptcha() {
        try {
            log.info("Generating image captcha with HttpOnly cookie...");
            Map.Entry<CaptchaResponse, String> result = captchaService.generateCaptchaWithCookie();
            CaptchaResponse captcha = result.getKey();
            String cookieHeader = result.getValue();
            
            log.info("Image captcha generated successfully (ID stored in cookie)");
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookieHeader)
                    .body(captcha);
        } catch (Exception e) {
            log.error("Failed to generate captcha: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Không thể tạo captcha"));
        }
    }
    
    /**
     * @deprecated This endpoint is deprecated. Use {@link #getCaptcha()} instead for secure image captcha.
     * Simple captcha sends the answer to frontend which is a security vulnerability.
     */
    @Deprecated
    @GetMapping("/captcha/simple")
    public ResponseEntity<?> getSimpleCaptcha() {
        try {
            log.info("Generating simple captcha...");
            Map<String, String> captchaData = captchaService.generateSimpleCaptcha();
            log.info("Simple captcha generated successfully: {}", captchaData.get("captchaId"));
            // Set HttpOnly cookie with captcha_id so server-side validation works the same
            String captchaId = captchaData.get("captchaId");
            ResponseCookie cookie = ResponseCookie.from("captcha_id", captchaId)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(5 * 60)
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(captchaData);
        } catch (Exception e) {
            log.error("Error generating simple captcha: {}", e.getMessage(), e);
            // Return a fallback captcha without Redis
            Map<String, String> fallbackCaptcha = new HashMap<>();
            fallbackCaptcha.put("captchaId", "fallback-" + System.currentTimeMillis());
            fallbackCaptcha.put("captchaText", "FALLBACK");
            return ResponseEntity.ok(fallbackCaptcha);
        }
    }

    // Registration flow: FE validates, BE validates again, send OTP async, immediately respond with nextUrl
    @PostMapping("/register")
    @UserActivity(action = "REGISTER", category = UserActivityLog.Category.ACCOUNT)
    public ResponseEntity<?> register(@RequestBody @Valid UserCreateRequest request, HttpServletRequest http, HttpServletResponse httpResponse) {
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
            
            // Validate captcha - captchaId from HttpOnly cookie
            if (request.getCaptchaCode() == null || request.getCaptchaCode().trim().isEmpty()) {
                // Record failed attempt for missing captcha
                ipLockoutService.recordFailedAttempt(ipAddress, request.getEmail() != null ? request.getEmail() : "unknown");
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_REQUIRED, ipAddress, 
                        "Registration without captcha");
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Vui lòng nhập mã xác thực",
                    "captchaRequired", true
                ));
            }
            
            // Validate captcha using ID from HttpOnly cookie (IMAGE captcha)
            boolean captchaValid = captchaService.validateCaptcha(http, request.getCaptchaCode());
            
            if (!captchaValid) {
                // Record failed attempt for invalid captcha
                ipLockoutService.recordFailedAttempt(ipAddress, request.getEmail() != null ? request.getEmail() : "unknown");
                captchaRateLimitService.recordFailedCaptchaAttempt(ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_FAILED, ipAddress, 
                        "Registration with invalid captcha");
                // Generate new captcha with new cookie
                Map.Entry<CaptchaResponse, String> newCaptchaResult = captchaService.generateCaptchaWithCookie();
                ResponseCookie clearCookie = ResponseCookie.from("captcha_id", "")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();
                return ResponseEntity.status(400)
                        .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                        .header(HttpHeaders.SET_COOKIE, newCaptchaResult.getValue())
                        .body(Map.of(
                            "error", "Mã xác thực không đúng, vui lòng nhập lại",
                            "captchaRequired", true,
                            "captcha", newCaptchaResult.getKey()
                        ));
            }
            
            // Clear cookie after successful validation
            ResponseCookie clearCookie = ResponseCookie.from("captcha_id", "")
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            
            // Add delay to prevent timing attacks
            Thread.sleep(500 + new Random().nextInt(500));
            
            // Call userService.register() - CATCH specific exceptions
            try {
                userService.register(request);
            } catch (RuntimeException e) {
                // Record failed attempt for registration errors
                String errorType = e.getMessage();
                String identifier = request.getEmail() != null ? request.getEmail() : 
                                   (request.getUsername() != null ? request.getUsername() : "unknown");
                if ("EMAIL_EXISTS".equals(errorType)) {
                    ipLockoutService.recordFailedAttempt(ipAddress, identifier);
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Email đã được sử dụng",
                        "field", "email"
                    ));
                } else if ("USERNAME_EXISTS".equals(errorType)) {
                    ipLockoutService.recordFailedAttempt(ipAddress, identifier);
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Tên đăng nhập đã tồn tại",
                        "field", "username"
                    ));
                }
                // Record failed attempt for other registration errors
                ipLockoutService.recordFailedAttempt(ipAddress, identifier);
                throw e; // Re-throw other exceptions
            }
            
            // Explicit user activity for successful registration
            try {
                String userAgent = http.getHeader("User-Agent");
                userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
                    userActivityLogService.logRegister(u, ipAddress, userAgent, http.getRequestURI(), http.getMethod(), true, null);
                });
            } catch (Exception ignore) {}

            // Record IP request
            rateLimitService.recordIpRequest(ipAddress, "register");
            
            // Log successful registration attempt
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.REGISTER_SUCCESS, 
                ipAddress, 
                "OTP sent to: " + request.getEmail()
            );
            
            // Always return success to prevent email enumeration
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(ApiResponse.success(
                        "Nếu email hợp lệ, chúng tôi đã gửi mã OTP",
                        Map.of("nextUrl", "/verify-otp?email=" + request.getEmail())
                    ));
            
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            // Record failed attempt for unexpected errors
            try {
                String ipAddress = getClientIpAddress(http);
                String identifier = request.getEmail() != null ? request.getEmail() : 
                                   (request.getUsername() != null ? request.getUsername() : "unknown");
                ipLockoutService.recordFailedAttempt(ipAddress, identifier);
            } catch (Exception ex) {
                log.error("Failed to record failed attempt: {}", ex.getMessage());
            }
            // Don't leak information
            ResponseCookie clearCookieForError = ResponseCookie.from("captcha_id", "")
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearCookieForError.toString())
                    .body(ApiResponse.success(
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
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
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
            
            // Validate captcha - captchaId from HttpOnly cookie
            if (request.getCaptchaCode() == null || request.getCaptchaCode().trim().isEmpty()) {
                // Record failed attempt for missing captcha
                ipLockoutService.recordFailedAttempt(ipAddress, request.getEmail() != null ? request.getEmail() : "unknown");
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_REQUIRED, ipAddress, 
                        "Forgot password without captcha");
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Vui lòng nhập mã xác thực",
                    "captchaRequired", true
                ));
            }
            
            // Validate captcha using ID from HttpOnly cookie (IMAGE captcha)
            log.info("Forgot password captcha validation - User input: {}", request.getCaptchaCode());
            boolean captchaValid = captchaService.validateCaptcha(httpRequest, request.getCaptchaCode());
            log.info("Forgot password captcha validation result: {}", captchaValid);
            
            if (!captchaValid) {
                // Record failed attempt for invalid captcha
                ipLockoutService.recordFailedAttempt(ipAddress, request.getEmail() != null ? request.getEmail() : "unknown");
                captchaRateLimitService.recordFailedCaptchaAttempt(ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_FAILED, ipAddress, 
                        "Forgot password with invalid captcha");
                // Generate new captcha with new cookie
                Map.Entry<CaptchaResponse, String> newCaptchaResult = captchaService.generateCaptchaWithCookie();
                ResponseCookie clearCookie = ResponseCookie.from("captcha_id", "")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();
                return ResponseEntity.status(400)
                        .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                        .header(HttpHeaders.SET_COOKIE, newCaptchaResult.getValue())
                        .body(Map.of(
                            "error", "Mã xác thực không đúng, vui lòng nhập lại",
                            "captchaRequired", true,
                            "captcha", newCaptchaResult.getKey()
                        ));
            }
            
            // Clear cookie after successful validation
            ResponseCookie clearCookie = ResponseCookie.from("captcha_id", "")
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            
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
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(Map.of(
                        "success", true,
                        "message", "Mã OTP đã được gửi đến email của bạn",
                        "nextUrl", "/verify-otp?email=" + request.getEmail() + "&type=forgot_password"
                    ));
            
        } catch (Exception e) {
            log.error("Forgot password error: {}", e.getMessage(), e);
            // Record failed attempt for unexpected errors
            try {
                String ipAddress = getClientIpAddress(httpRequest);
                String identifier = request.getEmail() != null ? request.getEmail() : "unknown";
                ipLockoutService.recordFailedAttempt(ipAddress, identifier);
            } catch (Exception ex) {
                log.error("Failed to record failed attempt: {}", ex.getMessage());
            }
            // Always return success to prevent email enumeration
            ResponseCookie clearCookieForError = ResponseCookie.from("captcha_id", "")
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearCookieForError.toString())
                    .body(Map.of(
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
            
            // Note: IP lockout check is already handled by IpBlockingFilter at filter layer
            // No need to check again here - filter will block request before reaching controller
            
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
                // Record failed attempt for IP lockout
                ipLockoutService.recordFailedAttempt(ipAddress, request.getEmail());
                
                // Check if lockout error
                if (e.getMessage().contains("nhập sai OTP quá 5 lần") || e.getMessage().contains("nhập sai OTP quá 10 lần")) {
                    return ResponseEntity.status(429).body(Map.of(
                        "error", e.getMessage(),
                        "locked", true
                    ));
                }
                throw e; // Re-throw other errors
            }
            
            // CRITICAL SECURITY: Mark OTP as verified before generating reset token
            otpService.markOtpVerified(request.getEmail(), "forgot_password");
            
            // Generate resetToken and save to Redis (will check OTP verification state)
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
            
            // Note: IP lockout check is already handled by IpBlockingFilter at filter layer
            // No need to check again here - filter will block request before reaching controller
            
            // CRITICAL SECURITY: Check reset token lockout before validating token
            if (resetTokenLockoutService.isLocked(email, ipAddress)) {
                Long remainingSeconds = resetTokenLockoutService.getLockoutTimeRemaining(email, ipAddress);
                long minutes = remainingSeconds != null ? remainingSeconds / 60 : 60;
                String errorMessage = "Bạn đã thử sai quá nhiều lần. Vui lòng thử lại sau " + minutes + " phút";
                
                log.warn("Reset password blocked - locked for email: {} from IP: {}", email, ipAddress);
                securityEventService.logSecurityEvent(
                    SecurityEvent.EventType.RESET_TOKEN_LOCKED,
                    ipAddress,
                    email,
                    "Reset password attempt blocked - account locked"
                );
                
                return ResponseEntity.status(429).body(Map.of(
                    "error", errorMessage,
                    "locked", true
                ));
            }
            
            // Log security event
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.RESET_TOKEN_VALIDATION_ATTEMPT, 
                ipAddress,
                email,
                "Reset password attempt for: " + email
            );
            
            // Validate resetToken and reset password
            // This will also check that OTP was verified
            boolean isValidToken = otpService.validateResetToken(resetToken, email);
            
            if (!isValidToken) {
                // Track failed attempt for both reset token lockout and IP lockout
                resetTokenLockoutService.recordFailedAttempt(email, ipAddress);
                ipLockoutService.recordFailedAttempt(ipAddress, email);
                
                // Log failed validation
                securityEventService.logSecurityEvent(
                    SecurityEvent.EventType.RESET_TOKEN_VALIDATION_FAILED,
                    ipAddress,
                    email,
                    "Reset token validation failed - invalid token or OTP not verified"
                );
                
                // Check if locked after this failed attempt
                if (resetTokenLockoutService.isLocked(email, ipAddress)) {
                    Long remainingSeconds = resetTokenLockoutService.getLockoutTimeRemaining(email, ipAddress);
                    long minutes = remainingSeconds != null ? remainingSeconds / 60 : 60;
                    return ResponseEntity.status(429).body(Map.of(
                        "error", "Token không hợp lệ. Bạn đã thử sai quá nhiều lần. Vui lòng thử lại sau " + minutes + " phút",
                        "locked", true
                    ));
                }
                
                return ResponseEntity.badRequest().body(ApiResponse.error("Token không hợp lệ hoặc đã hết hạn. Vui lòng thực hiện lại từ đầu."));
            }
            
            // Token is valid, proceed with password reset
            userService.resetPasswordWithToken(email, resetToken, newPassword);
            
            // Clear lockout attempts on successful reset
            resetTokenLockoutService.clearAttempts(email, ipAddress);
            
            // Log successful reset
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.PASSWORD_RESET, 
                ipAddress,
                email,
                "Password reset successfully for: " + email
            );
            
            return ResponseEntity.ok(ApiResponse.success("Mật khẩu đã được đặt lại thành công"));
                    
        } catch (Exception e) {
            String email = request.get("email");
            String ipAddress = getClientIpAddress(httpRequest);
            
            log.error("Reset password failed for email: {} from IP: {}, error: {}", 
                email, ipAddress, e.getMessage());
            
            // Track failed attempt if not already locked
            if (email != null && !resetTokenLockoutService.isLocked(email, ipAddress)) {
                resetTokenLockoutService.recordFailedAttempt(email, ipAddress);
            }
            
            // Log failed reset
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.RESET_TOKEN_VALIDATION_FAILED, 
                ipAddress,
                email,
                "Reset password failed for: " + email + ", reason: " + e.getMessage()
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

