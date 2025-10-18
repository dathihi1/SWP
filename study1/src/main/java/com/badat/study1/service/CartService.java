package com.badat.study1.service;

import com.badat.study1.model.*;
import com.badat.study1.repository.CartItemRepository;
import com.badat.study1.repository.CartRepository;
import com.badat.study1.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Xử lý kiểm tra người dùng đã đăng nhập và cast đúng kiểu
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            throw new IllegalStateException("User not authenticated or principal type is incorrect.");
        }
        return (User) auth.getPrincipal();
    }

    /**
     * Get or create the current user's cart.
     * @param fetchItems If true, uses Fetch Join to load CartItems and Product eagerly (for display).
     * @return The loaded or newly created Cart.
     */
    @Transactional
    public Cart getOrCreateMyCart(boolean fetchItems) {
        User user = getCurrentUser();
        Optional<Cart> existing;

        if (fetchItems) {
            // Use Fetch Join when displaying the cart (Controller calls this with true)
            existing = cartRepository.findByUserWithItems(user);
        } else {
            // Use standard query for internal CRUD operations
            existing = cartRepository.findByUser(user);
        }

        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new Cart if none exists
        Cart cart = Cart.builder()
                .user(user)
                .userId(user.getId())
                .build();
        return cartRepository.save(cart);
    }

    // Default method for internal CRUD operations (no Fetch Items)
    @Transactional
    public Cart getOrCreateMyCart() {
        return getOrCreateMyCart(false);
    }

    @Transactional
    public Cart addProduct(Long productId, int quantity) {
        if (quantity <= 0) {
            quantity = 1;
        }

        Cart cart = getOrCreateMyCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Check if CartItem already exists
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            // Add new CartItem
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(item);

            // Update the list of items in Cart (essential for subsequent operations)
            if (cart.getItems() != null) {
                cart.getItems().add(item);
            }
        }

        // Return the fully fetched Cart to the Controller/client
        return getOrCreateMyCart(true);
    }

    @Transactional
    public Cart updateQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            return removeProduct(productId);
        }
        Cart cart = getOrCreateMyCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new IllegalArgumentException("Item not in cart"));
        item.setQuantity(quantity);
        cartItemRepository.save(item);

        // Return the fully fetched Cart
        return getOrCreateMyCart(true);
    }

    @Transactional
    public Cart removeProduct(Long productId) {
        Cart cart = getOrCreateMyCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new IllegalArgumentException("Item not in cart"));

        // Remove item from the list in Cart before deleting the entity
        if (cart.getItems() != null) {
            cart.getItems().remove(item);
        }
        cartItemRepository.delete(item);

        // Return the fully fetched Cart (with the item removed)
        return getOrCreateMyCart(true);
    }

    @Transactional
    public Cart clearCart() {
        Cart cart = getOrCreateMyCart();
        // Delete all CartItems associated with this Cart
        if (cart.getItems() != null) {
            cart.getItems().forEach(cartItemRepository::delete);
            cart.getItems().clear();
        }
        cartRepository.save(cart); // Save Cart to update the relationship

        // Return the empty, fully fetched Cart
        return getOrCreateMyCart(true);
    }
}
