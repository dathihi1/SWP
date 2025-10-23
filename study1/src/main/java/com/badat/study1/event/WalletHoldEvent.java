package com.badat.study1.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi có wallet hold mới được tạo hoặc expired
 * Trigger system sẽ lắng nghe event này để xử lý ngay lập tức
 */
@Getter
public class WalletHoldEvent extends ApplicationEvent {
    
    private final Long holdId;
    private final Long userId;
    private final String eventType;
    
    public WalletHoldEvent(Object source, Long holdId, Long userId, String eventType) {
        super(source);
        this.holdId = holdId;
        this.userId = userId;
        this.eventType = eventType;
    }
    
    public static WalletHoldEvent holdCreated(Object source, Long holdId, Long userId) {
        return new WalletHoldEvent(source, holdId, userId, "HOLD_CREATED");
    }
    
    public static WalletHoldEvent holdExpired(Object source, Long holdId, Long userId) {
        return new WalletHoldEvent(source, holdId, userId, "HOLD_EXPIRED");
    }
}

