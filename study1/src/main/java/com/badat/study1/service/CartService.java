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
        return (User) auth.getPrincipal();
    }

    @Transactional
    public Cart getOrCreateMyCart() {
        User user = getCurrentUser();
        Optional<Cart> existing = cartRepository.findByUser(user);
        if (existing.isPresent()) {
            return existing.get();
        }
        Cart cart = Cart.builder().user(user).build();
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart addProduct(Long productId, int quantity) {
        if (quantity <= 0) {
            quantity = 1;
        }
        Cart cart = getOrCreateMyCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Optional<CartItem> existing = cartItemRepository.findByCartAndProduct(cart, product);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }
        return cartRepository.findById(cart.getId()).orElse(cart);
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
        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    @Transactional
    public Cart removeProduct(Long productId) {
        Cart cart = getOrCreateMyCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new IllegalArgumentException("Item not in cart"));
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    @Transactional
    public Cart clearCart() {
        Cart cart = getOrCreateMyCart();
        cart.getItems().forEach(cartItemRepository::delete);
        cart.getItems().clear();
        return cartRepository.save(cart);
    }
}



