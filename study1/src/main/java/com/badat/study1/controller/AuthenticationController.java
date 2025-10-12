package com.badat.study1.controller;

import com.badat.study1.dto.request.ApiResponse;
import com.badat.study1.dto.request.LoginRequest;
import com.badat.study1.dto.response.LoginResponse;
import com.badat.study1.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/auth/me")
    public ApiResponse<Object> getCurrentUser() {
        // Lấy thông tin user hiện tại từ SecurityContext
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            
            com.badat.study1.model.User user = (com.badat.study1.model.User) authentication.getPrincipal();
            
            // Tạo response object với thông tin user
            java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getRealUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole().name());
            userInfo.put("fullName", user.getFullName());
            
            return ApiResponse.builder()
                    .result(userInfo)
                    .build();
        }
        
        return ApiResponse.builder()
                .message("User not authenticated")
                .build();
    }

}
