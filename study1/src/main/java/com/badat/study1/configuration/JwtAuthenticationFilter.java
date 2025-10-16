package com.badat.study1.configuration;

import com.badat.study1.service.JwtService;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

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
        log.debug("Request to: {}, Token found: {}", request.getRequestURI(), token != null);
        
        if (token != null) {
            log.debug("Token: {}", token.substring(0, Math.min(50, token.length())) + "...");
        }
        
        if (token != null) {
            try {
                String username = jwtService.extractUsername(token);
                log.debug("Extracted username: {}", username);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("User {} authenticated successfully", username);
                    } else {
                        log.debug("Token validation failed for user: {}", username);
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
            log.debug("Token found in Authorization header");
            return auth.substring(7);
        }
        
        // Try cookie as fallback
        Cookie[] cookies = request.getCookies();
        log.debug("Cookies: {}", cookies != null ? cookies.length : 0);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.debug("Cookie: {} = {}", cookie.getName(), cookie.getValue() != null ? cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "..." : "null");
                if ("accessToken".equals(cookie.getName())) {
                    log.debug("Token found in cookie");
                    return cookie.getValue();
                }
            }
        }
        
        log.debug("No token found");
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip JWT filter for static resources and public endpoints only
        return path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.equals("/auth/login") ||
               path.equals("/auth/register") ||
               path.startsWith("/users/") ||
               path.equals("/favicon.ico") ||
               path.equals("/login") ||
               path.equals("/register") ||
               path.equals("/verify-otp");
    }
}


