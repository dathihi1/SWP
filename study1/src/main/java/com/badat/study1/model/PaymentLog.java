package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "paymentlog")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLog extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "user_id", nullable = false)
    Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    Type type;
    
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;
    
    @Column(name = "gateway", length = 50)
    @Builder.Default
    String gateway = "VNPAY";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    Status status = Status.PENDING;
    
    public enum Type {
        DEPOSIT, WITHDRAW
    }
    
    public enum Status {
        PENDING, SUCCESS, FAILED
    }
}
