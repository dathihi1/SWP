package com.badat.study1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service để test race condition scenarios
 * Mô phỏng 2 người mua cùng lúc khi kho chỉ còn 1 sản phẩm
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaceConditionTestService {
    
    private final InventoryReservationService inventoryReservationService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    /**
     * Test race condition: 2 users mua cùng lúc, kho chỉ còn 1 sản phẩm
     */
    public void testRaceCondition(Long productId) {
        log.info("=== TESTING RACE CONDITION ===");
        log.info("Product ID: {}", productId);
        log.info("Simulating 2 users buying simultaneously when only 1 item left in stock");
        
        // Tạo 2 users mua cùng lúc
        CompletableFuture<Void> user1 = CompletableFuture.runAsync(() -> {
            try {
                log.info("User 1 attempting to buy product {}", productId);
                Map<Long, Integer> quantities = new HashMap<>();
                quantities.put(productId, 1);
                
                var result = inventoryReservationService.reserveInventory(quantities, 1L);
                if (result.isSuccess()) {
                    log.info("✅ User 1 SUCCESS: {}", result.getMessage());
                } else {
                    log.warn("❌ User 1 FAILED: {}", result.getMessage());
                }
            } catch (Exception e) {
                log.error("❌ User 1 ERROR: {}", e.getMessage());
            }
        }, executorService);
        
        CompletableFuture<Void> user2 = CompletableFuture.runAsync(() -> {
            try {
                // Delay 100ms để mô phỏng gần như cùng lúc
                Thread.sleep(100);
                log.info("User 2 attempting to buy product {}", productId);
                Map<Long, Integer> quantities = new HashMap<>();
                quantities.put(productId, 1);
                
                var result = inventoryReservationService.reserveInventory(quantities, 2L);
                if (result.isSuccess()) {
                    log.info("✅ User 2 SUCCESS: {}", result.getMessage());
                } else {
                    log.warn("❌ User 2 FAILED: {}", result.getMessage());
                }
            } catch (Exception e) {
                log.error("❌ User 2 ERROR: {}", e.getMessage());
            }
        }, executorService);
        
        // Chờ cả 2 users hoàn thành
        CompletableFuture.allOf(user1, user2).join();
        
        log.info("=== RACE CONDITION TEST COMPLETED ===");
    }
    
    /**
     * Test với nhiều users hơn (5 users, 2 sản phẩm)
     */
    public void testMultipleUsersRaceCondition(Long productId) {
        log.info("=== TESTING MULTIPLE USERS RACE CONDITION ===");
        log.info("Product ID: {}", productId);
        log.info("Simulating 5 users buying simultaneously when only 2 items left in stock");
        
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] users = new CompletableFuture[5];
        
        for (int i = 0; i < 5; i++) {
            final int userId = i + 1;
            users[i] = CompletableFuture.runAsync(() -> {
                try {
                    // Random delay 0-200ms để mô phỏng real-world scenario
                    Thread.sleep((long) (Math.random() * 200));
                    
                    log.info("User {} attempting to buy product {}", userId, productId);
                    Map<Long, Integer> quantities = new HashMap<>();
                    quantities.put(productId, 1);
                    
                    var result = inventoryReservationService.reserveInventory(quantities, (long) userId);
                    if (result.isSuccess()) {
                        log.info("✅ User {} SUCCESS: {}", userId, result.getMessage());
                    } else {
                        log.warn("❌ User {} FAILED: {}", userId, result.getMessage());
                    }
                } catch (Exception e) {
                    log.error("❌ User {} ERROR: {}", userId, e.getMessage());
                }
            }, executorService);
        }
        
        // Chờ tất cả users hoàn thành
        CompletableFuture.allOf(users).join();
        
        log.info("=== MULTIPLE USERS RACE CONDITION TEST COMPLETED ===");
    }
    
    /**
     * Test performance với high concurrency
     */
    public void testHighConcurrencyPerformance(Long productId, int numberOfUsers) {
        log.info("=== TESTING HIGH CONCURRENCY PERFORMANCE ===");
        log.info("Product ID: {}, Number of Users: {}", productId, numberOfUsers);
        
        long startTime = System.currentTimeMillis();
        
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] users = new CompletableFuture[numberOfUsers];
        
        for (int i = 0; i < numberOfUsers; i++) {
            final int userId = i + 1;
            users[i] = CompletableFuture.runAsync(() -> {
                try {
                    Map<Long, Integer> quantities = new HashMap<>();
                    quantities.put(productId, 1);
                    
                    var result = inventoryReservationService.reserveInventory(quantities, (long) userId);
                    if (result.isSuccess()) {
                        log.debug("✅ User {} SUCCESS", userId);
                    } else {
                        log.debug("❌ User {} FAILED: {}", userId, result.getMessage());
                    }
                } catch (Exception e) {
                    log.error("❌ User {} ERROR: {}", userId, e.getMessage());
                }
            }, executorService);
        }
        
        // Chờ tất cả users hoàn thành
        CompletableFuture.allOf(users).join();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        log.info("=== HIGH CONCURRENCY TEST COMPLETED ===");
        log.info("Total Users: {}", numberOfUsers);
        log.info("Total Time: {} ms", duration);
        log.info("Average Time per User: {} ms", duration / numberOfUsers);
        log.info("Throughput: {} users/second", (numberOfUsers * 1000) / duration);
    }
}
