package com.badat.study1.service;

import com.badat.study1.model.PaymentQueue;
import com.badat.study1.model.WalletHold;
import com.badat.study1.repository.PaymentQueueRepository;
import com.badat.study1.repository.WalletHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service để trigger xử lý payment ngay lập tức khi có payment mới
 * Kết hợp với cron jobs để đảm bảo không bỏ sót
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTriggerService {
    
    private final PaymentQueueRepository paymentQueueRepository;
    private final WalletHoldRepository walletHoldRepository;
    private final PaymentQueueService paymentQueueService;
    private final WalletHoldService walletHoldService;
    
    // Thread pool cho parallel processing
    private final ExecutorService executorService = Executors.newFixedThreadPool(50);
    
    /**
     * Trigger xử lý payment ngay lập tức khi có payment mới
     */
    @Transactional
    public void triggerPaymentProcessing(Long paymentId) {
        log.info("Triggering immediate payment processing for payment: {}", paymentId);
        
        try {
            PaymentQueue payment = paymentQueueRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
            
            if (payment.getStatus() != PaymentQueue.Status.PENDING) {
                log.info("Payment {} is not PENDING, current status: {}", paymentId, payment.getStatus());
                return;
            }
            
            // Xử lý ngay lập tức với parallel processing
            CompletableFuture.runAsync(() -> {
                try {
                    paymentQueueService.processPaymentItem(payment);
                    log.info("Triggered payment processing completed for payment: {}", paymentId);
                } catch (Exception e) {
                    log.error("Error in triggered payment processing for payment {}: {}", paymentId, e.getMessage());
                }
            }, executorService);
            
        } catch (Exception e) {
            log.error("Failed to trigger payment processing for payment {}: {}", paymentId, e.getMessage());
        }
    }
    
    /**
     * Trigger xử lý wallet hold ngay lập tức
     */
    @Transactional
    public void triggerWalletHoldProcessing(Long holdId) {
        log.info("Triggering immediate wallet hold processing for hold: {}", holdId);
        
        try {
            WalletHold hold = walletHoldRepository.findById(holdId)
                .orElseThrow(() -> new RuntimeException("Hold not found: " + holdId));
            
            if (hold.getStatus() != WalletHold.Status.PENDING) {
                log.info("Hold {} is not PENDING, current status: {}", holdId, hold.getStatus());
                return;
            }
            
            // Kiểm tra nếu hold đã expired
            if (hold.getExpiresAt().isBefore(Instant.now())) {
                log.info("Hold {} has expired, processing immediately", holdId);
                
                CompletableFuture.runAsync(() -> {
                    try {
                        walletHoldService.completeHold(holdId);
                        log.info("Triggered hold processing completed for hold: {}", holdId);
                    } catch (Exception e) {
                        log.error("Error in triggered hold processing for hold {}: {}", holdId, e.getMessage());
                    }
                }, executorService);
            }
            
        } catch (Exception e) {
            log.error("Failed to trigger hold processing for hold {}: {}", holdId, e.getMessage());
        }
    }
    
    /**
     * Batch trigger cho nhiều payments cùng lúc
     */
    @Transactional
    public void triggerBatchPaymentProcessing(List<Long> paymentIds) {
        log.info("Triggering batch payment processing for {} payments", paymentIds.size());
        
        // Chia thành các batch nhỏ để xử lý parallel
        int batchSize = 10;
        for (int i = 0; i < paymentIds.size(); i += batchSize) {
            List<Long> batch = paymentIds.subList(i, Math.min(i + batchSize, paymentIds.size()));
            
            CompletableFuture.runAsync(() -> {
                for (Long paymentId : batch) {
                    try {
                        triggerPaymentProcessing(paymentId);
                    } catch (Exception e) {
                        log.error("Error in batch payment processing for payment {}: {}", paymentId, e.getMessage());
                    }
                }
            }, executorService);
        }
    }
    
    /**
     * Trigger dựa trên số lượng payments trong queue
     */
    @Transactional
    public void triggerBasedOnQueueSize() {
        try {
            long pendingCount = paymentQueueRepository.countByStatus(PaymentQueue.Status.PENDING);
            long expiredHoldsCount = walletHoldRepository.countByStatusAndExpiresAtBefore(
                WalletHold.Status.PENDING, Instant.now());
            
            log.info("Queue status - Pending payments: {}, Expired holds: {}", pendingCount, expiredHoldsCount);
            
            // Nếu có quá nhiều pending payments, trigger xử lý ngay
            if (pendingCount > 100) {
                log.info("High pending payments count: {}, triggering batch processing", pendingCount);
                
                List<PaymentQueue> pendingPayments = paymentQueueRepository
                    .findByStatusOrderByCreatedAtAsc(PaymentQueue.Status.PENDING);
                
                // Lấy 50 payments đầu tiên để xử lý
                List<Long> paymentIds = pendingPayments.stream()
                    .limit(50)
                    .map(PaymentQueue::getId)
                    .toList();
                
                triggerBatchPaymentProcessing(paymentIds);
            }
            
            // Nếu có expired holds, trigger xử lý ngay
            if (expiredHoldsCount > 0) {
                log.info("Found {} expired holds, triggering processing", expiredHoldsCount);
                
                List<WalletHold> expiredHolds = walletHoldRepository
                    .findByStatusAndExpiresAtBefore(WalletHold.Status.PENDING, Instant.now());
                
                for (WalletHold hold : expiredHolds) {
                    triggerWalletHoldProcessing(hold.getId());
                }
            }
            
        } catch (Exception e) {
            log.error("Error in queue-based triggering: {}", e.getMessage());
        }
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
