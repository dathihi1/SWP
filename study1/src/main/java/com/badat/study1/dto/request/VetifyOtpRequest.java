package com.badat.study1.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VetifyOtpRequest {
    private String email;
    private String otp;
}
