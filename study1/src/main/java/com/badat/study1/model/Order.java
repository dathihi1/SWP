package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "`order`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "buyer_id", nullable = false)
    Long buyerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_id", insertable = false, updatable = false)
    User buyer;

    @Column(name = "seller_id", nullable = false)
    Long sellerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", insertable = false, updatable = false)
    User seller;

    @Column(name = "shop_id", nullable = false)
    Long shopId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    Shop shop;

    @Column(name = "stall_id", nullable = false)
    Long stallId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stall_id", insertable = false, updatable = false)
    Stall stall;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    Status status = Status.PENDING;

    @Column(name = "payment_method", length = 50)
    String paymentMethod;

    @Column(name = "order_code", length = 100, unique = true)
    String orderCode;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

    public enum Status {
        PENDING, COMPLETED, CANCELLED, REFUNDED
    }
}
