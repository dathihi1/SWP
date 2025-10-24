package com.badat.study1.service;

import com.badat.study1.model.*;
import com.badat.study1.repository.CartItemRepository;
import com.badat.study1.repository.CartRepository;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Lấy người dùng hiện tại từ Spring Security Context.
     * @return Đối tượng User đã đăng nhập.
     * @throws IllegalStateException nếu người dùng chưa xác thực hoặc Principal không đúng kiểu.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Xử lý kiểm tra người dùng đã đăng nhập và cast đúng kiểu
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            // Trong môi trường thực tế, có thể trả về một User mặc định hoặc xử lý lại
            // Ở đây, ta ném ngoại lệ vì giỏ hàng yêu cầu người dùng đã xác thực.
            throw new IllegalStateException("User not authenticated or principal type is incorrect.");
        }
        return (User) auth.getPrincipal();
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

    @Transactional(readOnly = true)
    public Cart getMyCartWithItems() {
        return getOrCreateMyCart(true);
    }

    /**
     * Xóa tất cả sản phẩm khỏi giỏ hàng
     */
    @Transactional
    public Cart clearCart() {
        Cart cart = getOrCreateMyCart(false);
        
        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
            cartRepository.save(cart);
        }
        
        return cart;
    }
    @Transactional(readOnly = true)
    public CartPaymentInfo getCartPaymentInfo() {
        Cart cart = getOrCreateMyCart(true);
        
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return CartPaymentInfo.builder()
                    .cartId(cart.getId())
                    .totalItems(0)
                    .totalAmount(BigDecimal.ZERO)
                    .cartItems(new java.util.ArrayList<>())
                    .build();
        }
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Map<String, Object>> cartItems = new java.util.ArrayList<>();
        
        for (CartItem item : cart.getItems()) {
            BigDecimal itemTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
            
            // Lấy warehouseId thực tế từ warehouse đầu tiên có sẵn
            Long warehouseId = item.getProduct().getId(); // Default fallback
            try {
                Optional<Warehouse> warehouse = warehouseRepository.findFirstByProductIdAndLockedFalseAndIsDeleteFalse(item.getProduct().getId());
                if (warehouse.isPresent()) {
                    warehouseId = warehouse.get().getId();
                }
            } catch (Exception e) {
                log.warn("Failed to get warehouse for product {}, using productId as fallback", item.getProduct().getId());
            }
            
            Map<String, Object> cartItemData = new java.util.HashMap<>();
            cartItemData.put("productId", item.getProduct().getId());
            cartItemData.put("name", item.getProduct().getName());
            cartItemData.put("quantity", item.getQuantity());
            cartItemData.put("price", item.getProduct().getPrice());
            cartItemData.put("warehouseId", warehouseId);
            
            // Lấy stallId an toàn
            Long stallId = null;
            try {
                if (item.getProduct().getStall() != null) {
                    stallId = item.getProduct().getStall().getId();
                } else {
                    stallId = item.getProduct().getStallId(); // Fallback
                }
            } catch (Exception e) {
                log.warn("Failed to get stallId for product {}, using fallback", item.getProduct().getId());
                stallId = item.getProduct().getStallId();
            }
            cartItemData.put("stallId", stallId);
            
            cartItemData.put("shopId", item.getProduct().getShopId());
            cartItemData.put("sellerId", item.getProduct().getShop() != null ? item.getProduct().getShop().getUserId() : null);
            
            
            cartItems.add(cartItemData);
        }
        
        return CartPaymentInfo.builder()
                .cartId(cart.getId())
                .totalItems(cart.getItems().size())
                .totalAmount(totalAmount)
                .cartItems(cartItems)
                .build();
    }

    /**
     * DTO cho thông tin cart thanh toán
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CartPaymentInfo {
        private Long cartId;
        private Integer totalItems;
        private BigDecimal totalAmount;
        private List<Map<String, Object>> cartItems;
    }
}
