package com.badat.study1.controller;

import com.badat.study1.dto.response.CartDTO;
import com.badat.study1.model.Cart;
import com.badat.study1.service.CartService;
import com.badat.study1.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final WarehouseRepository warehouseRepository;

    /**
     * Test endpoint để kiểm tra authentication
     */
    @GetMapping("/test")
    public ResponseEntity<?> testCart() {
        try {
            log.info("Testing cart endpoint - checking authentication");
            return ResponseEntity.ok().body("{\"message\": \"Cart API is working\", \"timestamp\": \"" + System.currentTimeMillis() + "\"}");
        } catch (Exception e) {
            log.error("Error in test endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin giỏ hàng hiện tại của người dùng.
     * SỬA: Gọi service với tham số true để sử dụng Fetch Join, tải đầy đủ CartItem và Product.
     */
    @GetMapping
    public ResponseEntity<?> getMyCart() {
        try {
            log.info("Getting cart for current user");
            Cart cart = cartService.getOrCreateMyCart(true);
            log.info("Cart retrieved successfully with {} items", cart.getItems() != null ? cart.getItems().size() : 0);
            
            // Convert to DTO to avoid Jackson serialization issues
            CartDTO cartDTO = CartDTO.fromEntity(cart);
            return ResponseEntity.ok(cartDTO);
        } catch (IllegalStateException e) {
            log.warn("User not authenticated: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        } catch (Exception e) {
            log.error("Error getting cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error getting cart: " + e.getMessage());
        }
    }

    @PostMapping("/add/{productId}")
    public ResponseEntity<?> addToCart(@PathVariable Long productId, @RequestParam(defaultValue = "1") int quantity) {
        try {
            log.info("Adding product {} to cart with quantity {}", productId, quantity);
            Cart cart = cartService.addProduct(productId, quantity);
            log.info("Product added successfully");
            
            // Convert to DTO to avoid Jackson serialization issues
            CartDTO cartDTO = CartDTO.fromEntity(cart);
            return ResponseEntity.ok(cartDTO);
        } catch (IllegalStateException e) {
            log.warn("User not authenticated: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        } catch (Exception e) {
            log.error("Error adding product to cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding product: " + e.getMessage());
        }
    }

    @PutMapping("/update/{productId}")
    public ResponseEntity<?> updateQuantity(@PathVariable Long productId, @RequestParam int quantity) {
        try {
            log.info("Updating quantity for product {} to {}", productId, quantity);
            Cart cart = cartService.updateQuantity(productId, quantity);
            
            // Convert to DTO to avoid Jackson serialization issues
            CartDTO cartDTO = CartDTO.fromEntity(cart);
            return ResponseEntity.ok(cartDTO);
        } catch (IllegalStateException e) {
            log.warn("User not authenticated: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        } catch (Exception e) {
            log.error("Error updating quantity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating quantity: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeItem(@PathVariable Long productId) {
        try {
            log.info("Removing product {} from cart", productId);
            Cart cart = cartService.removeProduct(productId);
            
            // Convert to DTO to avoid Jackson serialization issues
            CartDTO cartDTO = CartDTO.fromEntity(cart);
            return ResponseEntity.ok(cartDTO);
        } catch (IllegalStateException e) {
            log.warn("User not authenticated: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        } catch (Exception e) {
            log.error("Error removing product", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error removing product: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart() {
        try {
            log.info("Clearing cart");
            Cart cart = cartService.clearCart();
            
            // Convert to DTO to avoid Jackson serialization issues
            CartDTO cartDTO = CartDTO.fromEntity(cart);
            return ResponseEntity.ok(cartDTO);
        } catch (IllegalStateException e) {
            log.warn("User not authenticated: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        } catch (Exception e) {
            log.error("Error clearing cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error clearing cart: " + e.getMessage());
        }
    }
    
    @GetMapping("/check-stock/{productVariantId}")
    public ResponseEntity<?> checkStock(@PathVariable Long productVariantId) {
        try {
            log.info("Checking stock for product variant: {}", productVariantId);
            
            // Count available warehouse items for this product variant
            long availableStock = warehouseRepository.countByProductVariantIdAndLockedFalseAndIsDeleteFalse(productVariantId);
            
            return ResponseEntity.ok(Map.of(
                "productVariantId", productVariantId,
                "availableStock", availableStock,
                "message", "Available stock: " + availableStock
            ));
        } catch (Exception e) {
            log.error("Error checking stock for product variant: {}", productVariantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error checking stock: " + e.getMessage());
        }
    }
}
