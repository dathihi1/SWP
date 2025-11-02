package com.badat.study1.service;

import com.badat.study1.repository.ApiCallLogRepository;
import com.badat.study1.repository.UserActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogCleanupScheduler {
    
    private final UserActivityLogRepository userActivityLogRepository;
    private final ApiCallLogRepository apiCallLogRepository;
    
    /**
     * Cleanup old logs daily at 2 AM
     * - UserActivityLog: Keep 90 days
     * - ApiCallLog: Keep 30 days
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldLogs() {
        try {
            log.info("Starting log cleanup process...");
            
            // Calculate cutoff dates
            LocalDateTime userActivityCutoff = LocalDateTime.now().minusDays(90);
            LocalDateTime apiCallCutoff = LocalDateTime.now().minusDays(30);
            
            // Cleanup UserActivityLog (90 days retention)
            long userActivityDeleted = userActivityLogRepository.countByCreatedAtBefore(userActivityCutoff);
            if (userActivityDeleted > 0) {
                userActivityLogRepository.deleteByCreatedAtBefore(userActivityCutoff);
                log.info("Deleted {} old user activity logs (older than 90 days)", userActivityDeleted);
            }
            
            // Cleanup ApiCallLog (30 days retention)
            long apiCallDeleted = apiCallLogRepository.countByCreatedAtBefore(apiCallCutoff);
            if (apiCallDeleted > 0) {
                apiCallLogRepository.deleteByCreatedAtBefore(apiCallCutoff);
                log.info("Deleted {} old API call logs (older than 30 days)", apiCallDeleted);
            }
            
            log.info("Log cleanup process completed successfully");
            
        } catch (Exception e) {
            log.error("Error during log cleanup process: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Weekly cleanup for additional maintenance
     * Runs every Sunday at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void weeklyMaintenance() {
        try {
            log.info("Starting weekly log maintenance...");
            
            // Additional cleanup for very old logs (1 year)
            LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
            
            long veryOldUserLogs = userActivityLogRepository.countByCreatedAtBefore(oneYearAgo);
            long veryOldApiLogs = apiCallLogRepository.countByCreatedAtBefore(oneYearAgo);
            
            if (veryOldUserLogs > 0) {
                userActivityLogRepository.deleteByCreatedAtBefore(oneYearAgo);
                log.info("Deleted {} very old user activity logs (older than 1 year)", veryOldUserLogs);
            }
            
            if (veryOldApiLogs > 0) {
                apiCallLogRepository.deleteByCreatedAtBefore(oneYearAgo);
                log.info("Deleted {} very old API call logs (older than 1 year)", veryOldApiLogs);
            }
            
            log.info("Weekly log maintenance completed");
            
        } catch (Exception e) {
            log.error("Error during weekly log maintenance: {}", e.getMessage(), e);
        }
    }
}


























