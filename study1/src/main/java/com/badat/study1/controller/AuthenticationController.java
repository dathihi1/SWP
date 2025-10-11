package com.badat.study1.controller;

import com.badat.study1.dto.request.ApiResponse;
import com.badat.study1.dto.request.LoginRequest;
import com.badat.study1.dto.response.LoginResponse;
import com.badat.study1.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authenticationService.login(loginRequest);
        return ApiResponse.<LoginResponse>builder()
                .result(loginResponse)
                .build();
    }

    @PostMapping("/auth/logout")
    public ApiResponse<String> logout(@RequestHeader("Authorization") String authHeader) throws ParseException {

            String token = authHeader.replace("Bearer ", "");
            authenticationService.logout(token);
            return ApiResponse.<String>builder()
                    .message("Logout success")
                    .build();
    }


}
