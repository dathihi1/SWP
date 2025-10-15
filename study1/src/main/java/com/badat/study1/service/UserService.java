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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public UserService(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public UserCreateResponse createUser(UserCreateRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
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

        // Tạo ví cho user sau khi đăng ký thành công
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .build();
        walletRepository.save(wallet);

        return UserCreateResponse.builder().
                email(user.getEmail()).
                build();
    }
}