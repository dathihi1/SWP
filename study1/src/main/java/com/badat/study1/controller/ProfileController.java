package com.badat.study1.controller;

import com.badat.study1.dto.request.ProfileUpdateRequest;
import com.badat.study1.dto.request.ChangePasswordRequest;
import com.badat.study1.dto.response.ApiResponse;
import com.badat.study1.dto.response.ProfileResponse;
import com.badat.study1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
        try {
            String username = authentication.getName();
            ProfileResponse profile = userService.getProfile(username);
            return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
        } catch (Exception e) {
            log.error("Error getting profile: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get profile: " + e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            ProfileResponse updatedProfile = userService.updateProfile(username, request);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedProfile));
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update profile: " + e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteProfile(Authentication authentication) {
        try {
            String username = authentication.getName();
            userService.deleteProfile(username);
            return ResponseEntity.ok(ApiResponse.success("Profile deactivated successfully", null));
        } catch (Exception e) {
            log.error("Error deleting profile: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete profile: " + e.getMessage()));
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfileByUsername(@PathVariable String username) {
        try {
            ProfileResponse profile = userService.getProfile(username);
            return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
        } catch (Exception e) {
            log.error("Error getting profile by username: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get profile: " + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            userService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
        } catch (RuntimeException e) {
            if ("Mật khẩu không đúng".equals(e.getMessage())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu không đúng"));
            }
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi đổi mật khẩu"));
        }
    }
}
