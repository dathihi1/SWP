package com.badat.study1.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi có payment mới được tạo
 * Trigger system sẽ lắng nghe event này để xử lý ngay lập tức
 */
@Getter
public class PaymentEvent extends ApplicationEvent {
    
    private final Long paymentId;
    private final Long userId;
    private final String eventType;
    
    public PaymentEvent(Object source, Long paymentId, Long userId, String eventType) {
        super(source);
        this.paymentId = paymentId;
        this.userId = userId;
        this.eventType = eventType;
    }
    
    public static PaymentEvent paymentCreated(Object source, Long paymentId, Long userId) {
        return new PaymentEvent(source, paymentId, userId, "PAYMENT_CREATED");
    }
    
    public static PaymentEvent paymentFailed(Object source, Long paymentId, Long userId) {
        return new PaymentEvent(source, paymentId, userId, "PAYMENT_FAILED");
    }
}

