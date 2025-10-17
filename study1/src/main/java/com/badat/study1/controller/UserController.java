package com.badat.study1.controller;

import com.badat.study1.dto.request.UserCreateRequest;
import com.badat.study1.dto.request.VerifyRequest;
import com.badat.study1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/users/register")
    public ResponseEntity<String> registerUser(@RequestBody UserCreateRequest request) {
        try {
            userService.register(request);
            return ResponseEntity.ok("Registered successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/users/verify")
    public ResponseEntity<String> verifyUser(@RequestBody VerifyRequest request) {
        try {
            userService.verify(request.getEmail(), request.getOtp());
            return ResponseEntity.ok("User verified successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}