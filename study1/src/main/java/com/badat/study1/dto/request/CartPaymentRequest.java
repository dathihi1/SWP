package com.badat.study1.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartPaymentRequest {
    
    private List<Map<String, Object>> cartItems;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String notes;
}
