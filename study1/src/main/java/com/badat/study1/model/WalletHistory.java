package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "wallethistory")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletHistory extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "wallet_id", nullable = false)
    Long walletId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", insertable = false, updatable = false)
    Wallet wallet;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    Type type;
    
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;
    
    @Column(name = "reference_id")
    Long referenceId;
    
    @Column(name = "description", columnDefinition = "TEXT")
    String description;
    
    public enum Type {
        DEPOSIT, WITHDRAW, PURCHASE, REFUND
    }
}
