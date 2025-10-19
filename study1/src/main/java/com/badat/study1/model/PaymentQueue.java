package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentQueue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;

    @Column(name = "cart_data", columnDefinition = "JSON", nullable = false)
    String cartData;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    Status status = Status.PENDING;

    @Column(name = "processed_at")
    Instant processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    String errorMessage;

    public enum Status {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
