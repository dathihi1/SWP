package com.badat.study1.service;

import com.badat.study1.dto.request.UserCreateRequest;
import com.badat.study1.dto.response.UserCreateResponse;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.WalletRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        
        if(userRepository.existsByUsername(request.getUsername())){
            throw new RuntimeException("Username already in use");
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

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
