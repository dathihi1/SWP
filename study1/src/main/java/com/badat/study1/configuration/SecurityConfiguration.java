package com.badat.study1.configuration;

import com.badat.study1.service.CustomOAuth2UserService;
import com.badat.study1.service.UserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {

    private static final String[] AUTH_WHITELIST = {"/", "/index", "/home", 
            "/products/**", 
            "/product/**", 
            "/api/products/*/sub-products", 
            "/cart", 
            "/auth/**", 
            "/api/auth/login", "/api/auth/register", "/api/auth/forgot-password", "/api/auth/verify-otp", "/api/auth/verify-register-otp", "/api/auth/verify-forgot-password-otp", "/api/auth/reset-password", "/api/auth/check-otp-lockout", "/api/auth/captcha/**",
            "/api/cart/test", // Thêm test endpoint vào whitelist
            "/users/**", 
            "/login", "/register", "/verify-otp", "/forgot-password", "/reset-password", 
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
    private final UserActivityLogService userActivityLogService;
    private final ApiCallLogFilter apiCallLogFilter;
    private final IpBlockingFilter ipBlockingFilter;
    private final JwtService jwtService;
    private final AdminAuthenticationSuccessHandler adminAuthenticationSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

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
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(adminAuthenticationSuccessHandler)
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(customAuthorizationRequestResolver())
                        )
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
        // Add IP blocking filter FIRST - chặn IP bị lock trước khi request đến controller/security layers
        // Điều này giúp tiết kiệm tài nguyên server vì không cần xử lý request ở các layer sau
        http.addFilterBefore(ipBlockingFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        
        // Add API call logging filter
        http.addFilterBefore(apiCallLogFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        
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

                    // Log OAuth2 login activity
                    try {
                        String ipAddress = getClientIpAddress(request);
                        String userAgent = request.getHeader("User-Agent");
                        userActivityLogService.logLogin(oauth2User.getUser(), ipAddress, userAgent, 
                            request.getRequestURI(), request.getMethod(), true, null);
                    } catch (Exception e) {
                        log.warn("Failed to log OAuth2 login activity: {}", e.getMessage());
                    }

                    // Generate JWT token
                    String accessToken = jwtService.generateAccessToken(oauth2User.getUser());

                    // Set JWT token as cookie
                    String cookieValue = "accessToken=" + accessToken + "; Path=/; Max-Age=3600; SameSite=Lax";
                    if (request.isSecure()) {
                        cookieValue += "; Secure";
                    }
                    response.setHeader("Set-Cookie", cookieValue);

                    try {
                        // Check if user is admin and redirect accordingly
                        if (oauth2User.getUser().getRole().name().equals("ADMIN")) {
                            response.sendRedirect("/admin");
                        } else {
                            response.sendRedirect("/?login=success");
                        }
                        
                        // Log OAuth2 login to audit
                        String ip = request.getHeader("X-Forwarded-For");
                        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                            ip = request.getHeader("X-Real-IP");
                        }
                        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                            ip = request.getRemoteAddr();
                        }
                        String ua = request.getHeader("User-Agent");
                        // Defer to AuditLogService via ApplicationContext if available is complicated here; simply attach to JWT and log elsewhere.
                        // For now, set headers for a downstream filter/service to log.
                        response.setHeader("X-Auth-Logged", "oauth2");
                        response.setHeader("X-Client-IP", ip);
                        if (ua != null) response.setHeader("X-Client-UA", ua);
                    } catch (Exception e) {
                        log.error("Error in OAuth2 success handler: {}", e.getMessage());
                        try {
                            response.sendRedirect("/login?error=oauth2_redirect_failed");
                        } catch (Exception redirectError) {
                            log.error("Failed to redirect after OAuth2 error: {}", redirectError.getMessage());
                        }
                    }
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
    public OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver resolver = 
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(
            customizer -> customizer.additionalParameters(params -> 
                params.put("prompt", "consent")
            )
        );
        return resolver;
    }

    @Bean
    public AuthenticationFailureHandler oauth2FailureHandler() {
        return (request, response, exception) -> {
            log.warn("OAuth2 authentication failed: {}", exception.getMessage());
            try {
                String errorMessage = "oauth2_failed";
                
                // Check if it's an email duplication error
                if (exception.getMessage() != null && 
                    exception.getMessage().contains("đã được sử dụng cho tài khoản đăng ký thủ công")) {
                    errorMessage = "email_already_used_local";
                }
                
                response.sendRedirect("/login?error=" + errorMessage);
            } catch (Exception e) {
                log.error("Error redirecting after OAuth2 failure: {}", e.getMessage());
            }
        };
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

}