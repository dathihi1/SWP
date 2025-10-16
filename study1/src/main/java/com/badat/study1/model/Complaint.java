package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "complaint")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "transaction_id", nullable = false)
    Long transactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    Transaction transaction;
    
    @Column(name = "buyer_id", nullable = false)
    Long buyerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", insertable = false, updatable = false)
    User buyer;
    
    @Column(name = "description", columnDefinition = "TEXT")
    String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    Status status = Status.PENDING;
    
    public enum Status {
        PENDING, APPROVED, REJECTED
    }
}
