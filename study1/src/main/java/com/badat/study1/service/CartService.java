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

    /**
     * Lấy người dùng hiện tại từ Spring Security Context.
     * @return Đối tượng User đã đăng nhập.
     * @throws IllegalStateException nếu người dùng chưa xác thực hoặc Principal không đúng kiểu.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Xử lý kiểm tra người dùng đã đăng nhập và cast đúng kiểu
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated.");
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        } else {
            // Log the actual principal type for debugging
            System.out.println("DEBUG: Principal type is " + principal.getClass().getName());
            throw new IllegalStateException("Principal type is incorrect. Expected User, got: " + principal.getClass().getName());
        }
    }

    /**
     * Lấy hoặc tạo giỏ hàng cho người dùng hiện tại.
     * @param fetchItems Nếu true, sử dụng Fetch Join để tải đầy đủ CartItems và Product eagerly (cho hiển thị/trả về API).
     * @return Giỏ hàng đã tải hoặc mới tạo.
     */
    @Transactional
    public Cart getOrCreateMyCart(boolean fetchItems) {
        User user = getCurrentUser();
        Optional<Cart> existing;

        if (fetchItems) {
            // Tải đầy đủ CartItem và Product cho việc hiển thị tối ưu (tránh N+1 problem)
            existing = cartRepository.findByUserWithItems(user);
        } else {
            // Tải Cart cơ bản, không cần CartItem cho logic thêm/xóa/cập nhật
            existing = cartRepository.findByUser(user);
        }

        if (existing.isPresent()) {
            return existing.get();
        }

        // Tạo giỏ hàng mới (đảm bảo userId và user được set)
        Cart cart = Cart.builder().user(user).userId(user.getId()).build();
        return cartRepository.save(cart);
    }

    /**
     * Overload cho các phương thức nội bộ không cần fetch items ngay lập tức (chỉ cần Cart object).
     */
    private Cart getOrCreateMyCart() {
        return getOrCreateMyCart(false);
    }

    @Transactional
    public Cart addProduct(Long productId, int quantity) {
        if (quantity <= 0) {
            quantity = 1; // Đảm bảo số lượng luôn dương khi thêm
        }

        Cart cart = getOrCreateMyCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Tìm CartItem hiện có
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);

        if (existingItem.isPresent()) {
            // Cập nhật số lượng
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            // Tạo CartItem mới
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            // Thêm vào list trong Cart để Hibernate biết và quản lý mối quan hệ
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        // Trả về Cart đã được tải đầy đủ (Fetch Join) cho phản hồi API
        return getOrCreateMyCart(true);
    }

    @Transactional
    public Cart updateQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            // Nếu số lượng <= 0, coi như xóa sản phẩm
            return removeProduct(productId);
        }

        Cart cart = getOrCreateMyCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new IllegalArgumentException("Item not in cart"));

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        // Trả về Cart đã được tải đầy đủ (Fetch Join)
        return getOrCreateMyCart(true);
    }

    @Transactional
    public Cart removeProduct(Long productId) {
        Cart cart = getOrCreateMyCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new IllegalArgumentException("Item not in cart"));

        // Xóa item khỏi list trong Cart trước khi xóa entity
        if (cart.getItems() != null) {
            cart.getItems().remove(item);
        }
        cartItemRepository.delete(item);

        // Trả về Cart đã được tải đầy đủ (Fetch Join)
        return getOrCreateMyCart(true);
    }

    @Transactional
    public Cart clearCart() {
        Cart cart = getOrCreateMyCart();

        // Xóa tất cả CartItems liên kết với Cart này
        if (cart.getItems() != null) {
            cart.getItems().forEach(cartItemRepository::delete);
            cart.getItems().clear(); // Dọn dẹp list trong đối tượng Cart
        }

        // Trả về Cart đã được tải đầy đủ (Fetch Join) (hiện tại sẽ là giỏ hàng rỗng)
        return getOrCreateMyCart(true);
    }
}
