package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
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

    @Column(name = "cccd")
    String cccd;

    @Column(name = "bank_account_id", nullable = false)
    Long bankAccountId;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "address", length = 255)
    String address;

    @Column(name = "phone", length = 20)
    String phone;

    @Column(name = "email", length = 100)
    String email;

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

    public enum Status {
        ACTIVE, INACTIVE, SUSPENDED, PENDING
    }

}


