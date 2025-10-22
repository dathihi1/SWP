package com.badat.study1.service;

import com.badat.study1.dto.request.ProfileUpdateRequest;
import com.badat.study1.dto.request.UpdateProfileRequest;
import com.badat.study1.dto.request.UserCreateRequest;
import com.badat.study1.dto.response.ProfileResponse;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Temporary storage for OTP and registration data (legacy - not used anymore)
    private final Map<String, String> otpStorage = new HashMap<>();

    public UserService(UserRepository userRepository, WalletRepository walletRepository, EmailService emailService, OtpService otpService, RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.emailService = emailService;
        this.otpService = otpService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public void register(UserCreateRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }

        // Store pending registration data in Redis
        String registrationKey = "pending_registration:" + request.getEmail();
        redisTemplate.opsForValue().set(registrationKey, request, 10, java.util.concurrent.TimeUnit.MINUTES);
        
        // Use OtpService to send OTP with rate limiting
        otpService.sendOtp(request.getEmail(), "register");
        
        log.info("Registration OTP queued for email: {}", request.getEmail());
    }
    
    public void verify(String email, String otp) {
        // Use OtpService to verify OTP with attempt tracking
        boolean isValid = otpService.verifyOtp(email, otp, "register");
        if (!isValid) {
            throw new RuntimeException("Mã OTP không hợp lệ, đã hết hạn hoặc đã vượt quá số lần thử (5 lần)");
        }
        
        // Get pending registration data from Redis
        String registrationKey = "pending_registration:" + email;
        Object rawData = redisTemplate.opsForValue().get(registrationKey);
        if (rawData == null) {
            throw new RuntimeException("Không tìm thấy thông tin đăng ký hoặc đã hết hạn");
        }
        
        log.info("Raw data type: {}", rawData.getClass().getName());
        log.info("Raw data content: {}", rawData);
        
        UserCreateRequest request = convertToUserCreateRequest(rawData);
        if (request == null) {
            throw new RuntimeException("Dữ liệu đăng ký không hợp lệ");
        }
        
        // Create user with ACTIVE status
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername()) // Dùng username từ form
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .provider("LOCAL") // Đánh dấu là đăng ký manual
                .build();
        
        // Set isDelete explicitly (since it's inherited from BaseEntity)
        user.setIsDelete(false);
        
        // Save user - JPA Auditing will automatically set:
        // - createdAt: current timestamp
        // - createdBy: current authenticated user or "SYSTEM"
        // - updatedAt: current timestamp
        userRepository.save(user);
        
        log.info("User created and activated successfully: {} with audit fields - createdBy: {}, createdAt: {}", 
                user.getUsername(), user.getCreatedBy(), user.getCreatedAt());
        
        // Create wallet for user
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .balance(BigDecimal.ZERO)
                .build();
        walletRepository.save(wallet);
        
        // Clean up OTP and pending registration
        otpStorage.remove(email);
        // Clean up temporary data from Redis
        redisTemplate.delete(registrationKey);
        
        log.info("User account activated for email: {}", email);
    }
    
    
    // Profile CRUD operations
    public ProfileResponse getProfile(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        Optional<Wallet> walletOpt = walletRepository.findByUserId(user.getId());
        BigDecimal walletBalance = walletOpt.map(Wallet::getBalance).orElse(BigDecimal.ZERO);
        
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .walletBalance(walletBalance)
                .totalOrders(0L); // TODO: Implement actual count from orders
        
        // Only include shop and sales data for ADMIN role
        if (user.getRole() == User.Role.ADMIN) {
            builder.totalShops(0L); // TODO: Implement actual count from shops
            builder.totalSales(0L); // TODO: Implement actual count from sales
        } else {
            builder.totalShops(0L);
            builder.totalSales(0L);
        }
        
        return builder.build();
    }
    
    public ProfileResponse updateProfile(String username, ProfileUpdateRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        // Update profile fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        // Save user - JPA Auditing will automatically set:
        // - updatedAt: current timestamp
        userRepository.save(user);
        
        log.info("Profile updated for user: {} - updatedAt: {}", 
                username, user.getUpdatedAt());
        
        // Return updated profile
        return getProfile(username);
    }
    
    public void deleteProfile(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        user.setIsDelete(true);
        
        // Set deletedBy manually (since this is a soft delete operation)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deletedBy = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) 
                          ? auth.getName() : "SYSTEM";
        user.setDeletedBy(deletedBy);
        
        // Save user - JPA Auditing will automatically set:
        // - updatedAt: current timestamp
        userRepository.save(user);
        
        log.info("Profile soft deleted for user: {} - deletedBy: {}, updatedAt: {}", 
                username, user.getDeletedBy(), user.getUpdatedAt());
    }
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    // Change password
    public void changePassword(String username, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", username);
    }
    
    // Change password by user ID
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for user ID: {}", userId);
    }

    // Forgot password methods
    public void sendForgotPasswordOtp(String email) {
        // Check if user exists
        Optional<User> userOpt = userRepository.findByEmailAndIsDeleteFalse(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }
        
        // Use OtpService to send OTP with rate limiting
        otpService.sendOtp(email, "forgot_password");
        
        log.info("Forgot password OTP queued for email: {}", email);
    }
    
    public void verifyForgotPasswordOtp(String email, String otp) {
        // Use OtpService to verify OTP with attempt tracking
        boolean isValid = otpService.verifyOtp(email, otp, "forgot_password");
        if (!isValid) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }
        
        // Check if user exists
        Optional<User> userOpt = userRepository.findByEmailAndIsDeleteFalse(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }
        
        log.info("Forgot password OTP verified for email: {}", email);
    }
    
    public void resetPassword(String email, String resetToken, String newPassword) {
        // Validate password strength
        if (newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        
        // Validate reset token
        boolean isValidToken = otpService.validateResetToken(resetToken, email);
        if (!isValidToken) {
            throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn");
        }
        
        // Find user
        Optional<User> userOpt = userRepository.findByEmailAndIsDeleteFalse(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }
        
        // Update password
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Invalidate reset token
        otpService.invalidateResetToken(resetToken, email);
        
        log.info("Password reset successfully for email: {}", email);
    }
    
    // New method for updating profile with validation
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        // Update profile fields with validation
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }
        
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }
        
        // Save user - JPA Auditing will automatically set updatedAt
        userRepository.save(user);
        
        log.info("Profile updated for user ID: {} - updatedAt: {}", 
                userId, user.getUpdatedAt());
        
        return user;
    }
    
    // Async methods for sending emails
    @Async
    public void sendOTPAsync(String email, String otp, String purpose) {
        try {
            String subject = "Mã OTP " + purpose;
            String body = "Mã OTP để " + purpose + " của bạn là: " + otp + 
                         "\nMã này sẽ hết hạn sau 10 phút." +
                         "\nVui lòng nhập mã này để hoàn tất " + purpose + ".";
            emailService.sendEmail(email, subject, body);
            log.info("OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
        }
    }
    

    // Avatar management methods
    @Transactional
    public void uploadAvatar(Long userId, MultipartFile file) throws IOException {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        // Convert file to byte array
        byte[] avatarBytes = file.getBytes();
        
        // Update user with byte data
        user.setAvatarData(avatarBytes);
        userRepository.save(user);
        
        log.info("Avatar uploaded for user ID: {} as byte array ({} bytes)", userId, avatarBytes.length);
    }
    
    public byte[] getAvatar(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return getDefaultAvatar();
            }

            User user = userOpt.get();

            // Use avatarData (byte array) or default avatar
            if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
                return user.getAvatarData();
            }

            return getDefaultAvatar();
        } catch (Exception e) {
            log.error("Error getting avatar for user {}: {}", userId, e.getMessage());
            return getDefaultAvatar();
        }
    }
    
    @Transactional
    public void deleteAvatar(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        // Clear database references (byte data only)
        user.setAvatarData(null);
        userRepository.save(user);
        
        log.info("Avatar deleted for user ID: {} (byte data cleared)", userId);
    }
    
    private UserCreateRequest convertToUserCreateRequest(Object rawData) {
        try {
            if (rawData instanceof UserCreateRequest) {
                return (UserCreateRequest) rawData;
            }
            
            if (rawData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) rawData;
                
                // Handle type conversion issues
                UserCreateRequest request = new UserCreateRequest();
                request.setEmail((String) map.get("email"));
                request.setUsername((String) map.get("username"));
                request.setPassword((String) map.get("password"));
                
                return request;
            }
            
            // Try direct conversion with safe type handling
            try {
                return objectMapper.convertValue(rawData, UserCreateRequest.class);
            } catch (Exception e) {
                log.error("ObjectMapper conversion failed, trying manual conversion: {}", e.getMessage());
                
                // Fallback: manual conversion
                if (rawData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) rawData;
                    UserCreateRequest request = new UserCreateRequest();
                    request.setEmail(convertToString(map.get("email")));
                    request.setUsername(convertToString(map.get("username")));
                    request.setPassword(convertToString(map.get("password")));
                    return request;
                }
                
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to convert raw data to UserCreateRequest: {}", e.getMessage());
            log.error("Raw data type: {}", rawData != null ? rawData.getClass().getName() : "null");
            log.error("Raw data content: {}", rawData);
            return null;
        }
    }
    
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }
    
    public byte[] getDefaultAvatar() {
        try {
            Resource resource = new ClassPathResource("static/images/default-avatar.svg");
            InputStream inputStream = resource.getInputStream();
            return inputStream.readAllBytes();
        } catch (IOException e) {
            log.error("Error loading default avatar: {}", e.getMessage());
            // Return a simple SVG as fallback
            String svgContent = "<svg width=\"100\" height=\"100\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\"><circle cx=\"50\" cy=\"50\" r=\"50\" fill=\"#e9ecef\"/><circle cx=\"50\" cy=\"35\" r=\"15\" fill=\"#6c757d\"/><path d=\"M20 80 Q20 65 35 65 L65 65 Q80 65 80 80 L80 85 L20 85 Z\" fill=\"#6c757d\"/></svg>";
            return svgContent.getBytes();
        }
    }
}