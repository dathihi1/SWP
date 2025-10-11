package com.badat.study1.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    private final String [] WHILE_LIST = {"/auth/**",       // Cho phép tất cả các endpoint trong /auth (login, logout)
            "/users/register",  // Cho phép đăng ký
            "/users/verify",    // Cho phép xác thực OTP
            "/",                // Trang chủ
            "/login",           // Trang đăng nhập (view)
            "/register",        // Trang đăng ký (view)
            "/verify"};           // Trang xác thực (view)}

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Vô hiệu hóa CSRF
                .authorizeHttpRequests(auth -> auth.requestMatchers(WHILE_LIST).permitAll()
                        //.requestMatchers("/auth/login").permitAll() // Cho phép tất cả truy cập vào /api/v1/auth/**
                        .anyRequest().authenticated() // Tất cả các request khác đều cần xác thực
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Cấu hình session stateless
                )
                .authenticationProvider(authenticationProvider) // Cung cấp provider xác thực
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Thêm JWT filter

        return http.build();
    }
}
