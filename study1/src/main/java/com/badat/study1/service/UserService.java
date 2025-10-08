package com.badat.study1.service;

import com.badat.study1.dto.request.UserCreateRequest;
import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;

    private final Map<String, User> unverifiedUsers = new HashMap<>();
    private final Map<String, String> otpMap = new HashMap<>();

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public void register(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        String otp = generateOtp();
        unverifiedUsers.put(request.getEmail(), user);
        otpMap.put(request.getEmail(), otp);

        emailService.sendEmail(request.getEmail(), "Your OTP Code", "Your OTP code is: " + otp);
    }

    public void verify(String email, String otp) {
        if (!otpMap.containsKey(email) || !otpMap.get(email).equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        User user = unverifiedUsers.get(email);
        userRepository.save(user);

        unverifiedUsers.remove(email);
        otpMap.remove(email);
    }

    private String generateOtp() {
        return "9999999";
    }
}