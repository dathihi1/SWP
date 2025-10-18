package com.badat.study1.controller;

import com.badat.study1.model.Cart;
import com.badat.study1.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    /**
     * Lấy thông tin giỏ hàng hiện tại của người dùng.
     * SỬA: Gọi service với tham số true để sử dụng Fetch Join, tải đầy đủ CartItem và Product.
     */
    @GetMapping
    public Cart getMyCart() {
        return cartService.getOrCreateMyCart(true);
    }

    @PostMapping("/add/{productId}")
    public Cart addToCart(@PathVariable Long productId, @RequestParam(defaultValue = "1") int quantity) {
        return cartService.addProduct(productId, quantity);
    }

    @PutMapping("/update/{productId}")
    public Cart updateQuantity(@PathVariable Long productId, @RequestParam int quantity) {
        return cartService.updateQuantity(productId, quantity);
    }

    @DeleteMapping("/remove/{productId}")
    public Cart removeItem(@PathVariable Long productId) {
        return cartService.removeProduct(productId);
    }

    @DeleteMapping("/clear")
    public Cart clearCart() {
        return cartService.clearCart();
    }
}
