package com.badat.study1.dto.response;

import com.badat.study1.model.Product;
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
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String type;
    private String status;
    private String uniqueKey;
    private Long shopId;
    private Long stallId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductDTO fromEntity(Product product) {
        if (product == null) {
            return null;
        }

        return ProductDTO.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .quantity(product.getQuantity())
            .type(product.getType())
            .status(product.getStatus() != null ? product.getStatus().name() : null)
            .uniqueKey(product.getUniqueKey())
            .shopId(product.getShop() != null ? product.getShop().getId() : null)
            .stallId(product.getStall() != null ? product.getStall().getId() : null)
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }
}
