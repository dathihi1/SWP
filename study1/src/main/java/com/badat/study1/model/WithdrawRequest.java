package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "withdrawrequest")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequest extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "shop_id", nullable = false)
    Long shopId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    Shop shop;
    
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    Status status = Status.PENDING;
    
    @Column(name = "bank_account_number", nullable = false, length = 50)
    String bankAccountNumber;
    
    @Column(name = "bank_account_name", nullable = false, length = 100)
    String bankAccountName;
    
    @Column(name = "bank_name", nullable = false, length = 100)
    String bankName;
    
    @Column(name = "note", length = 255)
    String note;
    
    public enum Status {
        PENDING, APPROVED, REJECTED, CANCELLED
    }
}
