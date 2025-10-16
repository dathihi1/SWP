package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "bankaccount")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "user_id", nullable = false)
    Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;
    
    @Column(name = "bank_name", nullable = false, length = 100)
    String bankName;
    
    @Column(name = "account_number", unique = true, nullable = false, length = 50)
    String accountNumber;
    
    @Column(name = "verified_at")
    LocalDateTime verifiedAt;
}
