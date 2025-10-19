package com.badat.study1.service;

import com.badat.study1.model.PaymentQueue;
import com.badat.study1.model.Warehouse;
import com.badat.study1.model.Order;
import com.badat.study1.model.WalletHold;
import com.badat.study1.repository.PaymentQueueRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentQueueService {
    
    private final PaymentQueueRepository paymentQueueRepository;
    private final WalletHoldService walletHoldService;
    private final WarehouseLockService warehouseLockService;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    
    /**
     * Thêm payment request vào queue
     */
    @Transactional
    public void enqueuePayment(Long userId, List<Map<String, Object>> cartItems, BigDecimal totalAmount) {
        log.info("Enqueuing payment for user {}: {} VND", userId, totalAmount);
        
        try {
            String cartData = objectMapper.writeValueAsString(cartItems);
            
            PaymentQueue paymentQueue = PaymentQueue.builder()
                .userId(userId)
                .cartData(cartData)
                .totalAmount(totalAmount)
                .status(PaymentQueue.Status.PENDING)
                .build();
                
            paymentQueueRepository.save(paymentQueue);
            
            log.info("Payment queued successfully with ID: {}", paymentQueue.getId());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cart data", e);
            throw new RuntimeException("Failed to serialize cart data", e);
        }
    }
    
    /**
     * Cron job xử lý payment queue mỗi 10 giây - đơn giản hóa
     */
    @Scheduled(fixedRate = 10000) // Mỗi 10 giây
    public void processPaymentQueue() {
        log.info("Processing payment queue...");
        
        try {
            List<PaymentQueue> pendingPayments = paymentQueueRepository
                .findByStatusOrderByCreatedAtAsc(PaymentQueue.Status.PENDING);
                
            log.info("Found {} pending payments", pendingPayments.size());
            
            for (PaymentQueue payment : pendingPayments) {
                try {
                    processPaymentItem(payment);
                } catch (Exception e) {
                    log.error("Failed to process payment {}: {}", payment.getId(), e.getMessage());
                    // Error handling đã được xử lý trong processPaymentItem
                }
            }
        } catch (Exception e) {
            log.error("Error in payment queue processing: {}", e.getMessage());
        }
    }
    
    /**
     * Xử lý một payment item - chỉ hold tiền sau khi thành công
     */
    @Transactional
    public void processPaymentItem(PaymentQueue payment) {
        log.info("Processing payment item: {} for user: {}", payment.getId(), payment.getUserId());
        
        try {
            // 1. Mark as processing
            payment.setStatus(PaymentQueue.Status.PROCESSING);
            payment.setProcessedAt(Instant.now());
            paymentQueueRepository.save(payment);
            
            // 2. Parse cart data
            List<Map<String, Object>> cartItems = parseCartData(payment.getCartData());
            
            // 3. Lock warehouse items TRƯỚC (kiểm tra availability)
            List<Long> productIds = cartItems.stream()
                .map(item -> Long.valueOf(item.get("productId").toString()))
                .toList();
                
            List<Warehouse> lockedItems = warehouseLockService.lockWarehouseItems(productIds);
            
            // 4. Tạo orders đơn giản - không truy cập relationships
            String orderId = "ORDER_" + payment.getUserId() + "_" + System.currentTimeMillis();
            createSimpleOrders(payment.getUserId(), cartItems, lockedItems, orderId);
            
            // 5. CHỈ SAU KHI THÀNH CÔNG mới hold money
            walletHoldService.holdMoney(payment.getUserId(), payment.getTotalAmount(), orderId);
            
            // 6. Mark as completed
            payment.setStatus(PaymentQueue.Status.COMPLETED);
            paymentQueueRepository.save(payment);
            
            log.info("Payment processed successfully: {} - Money held, buyer can receive items immediately", payment.getId());
            
        } catch (Exception e) {
            log.error("Error processing payment {}: {}", payment.getId(), e.getMessage());
            
            // Nếu lỗi → KHÔNG hold tiền, chỉ unlock warehouse và báo lỗi
            try {
                handlePaymentErrorWithoutHold(payment, e.getMessage());
                markPaymentAsFailed(payment.getId(), "Payment failed - no money held");
                log.info("Payment failed for payment {} - no money was held", payment.getId());
            } catch (Exception errorHandlingException) {
                log.error("Failed to handle payment error for payment {}: {}", payment.getId(), errorHandlingException.getMessage());
                markPaymentAsFailed(payment.getId(), "Payment failed - error handling failed");
            }
        }
    }
    
    /**
     * Tạo orders đơn giản - sử dụng ID trực tiếp thay vì truy cập relationships
     */
    @Transactional
    private void createSimpleOrders(Long userId, List<Map<String, Object>> cartItems, List<Warehouse> lockedItems, String orderId) {
        log.info("Creating simple orders for user: {} with {} items", userId, cartItems.size());
        
        for (int i = 0; i < cartItems.size() && i < lockedItems.size(); i++) {
            try {
                Map<String, Object> cartItem = cartItems.get(i);
                Warehouse lockedItem = lockedItems.get(i);
                
                // Lấy thông tin từ cart data và warehouse ID trực tiếp
                Long productId = Long.valueOf(cartItem.get("productId").toString());
                BigDecimal unitPrice = new BigDecimal(cartItem.get("price").toString());
                Integer quantity = Integer.valueOf(cartItem.get("quantity").toString());
                
                // Sử dụng ID trực tiếp thay vì truy cập relationships
                Order order = orderService.createOrder(
                    userId,                    // buyerId
                    lockedItem.getUser().getId(),   // sellerId (từ warehouse.user)
                    lockedItem.getShop().getId(),   // shopId (từ warehouse.shop)
                    lockedItem.getStall().getId(),  // stallId (từ warehouse.stall)
                    productId,               // productId
                    lockedItem.getId(),      // warehouseId
                    quantity,                // quantity
                    unitPrice,               // unitPrice
                    "WALLET",               // paymentMethod
                    "Order from cart payment", // notes
                    orderId                  // customOrderCode
                );
                
                log.info("Order created for user {}: Order ID {} for product {} (warehouse {})", 
                    userId, order.getId(), productId, lockedItem.getId());
                
                // Mark warehouse item as delivered
                warehouseLockService.markAsDelivered(lockedItem.getId());
                
            } catch (Exception e) {
                log.error("Failed to create order for user {}: cart item {}", userId, cartItems.get(i), e);
                throw e;
            }
        }
        
        log.info("Successfully created all orders for user: {}", userId);
    }
    
    /**
     * Xử lý lỗi payment - hoàn tiền và unlock warehouse
     */
    @Transactional
    private void handlePaymentError(PaymentQueue payment, String errorMessage) {
        log.info("Handling payment error for payment {}: {}", payment.getId(), errorMessage);
        
        try {
            // Parse cart data để unlock warehouse
            List<Map<String, Object>> cartItems = parseCartData(payment.getCartData());
            
            // Unlock tất cả warehouse items đã lock
            for (Map<String, Object> cartItem : cartItems) {
                try {
                    Long productId = Long.valueOf(cartItem.get("productId").toString());
                    warehouseLockService.unlockWarehouseItems(List.of(productId));
                    log.info("Unlocked warehouse items for product: {}", productId);
                } catch (Exception unlockError) {
                    log.error("Failed to unlock warehouse for product {}: {}", 
                        cartItem.get("productId"), unlockError.getMessage());
                }
            }
            
            // Hoàn tiền về ví user - tìm hold theo userId và totalAmount
            try {
                List<WalletHold> userHolds = walletHoldService.getActiveHolds(payment.getUserId());
                for (WalletHold hold : userHolds) {
                    if (hold.getAmount().equals(payment.getTotalAmount()) && 
                        hold.getStatus() == WalletHold.Status.PENDING) {
                        
                        walletHoldService.releaseHold(hold.getId());
                        log.info("Released hold {} for user {} with amount {}", 
                            hold.getId(), payment.getUserId(), hold.getAmount());
                        break;
                    }
                }
            } catch (Exception refundError) {
                log.error("Failed to refund money for payment {}: {}", payment.getId(), refundError.getMessage());
                // Không throw exception để không block việc unlock warehouse
            }
            
            log.info("Successfully handled payment error {}: unlocked warehouse and attempted refund", payment.getId());
            
        } catch (Exception e) {
            log.error("Failed to handle payment error {}: {}", payment.getId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Xử lý lỗi payment mà KHÔNG có tiền hold - chỉ unlock warehouse
     */
    @Transactional
    private void handlePaymentErrorWithoutHold(PaymentQueue payment, String errorMessage) {
        log.info("Handling payment error without hold for payment {}: {}", payment.getId(), errorMessage);
        
        try {
            // Parse cart data để unlock warehouse
            List<Map<String, Object>> cartItems = parseCartData(payment.getCartData());
            
            // Unlock warehouse items
            for (Map<String, Object> cartItem : cartItems) {
                try {
                    Long productId = Long.valueOf(cartItem.get("productId").toString());
                    warehouseLockService.unlockWarehouseItems(List.of(productId));
                    log.info("Unlocked warehouse for product {}", productId);
                } catch (Exception unlockError) {
                    log.error("Failed to unlock warehouse for product {}: {}", 
                        cartItem.get("productId"), unlockError.getMessage());
                }
            }
            
            log.info("Successfully handled payment error without hold {}: unlocked warehouse", payment.getId());
            
        } catch (Exception e) {
            log.error("Failed to handle payment error without hold {}: {}", payment.getId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Mark payment as failed
     */
    private void markPaymentAsFailed(Long paymentId, String errorMessage) {
        try {
            PaymentQueue payment = paymentQueueRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
                
            payment.setStatus(PaymentQueue.Status.FAILED);
            payment.setErrorMessage(errorMessage);
            paymentQueueRepository.save(payment);
            
            log.info("Payment marked as failed: {} - {}", paymentId, errorMessage);
            
        } catch (Exception e) {
            log.error("Failed to mark payment as failed: {}", paymentId, e);
        }
    }
    
    /**
     * Parse cart data từ JSON
     */
    private List<Map<String, Object>> parseCartData(String cartDataJson) {
        try {
            return objectMapper.readValue(
                cartDataJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );
        } catch (Exception e) {
            log.error("Failed to parse cart data: {}", e.getMessage());
            throw new RuntimeException("Failed to parse cart data", e);
        }
    }
    
    /**
     * Lấy trạng thái payment
     */
    public PaymentQueue getPaymentStatus(Long paymentId) {
        return paymentQueueRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }
    
    /**
     * Lấy danh sách payments của user
     */
    public List<PaymentQueue> getUserPayments(Long userId) {
        return paymentQueueRepository.findByUserIdAndStatus(userId, PaymentQueue.Status.PENDING);
    }
}
