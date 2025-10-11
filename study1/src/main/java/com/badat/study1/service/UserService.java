package com.badat.study1.service;

import com.badat.study1.dto.request.RegisterRequest;
import com.badat.study1.dto.request.VetifyOtpRequest;
import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;

    private final Map<String, User> unverifiedUsers = new HashMap<>();
    private final Map<String, String> otpMap = new HashMap<>();

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER) // Gán role mặc định
                .status(User.Status.ACTIVE) // Gán trạng thái mặc định
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();

        String otp = generateOtp();
        unverifiedUsers.put(request.getEmail(), user);
        otpMap.put(request.getEmail(), otp);

        emailService.sendEmail(request.getEmail(), "Your OTP Code", "Your OTP code is: " + otp);
    }

    public void verify(VetifyOtpRequest request) {
        String email = request.getEmail();
        String otp = request.getOtp();
        if (!otpMap.containsKey(email) || !otpMap.get(email).equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        User user = unverifiedUsers.get(email);
        userRepository.save(user);

        unverifiedUsers.remove(email);
        otpMap.remove(email);
    }

    // Thay thế hàm OTP cũ
    private String generateOtp() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }
}