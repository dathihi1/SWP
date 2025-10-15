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

    public void register(UserCreateRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }
        
        // Generate OTP
        String otp = generateOTP();
        
        // Store OTP and registration data temporarily
        otpStorage.put(request.getEmail(), otp);
        pendingRegistrations.put(request.getEmail(), request);
        
        // Send OTP via email
        String subject = "Verify your account";
        String body = "Your OTP is: " + otp + "\nThis OTP will expire in 10 minutes.";
        emailService.sendEmail(request.getEmail(), subject, body);
    }
    
    public void verify(String email, String otp) {
        // Check if OTP exists and matches
        String storedOtp = otpStorage.get(email);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        
        // Get pending registration data
        UserCreateRequest request = pendingRegistrations.get(email);
        if (request == null) {
            throw new RuntimeException("Registration data not found");
        }
        
        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();
        
        userRepository.save(user);
        
        // Create wallet for user
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .balance(BigDecimal.ZERO)
                .build();
        walletRepository.save(wallet);
        
        // Clean up temporary data
        otpStorage.remove(email);
        pendingRegistrations.remove(email);
    }
    
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

}