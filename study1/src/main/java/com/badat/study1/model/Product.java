package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "shop_id", nullable = false)
    Long shopId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    Shop shop;
    
    @Column(name = "stall_id", nullable = false)
    Long stallId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id", insertable = false, updatable = false)
    Stall stall;
    
    @Column(name = "type", nullable = false, length = 50)
    String type;
    
    @Column(name = "name", nullable = false, length = 100)
    String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    String description;
    
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