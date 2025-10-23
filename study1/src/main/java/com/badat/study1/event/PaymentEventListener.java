package com.badat.study1.event;

import com.badat.study1.service.PaymentTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener để lắng nghe payment events và trigger xử lý ngay lập tức
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {
    
    private final PaymentTriggerService paymentTriggerService;
    
    /**
     * Lắng nghe payment created event và trigger xử lý ngay lập tức
     */
    @EventListener
    @Async("paymentTaskExecutor")
    public void handlePaymentCreated(PaymentEvent event) {
        log.info("Received payment created event for payment: {}", event.getPaymentId());
        
        try {
            // Trigger xử lý payment ngay lập tức
            paymentTriggerService.triggerPaymentProcessing(event.getPaymentId());
            
        } catch (Exception e) {
            log.error("Error handling payment created event for payment {}: {}", 
                    event.getPaymentId(), e.getMessage());
        }
    }
    
    /**
     * Lắng nghe payment failed event
     */
    @EventListener
    @Async("paymentTaskExecutor")
    public void handlePaymentFailed(PaymentEvent event) {
        log.info("Received payment failed event for payment: {}", event.getPaymentId());
        
        // Có thể thêm logic xử lý khi payment failed
        // Ví dụ: gửi notification, retry logic, etc.
    }
}
