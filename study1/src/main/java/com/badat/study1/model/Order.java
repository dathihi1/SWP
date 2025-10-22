package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

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

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    BigDecimal totalAmount;

    @Column(name = "total_commission_amount", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal totalCommissionAmount = BigDecimal.ZERO;

    @Column(name = "total_seller_amount", precision = 15, scale = 2, nullable = false)
    BigDecimal totalSellerAmount;

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

    // One-to-many relationship với OrderItem
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<OrderItem> orderItems;

    public enum Status {
        PENDING, COMPLETED, CANCELLED, REFUNDED
    }
}
