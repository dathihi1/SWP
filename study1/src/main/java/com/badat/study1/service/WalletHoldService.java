package com.badat.study1.service;

import com.badat.study1.model.Wallet;
import com.badat.study1.model.WalletHold;
import com.badat.study1.model.WalletHistory;
import com.badat.study1.model.Order;
import com.badat.study1.model.OrderItem;
import com.badat.study1.repository.WalletHoldRepository;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletHoldService {
    
    private final WalletHoldRepository walletHoldRepository;
    private final WalletRepository walletRepository;
    private final WalletHistoryService walletHistoryService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;
    private final RedisLockRegistry redisLockRegistry;
    
    /**
     * Hold money trong ví user với thời gian 1 phút (để test)
     * Sử dụng Redis lock để tránh race condition khi cùng 1 user
     */
    @Transactional
    public void holdMoney(Long userId, BigDecimal amount, String orderId) {
        log.info("Holding money for user {}: {} VND, order: {}", userId, amount, orderId);
        
        // User-level lock để tránh race condition khi cùng 1 user
        String userLockKey = "user:wallet:lock:" + userId;
        Lock userLock = redisLockRegistry.obtain(userLockKey);
        
        try {
            if (userLock.tryLock(10, TimeUnit.SECONDS)) {
                log.info("Acquired user wallet lock for user: {}", userId);
                
                // 1. Kiểm tra số dư (double-check trong lock)
                Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
                    
                log.info("Current wallet balance for user {}: {} VND", userId, wallet.getBalance());
                
                if (wallet.getBalance().compareTo(amount) < 0) {
                    throw new RuntimeException("Insufficient balance. Required: " + amount + ", Available: " + wallet.getBalance());
                }
                
                // 2. Trừ tiền khỏi wallet
                BigDecimal newBalance = wallet.getBalance().subtract(amount);
                wallet.setBalance(newBalance);
                walletRepository.save(wallet);
                
                log.info("Wallet balance updated for user {}: {} -> {} VND", userId, wallet.getBalance(), newBalance);
            } else {
                throw new RuntimeException("Failed to acquire wallet lock for user: " + userId + " - another operation is in progress");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for wallet lock for user: " + userId, e);
        } finally {
            userLock.unlock();
            log.info("Released user wallet lock for user: {}", userId);
        }
        
        // 3. Tạo hold record với thời gian 1 phút
        WalletHold hold = WalletHold.builder()
            .userId(userId)
            .amount(amount)
            .orderId(orderId)
            .status(WalletHold.Status.PENDING)
            .expiresAt(Instant.now().plus(1, ChronoUnit.MINUTES)) // 1 phút để test
            .build();
            
        walletHoldRepository.save(hold);
        
        // 4. Tạo wallet history
        try {
            Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
            walletHistoryService.saveHistory(
                wallet.getId(),
                amount.negate(), // Số âm vì đây là hold
                orderId,
                null,
                WalletHistory.Type.PURCHASE,
                WalletHistory.Status.PENDING,
                "Money held for payment - Order: " + orderId
            );
        } catch (Exception e) {
            log.warn("Failed to create wallet history for hold: {}", e.getMessage());
        }
        
        log.info("Money hold created successfully for user {}: {} VND", userId, amount);
    }
    
    /**
     * Release hold money về ví user
     * Sử dụng Redis lock để tránh race condition
     */
    @Transactional
    public void releaseHold(Long holdId) {
        log.info("Releasing hold: {}", holdId);
        
        WalletHold hold = walletHoldRepository.findById(holdId)
            .orElseThrow(() -> new RuntimeException("Hold not found: " + holdId));
            
        if (hold.getStatus() != WalletHold.Status.PENDING) {
            log.warn("Hold {} is not in PENDING status: {}", holdId, hold.getStatus());
            return;
        }
        
        // User-level lock để tránh race condition
        String userLockKey = "user:wallet:lock:" + hold.getUserId();
        Lock userLock = redisLockRegistry.obtain(userLockKey);
        
        try {
            if (userLock.tryLock(10, TimeUnit.SECONDS)) {
                log.info("Acquired user wallet lock for release hold: {}", holdId);
                
                // 1. Cập nhật hold status
                hold.setStatus(WalletHold.Status.CANCELLED);
                walletHoldRepository.save(hold);
                
                // 2. Hoàn tiền về ví
                Wallet wallet = walletRepository.findByUserId(hold.getUserId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + hold.getUserId()));
                    
                BigDecimal newBalance = wallet.getBalance().add(hold.getAmount());
                wallet.setBalance(newBalance);
                walletRepository.save(wallet);
                
                log.info("Wallet balance restored for user {}: {} -> {} VND", hold.getUserId(), wallet.getBalance(), newBalance);
            } else {
                throw new RuntimeException("Failed to acquire wallet lock for release hold: " + holdId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for wallet lock for release hold: " + holdId, e);
        } finally {
            userLock.unlock();
            log.info("Released user wallet lock for release hold: {}", holdId);
        }
        
        // 3. Update wallet history - update PURCHASE hiện tại thành SUCCESS và tạo REFUND
        try {
            Wallet wallet = walletRepository.findByUserId(hold.getUserId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + hold.getUserId()));
                
            // Update PURCHASE record thành SUCCESS
            walletHistoryService.updateWalletHistoryStatus(
                wallet.getId(),
                hold.getOrderId(),
                WalletHistory.Type.PURCHASE,
                WalletHistory.Status.SUCCESS,
                "Hold released - Order: " + hold.getOrderId()
            );
            
            // Tạo REFUND record mới
            walletHistoryService.saveHistory(
                wallet.getId(),
                hold.getAmount(), // Số dương vì đây là hoàn tiền
                hold.getOrderId(),
                null,
                WalletHistory.Type.REFUND,
                WalletHistory.Status.SUCCESS,
                "Hold released - Order: " + hold.getOrderId()
            );
        } catch (Exception e) {
            log.warn("Failed to update wallet history for release: {}", e.getMessage());
        }
        
        log.info("Hold released successfully for user {}: {} VND", hold.getUserId(), hold.getAmount());
    }
    
    /**
     * Release hold money về ví user bằng userId và orderId
     * Sử dụng Redis lock để tránh race condition
     */
    @Transactional
    public void releaseHold(Long userId, String orderId) {
        log.info("Releasing hold for user {} with orderId: {}", userId, orderId);
        
        // User-level lock để tránh race condition
        String userLockKey = "user:wallet:lock:" + userId;
        Lock userLock = redisLockRegistry.obtain(userLockKey);
        
        try {
            if (userLock.tryLock(10, TimeUnit.SECONDS)) {
                log.info("Acquired user wallet lock for release hold: user={}, orderId={}", userId, orderId);
                
                // Tìm hold theo userId và orderId
                List<WalletHold> userHolds = walletHoldRepository.findByUserIdAndOrderIdAndStatus(userId, orderId, WalletHold.Status.PENDING);
                
                if (userHolds.isEmpty()) {
                    log.warn("No pending holds found for user {} with orderId: {}", userId, orderId);
                    return;
                }
                
                // Release tất cả holds cho user và orderId này
                for (WalletHold hold : userHolds) {
                    if (hold.getStatus() == WalletHold.Status.PENDING) {
                        // 1. Cập nhật hold status
                        hold.setStatus(WalletHold.Status.CANCELLED);
                        walletHoldRepository.save(hold);
                        
                        // 2. Hoàn tiền về ví
                        Wallet wallet = walletRepository.findByUserId(userId)
                            .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
                            
                        BigDecimal newBalance = wallet.getBalance().add(hold.getAmount());
                        wallet.setBalance(newBalance);
                        walletRepository.save(wallet);
                        
                        log.info("Hold {} released for user {}: {} VND", hold.getId(), userId, hold.getAmount());
                        
                        // 3. Update wallet history - update PURCHASE hiện tại thành REFUND
                        try {
                            walletHistoryService.updateWalletHistoryStatus(
                                wallet.getId(),
                                hold.getOrderId(),
                                WalletHistory.Type.PURCHASE,
                                WalletHistory.Status.SUCCESS, // Update PURCHASE thành SUCCESS trước
                                "Hold released - Order: " + hold.getOrderId()
                            );
                            
                            // Sau đó tạo REFUND record mới
                            walletHistoryService.saveHistory(
                                wallet.getId(),
                                hold.getAmount(), // Số dương vì đây là hoàn tiền
                                hold.getOrderId(),
                                null,
                                WalletHistory.Type.REFUND,
                                WalletHistory.Status.SUCCESS,
                                "Hold released - Order: " + hold.getOrderId()
                            );
                        } catch (Exception e) {
                            log.warn("Failed to update wallet history for release: {}", e.getMessage());
                        }
                    }
                }
                
                log.info("All holds released successfully for user {} with orderId: {}", userId, orderId);
            } else {
                throw new RuntimeException("Failed to acquire wallet lock for user: " + userId + " - another operation is in progress");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for wallet lock for user: " + userId, e);
        } finally {
            userLock.unlock();
            log.info("Released user wallet lock for user: {}", userId);
        }
    }
    
    /**
     * Complete hold - chuyển tiền cho seller và admin
     */
    @Transactional
    public void completeHold(Long holdId) {
        log.info("Completing hold: {}", holdId);
        
        WalletHold hold = walletHoldRepository.findById(holdId)
            .orElseThrow(() -> new RuntimeException("Hold not found: " + holdId));
            
        if (hold.getStatus() != WalletHold.Status.PENDING) {
            log.warn("Hold {} is not in PENDING status: {}", holdId, hold.getStatus());
            return;
        }
        
        // 1. Cập nhật hold status
        hold.setStatus(WalletHold.Status.COMPLETED);
        walletHoldRepository.save(hold);
        
        // 2. Tạo wallet history cho buyer (chi tiêu)
        try {
            Wallet wallet = walletRepository.findByUserId(hold.getUserId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + hold.getUserId()));
                
            walletHistoryService.saveHistory(
                wallet.getId(),
                hold.getAmount().negate(), // Số âm vì đây là chi tiêu
                hold.getOrderId(),
                null,
                WalletHistory.Type.PURCHASE,
                WalletHistory.Status.SUCCESS,
                "Payment completed - Order: " + hold.getOrderId()
            );
        } catch (Exception e) {
            log.warn("Failed to create wallet history for buyer: {}", e.getMessage());
        }
        
        // 3. Phân phối tiền cho seller và admin
        try {
            Optional<Order> orderOpt = orderRepository.findByOrderCode(hold.getOrderId());
            List<Order> orders = orderOpt.map(List::of).orElse(List.of());
            
            if (!orders.isEmpty()) {
                distributePaymentToSellerAndAdmin(hold, orders);
            } else {
                log.warn("No orders found for hold {}, cannot distribute payment", hold.getId());
            }
        } catch (Exception e) {
            log.error("Failed to distribute payment for hold {}: {}", hold.getId(), e.getMessage());
        }
        
        log.info("Hold completed successfully for user {}: {} VND", hold.getUserId(), hold.getAmount());
    }
    
    /**
     * Cron job chạy mỗi 5 giây để xử lý expired holds - chuyển tiền cho seller/admin
     * Kết hợp với trigger system để tăng tốc xử lý
     */
    @Scheduled(fixedRate = 5000) // Mỗi 5 giây - tăng tần suất
    public void processExpiredHolds() {
        log.info("Processing expired holds...");
        
        List<WalletHold> expiredHolds = walletHoldRepository
            .findByStatusAndExpiresAtBefore(WalletHold.Status.PENDING, Instant.now());
            
        log.info("Found {} expired holds", expiredHolds.size());
        
        // Xử lý batch expired holds để tăng tốc độ
        processBatchExpiredHolds(expiredHolds);
    }
    
    /**
     * Chuyển tiền cho seller và admin theo commission
     */
    @Transactional
    private void distributePaymentToSellerAndAdmin(WalletHold hold, List<Order> orders) {
        log.info("Distributing payment for hold {} to seller and admin", hold.getId());
        
        BigDecimal totalAmount = hold.getAmount();
        BigDecimal totalCommissionAmount = BigDecimal.ZERO;
        BigDecimal totalSellerAmount = BigDecimal.ZERO;
        
        // Tính tổng commission và seller amount từ tất cả orders
        for (Order order : orders) {
            totalCommissionAmount = totalCommissionAmount.add(order.getTotalCommissionAmount());
            totalSellerAmount = totalSellerAmount.add(order.getTotalSellerAmount());
        }
        
        // 1. Cập nhật hold status
        hold.setStatus(WalletHold.Status.COMPLETED);
        walletHoldRepository.save(hold);
        
        // 2. Chuyển tiền cho seller
        if (totalSellerAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                // Lấy seller từ OrderItem đầu tiên - load trực tiếp từ database
                Long sellerId = null;
                if (!orders.isEmpty()) {
                    // Load OrderItems trực tiếp từ database thay vì từ Order object
                    List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orders.get(0).getId());
                    if (!orderItems.isEmpty()) {
                        sellerId = orderItems.get(0).getSellerId();
                        log.info("Found sellerId from OrderItem: {} (from {} orderItems)", sellerId, orderItems.size());
                    }
                }
                
                if (sellerId != null) {
                    Wallet sellerWallet = walletRepository.findByUserId(sellerId)
                        .orElseThrow(() -> new RuntimeException("Seller wallet not found"));
                        
                    BigDecimal newSellerBalance = sellerWallet.getBalance().add(totalSellerAmount);
                    sellerWallet.setBalance(newSellerBalance);
                    walletRepository.save(sellerWallet);
                    
                    // Tạo wallet history cho seller
                    walletHistoryService.saveHistory(
                        sellerWallet.getId(),
                        totalSellerAmount,
                        hold.getOrderId(),
                        null,
                        WalletHistory.Type.SALE_SUCCESS,
                        WalletHistory.Status.SUCCESS,
                        "Payment received from order: " + hold.getOrderId()
                    );
                    
                    log.info("Transferred {} VND to seller {}", totalSellerAmount, sellerId);
                } else {
                    log.warn("No seller found for order {}", hold.getOrderId());
                }
                
            } catch (Exception e) {
                log.error("Failed to transfer money to seller: {}", e.getMessage());
            }
        }
        
        // 3. Chuyển commission cho admin (giả sử admin có userId = 1)
        if (totalCommissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                Wallet adminWallet = walletRepository.findByUserId(1L)
                    .orElseThrow(() -> new RuntimeException("Admin wallet not found"));
                    
                BigDecimal newAdminBalance = adminWallet.getBalance().add(totalCommissionAmount);
                adminWallet.setBalance(newAdminBalance);
                walletRepository.save(adminWallet);
                
                // Tạo wallet history cho admin
                walletHistoryService.saveHistory(
                    adminWallet.getId(),
                    totalCommissionAmount,
                    hold.getOrderId(),
                    null,
                    WalletHistory.Type.COMMISSION,
                    WalletHistory.Status.SUCCESS,
                    "Commission received from order: " + hold.getOrderId()
                );
                
                log.info("Transferred {} VND commission to admin", totalCommissionAmount);
                
            } catch (Exception e) {
                log.error("Failed to transfer commission to admin: {}", e.getMessage());
            }
        }
        
        // 4. Update wallet history cho buyer (chi tiêu) - update PURCHASE hiện tại thành SUCCESS
        try {
            Wallet buyerWallet = walletRepository.findByUserId(hold.getUserId())
                .orElseThrow(() -> new RuntimeException("Buyer wallet not found"));
                
            walletHistoryService.updateWalletHistoryStatus(
                buyerWallet.getId(),
                hold.getOrderId(),
                WalletHistory.Type.PURCHASE,
                WalletHistory.Status.SUCCESS,
                "Payment completed - Order: " + hold.getOrderId()
            );
        } catch (Exception e) {
            log.warn("Failed to update wallet history for buyer: {}", e.getMessage());
        }
        
        // 5. Cập nhật trạng thái tất cả orders thành COMPLETED
        try {
            orderService.updateOrderStatusByOrderCode(hold.getOrderId(), Order.Status.COMPLETED);
            log.info("Successfully updated all orders with orderCode {} to COMPLETED status", hold.getOrderId());
        } catch (Exception e) {
            log.error("Failed to update order status to COMPLETED for orderCode {}: {}", hold.getOrderId(), e.getMessage());
        }
        
        log.info("Payment distribution completed for hold {}: {} VND total", hold.getId(), totalAmount);
    }
    
    /**
     * Lấy danh sách holds đang active của user
     */
    public List<WalletHold> getActiveHolds(Long userId) {
        return walletHoldRepository.findActiveHoldsByUser(userId, WalletHold.Status.PENDING, Instant.now());
    }
    
    /**
     * Lấy tổng số tiền đang bị hold
     */
    public BigDecimal getTotalHeldAmount(Long userId) {
        return getActiveHolds(userId).stream()
            .map(WalletHold::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Xử lý batch expired holds với parallel processing
     */
    private void processBatchExpiredHolds(List<WalletHold> expiredHolds) {
        if (expiredHolds.isEmpty()) {
            return;
        }
        
        // Chia thành các batch nhỏ để xử lý parallel
        int batchSize = 10; // Xử lý 10 holds/lần
        int totalBatches = (int) Math.ceil((double) expiredHolds.size() / batchSize);
        
        log.info("Processing {} expired holds in {} batches of size {}", 
                expiredHolds.size(), totalBatches, batchSize);
        
        for (int i = 0; i < expiredHolds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, expiredHolds.size());
            List<WalletHold> batch = expiredHolds.subList(i, endIndex);
            
            // Xử lý batch này
            processSingleBatchExpiredHolds(batch);
        }
    }
    
    /**
     * Xử lý một batch expired holds
     */
    private void processSingleBatchExpiredHolds(List<WalletHold> batch) {
        log.info("Processing batch of {} expired holds", batch.size());
        
        for (WalletHold hold : batch) {
            try {
                log.info("Processing expired hold: {} for user: {}", hold.getId(), hold.getUserId());
                
                // Tìm order tương ứng để lấy thông tin seller và commission
                Optional<Order> orderOpt = orderRepository.findByOrderCode(hold.getOrderId());
                List<Order> orders = orderOpt.map(List::of).orElse(List.of());
                
                if (!orders.isEmpty()) {
                    // Chuyển tiền cho seller và admin theo commission
                    distributePaymentToSellerAndAdmin(hold, orders);
                } else {
                    // Nếu không tìm thấy order, hoàn tiền về buyer
                    log.warn("No orders found for hold {}, refunding to buyer", hold.getId());
                    releaseHold(hold.getId());
                }
                
            } catch (Exception e) {
                log.error("Failed to process expired hold {}: {}", hold.getId(), e.getMessage());
                // Fallback: hoàn tiền về buyer nếu có lỗi
                try {
                    releaseHold(hold.getId());
                } catch (Exception fallbackError) {
                    log.error("Failed to fallback release hold {}: {}", hold.getId(), fallbackError.getMessage());
                }
            }
        }
    }
}
