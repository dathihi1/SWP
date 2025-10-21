package com.badat.study1.configuration;

import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.service.JwtService;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Collections;
import java.util.Optional;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip JWT filter for static resources and public endpoints
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        log.info("JWT Filter - Request to: {}, Token found: {}", request.getRequestURI(), token != null);
        
        if (token != null) {
            log.info("JWT Filter - Token: {}", token.substring(0, Math.min(50, token.length())) + "...");
            log.info("JWT Filter - Token length: {}", token.length());
        } else {
            log.warn("JWT Filter - No token found for request: {}", request.getRequestURI());
        }
        
        if (token != null) {
            try {
                String username = jwtService.extractUsername(token);
                log.info("JWT Filter - Extracted username: {}", username);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Check if user exists and get user details
                    Optional<User> userOpt = userRepository.findByUsername(username);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        
                        // For Google users, create authentication directly without password validation
                        if ("GOOGLE".equals(user.getProvider())) {
                            try {
                                if (jwtService.verifyToken(token)) {
                                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                            user,
                                            null,
                                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                                    );
                                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                    SecurityContextHolder.getContext().setAuthentication(authentication);
                                    log.info("JWT Filter - Google user {} authenticated successfully", username);
                                } else {
                                    log.warn("JWT Filter - Token validation failed for Google user: {}", username);
                                }
                            } catch (Exception e) {
                                log.warn("JWT Filter - Token validation exception for Google user {}: {}", username, e.getMessage());
                            }
                        } else {
                            // For LOCAL users, use UserDetailsService
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            if (jwtService.isTokenValid(token, userDetails)) {
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                log.info("JWT Filter - Local user {} authenticated successfully", username);
                            } else {
                                log.warn("JWT Filter - Token validation failed for local user: {}", username);
                            }
                        }
                    } else {
                        log.warn("JWT Filter - User not found: {}", username);
                    }
                }
            } catch (Exception e) {
                log.warn("JWT authentication failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // Try Authorization header first
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            log.info("Token found in Authorization header");
            return auth.substring(7);
        }
        
        // Try cookie as fallback
        Cookie[] cookies = request.getCookies();
        log.info("Cookies count: {}", cookies != null ? cookies.length : 0);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info("Cookie: {} = {}", cookie.getName(), cookie.getValue() != null ? cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "..." : "null");
                if ("accessToken".equals(cookie.getName())) {
                    log.info("Token found in cookie");
                    return cookie.getValue();
                }
            }
        }
        
        log.warn("No token found in Authorization header or cookies");
        return null;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip JWT filter for static resources and public endpoints only
        return path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/auth/") ||           // Allow all /auth/* endpoints
               path.equals("/api/auth/login") ||      // Allow login endpoint
               path.equals("/api/auth/login-form") || // Allow form login endpoint
               path.equals("/api/auth/register") ||   // Allow register endpoint
               path.equals("/api/auth/forgot-password") || // Allow forgot password
               path.equals("/api/auth/verify-otp") ||   // Allow verify OTP
               path.startsWith("/users/") ||
               path.equals("/favicon.ico") ||
               path.equals("/login") ||
               path.equals("/logout") ||
               path.equals("/register") ||
               path.equals("/verify-otp") ||
               path.equals("/forgot-password") ||
               path.equals("/admin/withdraw-requests") ||
               path.startsWith("/api/admin/withdraw/");
    }
}


