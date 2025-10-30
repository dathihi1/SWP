package com.badat.study1.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String username;
    private String password;
    // captchaId is now read from HttpOnly cookie, not from request body
    private String captchaCode;    // Optional - required after 3 failed attempts
    private String captcha;        // Simple captcha from frontend (for backward compatibility)
}
