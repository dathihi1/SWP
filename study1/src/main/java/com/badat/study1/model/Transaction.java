package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "buyer_id", nullable = false)
    Long buyerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", insertable = false, updatable = false)
    User buyer;

    @Column(name = "seller_id", nullable = false)
    Long sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", insertable = false, updatable = false)
    User seller;

    @Column(name = "shop_id", nullable = false)
    Long shopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    Shop shop;

    @Column(name = "stall_id", nullable = false)
    Long stallId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id", insertable = false, updatable = false)
    Stall stall;

    @Column(name = "product_id", nullable = false)
    Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    Product product;

    @Column(name = "warehouse_item_id", nullable = false)
    Long warehouseItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_item_id", insertable = false, updatable = false)
    Warehouse warehouseItem;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    Status status = Status.PENDING;

    @Column(name = "payment_method", length = 50)
    String paymentMethod;

    @Column(name = "transaction_code", length = 100, unique = true)
    String transactionCode;

    @Column(name = "delivery_data", columnDefinition = "TEXT")
    String deliveryData;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "completed_at")
    Instant completedAt;

    public enum Status {
        PENDING, SUCCESS, FAILED, REFUNDED
    }
}


