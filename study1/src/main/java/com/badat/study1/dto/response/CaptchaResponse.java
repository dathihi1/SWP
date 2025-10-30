package com.badat.study1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {
    // captchaId is now stored in HttpOnly cookie, not in response body
    private String captchaImage; // Base64 encoded image
    private int expiresIn; // Seconds
}

