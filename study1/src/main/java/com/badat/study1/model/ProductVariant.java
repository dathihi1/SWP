package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variant")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "shop_id", nullable = false)
    Long shopId;

    // SỬA: Thay đổi FetchType sang EAGER để tải Shop cùng Product (khắc phục lỗi Lazy Loading).
    // Đảm bảo Entity Shop tồn tại và có Getter/Setter.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    Shop shop;
    
    @Column(name = "product_id", nullable = false)
    Long productId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    Product product;
    
    @Column(name = "subcategory", nullable = false, length = 100)
    @Builder.Default
    String subcategory = "Khác";

    @Column(name = "name", nullable = false, length = 100)
    String name;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    BigDecimal price;

    @Column(name = "quantity")
    @Builder.Default
    Integer quantity = 1;

    @Column(name = "unique_key", unique = true, nullable = false)
    String uniqueKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    Status status = Status.UNAVAILABLE;
    
    public enum Status {
        AVAILABLE, UNAVAILABLE
    }
}
