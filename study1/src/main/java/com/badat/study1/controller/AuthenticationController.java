package com.badat.study1.controller;

import com.badat.study1.dto.request.LoginRequest;
import com.badat.study1.dto.response.LoginResponse;
import com.badat.study1.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        return authenticationService.login(loginRequest);
    }

    @PostMapping("/auth/logout")
    public void logout(@RequestHeader("Authorization") String authHeader) throws ParseException {
        log.info(authHeader);
        authenticationService.logout(authHeader.replace("Bearer ", ""));
    }

    @GetMapping("/auth/debug")
    public String debugAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return "Authentication: " + (auth != null ? auth.getName() : "null");
    }

    @GetMapping("/logout")
    public String logoutPage() {
        return "redirect:/";
    }
}
