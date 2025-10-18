package com.badat.study1.service;

import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailServiceCustomizer implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User does not exist"));

        // Kiểm tra nếu user đăng ký bằng Google mà cố đăng nhập manual
        if ("GOOGLE".equals(user.getProvider())) {
            log.warn("Attempted manual login for Google-registered user: {}", username);
            throw new BadCredentialsException("Tài khoản hoặc mật khẩu không đúng");
        }

        return user;
    }
}
