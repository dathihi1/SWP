package com.badat.study1.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * FilterChain riêng cho khu vực /admin/**
 * Ưu tiên cao hơn (Order 50) để không ảnh hưởng filter chain hiện có của bạn.
 * Giả định hệ thống đã có Security chung. Ở đây chỉ thêm mới.
 */
@Configuration
public class AdminSecurityConfig {

    @Bean
    @Order(50)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/templates/admin/**", "/admin-login", "/templates/admin/logout")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin-login", "/templates/admin/logout").permitAll()
                        .requestMatchers("/templates/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/admin-login")
                        .loginProcessingUrl("/admin-login")
                        .defaultSuccessUrl("/templates/admin", true)
                        .failureUrl("/admin-login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/templates/admin/logout")
                        .logoutSuccessUrl("/admin-login?logout")
                )
                .csrf(Customizer.withDefaults());
        return http.build();
    }
}
