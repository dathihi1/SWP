package com.badat.study1.controller;

import com.badat.study1.dto.request.ApiResponse;
import com.badat.study1.dto.request.RegisterRequest;
import com.badat.study1.dto.request.VetifyOtpRequest;
import com.badat.study1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/users/register")
    public ApiResponse<String> registerUser(@RequestBody RegisterRequest request) {
        userService.register(request);
        return ApiResponse.<String>builder()
                .message("Register success")
                .build();
    }

    @PostMapping("/users/verify")
    public ApiResponse<String> verifyUser(@RequestBody VetifyOtpRequest request) {
        userService.verify(request);
        return ApiResponse.<String>builder()
                .message("Verify success")
                .build();
    }
}