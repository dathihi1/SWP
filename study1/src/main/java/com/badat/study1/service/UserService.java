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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    // Temporary storage for OTP and registration data
    private final Map<String, String> otpStorage = new HashMap<>();
    private final Map<String, UserCreateRequest> pendingRegistrations = new HashMap<>();
    
    // Temporary storage for forgot password OTP
    private final Map<String, String> forgotPasswordOtpStorage = new HashMap<>();

    public UserService(UserRepository userRepository, WalletRepository walletRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.emailService = emailService;
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

        // Generate OTP
        String otp = generateOTP();
        
        // Store OTP and registration data temporarily
        otpStorage.put(request.getEmail(), otp);
        pendingRegistrations.put(request.getEmail(), request);
        
        // Send OTP via email asynchronously
        sendOTPAsync(request.getEmail(), otp, "kích hoạt tài khoản");
        
        log.info("Registration OTP queued for email: {}", request.getEmail());
    }
    
    public void verify(String email, String otp) {
        // Check if OTP exists and matches
        String storedOtp = otpStorage.get(email);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }
        
        // Get pending registration data
        UserCreateRequest request = pendingRegistrations.get(email);
        if (request == null) {
            throw new RuntimeException("Không tìm thấy thông tin đăng ký");
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
        pendingRegistrations.remove(email);
        
        log.info("User account activated for email: {}", email);
    }
    
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
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

    // Forgot password methods
    public void sendForgotPasswordOtp(String email) {
        // Check if user exists
        Optional<User> userOpt = userRepository.findByEmailAndIsDeleteFalse(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }
        
        // Generate OTP
        String otp = generateOTP();
        
        // Store OTP temporarily
        forgotPasswordOtpStorage.put(email, otp);
        
        // Send OTP via email asynchronously
        sendForgotPasswordOTPAsync(email, otp);
        
        log.info("Forgot password OTP queued for email: {}", email);
    }
    
    public void verifyForgotPasswordOtp(String email, String otp) {
        // Check if OTP exists and matches
        String storedOtp = forgotPasswordOtpStorage.get(email);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }
        
        // Check if user exists
        Optional<User> userOpt = userRepository.findByEmailAndIsDeleteFalse(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }
        
        log.info("Forgot password OTP verified for email: {}", email);
    }
    
    public void resetPassword(String email, String otp, String newPassword, String repassword) {
        // Validate passwords match
        if (!newPassword.equals(repassword)) {
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }
        
        // Validate password strength
        if (newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        
        // Check if OTP exists and matches
        String storedOtp = forgotPasswordOtpStorage.get(email);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
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
        
        // Clean up OTP
        forgotPasswordOtpStorage.remove(email);
        
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
    
    @Async
    public void sendForgotPasswordOTPAsync(String email, String otp) {
        try {
            String subject = "Mã OTP đặt lại mật khẩu";
            String body = "Mã OTP để đặt lại mật khẩu của bạn là: " + otp + 
                         "\nMã này sẽ hết hạn sau 10 phút." +
                         "\nNếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.";
            emailService.sendEmail(email, subject, body);
            log.info("Forgot password OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send forgot password OTP email to {}: {}", email, e.getMessage());
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
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "avatar_" + userId + "_" + System.currentTimeMillis() + fileExtension;
        
        // Use external directory for file storage (outside classpath)
        String projectRoot = System.getProperty("user.dir");
        String profileDir = projectRoot + File.separator + "uploads" + File.separator + "avatars";
        log.info("Using external profile directory: {}", profileDir);
        
        log.info("Final profile directory: {}", profileDir);
        log.info("Directory exists: {}", new File(profileDir).exists());
        
        // Create profile directory if it doesn't exist
        File directory = new File(profileDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("Created profile directory: {}", profileDir);
            } else {
                log.warn("Failed to create profile directory: {}", profileDir);
            }
        }
        
        // Save file to filesystem
        String filePath = profileDir + File.separator + filename;
        File avatarFile = new File(filePath);
        log.info("Attempting to save file to: {}", filePath);
        log.info("File parent directory exists: {}", avatarFile.getParentFile().exists());
        log.info("File parent directory writable: {}", avatarFile.getParentFile().canWrite());
        log.info("File size: {} bytes", file.getSize());
        
        try {
            file.transferTo(avatarFile);
            log.info("Avatar file saved successfully: {}", filePath);
            log.info("File exists after save: {}", avatarFile.exists());
            log.info("File size after save: {} bytes", avatarFile.length());
        } catch (Exception e) {
            log.error("Failed to save avatar file: {}", e.getMessage());
            log.error("Exception details: ", e);
            throw new IOException("Failed to save avatar file: " + e.getMessage(), e);
        }
        
        // Update user with file path instead of byte data
        user.setAvatarUrl("/uploads/avatars/" + filename);
        user.setAvatarData(null); // Clear byte data when using file storage
        userRepository.save(user);
        
        log.info("Avatar uploaded for user ID: {} to file: {}", userId, filename);
        log.info("Avatar URL set to: {}", user.getAvatarUrl());
    }
    
    public byte[] getAvatar(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return getDefaultAvatar();
            }

            User user = userOpt.get();

            // Priority: avatarUrl (file) > avatarData (legacy) > default avatar
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                // Use external directory path
                String projectRoot = System.getProperty("user.dir");
                String filePath = projectRoot + File.separator + "uploads" + File.separator + "avatars" + File.separator + new File(user.getAvatarUrl()).getName();
                File avatarFile = new File(filePath);
                if (avatarFile.exists()) {
                    return Files.readAllBytes(avatarFile.toPath());
                } else {
                    log.warn("Avatar file not found: {}", filePath);
                }
            }

            // Legacy: check avatarData
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
        
        // Delete physical file if exists
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            // Use external directory path
            String projectRoot = System.getProperty("user.dir");
            String filePath = projectRoot + File.separator + "uploads" + File.separator + "avatars" + File.separator + new File(user.getAvatarUrl()).getName();
            File avatarFile = new File(filePath);
            if (avatarFile.exists()) {
                if (avatarFile.delete()) {
                    log.info("Avatar file deleted: {}", filePath);
                } else {
                    log.warn("Failed to delete avatar file: {}", filePath);
                }
            }
        }
        
        // Clear database references
        user.setAvatarData(null);
        user.setAvatarUrl(null);
        userRepository.save(user);
        
        log.info("Avatar deleted for user ID: {}", userId);
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