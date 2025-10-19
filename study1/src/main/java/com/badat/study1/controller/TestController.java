package com.badat.study1.controller;

import com.badat.study1.model.WalletHold;
import com.badat.study1.model.Order;
import com.badat.study1.service.WalletHoldService;
import com.badat.study1.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {
    
    private final WalletHoldService walletHoldService;
    private final OrderRepository orderRepository;
    
    /**
     * Test endpoint để kiểm tra holds và orders
     */
    @GetMapping("/holds-and-orders")
    public ResponseEntity<Map<String, Object>> getHoldsAndOrders() {
        try {
            // Lấy tất cả holds đang pending
            List<WalletHold> pendingHolds = walletHoldService.getActiveHolds(1L); // Test với user 1
            
            // Lấy tất cả orders
            List<Order> allOrders = orderRepository.findAll();
            
            Map<String, Object> result = new HashMap<>();
            result.put("pendingHolds", pendingHolds);
            result.put("allOrders", allOrders);
            result.put("holdCount", pendingHolds.size());
            result.put("orderCount", allOrders.size());
            
            // Kiểm tra matching
            Map<String, Object> matching = new HashMap<>();
            for (WalletHold hold : pendingHolds) {
                boolean hasMatchingOrder = orderRepository.findByOrderCode(hold.getOrderId()).isPresent();
                matching.put("hold_" + hold.getId(), Map.of(
                    "orderId", hold.getOrderId(),
                    "hasMatchingOrder", hasMatchingOrder,
                    "amount", hold.getAmount(),
                    "expiresAt", hold.getExpiresAt()
                ));
            }
            result.put("matching", matching);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting holds and orders", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Test endpoint để force process expired holds
     */
    @PostMapping("/force-process-holds")
    public ResponseEntity<Map<String, Object>> forceProcessHolds() {
        try {
            log.info("Force processing expired holds...");
            walletHoldService.processExpiredHolds();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Expired holds processed successfully");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error force processing holds", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}