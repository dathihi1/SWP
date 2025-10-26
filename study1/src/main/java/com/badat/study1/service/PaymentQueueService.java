package com.badat.study1.service;

import com.badat.study1.model.PaymentQueue;
import com.badat.study1.model.Warehouse;
import com.badat.study1.model.Order;
import com.badat.study1.model.OrderItem;
import com.badat.study1.repository.PaymentQueueRepository;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.event.PaymentEvent;
import org.springframework.integration.redis.util.RedisLockRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Thêm payment request vào queue với validation stock trước và user-level lock
     */
    @Transactional
    public Long enqueuePayment(Long userId, List<Map<String, Object>> cartItems, BigDecimal totalAmount) {
        log.info("Enqueuing payment for user {}: {} VND", userId, totalAmount);
        
        // User-level lock để tránh multiple concurrent payments từ cùng 1 user
        String userPaymentLockKey = "user:payment:lock:" + userId;
        Lock userPaymentLock = redisLockRegistry.obtain(userPaymentLockKey);
        
        try {
            if (userPaymentLock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)) {
                log.info("Acquired user payment lock for user: {}", userId);
                
                try {
                    // 1. Kiểm tra xem user có payment đang pending không
                    List<PaymentQueue> pendingPayments = paymentQueueRepository
                        .findByUserIdAndStatusOrderByCreatedAtDesc(userId, PaymentQueue.Status.PENDING);
                    
                    if (!pendingPayments.isEmpty()) {
                        log.warn("User {} already has {} pending payments. Rejecting new payment request.", 
                            userId, pendingPayments.size());
                        throw new RuntimeException("Bạn đã có thanh toán đang chờ xử lý. Vui lòng đợi hoàn tất trước khi tạo thanh toán mới.");
                    }
                    
                    // 2. VALIDATE STOCK TRƯỚC KHI ENQUEUE
                    validateStockAvailability(cartItems);
                    
                    // 3. Kiểm tra số dư ví
                    validateUserBalance(userId, totalAmount);
                    
                    String cartData = objectMapper.writeValueAsString(cartItems);
                    
                    PaymentQueue paymentQueue = PaymentQueue.builder()
                        .userId(userId)
                        .cartData(cartData)
                        .totalAmount(totalAmount)
                        .status(PaymentQueue.Status.PENDING)
                        .build();
                        
                    paymentQueueRepository.save(paymentQueue);
                    
                    // Publish event để trigger xử lý ngay lập tức
                    eventPublisher.publishEvent(PaymentEvent.paymentCreated(this, paymentQueue.getId(), userId));
                    
                    log.info("Payment queued successfully with ID: {} for user: {}", paymentQueue.getId(), userId);
                    return paymentQueue.getId();
                    
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize cart data", e);
                    throw new RuntimeException("Failed to serialize cart data", e);
                }
            } else {
                log.warn("Failed to acquire user payment lock for user: {} - another payment is in progress", userId);
                throw new RuntimeException("Bạn đang có thanh toán đang được xử lý. Vui lòng đợi hoàn tất trước khi tạo thanh toán mới.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bị gián đoạn khi tạo thanh toán", e);
        } finally {
            userPaymentLock.unlock();
            log.info("Released user payment lock for user: {}", userId);
        }
    }
    
    /**
     * Validate stock availability trước khi enqueue payment với Redis lock
     */
    private void validateStockAvailability(List<Map<String, Object>> cartItems) {
        log.info("Validating stock availability for {} cart items", cartItems.size());
        
        // Tạo map để group theo productId và tính tổng quantity
        Map<Long, Integer> productQuantities = new java.util.HashMap<>();
        for (Map<String, Object> cartItem : cartItems) {
            Long productId = Long.valueOf(cartItem.get("productId").toString());
            Integer quantity = Integer.valueOf(cartItem.get("quantity").toString());
            productQuantities.put(productId, productQuantities.getOrDefault(productId, 0) + quantity);
        }
        
        // Validate từng product với Redis lock
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requiredQuantity = entry.getValue();
            
            String lockKey = "stock:validate:" + productId;
            Lock lock = redisLockRegistry.obtain(lockKey);
            
            try {
                if (lock.tryLock(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.info("Acquired stock validation lock for product: {}", productId);
                    
                    // Kiểm tra số lượng có sẵn trong lock
                    long availableCount = warehouseLockService.getAvailableStockCount(productId);
                    
                    if (availableCount < requiredQuantity) {
                        log.warn("Insufficient stock for product {}. Required: {}, Available: {}", 
                            productId, requiredQuantity, availableCount);
                        throw new RuntimeException("Không đủ hàng cho sản phẩm ID: " + productId + 
                            " (cần " + requiredQuantity + ", chỉ có " + availableCount + ")");
                    }
                    
                    log.info("Stock validation passed for product {}: {} available", productId, availableCount);
                } else {
                    log.warn("Failed to acquire stock validation lock for product: {}", productId);
                    throw new RuntimeException("Không thể kiểm tra hàng cho sản phẩm: " + productId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Bị gián đoạn khi kiểm tra hàng cho sản phẩm: " + productId, e);
            } finally {
                lock.unlock();
            }
        }
        
        log.info("All stock validations passed");
    }
    
    /**
     * Validate user balance trước khi enqueue payment
     */
    private void validateUserBalance(Long userId, BigDecimal requiredAmount) {
        log.info("Validating balance for user {}: {} VND", userId, requiredAmount);
        
        // Sử dụng WalletHoldService để check balance với user-level lock
        try {
            // Tạm thời hold 0 VND để check balance (sẽ được release ngay)
            String tempOrderId = "TEMP_CHECK_" + userId + "_" + System.currentTimeMillis();
            walletHoldService.holdMoney(userId, BigDecimal.ZERO, tempOrderId);
            
            // Release ngay lập tức
            walletHoldService.releaseHold(userId, tempOrderId);
            
            log.info("Balance validation passed for user {}", userId);
        } catch (Exception e) {
            log.warn("Balance validation failed for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Số dư không đủ để thực hiện thanh toán: " + e.getMessage());
        }
    }
    
    /**
     * Cron job xử lý payment queue mỗi 1 giây - với distributed lock để tránh race condition
     * Kết hợp với trigger system để tăng tốc xử lý
     */
    @Scheduled(fixedRate = 1000) // Mỗi 1 giây - tăng tần suất
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
                    
                    // Xử lý batch payments để tăng tốc độ
                    processBatchPayments(pendingPayments);
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

                    // 3. Generate order id
                    orderId = "ORDER_" + payment.getUserId() + "_" + System.currentTimeMillis();
                    
                    // 4. LOCK WAREHOUSE ITEMS TRƯỚC (Reserve inventory trước khi hold money)
                    Map<Long, Integer> productQuantities = new java.util.HashMap<>();
                    for (Map<String, Object> cartItem : cartItems) {
                        Long productId = Long.valueOf(cartItem.get("productId").toString());
                        Integer quantity = Integer.valueOf(cartItem.get("quantity").toString());
                        productQuantities.put(productId, productQuantities.getOrDefault(productId, 0) + quantity);
                    }

                    // Reserve warehouse items với timeout TRƯỚC khi hold money để tránh hold tiền mà không có hàng
                    List<Warehouse> lockedItems = warehouseLockService.reserveWarehouseItemsWithTimeout(productQuantities, payment.getUserId(), 5); // 5 phút timeout
                    
                    // 5. HOLD MONEY SAU KHI ĐÃ LOCK ĐƯỢC HÀNG
                    walletHoldService.holdMoney(payment.getUserId(), payment.getTotalAmount(), orderId);
                    
                    // 6. Kiểm tra lại sau khi lock - tính tổng số lượng cần thiết
                    int totalRequiredQuantity = productQuantities.values().stream().mapToInt(Integer::intValue).sum();
                    if (lockedItems.isEmpty() || lockedItems.size() < totalRequiredQuantity) {
                        log.warn("Failed to lock enough warehouse items for user: {} (required: {}, locked: {}), unlocking warehouse items", 
                            payment.getUserId(), totalRequiredQuantity, lockedItems.size());
                        // Unlock warehouse items vì chưa hold money
                        try {
                            for (Warehouse item : lockedItems) {
                                warehouseLockService.unlockWarehouseItem(item.getId());
                            }
                        } catch (Exception unlockError) {
                            log.error("Failed to unlock warehouse items during lock failure: {}", unlockError.getMessage());
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
    
    /**
     * Xử lý batch payments với parallel processing
     */
    private void processBatchPayments(List<PaymentQueue> pendingPayments) {
        if (pendingPayments.isEmpty()) {
            return;
        }
        
        // Chia thành các batch nhỏ để xử lý parallel
        int batchSize = 20; // Xử lý 20 payments/lần
        int totalBatches = (int) Math.ceil((double) pendingPayments.size() / batchSize);
        
        log.info("Processing {} payments in {} batches of size {}", 
                pendingPayments.size(), totalBatches, batchSize);
        
        for (int i = 0; i < pendingPayments.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, pendingPayments.size());
            List<PaymentQueue> batch = pendingPayments.subList(i, endIndex);
            
            // Xử lý batch này
            processSingleBatch(batch);
        }
    }
    
    /**
     * Xử lý một batch payments
     */
    private void processSingleBatch(List<PaymentQueue> batch) {
        log.info("Processing batch of {} payments", batch.size());
        
        for (PaymentQueue payment : batch) {
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
    }
    
}
