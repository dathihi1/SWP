package com.badat.study1.event;

import com.badat.study1.service.PaymentTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener để lắng nghe wallet hold events và trigger xử lý ngay lập tức
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletHoldEventListener {
    
    private final PaymentTriggerService paymentTriggerService;
    
    /**
     * Lắng nghe hold created event
     */
    @EventListener
    @Async("walletHoldTaskExecutor")
    public void handleHoldCreated(WalletHoldEvent event) {
        log.info("Received hold created event for hold: {}", event.getHoldId());
        
        // Có thể thêm logic xử lý khi hold được tạo
        // Ví dụ: gửi notification cho user
    }
    
    /**
     * Lắng nghe hold expired event và trigger xử lý ngay lập tức
     */
    @EventListener
    @Async("walletHoldTaskExecutor")
    public void handleHoldExpired(WalletHoldEvent event) {
        log.info("Received hold expired event for hold: {}", event.getHoldId());
        
        try {
            // Trigger xử lý hold expired ngay lập tức
            paymentTriggerService.triggerWalletHoldProcessing(event.getHoldId());
            
        } catch (Exception e) {
            log.error("Error handling hold expired event for hold {}: {}", 
                    event.getHoldId(), e.getMessage());
        }
    }
}
