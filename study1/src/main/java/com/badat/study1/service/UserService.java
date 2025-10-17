package com.badat.study1.service;

import com.badat.study1.dto.request.ProfileUpdateRequest;
import com.badat.study1.dto.request.ChangePasswordRequest;
import com.badat.study1.dto.request.UserCreateRequest;
import com.badat.study1.dto.response.ProfileResponse;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Directly create user (OTP disabled)
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();
        
        // Set isDelete explicitly (since it's inherited from BaseEntity)
        user.setIsDelete(false);
        
        // Save user - JPA Auditing will automatically set:
        // - createdAt: current timestamp
        // - createdBy: current authenticated user or "SYSTEM"
        // - updatedAt: current timestamp
        userRepository.save(user);
        
        log.info("User created successfully: {} with audit fields - createdBy: {}, createdAt: {}", 
                user.getUsername(), user.getCreatedBy(), user.getCreatedAt());
        
        // Create wallet for user
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .balance(BigDecimal.ZERO)
                .build();
        walletRepository.save(wallet);
    }
    
    public void verify(String email, String otp) {
        // OTP disabled: no-op
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
        
        // Send OTP via email
        String subject = "Mã OTP đặt lại mật khẩu";
        String body = "Mã OTP để đặt lại mật khẩu của bạn là: " + otp + 
                     "\nMã này sẽ hết hạn sau 10 phút." +
                     "\nNếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.";
        emailService.sendEmail(email, subject, body);
        
        log.info("Forgot password OTP sent to email: {}", email);
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

}