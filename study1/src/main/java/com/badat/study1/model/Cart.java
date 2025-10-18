package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List; // Import List

@Entity
@Table(name = "cart")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Khóa ngoại tới User
    @Column(name = "user_id", nullable = false, unique = true)
    Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;

    // Quan hệ OneToMany tới CartItem. Dùng LAZY, sẽ dùng FETCH JOIN khi hiển thị.
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<CartItem> items;

    /**
     * Tính tổng giá trị của tất cả CartItem trong giỏ hàng.
     * @return Tổng giá trị (double)
     */
    public double getTotal() {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        return items.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
    }
}
