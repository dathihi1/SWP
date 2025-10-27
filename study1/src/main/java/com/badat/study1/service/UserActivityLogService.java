package com.badat.study1.service;

import com.badat.study1.model.User;
import com.badat.study1.model.UserActivityLog;
import com.badat.study1.repository.UserActivityLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityLogService {
    
    private final UserActivityLogRepository userActivityLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Async("activityLogExecutor")
    public void logProductView(User user, Long productId, String productName, String ipAddress, String userAgent, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("productId", productId);
            details.put("productName", productName);
            details.put("timestamp", LocalDateTime.now());
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action("VIEW_PRODUCT")
                    .category(UserActivityLog.Category.SHOPPING)
                    .entityType("Product")
                    .entityId(productId)
                    .details(toJson(details))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Product view logged for user {}: product {} (success: {})", user.getUsername(), productName, success);
            
        } catch (Exception e) {
            log.error("Error logging product view for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logCartAction(User user, String action, Long productId, int quantity, String ipAddress, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("productId", productId);
            details.put("quantity", quantity);
            details.put("timestamp", LocalDateTime.now());
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action(action)
                    .category(UserActivityLog.Category.SHOPPING)
                    .entityType("Cart")
                    .entityId(productId)
                    .details(toJson(details))
                    .ipAddress(ipAddress)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Cart action logged for user {}: {} product {} (success: {})", user.getUsername(), action, productId, success);
            
        } catch (Exception e) {
            log.error("Error logging cart action for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logOrderAction(User user, String action, Long orderId, BigDecimal amount, String ipAddress, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("orderId", orderId);
            details.put("amount", amount != null ? amount.toString() : null);
            details.put("timestamp", LocalDateTime.now());
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action(action)
                    .category(UserActivityLog.Category.ORDER)
                    .entityType("Order")
                    .entityId(orderId)
                    .details(toJson(details))
                    .ipAddress(ipAddress)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Order action logged for user {}: {} order {} (success: {})", user.getUsername(), action, orderId, success);
            
        } catch (Exception e) {
            log.error("Error logging order action for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logPaymentAction(User user, String action, Long orderId, String paymentMethod, BigDecimal amount, boolean success, String ipAddress, String endpoint, String method, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("orderId", orderId);
            details.put("paymentMethod", paymentMethod);
            details.put("amount", amount != null ? amount.toString() : null);
            details.put("success", success);
            details.put("timestamp", LocalDateTime.now());
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action(action)
                    .category(UserActivityLog.Category.PAYMENT)
                    .entityType("Payment")
                    .entityId(orderId)
                    .details(toJson(details))
                    .ipAddress(ipAddress)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Payment action logged for user {}: {} order {} success={}", 
                    user.getUsername(), action, orderId, success);
            
        } catch (Exception e) {
            log.error("Error logging payment action for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logReviewAction(User user, String action, Long productId, int rating, String ipAddress, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("productId", productId);
            details.put("rating", rating);
            details.put("timestamp", LocalDateTime.now());
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action(action)
                    .category(UserActivityLog.Category.REVIEW)
                    .entityType("Review")
                    .entityId(productId)
                    .details(toJson(details))
                    .ipAddress(ipAddress)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Review action logged for user {}: {} product {} rating={} (success: {})", 
                    user.getUsername(), action, productId, rating, success);
            
        } catch (Exception e) {
            log.error("Error logging review action for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logAccountAction(User user, String action, String details, String ipAddress, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("details", details);
            detailsMap.put("timestamp", LocalDateTime.now());
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action(action)
                    .category(UserActivityLog.Category.ACCOUNT)
                    .entityType("Account")
                    .details(toJson(detailsMap))
                    .ipAddress(ipAddress)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Account action logged for user {}: {} (success: {})", user.getUsername(), action, success);
            
        } catch (Exception e) {
            log.error("Error logging account action for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logLogin(User user, String ipAddress, String userAgent, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("timestamp", LocalDateTime.now());
            details.put("success", true);
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action("LOGIN")
                    .category(UserActivityLog.Category.ACCOUNT)
                    .entityType("Account")
                    .entityId(user.getId())
                    .details(toJson(details))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Login logged for user {}: {} (success: {})", user.getUsername(), endpoint, success);
            
        } catch (Exception e) {
            log.error("Error logging login for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logLogout(User user, String ipAddress, String userAgent, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("timestamp", LocalDateTime.now());
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action("LOGOUT")
                    .category(UserActivityLog.Category.ACCOUNT)
                    .entityType("Account")
                    .entityId(user.getId())
                    .details(toJson(details))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Logout logged for user {}: {} (success: {})", user.getUsername(), endpoint, success);
            
        } catch (Exception e) {
            log.error("Error logging logout for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logRegister(User user, String ipAddress, String userAgent, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("timestamp", LocalDateTime.now());
            details.put("email", user.getEmail());
            details.put("username", user.getUsername());
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action("REGISTER")
                    .category(UserActivityLog.Category.ACCOUNT)
                    .entityType("Account")
                    .entityId(user.getId())
                    .details(toJson(details))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Register logged for user {}: {} (success: {})", user.getUsername(), endpoint, success);
            
        } catch (Exception e) {
            log.error("Error logging register for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logOtpVerify(User user, String ipAddress, String userAgent, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("timestamp", LocalDateTime.now());
            details.put("success", true);
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action("OTP_VERIFY")
                    .category(UserActivityLog.Category.ACCOUNT)
                    .entityType("Account")
                    .entityId(user.getId())
                    .details(toJson(details))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("OTP verify logged for user {}: {} (success: {})", user.getUsername(), endpoint, success);
            
        } catch (Exception e) {
            log.error("Error logging OTP verify for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logProfileUpdate(User user, String details, String ipAddress, String userAgent, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("details", details);
            detailsMap.put("timestamp", LocalDateTime.now());
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action("PROFILE_UPDATE")
                    .category(UserActivityLog.Category.ACCOUNT)
                    .entityType("Account")
                    .entityId(user.getId())
                    .details(toJson(detailsMap))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Profile update logged for user {}: {} (success: {})", user.getUsername(), endpoint, success);
            
        } catch (Exception e) {
            log.error("Error logging profile update for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logPasswordChange(User user, String ipAddress, String userAgent, String endpoint, String method, boolean success, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("timestamp", LocalDateTime.now());
            details.put("success", true);
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action("PASSWORD_CHANGE")
                    .category(UserActivityLog.Category.ACCOUNT)
                    .entityType("Account")
                    .entityId(user.getId())
                    .details(toJson(details))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("Password change logged for user {}: {} (success: {})", user.getUsername(), endpoint, success);
            
        } catch (Exception e) {
            log.error("Error logging password change for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    @Async("activityLogExecutor")
    public void logUserActivity(User user, String action, UserActivityLog.Category category, String entityType, Long entityId, String details, String endpoint, String method, String ipAddress, String userAgent, boolean success, String failureReason) {
        try {
            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("details", details);
            detailsMap.put("timestamp", LocalDateTime.now());
            
            UserActivityLog activityLog = UserActivityLog.builder()
                    .userId(user.getId())
                    .action(action)
                    .category(category)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(toJson(detailsMap))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint(endpoint)
                    .method(method)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            
            userActivityLogRepository.save(activityLog);
            log.info("User activity logged for user {}: {} - {} (success: {})", user.getUsername(), action, endpoint, success);
            
        } catch (Exception e) {
            log.error("Error logging user activity for user {}: {}", user.getUsername(), e.getMessage());
        }
    }
    
    public Page<UserActivityLog> getUserActivities(Long userId, String action, Boolean success, 
                                                   LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        try {
            log.info("Getting user activities for user: {}, action: {}, success: {}, from: {}, to: {}", 
                    userId, action, success, fromDate, toDate);
            
            return userActivityLogRepository.findUserActivitiesWithFilters(
                userId, action, success, fromDate, toDate, pageable);
        } catch (Exception e) {
            log.error("Error getting user activities for user {}: {}", userId, e.getMessage(), e);
            return Page.empty(pageable);
        }
    }
    
    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
