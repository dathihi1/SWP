package com.badat.study1.dto.response;

import com.badat.study1.model.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private String subcategory;
    private String status;
    private String uniqueKey;
    private Long shopId;
    private String shopName; // tên shop hiển thị ở giỏ hàng
    private Long productId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductDTO fromEntity(ProductVariant productVariant) {
        if (productVariant == null) {
            return null;
        }

        return ProductDTO.builder()
            .id(productVariant.getId())
            .name(productVariant.getName())
            .price(productVariant.getPrice())
            .quantity(productVariant.getQuantity())
            .subcategory(productVariant.getSubcategory())
            .status(productVariant.getStatus() != null ? productVariant.getStatus().name() : null)
            .uniqueKey(productVariant.getUniqueKey())
            .shopId(productVariant.getShop() != null ? productVariant.getShop().getId() : null)
            .shopName(productVariant.getShop() != null ? productVariant.getShop().getShopName() : null)
            .productId(productVariant.getProduct() != null ? productVariant.getProduct().getId() : null)
            .createdAt(productVariant.getCreatedAt())
            .updatedAt(productVariant.getUpdatedAt())
            .build();
    }
}
