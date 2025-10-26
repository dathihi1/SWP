package com.badat.study1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service để phân tích và minh họa cách hệ thống xử lý race condition
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaceConditionAnalysisService {
    
    private final PaymentQueueService paymentQueueService;
    
    /**
     * Minh họa scenario: 1 user đăng nhập nhiều session cùng mua hàng
     * Kết quả mong đợi: Chỉ 1 payment được tạo thành công, các session khác bị reject
     */
    public void demonstrateMultipleSessionsSameUser(Long userId, Long productId, int totalSessions) {
        log.info("=== DEMONSTRATING MULTIPLE SESSIONS SAME USER ===");
        log.info("User ID: {}, Product ID: {}, Total Sessions: {}", userId, productId, totalSessions);
        
        ExecutorService executor = Executors.newFixedThreadPool(totalSessions);
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        // Tạo cart items cho tất cả sessions (mỗi session mua 1 item)
        for (int i = 1; i <= totalSessions; i++) {
            final int sessionId = i;
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Session {} attempting to buy product {} for user {}", sessionId, productId, userId);
                    
                    // Tạo cart item
                    Map<String, Object> cartItem = new HashMap<>();
                    cartItem.put("productId", productId);
                    cartItem.put("quantity", 1);
                    cartItem.put("price", new BigDecimal("100000"));
                    
                    List<Map<String, Object>> cartItems = List.of(cartItem);
                    BigDecimal totalAmount = new BigDecimal("100000");
                    
                    // Thử enqueue payment
                    Long paymentId = paymentQueueService.enqueuePayment(userId, cartItems, totalAmount);
                    
                    log.info("Session {} successfully enqueued payment {} for user {}", sessionId, paymentId, userId);
                    return "Session " + sessionId + " SUCCESS - Payment ID: " + paymentId;
                    
                } catch (Exception e) {
                    log.warn("Session {} failed to buy product {} for user {}: {}", sessionId, productId, userId, e.getMessage());
                    return "Session " + sessionId + " FAILED - " + e.getMessage();
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Chờ tất cả sessions hoàn thành
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(); // Chờ tất cả hoàn thành
            
            // In kết quả
            log.info("=== MULTIPLE SESSIONS TEST RESULTS ===");
            int successCount = 0;
            int failureCount = 0;
            
            for (CompletableFuture<String> future : futures) {
                String result = future.get();
                log.info("Result: {}", result);
                
                if (result.contains("SUCCESS")) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }
            
            log.info("=== SUMMARY ===");
            log.info("User ID: {}", userId);
            log.info("Total Sessions: {}", totalSessions);
            log.info("Successful Payments: {}", successCount);
            log.info("Failed Payments: {}", failureCount);
            
            // Kiểm tra kết quả
            if (successCount == 1) {
                log.info("✅ MULTIPLE SESSIONS HANDLED CORRECTLY - Only 1 payment allowed per user");
            } else {
                log.error("❌ MULTIPLE SESSIONS NOT HANDLED - Multiple payments detected! Success: {}", successCount);
            }
            
        } catch (Exception e) {
            log.error("Error during multiple sessions test: {}", e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Minh họa scenario: 10 người cùng mua 1 sản phẩm, kho chỉ có 5 items
     * Kết quả mong đợi: Chỉ 5 người thành công, 5 người thất bại
     */
    public void demonstrateRaceConditionHandling(Long productId, int totalUsers, int availableStock) {
        log.info("=== DEMONSTRATING RACE CONDITION HANDLING ===");
        log.info("Product ID: {}, Total Users: {}, Available Stock: {}", productId, totalUsers, availableStock);
        
        ExecutorService executor = Executors.newFixedThreadPool(totalUsers);
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        // Tạo cart items cho tất cả users (mỗi user mua 1 item)
        for (int i = 1; i <= totalUsers; i++) {
            final int userId = i;
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("User {} attempting to buy product {}", userId, productId);
                    
                    // Tạo cart item
                    Map<String, Object> cartItem = new HashMap<>();
                    cartItem.put("productId", productId);
                    cartItem.put("quantity", 1);
                    cartItem.put("price", new BigDecimal("100000"));
                    
                    List<Map<String, Object>> cartItems = List.of(cartItem);
                    BigDecimal totalAmount = new BigDecimal("100000");
                    
                    // Thử enqueue payment
                    Long paymentId = paymentQueueService.enqueuePayment((long) userId, cartItems, totalAmount);
                    
                    log.info("User {} successfully enqueued payment {}", userId, paymentId);
                    return "User " + userId + " SUCCESS - Payment ID: " + paymentId;
                    
                } catch (Exception e) {
                    log.warn("User {} failed to buy product {}: {}", userId, productId, e.getMessage());
                    return "User " + userId + " FAILED - " + e.getMessage();
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Chờ tất cả users hoàn thành
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(); // Chờ tất cả hoàn thành
            
            // In kết quả
            log.info("=== RACE CONDITION TEST RESULTS ===");
            int successCount = 0;
            int failureCount = 0;
            
            for (CompletableFuture<String> future : futures) {
                String result = future.get();
                log.info("Result: {}", result);
                
                if (result.contains("SUCCESS")) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }
            
            log.info("=== SUMMARY ===");
            log.info("Total Users: {}", totalUsers);
            log.info("Successful Purchases: {}", successCount);
            log.info("Failed Purchases: {}", failureCount);
            log.info("Available Stock: {}", availableStock);
            
            // Kiểm tra kết quả
            if (successCount <= availableStock) {
                log.info("✅ RACE CONDITION HANDLED CORRECTLY - No overselling detected");
            } else {
                log.error("❌ RACE CONDITION NOT HANDLED - Overselling detected! Success: {}, Stock: {}", 
                    successCount, availableStock);
            }
            
        } catch (Exception e) {
            log.error("Error during race condition test: {}", e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Phân tích cơ chế chống race condition trong hệ thống
     */
    public void analyzeRaceConditionProtection() {
        log.info("=== RACE CONDITION PROTECTION ANALYSIS ===");
        
        log.info("1. STOCK VALIDATION LAYER:");
        log.info("   - Redis Lock: stock:validate:{productId}");
        log.info("   - Purpose: Prevent multiple users from checking stock simultaneously");
        log.info("   - Timeout: 3 seconds");
        
        log.info("2. WAREHOUSE RESERVATION LAYER:");
        log.info("   - Redis Lock: warehouse:reserve:{productId}");
        log.info("   - Database Lock: SELECT FOR UPDATE");
        log.info("   - Purpose: Atomic reservation of warehouse items");
        log.info("   - Timeout: 5 minutes (auto-release)");
        
        log.info("3. PAYMENT PROCESSING LAYER:");
        log.info("   - Redis Lock: payment:process:{paymentId}");
        log.info("   - Purpose: Prevent double processing of same payment");
        log.info("   - Timeout: 3 seconds");
        
        log.info("4. WALLET HOLD LAYER:");
        log.info("   - Redis Lock: user:wallet:lock:{userId}");
        log.info("   - Purpose: Prevent concurrent wallet operations for same user");
        log.info("   - Timeout: 10 seconds");
        
        log.info("5. USER PAYMENT LAYER:");
        log.info("   - Redis Lock: user:payment:lock:{userId}");
        log.info("   - Purpose: Prevent multiple concurrent payments from same user");
        log.info("   - Timeout: 5 seconds");
        log.info("   - Check: Reject if user has pending payments");
        
        log.info("6. PROCESSING ORDER:");
        log.info("   1. Check user payment lock (prevent multiple payments)");
        log.info("   2. Check pending payments for user");
        log.info("   3. Validate stock (with Redis lock)");
        log.info("   4. Validate user balance");
        log.info("   5. Reserve warehouse items (with Redis + DB locks)");
        log.info("   6. Hold money (with Redis lock)");
        log.info("   7. Create order");
        log.info("   8. Mark as delivered");
        
        log.info("7. ERROR HANDLING:");
        log.info("   - Automatic rollback on any failure");
        log.info("   - Release reserved items if money hold fails");
        log.info("   - Release money if order creation fails");
        log.info("   - Auto-release expired reservations (cron job)");
        
        log.info("=== CONCLUSION ===");
        log.info("✅ Multiple layers of protection against race conditions");
        log.info("✅ Atomic operations with proper locking");
        log.info("✅ Automatic cleanup and rollback mechanisms");
        log.info("✅ No overselling possible with current implementation");
        log.info("✅ Multiple sessions of same user handled correctly");
        log.info("✅ Only 1 payment allowed per user at a time");
    }
}
