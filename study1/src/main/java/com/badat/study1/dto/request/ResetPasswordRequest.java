package com.badat.study1.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    private String email;
    private String otp;            // For verify-otp endpoint
    private String resetToken;     // For reset-password endpoint
    private String newPassword;
    private String repassword;
}
