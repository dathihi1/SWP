package com.badat.study1.dto.response;

import com.badat.study1.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Long id;
    private Long cartId;
    private ProductDTO product;
    private Integer quantity;

    public static CartItemDTO fromEntity(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }

        return CartItemDTO.builder()
            .id(cartItem.getId())
            .cartId(cartItem.getCart().getId())
            .product(ProductDTO.fromEntity(cartItem.getProduct()))
            .quantity(cartItem.getQuantity())
            .build();
    }
}
