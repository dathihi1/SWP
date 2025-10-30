package com.badat.study1.service;

import com.badat.study1.dto.JwtInfo;
import com.badat.study1.dto.request.LoginRequest;
import com.badat.study1.dto.response.LoginResponse;
import com.badat.study1.model.RedisToken;
import com.badat.study1.model.User;
import com.badat.study1.repository.RedisTokenRepository;
import com.badat.study1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final JwtService jwtService;
    private final RedisTokenRepository redisTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public LoginResponse login(LoginRequest request, String ipAddress, String deviceInfo){
        // Find user by username (only active, non-deleted users)
        Optional<User> userOpt = userRepository.findByUsernameAndIsDeleteFalse(request.getUsername());
        if (userOpt.isEmpty()) {
            // Log failed attempt for non-existent user - this will be handled by UserActivityAspect
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không đúng");
        }
        
        User user = userOpt.get();
        
        // Check if account is locked
        if (loginAttemptService.isAccountLocked(user.getUsername())) {
            // Log failed attempt - this will be handled by UserActivityAspect
            throw new RuntimeException("Tài khoản đã bị khóa do quá nhiều lần đăng nhập sai. Vui lòng thử lại sau 15 phút.");
        }
        
        if (user.getStatus() == User.Status.LOCKED) {
            // Log failed attempt - this will be handled by UserActivityAspect
            throw new RuntimeException("Tài khoản đã bị khóa");
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Record failed attempt
            loginAttemptService.recordFailedLoginAttempt(user.getUsername(), ipAddress);
            // Log failed attempt - this will be handled by UserActivityAspect
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không đúng");
        }
        
        // Record successful login
        loginAttemptService.recordSuccessfulLogin(user.getUsername(), ipAddress);
        // Log successful login - this will be handled by UserActivityAspect
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        log.info("Login successful for user: {}", user.getUsername());
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public LoginResponse refreshToken(String refreshToken) throws ParseException {
        // Verify refresh token
        try {
            if (!jwtService.verifyToken(refreshToken)) {
                throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn");
            }
        } catch (Exception e) {
            throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
        
        // Extract username from refresh token
        String username = jwtService.extractUsername(refreshToken);
        
        // Find user by username (only active, non-deleted users)
        Optional<User> userOpt = userRepository.findByUsernameAndIsDeleteFalse(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User không tồn tại");
        }
        
        User user = userOpt.get();
        
        if (user.getStatus() == User.Status.LOCKED) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }
        
        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        
        log.info("Token refreshed for user: {}", username);
        
        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public void logout(String token) throws ParseException {
        JwtInfo jwtInfo = jwtService.parseToken(token);
        String jwtId = jwtInfo.getJwtId();
        Date issueTime = jwtInfo.getIssueTime();
        Date expireTime = jwtInfo.getExpireTime();
        
        // Don't blacklist already expired tokens
        if(expireTime.before(new Date())) {
            return;
        }
        
        // Store TTL in SECONDS for Redis @TimeToLive
        long ttlSeconds = (expireTime.getTime() - issueTime.getTime()) / 1000;
        if (ttlSeconds <= 0) {
            return;
        }

        RedisToken redisToken = RedisToken.builder()
                .jwtID(jwtId)
                .expirationTime(ttlSeconds)
                .build();
        
        try {
            redisTokenRepository.save(redisToken);
        } catch (Exception e) {
            log.warn("Failed to blacklist token (Redis unavailable): {}", e.getMessage());
            // Continue without throwing exception - logout should still work
        }
    }
}
