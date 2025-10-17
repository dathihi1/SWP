package com.badat.study1.configuration;

import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create test user if not exists
        if (userRepository.findByUsername("testuser").isEmpty()) {
            User testUser = User.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .build();
            testUser.setIsDelete(false);
            
            User savedUser = userRepository.save(testUser);
            log.info("Created test user: {}", savedUser.getUsername());
            
            // Create wallet for test user
            Wallet wallet = Wallet.builder()
                    .userId(savedUser.getId())
                    .balance(BigDecimal.ZERO)
                    .build();
            walletRepository.save(wallet);
            log.info("Created wallet for test user: {}", savedUser.getUsername());
        } else {
            log.info("Test user already exists");
        }
    }
}
