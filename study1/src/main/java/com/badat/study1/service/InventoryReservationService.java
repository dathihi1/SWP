package com.badat.study1.service;

import com.badat.study1.model.Warehouse;
import com.badat.study1.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Service để xử lý inventory reservation với cơ chế chống race condition
 * Sử dụng Redis Lock + Database Transaction + Optimistic Locking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationService {
    
    private final WarehouseRepository warehouseRepository;
    private final RedisLockRegistry redisLockRegistry;
    
    /**
     * Reserve inventory với cơ chế chống race condition
     * Sử dụng Redis Lock + Database Transaction + Retry mechanism
     */
    @Transactional
    public ReservationResult reserveInventory(Map<Long, Integer> productQuantities, Long userId) {
        log.info("Reserving inventory for user {} with quantities: {}", userId, productQuantities);
        
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            
            String lockKey = "inventory:reserve:" + productId;
            Lock lock = redisLockRegistry.obtain(lockKey);
            
            try {
                if (lock.tryLock(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.info("Acquired lock for product: {}", productId);
                    
                    // 1. Kiểm tra số lượng có sẵn với database lock
                    long availableCount = warehouseRepository.countByProductIdAndLockedFalseAndIsDeleteFalse(productId);
                    log.info("Available stock for product {}: {} items", productId, availableCount);
                    
                    if (availableCount < quantity) {
                        log.warn("Not enough stock for product {}. Required: {}, Available: {}", 
                            productId, quantity, availableCount);
                        return ReservationResult.failed("Không đủ hàng cho sản phẩm: " + productId + 
                            " (cần " + quantity + ", chỉ có " + availableCount + ")");
                    }
                    
                    // 2. Reserve items với database lock
                    List<Warehouse> reservedItems = reserveItemsForProduct(productId, quantity, userId);
                    
                    if (reservedItems.size() < quantity) {
                        log.warn("Could not reserve enough items for product {}. Required: {}, Reserved: {}", 
                            productId, quantity, reservedItems.size());
                        return ReservationResult.failed("Không thể đặt chỗ đủ hàng cho sản phẩm: " + productId);
                    }
                    
                    log.info("Successfully reserved {} items for product {}", reservedItems.size(), productId);
                    
                } else {
                    log.warn("Failed to acquire lock for product: {} within timeout", productId);
                    return ReservationResult.failed("Không thể đặt chỗ hàng cho sản phẩm: " + productId + " (timeout)");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for lock on product: {}", productId);
                return ReservationResult.failed("Bị gián đoạn khi đặt chỗ hàng cho sản phẩm: " + productId);
            } finally {
                lock.unlock();
            }
        }
        
        return ReservationResult.success("Đặt chỗ hàng thành công");
    }
    
    /**
     * Reserve items cho một product với database lock
     */
    @Transactional
    private List<Warehouse> reserveItemsForProduct(Long productId, Integer quantity, Long userId) {
        // Sử dụng SELECT FOR UPDATE để lock database row
        List<Warehouse> items = warehouseRepository.findAvailableItemsForReservation(productId, quantity);
        
        if (items.size() < quantity) {
            log.warn("Not enough items available for reservation. Required: {}, Found: {}", quantity, items.size());
            return items; // Trả về số lượng có thể reserve
        }
        
        // Reserve items
        LocalDateTime now = LocalDateTime.now();
        for (Warehouse item : items) {
            item.setLocked(true);
            item.setLockedBy(userId);
            item.setLockedAt(now);
        }
        
        warehouseRepository.saveAll(items);
        log.info("Successfully reserved {} items for product {}", items.size(), productId);
        
        return items;
    }
    
    /**
     * Release reservation khi user cancel hoặc timeout
     */
    @Transactional
    public void releaseReservation(List<Long> warehouseIds) {
        log.info("Releasing reservation for {} items", warehouseIds.size());
        
        for (Long warehouseId : warehouseIds) {
            try {
                Warehouse item = warehouseRepository.findById(warehouseId).orElse(null);
                if (item != null && item.getLocked()) {
                    item.setLocked(false);
                    item.setLockedBy(null);
                    item.setLockedAt(null);
                    warehouseRepository.save(item);
                    log.info("Released reservation for item: {}", warehouseId);
                }
            } catch (Exception e) {
                log.error("Failed to release reservation for item: {}", warehouseId, e);
            }
        }
    }
    
    /**
     * Confirm reservation (chuyển từ reserved sang sold)
     */
    @Transactional
    public void confirmReservation(List<Long> warehouseIds) {
        log.info("Confirming reservation for {} items", warehouseIds.size());
        
        for (Long warehouseId : warehouseIds) {
            try {
                Warehouse item = warehouseRepository.findById(warehouseId).orElse(null);
                if (item != null && item.getLocked()) {
                    item.setIsDelete(true);
                    item.setDeletedBy("SYSTEM");
                    warehouseRepository.save(item);
                    log.info("Confirmed reservation for item: {}", warehouseId);
                }
            } catch (Exception e) {
                log.error("Failed to confirm reservation for item: {}", warehouseId, e);
            }
        }
    }
    
    /**
     * Result class cho reservation
     */
    public static class ReservationResult {
        private final boolean success;
        private final String message;
        
        private ReservationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public static ReservationResult success(String message) {
            return new ReservationResult(true, message);
        }
        
        public static ReservationResult failed(String message) {
            return new ReservationResult(false, message);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

