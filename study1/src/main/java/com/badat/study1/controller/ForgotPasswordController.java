package com.badat.study1.controller;

import com.badat.study1.dto.request.ForgotPasswordRequest;
import com.badat.study1.dto.request.ResetPasswordRequest;
import com.badat.study1.dto.response.ApiResponse;
import com.badat.study1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> sendForgotPasswordOtp(@RequestBody ForgotPasswordRequest request) {
        try {
            userService.sendForgotPasswordOtp(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Mã OTP đã được gửi đến email của bạn", null));
        } catch (Exception e) {
            log.error("Error sending forgot password OTP: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể gửi mã OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@RequestBody ResetPasswordRequest request) {
        try {
            userService.verifyForgotPasswordOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(ApiResponse.success("Mã OTP hợp lệ", null));
        } catch (Exception e) {
            log.error("Error verifying OTP: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Mã OTP không hợp lệ: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request.getEmail(), request.getOtp(), 
                                   request.getNewPassword(), request.getRepassword());
            return ResponseEntity.ok(ApiResponse.success("Mật khẩu đã được đặt lại thành công", null));
        } catch (Exception e) {
            log.error("Error resetting password: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể đặt lại mật khẩu: " + e.getMessage()));
        }
    }
}
