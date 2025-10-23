package com.badat.study1.controller;

import com.badat.study1.service.RaceConditionTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Controller để test race condition scenarios
 */
@RestController
@RequestMapping("/api/test/race-condition")
@RequiredArgsConstructor
@Slf4j
public class RaceConditionTestController {
    
    private final RaceConditionTestService raceConditionTestService;
    
    /**
     * Test race condition với 2 users
     */
    @PostMapping("/test-2-users/{productId}")
    public String test2Users(@PathVariable Long productId) {
        log.info("Starting race condition test for 2 users with product: {}", productId);
        raceConditionTestService.testRaceCondition(productId);
        return "Race condition test completed for 2 users";
    }
    
    /**
     * Test race condition với 5 users
     */
    @PostMapping("/test-5-users/{productId}")
    public String test5Users(@PathVariable Long productId) {
        log.info("Starting race condition test for 5 users with product: {}", productId);
        raceConditionTestService.testMultipleUsersRaceCondition(productId);
        return "Race condition test completed for 5 users";
    }
    
    /**
     * Test high concurrency performance
     */
    @PostMapping("/test-high-concurrency/{productId}")
    public String testHighConcurrency(@PathVariable Long productId, @RequestParam(defaultValue = "10") int numberOfUsers) {
        log.info("Starting high concurrency test with {} users for product: {}", numberOfUsers, productId);
        raceConditionTestService.testHighConcurrencyPerformance(productId, numberOfUsers);
        return "High concurrency test completed with " + numberOfUsers + " users";
    }
}

