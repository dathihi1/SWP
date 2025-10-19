package com.badat.study1.dto.response;

import com.badat.study1.model.PaymentQueue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {
    
    private Long paymentId;
    private Long userId;
    private BigDecimal totalAmount;
    private PaymentQueue.Status status;
    private Instant createdAt;
    private Instant processedAt;
    private String errorMessage;
    private String message;
    private boolean success;
    
    public static PaymentStatusResponse fromEntity(PaymentQueue payment) {
        return PaymentStatusResponse.builder()
            .paymentId(payment.getId())
            .userId(payment.getUserId())
            .totalAmount(payment.getTotalAmount())
            .status(payment.getStatus())
            .createdAt(payment.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
            .processedAt(payment.getProcessedAt())
            .errorMessage(payment.getErrorMessage())
            .message("Payment status retrieved successfully")
            .success(true)
            .build();
    }
}
