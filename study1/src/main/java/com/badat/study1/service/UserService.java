package com.badat.study1.service;

import com.badat.study1.dto.request.UserCreateRequest;
import com.badat.study1.dto.response.UserCreateResponse;
import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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

        return UserCreateResponse.builder().
                email(user.getEmail()).
                build();
    }
}
