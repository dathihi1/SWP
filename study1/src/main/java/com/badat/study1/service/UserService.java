package com.badat.study1.service;

import com.badat.study1.dto.request.UserCreateRequest;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.WalletRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    // Temporary storage for OTP and registration data
    private final Map<String, String> otpStorage = new HashMap<>();
    private final Map<String, UserCreateRequest> pendingRegistrations = new HashMap<>();

    public UserService(UserRepository userRepository, WalletRepository walletRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.emailService = emailService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public void register(UserCreateRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }

        // Directly create user (OTP disabled)
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();
        userRepository.save(user);

        // Create wallet
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .balance(BigDecimal.ZERO)
                .build();
        walletRepository.save(wallet);
    }
    
    public void verify(String email, String otp) {
        // OTP disabled: no-op
    }
    
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

}