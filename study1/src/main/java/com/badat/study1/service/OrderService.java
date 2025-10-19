package com.badat.study1.service;

import com.badat.study1.model.*;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.repository.StallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final StallRepository stallRepository;
    
    /**
     * Tạo order mới từ thông tin giao dịch
     */
    @Transactional
    public Order createOrder(Long buyerId, Long sellerId, Long shopId, Long stallId, 
                            Long productId, Long warehouseId, Integer quantity, 
                            BigDecimal unitPrice, String paymentMethod, String notes) {
        return createOrder(buyerId, sellerId, shopId, stallId, productId, warehouseId, 
                          quantity, unitPrice, paymentMethod, notes, null);
    }
    
    /**
     * Tạo order mới từ thông tin giao dịch với orderCode tùy chỉnh
     */
    @Transactional
    public Order createOrder(Long buyerId, Long sellerId, Long shopId, Long stallId, 
                            Long productId, Long warehouseId, Integer quantity, 
                            BigDecimal unitPrice, String paymentMethod, String notes, String customOrderCode) {
        
        log.info("Creating order for buyer: {}, seller: {}, product: {}, quantity: {}", 
                buyerId, sellerId, productId, quantity);
        
        // Tính toán các số tiền
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        
        // Lấy commission rate từ stall
        BigDecimal commissionRate = getCommissionRate(stallId);
        BigDecimal commissionAmount = totalAmount.multiply(commissionRate).divide(BigDecimal.valueOf(100));
        BigDecimal sellerAmount = totalAmount.subtract(commissionAmount);
        
        // Tạo order code unique hoặc sử dụng customOrderCode
        String orderCode = customOrderCode != null ? customOrderCode : generateOrderCode();
        
        Order order = Order.builder()
                .buyerId(buyerId)
                .sellerId(sellerId)
                .shopId(shopId)
                .stallId(stallId)
                .productId(productId)
                .warehouseId(warehouseId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .sellerAmount(sellerAmount)
                .status(Order.Status.PENDING)
                .paymentMethod(paymentMethod)
                .orderCode(orderCode)
                .notes(notes)
                .build();
        
        Order savedOrder = orderRepository.save(order);
        
        log.info("Order created successfully: {} with total amount: {} VND, commission: {} VND", 
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
        Order updatedOrder = orderRepository.save(order);
        
        log.info("Order {} status updated to {}", orderId, status);
        return updatedOrder;
    }
    
    /**
     * Lấy danh sách orders của buyer
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }
    
    /**
     * Lấy danh sách orders của seller
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersBySeller(Long sellerId) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }
    
    /**
     * Lấy order theo order code
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode);
    }
    
    /**
     * Lấy commission rate từ stall
     */
    private BigDecimal getCommissionRate(Long stallId) {
        try {
            Stall stall = stallRepository.findById(stallId)
                    .orElseThrow(() -> new RuntimeException("Stall not found: " + stallId));
            
            // Nếu stall có discount_percentage, sử dụng làm commission rate
            if (stall.getDiscountPercentage() != null) {
                return BigDecimal.valueOf(stall.getDiscountPercentage());
            }
            
            // Mặc định commission rate là 5%
            return BigDecimal.valueOf(5.0);
            
        } catch (Exception e) {
            log.warn("Failed to get commission rate for stall {}, using default 5%", stallId);
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
        Long totalOrders = orderRepository.countBySellerIdAndStatus(sellerId, Order.Status.COMPLETED);
        Long pendingOrders = orderRepository.countBySellerIdAndStatus(sellerId, Order.Status.PENDING);
        
        return OrderStats.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
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
