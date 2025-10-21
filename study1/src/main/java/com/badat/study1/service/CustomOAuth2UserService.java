package com.badat.study1.service;

import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CustomOAuth2UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("CustomOAuth2UserService.loadUser called - Registration ID: {}", 
                userRequest.getClientRegistration().getRegistrationId());
        
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        log.info("OAuth2User loaded from Google - Email: {}", (String) oAuth2User.getAttribute("email"));
        
        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        if (!"google".equals(registrationId)) {
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
        }

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String googleId = (String) attributes.get("id");
        String picture = (String) attributes.get("picture"); // Google avatar URL
        
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Tìm user theo email
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            
            // Kiểm tra nếu user đã đăng ký bằng cách thủ công (provider = LOCAL)
            if ("LOCAL".equals(user.getProvider())) {
                // Cập nhật thông tin để hỗ trợ Google login
                user.setProvider("GOOGLE");
                user.setProviderId(googleId);
                user.setFullName(name);
                // Temporarily comment out to avoid database issues
                // if (picture != null && !picture.isEmpty()) {
                //     user.setAvatarUrl(picture);
                // }
                userRepository.save(user);
                log.info("Updated existing LOCAL user {} to support Google login", email);
            } else if ("GOOGLE".equals(user.getProvider()) && googleId.equals(user.getProviderId())) {
                // User đã đăng nhập bằng Google trước đó - cập nhật avatar nếu chưa có
                // Temporarily comment out to avoid database issues
                // if ((user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) && picture != null && !picture.isEmpty()) {
                //     user.setAvatarUrl(picture);
                //     userRepository.save(user);
                //     log.info("Updated avatar URL for existing Google user {}", email);
                // }
                log.info("Existing Google user {} logged in", email);
            } else {
                // Email đã được sử dụng bởi provider khác
                throw new OAuth2AuthenticationException("Email " + email + " is already associated with another account");
            }
        } else {
            // Tạo user mới
            user = createNewGoogleUser(email, name, googleId, picture);
            log.info("Created new Google user: {}", email);
        }

        return new CustomOAuth2User(
            user.getUsername(),
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
            attributes,
            user
        );
    }

    private User createNewGoogleUser(String email, String name, String googleId, String picture) {
        // Username = Email (theo yêu cầu)
        String username = email;
        
        // Đảm bảo username là unique (nếu email đã tồn tại)
        String originalUsername = username;
        int counter = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = originalUsername + "_" + counter;
            counter++;
        }

        // Tạo password random và hash
        String randomPassword = UUID.randomUUID().toString();
        
        User newUser = User.builder()
                .username(username)
                .email(email)
                .fullName(name)
                .password(passwordEncoder.encode(randomPassword))
                .provider("GOOGLE")
                .providerId(googleId)
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();
        
        // Set avatar URL after building to avoid issues with new fields
        // Temporarily comment out to avoid database issues
        // if (picture != null && !picture.isEmpty()) {
        //     newUser.setAvatarUrl(picture);
        // }

        return userRepository.save(newUser);
    }

    public static class CustomOAuth2User implements OAuth2User {
        private final String username;
        private final String password;
        private final java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities;
        private final Map<String, Object> attributes;
        private final User user;

        public CustomOAuth2User(String username, String password, 
                               java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities,
                               Map<String, Object> attributes, User user) {
            this.username = username;
            this.password = password;
            this.authorities = authorities;
            this.attributes = attributes;
            this.user = user;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getName() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public User getUser() {
            return user;
        }
    }
}
