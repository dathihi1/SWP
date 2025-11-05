package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shop")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    Long userId;

    @Column(name = "shop_name", nullable = false, length = 100)
    String shopName;

    @Column(name = "shop_owner_name", length = 100)
    String shopOwnerName;

    @Lob
    @Column(name = "cccd_front_image", columnDefinition = "LONGBLOB")
    byte[] cccdFrontImage;

    @Lob
    @Column(name = "cccd_back_image", columnDefinition = "LONGBLOB")
    byte[] cccdBackImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at")
    Instant updatedAt;

    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    Boolean isDelete = false;

    @Column(name = "commission_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal commissionRate = BigDecimal.valueOf(5.0);

    public enum Status {
        ACTIVE, INACTIVE
    }

}


