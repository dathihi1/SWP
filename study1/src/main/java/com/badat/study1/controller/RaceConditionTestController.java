package com.badat.study1.controller;

import com.badat.study1.service.RaceConditionAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller để test race condition handling
 */
@RestController
@RequestMapping("/api/race-condition")
@RequiredArgsConstructor
@Slf4j
public class RaceConditionTestController {
    
    private final RaceConditionAnalysisService raceConditionAnalysisService;
    
    /**
     * Test race condition với số lượng user và stock tùy chỉnh
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testRaceCondition(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "10") int totalUsers,
            @RequestParam(defaultValue = "5") int availableStock) {
        
        log.info("Starting race condition test - Product: {}, Users: {}, Stock: {}", 
            productId, totalUsers, availableStock);
        
        try {
            // Chạy test race condition
            raceConditionAnalysisService.demonstrateRaceConditionHandling(productId, totalUsers, availableStock);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Race condition test completed successfully");
            response.put("productId", productId);
            response.put("totalUsers", totalUsers);
            response.put("availableStock", availableStock);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during race condition test: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Race condition test failed: " + e.getMessage());
            response.put("productId", productId);
            response.put("totalUsers", totalUsers);
            response.put("availableStock", availableStock);
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Phân tích cơ chế chống race condition
     */
    @GetMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeRaceConditionProtection() {
        log.info("Analyzing race condition protection mechanisms");
        
        try {
            raceConditionAnalysisService.analyzeRaceConditionProtection();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Race condition protection analysis completed");
            response.put("analysis", "Check logs for detailed analysis");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during race condition analysis: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Race condition analysis failed: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Test case mặc định: 10 users, 5 items
     */
    @PostMapping("/test-default")
    public ResponseEntity<Map<String, Object>> testDefaultRaceCondition(@RequestParam Long productId) {
        return testRaceCondition(productId, 10, 5);
    }
    
    /**
     * Test case căng thẳng: 100 users, 10 items
     */
    @PostMapping("/test-stress")
    public ResponseEntity<Map<String, Object>> testStressRaceCondition(@RequestParam Long productId) {
        return testRaceCondition(productId, 100, 10);
    }
    
    /**
     * Test multiple sessions của cùng 1 user
     */
    @PostMapping("/test-multiple-sessions")
    public ResponseEntity<Map<String, Object>> testMultipleSessionsSameUser(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "5") int totalSessions) {
        
        log.info("Starting multiple sessions test - User: {}, Product: {}, Sessions: {}", 
            userId, productId, totalSessions);
        
        try {
            // Chạy test multiple sessions
            raceConditionAnalysisService.demonstrateMultipleSessionsSameUser(userId, productId, totalSessions);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Multiple sessions test completed successfully");
            response.put("userId", userId);
            response.put("productId", productId);
            response.put("totalSessions", totalSessions);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during multiple sessions test: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Multiple sessions test failed: " + e.getMessage());
            response.put("userId", userId);
            response.put("productId", productId);
            response.put("totalSessions", totalSessions);
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}
