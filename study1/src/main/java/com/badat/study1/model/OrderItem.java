package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "order_id", nullable = false)
    Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    Order order;

    @Column(name = "product_id", nullable = false)
    Long productId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    Product product;

    @Column(name = "warehouse_id", nullable = false)
    Long warehouseId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", insertable = false, updatable = false)
    Warehouse warehouse;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    Integer quantity = 1;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    BigDecimal totalAmount;

    @Column(name = "commission_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(name = "commission_amount", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "seller_amount", precision = 15, scale = 2, nullable = false)
    BigDecimal sellerAmount;

    @Column(name = "seller_id", nullable = false)
    Long sellerId;

    @Column(name = "shop_id", nullable = false)
    Long shopId;

    @Column(name = "stall_id", nullable = false)
    Long stallId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    Status status = Status.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

    public enum Status {
        PENDING, COMPLETED, CANCELLED, REFUNDED
    }
}
