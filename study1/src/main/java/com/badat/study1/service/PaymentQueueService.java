package com.badat.study1.service;

import com.badat.study1.model.PaymentQueue;
import com.badat.study1.model.Warehouse;
import com.badat.study1.model.Order;
import com.badat.study1.model.OrderItem;
import com.badat.study1.repository.PaymentQueueRepository;
import com.badat.study1.repository.OrderItemRepository;
import org.springframework.integration.redis.util.RedisLockRegistry;
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
import java.util.stream.Collectors;
import java.util.concurrent.locks.Lock;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentQueueService {
    
    private final PaymentQueueRepository paymentQueueRepository;
    private final WalletHoldService walletHoldService;
    private final WarehouseLockService warehouseLockService;
    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final ObjectMapper objectMapper;
    private final RedisLockRegistry redisLockRegistry;
    
    /**
     * Thêm payment request vào queue
     */
    @Transactional
    public Long enqueuePayment(Long userId, List<Map<String, Object>> cartItems, BigDecimal totalAmount) {
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
            return paymentQueue.getId();
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cart data", e);
            throw new RuntimeException("Failed to serialize cart data", e);
        }
    }
    
    /**
     * Cron job xử lý payment queue mỗi 10 giây - với distributed lock để tránh race condition
     */
    @Scheduled(fixedRate = 10000) // Mỗi 10 giây
    public void processPaymentQueue() {
        String lockKey = "payment-queue:process";
        Lock lock = redisLockRegistry.obtain(lockKey);
        
        try {
            if (lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)) {
                log.info("Processing payment queue with distributed lock...");
                
                try {
                    List<PaymentQueue> pendingPayments = paymentQueueRepository
                        .findByStatusOrderByCreatedAtAsc(PaymentQueue.Status.PENDING);
                        
                    log.info("Found {} pending payments", pendingPayments.size());
                    
                    // Xử lý từng payment một cách tuần tự để tránh race condition
                    for (PaymentQueue payment : pendingPayments) {
                        try {
                            // Kiểm tra lại status trước khi xử lý để tránh double processing
                            PaymentQueue currentPayment = paymentQueueRepository.findById(payment.getId()).orElse(null);
                            if (currentPayment == null || currentPayment.getStatus() != PaymentQueue.Status.PENDING) {
                                log.info("Payment {} already processed or deleted, skipping", payment.getId());
                                continue;
                            }
                            
                            processPaymentItem(payment);
                        } catch (Exception e) {
                            log.error("Failed to process payment {}: {}", payment.getId(), e.getMessage());
                            // Error handling đã được xử lý trong processPaymentItem
                        }
                    }
                } catch (Exception e) {
                    log.error("Error in payment queue processing: {}", e.getMessage());
                }
            } else {
                log.info("Another instance is processing payment queue, skipping...");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for payment queue lock", e);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Xử lý một payment item - chỉ hold tiền sau khi thành công
     */
    @Transactional
    public void processPaymentItem(PaymentQueue payment) {
        log.info("Processing payment item: {} for user: {}", payment.getId(), payment.getUserId());
        
        // Distributed lock cho payment item để tránh double processing
        String paymentLockKey = "payment:process:" + payment.getId();
        Lock paymentLock = redisLockRegistry.obtain(paymentLockKey);
        
        String orderId = null;
        try {
            if (paymentLock.tryLock(3, java.util.concurrent.TimeUnit.SECONDS)) {
                log.info("Acquired lock for payment: {}", payment.getId());
                
                try {
                    // 1. Mark as processing
                    payment.setStatus(PaymentQueue.Status.PROCESSING);
                    payment.setProcessedAt(Instant.now());
                    paymentQueueRepository.save(payment);
            
                    // 2. Parse cart data
                    List<Map<String, Object>> cartItems = parseCartData(payment.getCartData());
                    log.info("Parsed {} cart items for payment {}", cartItems.size(), payment.getId());
                    log.debug("Cart items details: {}", cartItems);

                    // 3. Generate order id and HOLD MONEY (stock already validated before enqueueing)
                    orderId = "ORDER_" + payment.getUserId() + "_" + System.currentTimeMillis();
                    walletHoldService.holdMoney(payment.getUserId(), payment.getTotalAmount(), orderId);

                    // 4. Lock warehouse items AFTER holding funds - tính theo số lượng
                    Map<Long, Integer> productQuantities = new java.util.HashMap<>();
                    for (Map<String, Object> cartItem : cartItems) {
                        Long productId = Long.valueOf(cartItem.get("productId").toString());
                        Integer quantity = Integer.valueOf(cartItem.get("quantity").toString());
                        productQuantities.put(productId, productQuantities.getOrDefault(productId, 0) + quantity);
                    }

                    List<Warehouse> lockedItems = warehouseLockService.lockWarehouseItemsWithQuantities(productQuantities);
                    
                    // 4.5. Kiểm tra lại sau khi lock - tính tổng số lượng cần thiết
                    int totalRequiredQuantity = productQuantities.values().stream().mapToInt(Integer::intValue).sum();
                    if (lockedItems.isEmpty() || lockedItems.size() < totalRequiredQuantity) {
                        log.warn("Failed to lock enough warehouse items for user: {} (required: {}, locked: {}), refunding money", 
                            payment.getUserId(), totalRequiredQuantity, lockedItems.size());
                        // Release hold trước khi throw exception - tìm hold theo orderId
                        try {
                            // Sử dụng method mới với user-level lock
                            walletHoldService.releaseHold(payment.getUserId(), orderId);
                        } catch (Exception releaseError) {
                            log.error("Failed to release hold during warehouse lock failure: {}", releaseError.getMessage());
                        }
                        throw new RuntimeException("Không thể khóa đủ số lượng hàng trong kho - có thể đã có người khác mua trước");
                    }

                    // 5. Create order with multiple items
                    createOrderWithItems(payment.getUserId(), cartItems, lockedItems, orderId);

                    // 6. Mark as completed
                    payment.setStatus(PaymentQueue.Status.COMPLETED);
                    paymentQueueRepository.save(payment);
                    
                    log.info("Payment processed successfully: {} - Money held, buyer can receive items immediately", payment.getId());
                    
                } catch (Exception e) {
                    log.error("Error processing payment {}: {}", payment.getId(), e.getMessage());
                    
                    // Nếu lỗi → unlock warehouse và hoàn tiền nếu đã hold
                    try {
                        handlePaymentError(payment, orderId, e.getMessage());
                        markPaymentAsFailed(payment.getId(), "Payment failed - reverted changes");
                        log.info("Payment failed for payment {} - reverted holds and locks where applicable", payment.getId());
                    } catch (Exception errorHandlingException) {
                        log.error("Failed to handle payment error for payment {}: {}", payment.getId(), errorHandlingException.getMessage());
                        markPaymentAsFailed(payment.getId(), "Payment failed - error handling failed");
                    }
                }
            } else {
                log.warn("Could not acquire lock for payment: {}, another instance might be processing", payment.getId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for payment lock: {}", payment.getId(), e);
        } finally {
            paymentLock.unlock();
        }
    }
    
    /**
     * Tạo order với nhiều items từ cart
     */
    @Transactional
    private void createOrderWithItems(Long userId, List<Map<String, Object>> cartItems, List<Warehouse> lockedItems, String orderId) {
        log.info("Creating order with items for user: {} with {} cart items and {} locked warehouse items", 
                userId, cartItems.size(), lockedItems.size());
        
        // Tạo Order chính với tất cả OrderItem
        Order order = orderService.createOrderFromCart(userId, cartItems, "WALLET", "Order from cart payment", orderId);
        
        // Cập nhật warehouseId thực tế cho các OrderItem
        updateOrderItemsWithActualWarehouseIds(order, lockedItems);
        
        // Mark tất cả warehouse items as delivered
        for (Warehouse lockedItem : lockedItems) {
            warehouseLockService.markAsDelivered(lockedItem.getId());
        }
        
        log.info("Successfully created order {} with {} items for user: {}", order.getId(), cartItems.size(), userId);
    }
    
    /**
     * Cập nhật warehouseId thực tế cho các OrderItem
     */
    @Transactional
    private void updateOrderItemsWithActualWarehouseIds(Order order, List<Warehouse> lockedItems) {
        log.info("Updating OrderItems with actual warehouse IDs for order: {}", order.getId());
        
        // Group locked items by productId
        Map<Long, List<Warehouse>> lockedItemsByProduct = lockedItems.stream()
                .collect(Collectors.groupingBy(warehouse -> warehouse.getProduct().getId()));
        
        // Get all OrderItems for this order
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());
        
        int warehouseIndex = 0;
        for (OrderItem orderItem : orderItems) {
            Long productId = orderItem.getProductId();
            List<Warehouse> productWarehouses = lockedItemsByProduct.get(productId);
            
            if (productWarehouses != null && !productWarehouses.isEmpty()) {
                // Gán warehouseId thực tế cho OrderItem
                Warehouse actualWarehouse = productWarehouses.get(warehouseIndex % productWarehouses.size());
                orderItem.setWarehouseId(actualWarehouse.getId());
                
                // Set sellerId từ warehouse
                if (actualWarehouse.getUser() != null) {
                    orderItem.setSellerId(actualWarehouse.getUser().getId());
                    log.info("Updated OrderItem {} with warehouseId: {} and sellerId: {} (productId: {})", 
                            orderItem.getId(), actualWarehouse.getId(), actualWarehouse.getUser().getId(), productId);
                } else {
                    log.warn("Warehouse {} has no user, cannot set sellerId for OrderItem {}", 
                            actualWarehouse.getId(), orderItem.getId());
                }
                
                warehouseIndex++;
            }
        }
        
        // Save updated OrderItems
        orderItemRepository.saveAll(orderItems);
        log.info("Updated {} OrderItems with actual warehouse IDs", orderItems.size());
    }
    
    /**
     * Xử lý lỗi payment - hoàn tiền và unlock warehouse
     */
    @Transactional
    private void handlePaymentError(PaymentQueue payment, String orderId, String errorMessage) {
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
            
            // Hoàn tiền về ví user - sử dụng method mới với user-level lock
            try {
                walletHoldService.releaseHold(payment.getUserId(), orderId);
                log.info("Released holds for user {} with orderId {}", payment.getUserId(), orderId);
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
