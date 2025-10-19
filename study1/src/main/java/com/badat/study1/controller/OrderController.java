package com.badat.study1.controller;

import com.badat.study1.model.Order;
import com.badat.study1.model.Warehouse;
import com.badat.study1.model.User;
import com.badat.study1.service.OrderService;
import com.badat.study1.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    private final WarehouseRepository warehouseRepository;
    
    /**
     * Lấy danh sách orders của user hiện tại với thông tin warehouse
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<Map<String, Object>>> getMyOrders() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            List<Order> orders = orderService.getOrdersByBuyer(user.getId());
            
            // Chuyển đổi orders thành format mới với thông tin warehouse
            List<Map<String, Object>> orderDetails = orders.stream().map(order -> {
                try {
                    Warehouse warehouse = warehouseRepository.findById(order.getWarehouseId()).orElse(null);
                    Map<String, Object> orderMap = new HashMap<>();
                    orderMap.put("id", order.getId());
                    orderMap.put("orderCode", order.getOrderCode());
                    orderMap.put("status", order.getStatus().name());
                    orderMap.put("totalAmount", order.getTotalAmount());
                    orderMap.put("unitPrice", order.getUnitPrice());
                    orderMap.put("quantity", order.getQuantity());
                    orderMap.put("createdAt", order.getCreatedAt());
                    orderMap.put("itemData", warehouse != null ? warehouse.getItemData() : "N/A");
                    orderMap.put("warehouseId", order.getWarehouseId());
                    return orderMap;
                } catch (Exception e) {
                    log.error("Error processing order {}: {}", order.getId(), e.getMessage());
                    Map<String, Object> orderMap = new HashMap<>();
                    orderMap.put("id", order.getId());
                    orderMap.put("orderCode", order.getOrderCode());
                    orderMap.put("status", order.getStatus().name());
                    orderMap.put("totalAmount", order.getTotalAmount());
                    orderMap.put("unitPrice", order.getUnitPrice());
                    orderMap.put("quantity", order.getQuantity());
                    orderMap.put("createdAt", order.getCreatedAt());
                    orderMap.put("itemData", "Error loading data");
                    orderMap.put("warehouseId", order.getWarehouseId());
                    return orderMap;
                }
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(orderDetails);
            
        } catch (Exception e) {
            log.error("Error getting user orders", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Lấy danh sách orders của seller
     */
    @GetMapping("/seller-orders")
    public ResponseEntity<List<Order>> getSellerOrders() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            List<Order> orders = orderService.getOrdersBySeller(user.getId());
            
            return ResponseEntity.ok(orders);
            
        } catch (Exception e) {
            log.error("Error getting seller orders", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Lấy order theo order code
     */
    @GetMapping("/{orderCode}")
    public ResponseEntity<Order> getOrderByCode(@PathVariable String orderCode) {
        try {
            Order order = orderService.getOrderByCode(orderCode)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            return ResponseEntity.ok(order);
            
        } catch (Exception e) {
            log.error("Error getting order by code: {}", orderCode, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Cập nhật trạng thái order
     */
    @PostMapping("/{orderId}/status")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        
        try {
            String statusStr = request.get("status");
            Order.Status status = Order.Status.valueOf(statusStr.toUpperCase());
            
            Order updatedOrder = orderService.updateOrderStatus(orderId, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order status updated successfully");
            response.put("order", updatedOrder);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating order status: {}", orderId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update order status: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Lấy thống kê orders của user
     */
    @GetMapping("/stats")
    public ResponseEntity<OrderService.OrderStats> getOrderStats() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            OrderService.OrderStats stats = orderService.getOrderStatsForBuyer(user.getId());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error getting order stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}