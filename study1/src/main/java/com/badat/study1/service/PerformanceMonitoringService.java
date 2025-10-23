package com.badat.study1.service;

import com.badat.study1.model.PaymentQueue;
import com.badat.study1.model.WalletHold;
import com.badat.study1.repository.PaymentQueueRepository;
import com.badat.study1.repository.WalletHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service để monitor performance của hệ thống payment processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringService {
    
    private final PaymentQueueRepository paymentQueueRepository;
    private final WalletHoldRepository walletHoldRepository;
    
    /**
     * Monitor performance mỗi 30 giây
     */
    @Scheduled(fixedRate = 30000)
    public void monitorPerformance() {
        try {
            // Đếm pending payments
            long pendingPayments = paymentQueueRepository.countByStatus(PaymentQueue.Status.PENDING);
            long processingPayments = paymentQueueRepository.countByStatus(PaymentQueue.Status.PROCESSING);
            long completedPayments = paymentQueueRepository.countByStatus(PaymentQueue.Status.COMPLETED);
            long failedPayments = paymentQueueRepository.countByStatus(PaymentQueue.Status.FAILED);
            
            // Đếm pending holds
            long pendingHolds = walletHoldRepository.countByStatus(WalletHold.Status.PENDING);
            long expiredHolds = walletHoldRepository.countByStatusAndExpiresAtBefore(
                WalletHold.Status.PENDING, Instant.now());
            
            // Tính processing rate (payments/minute)
            long totalProcessed = completedPayments + failedPayments;
            double processingRate = calculateProcessingRate();
            
            log.info("=== PAYMENT SYSTEM PERFORMANCE MONITOR ===");
            log.info("Payments - Pending: {}, Processing: {}, Completed: {}, Failed: {}", 
                    pendingPayments, processingPayments, completedPayments, failedPayments);
            log.info("Holds - Pending: {}, Expired: {}", pendingHolds, expiredHolds);
            log.info("Processing Rate: {:.2f} payments/minute", processingRate);
            log.info("Total Processed: {}", totalProcessed);
            
            // Cảnh báo nếu có quá nhiều pending
            if (pendingPayments > 1000) {
                log.warn("HIGH PENDING PAYMENTS: {} - System may be overloaded!", pendingPayments);
            }
            
            if (expiredHolds > 100) {
                log.warn("HIGH EXPIRED HOLDS: {} - Wallet hold processing may be slow!", expiredHolds);
            }
            
            // Cảnh báo nếu processing rate quá thấp
            if (processingRate < 10 && totalProcessed > 0) {
                log.warn("LOW PROCESSING RATE: {:.2f} payments/minute - System performance degraded!", processingRate);
            }
            
        } catch (Exception e) {
            log.error("Error monitoring performance: {}", e.getMessage());
        }
    }
    
    /**
     * Tính processing rate dựa trên payments được xử lý trong 1 phút gần nhất
     */
    private double calculateProcessingRate() {
        try {
            Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
            
            // Đếm payments được completed trong 1 phút gần nhất
            List<PaymentQueue> recentCompleted = paymentQueueRepository
                .findByStatusAndProcessedAtAfter(PaymentQueue.Status.COMPLETED, oneMinuteAgo);
            
            List<PaymentQueue> recentFailed = paymentQueueRepository
                .findByStatusAndProcessedAtAfter(PaymentQueue.Status.FAILED, oneMinuteAgo);
            
            long totalProcessedInLastMinute = recentCompleted.size() + recentFailed.size();
            
            return totalProcessedInLastMinute;
            
        } catch (Exception e) {
            log.error("Error calculating processing rate: {}", e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Lấy thống kê chi tiết về performance
     */
    public PerformanceStats getPerformanceStats() {
        try {
            long pendingPayments = paymentQueueRepository.countByStatus(PaymentQueue.Status.PENDING);
            long processingPayments = paymentQueueRepository.countByStatus(PaymentQueue.Status.PROCESSING);
            long completedPayments = paymentQueueRepository.countByStatus(PaymentQueue.Status.COMPLETED);
            long failedPayments = paymentQueueRepository.countByStatus(PaymentQueue.Status.FAILED);
            
            long pendingHolds = walletHoldRepository.countByStatus(WalletHold.Status.PENDING);
            long expiredHolds = walletHoldRepository.countByStatusAndExpiresAtBefore(
                WalletHold.Status.PENDING, Instant.now());
            
            double processingRate = calculateProcessingRate();
            
            return PerformanceStats.builder()
                .pendingPayments(pendingPayments)
                .processingPayments(processingPayments)
                .completedPayments(completedPayments)
                .failedPayments(failedPayments)
                .pendingHolds(pendingHolds)
                .expiredHolds(expiredHolds)
                .processingRate(processingRate)
                .timestamp(Instant.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting performance stats: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * DTO cho performance stats
     */
    @lombok.Data
    @lombok.Builder
    public static class PerformanceStats {
        private long pendingPayments;
        private long processingPayments;
        private long completedPayments;
        private long failedPayments;
        private long pendingHolds;
        private long expiredHolds;
        private double processingRate;
        private Instant timestamp;
    }
}

