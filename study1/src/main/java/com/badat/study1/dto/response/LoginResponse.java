package com.badat.study1.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class LoginResponse {
    private String refreshToken;
    private String accessToken;
}
