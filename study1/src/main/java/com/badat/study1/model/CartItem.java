package com.badat.study1.model;

import com.fasterxml.jackson.annotation.JsonBackReference; // <-- THÊM DÒNG NÀY
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cart_item")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonBackReference // <-- THÊM DÒNG NÀY
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    public double getSubtotal() {
        java.math.BigDecimal price = product != null && product.getPrice() != null
                ? product.getPrice()
                : java.math.BigDecimal.ZERO;
        return price.multiply(java.math.BigDecimal.valueOf(quantity)).doubleValue();
    }
}