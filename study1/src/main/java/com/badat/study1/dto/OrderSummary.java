package com.badat.study1.dto;

import com.badat.study1.model.Order;
import com.badat.study1.model.OrderItem;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class OrderSummary {
    Order order;
    List<OrderItem> items;
    BigDecimal totalAmount;
    int totalQuantity;
    String firstProductName;
    String stallName;
    BigDecimal unitPrice;
    OrderItem.Status status;
    LocalDateTime createdAt;
}


