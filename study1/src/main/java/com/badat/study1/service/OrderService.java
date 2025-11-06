package com.badat.study1.service;

import com.badat.study1.model.*;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ShopRepository shopRepository;

    /**
     * Tạo order mới với nhiều sản phẩm từ cart
     */
    @Transactional
    public Order createOrderFromCart(Long buyerId, List<Map<String, Object>> cartItems,
                                     String paymentMethod, String notes) {
        return createOrderFromCart(buyerId, cartItems, paymentMethod, notes, null);
    }

    /**
     * Tạo order mới với nhiều sản phẩm từ cart với orderCode tùy chỉnh
     */
    @Transactional
    public Order createOrderFromCart(Long buyerId, List<Map<String, Object>> cartItems,
                                     String paymentMethod, String notes, String customOrderCode) {

        log.info("Creating order from cart for buyer: {} with {} items", buyerId, cartItems.size());

        // Tạo order code unique hoặc sử dụng customOrderCode
        String orderCode = customOrderCode != null ? customOrderCode : generateOrderCode();

        // Lấy thông tin shop, stall, seller từ cart item đầu tiên
        Map<String, Object> firstCartItem = cartItems.get(0);
        Long stallId = Long.valueOf(firstCartItem.get("stallId").toString());

        // Lấy thông tin product để lấy shopId
        Product product = productRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + stallId));

        // Lấy sellerId từ shopId
        Shop shop = shopRepository.findById(product.getShopId())
                .orElseThrow(() -> new RuntimeException("Shop not found: " + product.getShopId()));
        Long sellerId = shop.getUserId();

        // Tạo Order chính
        Order order = Order.builder()
                .buyerId(buyerId)
                .shopId(product.getShopId())
                .productId(stallId)
                .sellerId(sellerId)
                .totalAmount(BigDecimal.ZERO)
                .totalCommissionAmount(BigDecimal.ZERO)
                .totalSellerAmount(BigDecimal.ZERO)
                .status(Order.Status.PENDING)
                .paymentMethod(paymentMethod)
                .orderCode(orderCode)
                .notes(notes)
                .build();

        Order savedOrder = orderRepository.save(order);

        // Tạo OrderItem cho từng sản phẩm trong cart
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCommissionAmount = BigDecimal.ZERO;
        BigDecimal totalSellerAmount = BigDecimal.ZERO;

        for (Map<String, Object> cartItem : cartItems) {
            try {
                // Validate và lấy các giá trị từ cartItem
                Object productIdObj = cartItem.get("productId");
                Object warehouseIdObj = cartItem.get("warehouseId");
                Object quantityObj = cartItem.get("quantity");
                Object priceObj = cartItem.get("price");
                Object stallIdObj = cartItem.get("stallId");
                Object variantNameObj = cartItem.get("name");

                if (productIdObj == null || warehouseIdObj == null || quantityObj == null ||
                        priceObj == null || stallIdObj == null) {
                    log.error("Missing required fields in cart item: {}", cartItem);
                    throw new RuntimeException("Missing required fields in cart item");
                }

                Long productId = Long.valueOf(productIdObj.toString());
                Long warehouseId = Long.valueOf(warehouseIdObj.toString());
                Integer quantity = Integer.valueOf(quantityObj.toString());
                BigDecimal unitPrice = new BigDecimal(priceObj.toString());
                Long itemStallId = Long.valueOf(stallIdObj.toString());

                // Lấy thông tin stall để tính commission
                BigDecimal commissionRate = getCommissionRate(itemStallId);

                // Tạo nhiều OrderItem riêng biệt cho mỗi quantity
                for (int i = 0; i < quantity; i++) {
                    // Tính toán các số tiền cho từng OrderItem (quantity = 1)
                    BigDecimal itemTotalAmount = unitPrice; // Mỗi item có quantity = 1
                    BigDecimal itemCommissionAmount = itemTotalAmount.multiply(commissionRate).divide(BigDecimal.valueOf(100));
                    BigDecimal itemSellerAmount = itemTotalAmount.subtract(itemCommissionAmount);

                    // Lấy sellerId từ warehouse (tạm thời set 0, sẽ được cập nhật sau)
                    Long itemSellerId = 0L; // Sẽ được cập nhật trong PaymentQueueService

                    // Tạo OrderItem với quantity = 1
                    OrderItem orderItem = OrderItem.builder()
                            .orderId(savedOrder.getId())
                            .productVariantId(productId)
                            .warehouseId(warehouseId) // Sẽ được cập nhật với warehouseId thực tế
                            .quantity(1) // Mỗi OrderItem có quantity = 1
                            .unitPrice(unitPrice)
                            .totalAmount(itemTotalAmount)
                            .commissionRate(commissionRate)
                            .commissionAmount(itemCommissionAmount)
                            .sellerAmount(itemSellerAmount)
                            .sellerId(itemSellerId) // Sẽ được cập nhật trong PaymentQueueService
                            .shopId(product.getShopId()) // Thêm shop_id
                            .productId(stallId) // Thêm product_id (product parent)
                            .productVariantName(variantNameObj != null ? variantNameObj.toString() : null)
                            .status(OrderItem.Status.PENDING)
                            .notes("Order item from cart - item " + (i + 1) + " of " + quantity)
                            .build();

                    orderItemRepository.save(orderItem);

                    // Cộng dồn vào tổng của Order
                    totalAmount = totalAmount.add(itemTotalAmount);
                    totalCommissionAmount = totalCommissionAmount.add(itemCommissionAmount);
                    totalSellerAmount = totalSellerAmount.add(itemSellerAmount);

                    log.info("Created order item {} for product: {}, warehouseId: {}, amount: {}",
                            i + 1, productId, warehouseId, itemTotalAmount);
                }

                log.info("Created {} order items for product: {} with total amount: {}",
                        quantity, productId, unitPrice.multiply(BigDecimal.valueOf(quantity)));

            } catch (Exception e) {
                log.error("Failed to create order item for cart item: {}", cartItem, e);
                throw new RuntimeException("Failed to create order item: " + e.getMessage());
            }
        }

        // Cập nhật tổng tiền của Order
        savedOrder.setTotalAmount(totalAmount);
        savedOrder.setTotalCommissionAmount(totalCommissionAmount);
        savedOrder.setTotalSellerAmount(totalSellerAmount);
        savedOrder.setStatus(Order.Status.COMPLETED);

        Order finalOrder = orderRepository.save(savedOrder);

        log.info("Order created successfully: {} with total amount: {} VND, commission: {} VND",
                finalOrder.getId(), totalAmount, totalCommissionAmount);

        return finalOrder;
    }

    /**
     * Tạo order đơn giản cho một sản phẩm (backward compatibility)
     */
    @Transactional
    public Order createSimpleOrder(Long buyerId, Long sellerId, Long shopId, Long stallId,
                                   Long productId, Long warehouseId, Integer quantity,
                                   BigDecimal unitPrice, String paymentMethod, String notes) {
        return createSimpleOrder(buyerId, sellerId, shopId, stallId, productId, warehouseId,
                quantity, unitPrice, paymentMethod, notes, null);
    }

    /**
     * Tạo order đơn giản cho một sản phẩm với orderCode tùy chỉnh (backward compatibility)
     */
    @Transactional
    public Order createSimpleOrder(Long buyerId, Long sellerId, Long shopId, Long stallId,
                                   Long productId, Long warehouseId, Integer quantity,
                                   BigDecimal unitPrice, String paymentMethod, String notes, String customOrderCode) {

        log.info("Creating simple order for buyer: {}, seller: {}, product: {}, quantity: {}",
                buyerId, sellerId, productId, quantity);

        // Tạo order code unique hoặc sử dụng customOrderCode
        String orderCode = customOrderCode != null ? customOrderCode : generateOrderCode();

        // Tính toán các số tiền
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // Lấy commission rate từ stall
        BigDecimal commissionRate = getCommissionRate(stallId);
        BigDecimal commissionAmount = totalAmount.multiply(commissionRate).divide(BigDecimal.valueOf(100));
        BigDecimal sellerAmount = totalAmount.subtract(commissionAmount);

        // Tạo Order chính
        Order order = Order.builder()
                .buyerId(buyerId)
                .shopId(shopId)
                .productId(stallId)
                .sellerId(sellerId)
                .totalAmount(totalAmount)
                .totalCommissionAmount(commissionAmount)
                .totalSellerAmount(sellerAmount)
                .status(Order.Status.COMPLETED)
                .paymentMethod(paymentMethod)
                .orderCode(orderCode)
                .notes(notes)
                .build();

        Order savedOrder = orderRepository.save(order);

        // Tạo OrderItem
        String variantName = null;
        try {
            variantName = productVariantRepository.findById(productId)
                    .map(ProductVariant::getName)
                    .orElse(null);
        } catch (Exception ignored) {}

        OrderItem orderItem = OrderItem.builder()
                .orderId(savedOrder.getId())
                .productVariantId(productId)
                .warehouseId(warehouseId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .sellerAmount(sellerAmount)
                .sellerId(sellerId) // Sử dụng sellerId được truyền vào
                .shopId(shopId) // Thêm shop_id
                .productId(stallId) // Thêm product_id (product parent)
                .productVariantName(variantName)
                .status(OrderItem.Status.COMPLETED)
                .notes("Simple order item")
                .build();

        orderItemRepository.save(orderItem);

        log.info("Simple order created successfully: {} with total amount: {} VND, commission: {} VND",
                savedOrder.getId(), totalAmount, commissionAmount);

        return savedOrder;
    }

    /**
     * Cập nhật trạng thái order
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.Status status) {
        log.info("Updating order {} status to {}", orderId, status);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setStatus(status);

        // Cập nhật trạng thái tất cả OrderItem
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        for (OrderItem orderItem : orderItems) {
            orderItem.setStatus(OrderItem.Status.valueOf(status.name()));
            orderItemRepository.save(orderItem);
        }

        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} status updated to {}", orderId, status);
        return updatedOrder;
    }

    /**
     * Cập nhật trạng thái tất cả orders trong một order group (cùng orderCode)
     */
    @Transactional
    public void updateOrderStatusByOrderCode(String orderCode, Order.Status status) {
        log.info("Updating all orders with orderCode {} to status {}", orderCode, status);

        // Tìm tất cả orders có orderCode chứa chuỗi này
        List<Order> orders = orderRepository.findByOrderCodeContaining(orderCode);

        if (orders.isEmpty()) {
            log.warn("No orders found with orderCode containing: {}", orderCode);
            return;
        }

        for (Order order : orders) {
            order.setStatus(status);

            // Cập nhật trạng thái tất cả OrderItem
            List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());
            for (OrderItem orderItem : orderItems) {
                orderItem.setStatus(OrderItem.Status.valueOf(status.name()));
                orderItemRepository.save(orderItem);
            }

            log.info("Updating order {} (ID: {}) status to {}", order.getOrderCode(), order.getId(), status);
        }

        orderRepository.saveAll(orders);
        log.info("Successfully updated {} orders with orderCode containing {} to status {}", orders.size(), orderCode, status);
    }

    /**
     * Lấy danh sách orders của buyer
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdWithOrderItems(buyerId);
    }

    /**
     * Lấy danh sách orders của buyer với filter
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByBuyerWithFilters(Long buyerId, String startDate, String endDate, 
                                                   String searchSeller, String searchProduct, String sortBy) {
        return orderRepository.findByBuyerIdWithFilters(buyerId, startDate, endDate, searchSeller, searchProduct, sortBy);
    }

    /**
     * Lấy danh sách orders của seller (từ OrderItem)
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersBySeller(Long sellerId) {
        // Tìm tất cả OrderItem có sellerId (từ warehouse.user)
        List<OrderItem> orderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(sellerId);

        // Lấy danh sách Order ID unique
        List<Long> orderIds = orderItems.stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .toList();

        return orderRepository.findAllById(orderIds);
    }

    /**
     * Lấy order theo order code
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode);
    }

    /**
     * Lấy order với chi tiết OrderItem
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderWithItems(Long orderId) {
        return orderRepository.findByIdWithOrderItems(orderId);
    }

    /**
     * Lấy commission rate từ shop
     */
    private BigDecimal getCommissionRate(Long productId) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            // Lấy shop để lấy commission rate
            Shop shop = shopRepository.findById(product.getShopId())
                    .orElseThrow(() -> new RuntimeException("Shop not found: " + product.getShopId()));

            // Sử dụng commission rate từ shop
            if (shop.getCommissionRate() != null) {
                return shop.getCommissionRate();
            }

            // Mặc định commission rate là 5%
            return BigDecimal.valueOf(5.0);

        } catch (Exception e) {
            log.warn("Failed to get commission rate for product {}, using default 5%", productId);
            return BigDecimal.valueOf(5.0);
        }
    }

    /**
     * Tạo order code unique
     */
    private String generateOrderCode() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD_" + timestamp + "_" + uuid;
    }

    /**
     * Tính toán thống kê cho seller
     */
    @Transactional(readOnly = true)
    public OrderStats getOrderStatsForSeller(Long sellerId) {
        // Đếm từ OrderItem thay vì Order
        Long totalOrderItems = orderItemRepository.countByWarehouseUserAndStatus(sellerId, OrderItem.Status.COMPLETED);
        Long pendingOrderItems = orderItemRepository.countByWarehouseUserAndStatus(sellerId, OrderItem.Status.PENDING);

        return OrderStats.builder()
                .totalOrders(totalOrderItems)
                .pendingOrders(pendingOrderItems)
                .build();
    }

    /**
     * Tính toán thống kê cho buyer
     */
    @Transactional(readOnly = true)
    public OrderStats getOrderStatsForBuyer(Long buyerId) {
        Long totalOrders = orderRepository.countByBuyerIdAndStatus(buyerId, Order.Status.COMPLETED);
        Long pendingOrders = orderRepository.countByBuyerIdAndStatus(buyerId, Order.Status.PENDING);

        return OrderStats.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .build();
    }

    /**
     * DTO cho thống kê order
     */
    @lombok.Data
    @lombok.Builder
    public static class OrderStats {
        private Long totalOrders;
        private Long pendingOrders;
    }
}