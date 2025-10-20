package com.badat.study1.configuration;

import com.badat.study1.service.CustomOAuth2UserService;
import com.badat.study1.service.JwtService;
import com.badat.study1.service.UserDetailServiceCustomizer;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {

    private static final String[] AUTH_WHITELIST = {"/", "/index", "/home", 
            "/products/**", 
            "/product/**", 
            "/cart", 
            "/auth/**", 
            "/api/auth/login", "/api/auth/register", "/api/auth/forgot-password", "/api/auth/verify-otp",
            "/api/cart/test", // Thêm test endpoint vào whitelist
            "/users/**", 
            "/login", "/register", "/verify-otp", "/forgot-password", 
            "/seller/register", 
            "/terms", "/faqs", 
            "/css/**", "/js/**", "/images/**", "/static/**", "/favicon.ico",
            "/stall-image/**",
            "/oauth2/**", "/login/oauth2/**",
            "/error", // Thêm /error vào whitelist để tránh authentication loop
            "/admin-simple", "/admin/test-withdraw", "/api/admin/withdraw/requests-simple", "/api/admin/withdraw/approve-simple/**", "/api/admin/withdraw/reject-simple/**"};

    private static final String[] API_PROTECTED_PATHS = {"/api/profile/**", "/api/auth/me", "/api/auth/logout", "/api/auth/refresh", "/api/cart/**"};

    private final UserDetailServiceCustomizer userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtService jwtService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring SecurityFilterChain with OAuth2 support");

        http
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .requestMatchers(API_PROTECTED_PATHS).authenticated()
                        .requestMatchers("/seller/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/withdraw").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/withdraw/**").hasAnyRole("SELLER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oauth2SuccessHandler())
                        .failureHandler(oauth2FailureHandler())
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            log.info("Authentication entry point triggered for: {}", req.getRequestURI());
                            res.sendRedirect("/login");
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
        // Add JWT filter before default authentication
        http.addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        log.info("SecurityFilterChain configured successfully");
        return http.build();
    }


    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler() {
        return (request, response, authentication) -> {
            try {
                log.info("OAuth2 Success Handler called - Principal type: {}",
                        authentication.getPrincipal().getClass().getSimpleName());

                if (authentication.getPrincipal() instanceof CustomOAuth2UserService.CustomOAuth2User) {
                    CustomOAuth2UserService.CustomOAuth2User oauth2User = (CustomOAuth2UserService.CustomOAuth2User) authentication.getPrincipal();

                    log.info("OAuth2 user authenticated: {}", oauth2User.getUser().getEmail());

                    // Generate JWT token
                    String accessToken = jwtService.generateAccessToken(oauth2User.getUser());

                    // Set JWT token as cookie
                    String cookieValue = "accessToken=" + accessToken + "; Path=/; Max-Age=3600; SameSite=Lax";
                    if (request.isSecure()) {
                        cookieValue += "; Secure";
                    }
                    response.setHeader("Set-Cookie", cookieValue);

                    // Also set in localStorage via JavaScript
                    response.sendRedirect("/?login=success&token=" + accessToken);
                } else {
                    log.warn("OAuth2 authentication failed - invalid user type: {}",
                            authentication.getPrincipal().getClass().getSimpleName());
                    response.sendRedirect("/login?error=oauth2_failed");
                }
            } catch (Exception e) {
                log.error("Error processing OAuth2 success", e);
                response.sendRedirect("/login?error=oauth2_failed");
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler oauth2FailureHandler() {
        return (request, response, exception) -> {
            log.warn("OAuth2 authentication failed: {}", exception.getMessage());
            response.sendRedirect("/login?error=oauth2_failed");
        };
    }

}