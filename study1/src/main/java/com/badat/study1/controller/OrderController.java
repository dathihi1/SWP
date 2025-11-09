package com.badat.study1.controller;

import com.badat.study1.model.Order;
import com.badat.study1.model.User;
import com.badat.study1.model.Product;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.service.OrderService;
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
    private final ProductRepository productRepository;
    
    /**
     * Lấy danh sách orders của user hiện tại với thông tin OrderItem
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<Map<String, Object>>> getMyOrders(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchSeller,
            @RequestParam(required = false) String searchProduct,
            @RequestParam(required = false) String sortBy) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            List<Order> orders = orderService.getOrdersByBuyerWithFilters(
                user.getId(), startDate, endDate, searchSeller, searchProduct, sortBy);
            
            // Chuyển đổi orders thành format mới với thông tin OrderItem
            List<Map<String, Object>> orderDetails = orders.stream().map(order -> {
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("id", order.getId());
                orderMap.put("orderCode", order.getOrderCode());
                orderMap.put("status", order.getStatus().name());
                orderMap.put("totalAmount", order.getTotalAmount());
                orderMap.put("totalCommissionAmount", order.getTotalCommissionAmount());
                orderMap.put("totalSellerAmount", order.getTotalSellerAmount());
                orderMap.put("paymentMethod", order.getPaymentMethod());
                orderMap.put("createdAt", order.getCreatedAt());
                orderMap.put("notes", order.getNotes());
                
                // Thêm thông tin OrderItem nếu có
                if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                    List<Map<String, Object>> orderItems = order.getOrderItems().stream().map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("id", item.getId());
                        itemMap.put("productId", item.getProductId());
                        itemMap.put("productName", item.getProductVariant() != null ? item.getProductVariant().getName() : "N/A");
                        itemMap.put("warehouseId", item.getWarehouseId());
                        itemMap.put("quantity", item.getQuantity());
                        itemMap.put("unitPrice", item.getUnitPrice());
                        itemMap.put("totalAmount", item.getTotalAmount());
                        itemMap.put("commissionAmount", item.getCommissionAmount());
                        itemMap.put("sellerAmount", item.getSellerAmount());
                        itemMap.put("status", item.getStatus().name());
                        // Product info
                        itemMap.put("stallId", item.getProductId());
                        try {
                            if (item.getProductId() != null) {
                                itemMap.put("stallName", productRepository.findById(item.getProductId()).map(Product::getProductName).orElse("N/A"));
                            }
                        } catch (Exception ex) {
                            itemMap.put("stallName", "N/A");
                        }
                        
                        // Thêm thông tin warehouse với itemData
                        if (item.getWarehouse() != null) {
                            Map<String, Object> warehouseInfo = new HashMap<>();
                            warehouseInfo.put("id", item.getWarehouse().getId());
                            warehouseInfo.put("itemData", item.getWarehouse().getItemData());
                            warehouseInfo.put("sellerId", item.getWarehouse().getUser() != null ? item.getWarehouse().getUser().getId() : null);
                            if (item.getWarehouse().getUser() != null) {
                                String username = item.getWarehouse().getUser().getUsername();
                                String fullName = item.getWarehouse().getUser().getFullName();
                                warehouseInfo.put("sellerName", username);
                                warehouseInfo.put("sellerFullName", fullName);
                            } else {
                                warehouseInfo.put("sellerName", "N/A");
                                warehouseInfo.put("sellerFullName", null);
                            }
                            itemMap.put("warehouse", warehouseInfo);
                        } else {
                            itemMap.put("warehouse", null);
                        }
                        
                        return itemMap;
                    }).collect(Collectors.toList());
                    orderMap.put("orderItems", orderItems);
                    orderMap.put("itemCount", orderItems.size());
                } else {
                    orderMap.put("orderItems", new java.util.ArrayList<>());
                    orderMap.put("itemCount", 0);
                }
                
                return orderMap;
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
     * Lấy chi tiết order với OrderItem theo ID
     */
    @GetMapping("/detail/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderWithItems(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            Map<String, Object> orderDetail = new HashMap<>();
            orderDetail.put("id", order.getId());
            orderDetail.put("orderCode", order.getOrderCode());
            orderDetail.put("status", order.getStatus().name());
            orderDetail.put("totalAmount", order.getTotalAmount());
            orderDetail.put("totalCommissionAmount", order.getTotalCommissionAmount());
            orderDetail.put("totalSellerAmount", order.getTotalSellerAmount());
            orderDetail.put("paymentMethod", order.getPaymentMethod());
            orderDetail.put("createdAt", order.getCreatedAt());
            orderDetail.put("notes", order.getNotes());
            
            // Thêm thông tin buyer
            if (order.getBuyer() != null) {
                Map<String, Object> buyerInfo = new HashMap<>();
                buyerInfo.put("id", order.getBuyer().getId());
                buyerInfo.put("username", order.getBuyer().getUsername());
                buyerInfo.put("email", order.getBuyer().getEmail());
                orderDetail.put("buyer", buyerInfo);
            }
            
            // Thêm thông tin OrderItem
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                List<Map<String, Object>> orderItems = order.getOrderItems().stream().map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId());
                    itemMap.put("productId", item.getProductId());
                    itemMap.put("productName", item.getProductVariant() != null ? item.getProductVariant().getName() : "N/A");
                    itemMap.put("warehouseId", item.getWarehouseId());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("unitPrice", item.getUnitPrice());
                    itemMap.put("totalAmount", item.getTotalAmount());
                    itemMap.put("commissionAmount", item.getCommissionAmount());
                    itemMap.put("sellerAmount", item.getSellerAmount());
                    itemMap.put("status", item.getStatus().name());
                    itemMap.put("notes", item.getNotes());
                    // Product info
                    itemMap.put("stallId", item.getProductId());
                    try {
                        if (item.getProductId() != null) {
                            itemMap.put("stallName", productRepository.findById(item.getProductId()).map(Product::getProductName).orElse("N/A"));
                        }
                    } catch (Exception ex) {
                        itemMap.put("stallName", "N/A");
                    }
                    
                    // Thêm thông tin warehouse nếu có
                    if (item.getWarehouse() != null) {
                        Map<String, Object> warehouseInfo = new HashMap<>();
                        warehouseInfo.put("id", item.getWarehouse().getId());
                        warehouseInfo.put("itemData", item.getWarehouse().getItemData());
                        warehouseInfo.put("sellerId", item.getWarehouse().getUser() != null ? item.getWarehouse().getUser().getId() : null);
                        if (item.getWarehouse().getUser() != null) {
                            String username = item.getWarehouse().getUser().getUsername();
                            String fullName = item.getWarehouse().getUser().getFullName();
                            warehouseInfo.put("sellerName", username);
                            warehouseInfo.put("sellerFullName", fullName);
                        } else {
                            warehouseInfo.put("sellerName", "N/A");
                            warehouseInfo.put("sellerFullName", null);
                        }
                        itemMap.put("warehouse", warehouseInfo);
                    }
                    
                    return itemMap;
                }).collect(Collectors.toList());
                orderDetail.put("orderItems", orderItems);
                orderDetail.put("itemCount", orderItems.size());
            } else {
                orderDetail.put("orderItems", new java.util.ArrayList<>());
                orderDetail.put("itemCount", 0);
            }
            
            return ResponseEntity.ok(orderDetail);
            
        } catch (Exception e) {
            log.error("Error getting order detail: {}", orderId, e);
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
    
    /**
     * Cập nhật trạng thái tất cả orders theo orderCode (cho testing)
     */
    @PostMapping("/update-status-by-code")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateOrderStatusByCode(
            @RequestBody Map<String, String> request) {
        
        try {
            String orderCode = request.get("orderCode");
            String statusStr = request.get("status");
            Order.Status status = Order.Status.valueOf(statusStr.toUpperCase());
            
            orderService.updateOrderStatusByOrderCode(orderCode, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All orders with orderCode " + orderCode + " updated to " + status);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating order status by code", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update order status: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}