package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "buyer_id", nullable = false)
    Long buyerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", insertable = false, updatable = false)
    User buyer;
    
    @Column(name = "product_id", nullable = false)
    Long productId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    Product product;
    
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    Status status = Status.PENDING;
    
    public enum Status {
        PENDING, HOLD, RELEASED, REFUND
    }
}
