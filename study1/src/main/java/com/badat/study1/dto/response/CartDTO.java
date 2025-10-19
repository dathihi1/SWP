package com.badat.study1.dto.response;

import com.badat.study1.model.Cart;
import com.badat.study1.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private Long id;
    private Long userId;
    private List<CartItemDTO> items;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CartDTO fromEntity(Cart cart) {
        if (cart == null) {
            return null;
        }

        List<CartItemDTO> itemDTOs = cart.getItems() != null 
            ? cart.getItems().stream()
                .map(CartItemDTO::fromEntity)
                .collect(Collectors.toList())
            : List.of();

        BigDecimal totalAmount = itemDTOs.stream()
            .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartDTO.builder()
            .id(cart.getId())
            .userId(cart.getUser().getId())
            .items(itemDTOs)
            .totalAmount(totalAmount)
            .createdAt(cart.getCreatedAt())
            .updatedAt(cart.getUpdatedAt())
            .build();
    }
}
