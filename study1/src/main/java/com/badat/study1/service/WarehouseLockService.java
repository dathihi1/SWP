package com.badat.study1.service;

import com.badat.study1.model.Warehouse;
import com.badat.study1.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseLockService {
    
    private final WarehouseRepository warehouseRepository;
    private final RedisLockRegistry redisLockRegistry;
    
    /**
     * Lock một warehouse item cho product
     */
    @Transactional
    public Warehouse lockWarehouseItem(Long productId) {
        String lockKey = "warehouse:lock:" + productId;
        Lock lock = redisLockRegistry.obtain(lockKey);
        
        log.info("Attempting to lock warehouse item for product: {}", productId);
        
        try {
            if (lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)) {
                log.info("Acquired lock for product: {}", productId);
                
                // Tìm item chưa bị lock
                Optional<Warehouse> itemOpt = warehouseRepository
                    .findFirstByProductIdAndLockedFalseAndIsDeleteFalse(productId);
                    
                if (itemOpt.isPresent()) {
                    Warehouse warehouse = itemOpt.get();
                    
                    // Lock item
                    warehouse.setLocked(true);
                    warehouse.setLockedBy(getCurrentUserId());
                    warehouse.setLockedAt(LocalDateTime.now());
                    warehouseRepository.save(warehouse);
                    
                    log.info("Successfully locked warehouse item: {} for product: {}", 
                        warehouse.getId(), productId);
                    return warehouse;
                } else {
                    log.warn("No available warehouse items for product: {}", productId);
                    throw new RuntimeException("No available warehouse items for product: " + productId);
                }
            } else {
                log.warn("Failed to acquire lock for product: {} within timeout", productId);
                throw new RuntimeException("Failed to acquire lock for product: " + productId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for lock on product: {}", productId);
            throw new RuntimeException("Interrupted while waiting for lock", e);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Lock nhiều warehouse items cho một danh sách products
     */
    @Transactional
    public List<Warehouse> lockWarehouseItems(List<Long> productIds) {
        List<Warehouse> lockedItems = new ArrayList<>();
        
        log.info("Locking warehouse items for {} products", productIds.size());
        
        try {
            for (Long productId : productIds) {
                try {
                    Warehouse lockedItem = lockWarehouseItem(productId);
                    lockedItems.add(lockedItem);
                } catch (RuntimeException e) {
                    log.warn("Failed to lock warehouse item for product {}: {}", productId, e.getMessage());
                    // Nếu không lock được một item nào đó → rollback tất cả
                    break;
                }
            }
            
            // Kiểm tra xem có lock được đủ số lượng không
            if (lockedItems.size() < productIds.size()) {
                log.warn("Only locked {}/{} items, rolling back all locks", lockedItems.size(), productIds.size());
                // Rollback: unlock tất cả items đã lock
                for (Warehouse item : lockedItems) {
                    try {
                        unlockWarehouseItem(item.getId());
                    } catch (Exception rollbackEx) {
                        log.error("Failed to unlock item during rollback: {}", item.getId(), rollbackEx);
                    }
                }
                return new ArrayList<>(); // Trả về empty list
            }
            
            log.info("Successfully locked {} warehouse items", lockedItems.size());
            return lockedItems;
            
        } catch (Exception e) {
            // Rollback: unlock tất cả items đã lock
            log.error("Failed to lock warehouse items, rolling back...", e);
            for (Warehouse item : lockedItems) {
                try {
                    unlockWarehouseItem(item.getId());
                } catch (Exception rollbackEx) {
                    log.error("Failed to unlock item during rollback: {}", item.getId(), rollbackEx);
                }
            }
            return new ArrayList<>(); // Trả về empty list thay vì throw exception
        }
    }
    
    /**
     * Lock warehouse items theo số lượng cụ thể cho mỗi product - OPTIMIZED VERSION
     * Sử dụng batch operations thay vì N+1 queries
     */
    @Transactional
    public List<Warehouse> lockWarehouseItemsWithQuantities(Map<Long, Integer> productQuantities) {
        List<Warehouse> lockedItems = new ArrayList<>();
        List<Warehouse> itemsToSave = new ArrayList<>();
        
        log.info("Locking warehouse items for {} products with quantities (OPTIMIZED)", productQuantities.size());
        
        try {
            for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
                Long productId = entry.getKey();
                Integer requiredQuantity = entry.getValue();

                log.info("Locking {} items for product {}", requiredQuantity, productId);

                String lockKey = "warehouse:lock:" + productId;
                Lock lock = redisLockRegistry.obtain(lockKey);

                try {
                    if (lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        log.info("Acquired lock for product: {}", productId);
                        
                        // 0. Kiểm tra số lượng có sẵn trước khi tìm
                        long availableCount = warehouseRepository.countByProductIdAndLockedFalseAndIsDeleteFalse(productId);
                        log.info("Available stock for product {}: {} items", productId, availableCount);
                        
                        if (availableCount < requiredQuantity) {
                            log.warn("Not enough stock for product {}. Required: {}, Available: {}", 
                                productId, requiredQuantity, availableCount);
                            throw new RuntimeException("Không đủ hàng cho sản phẩm: " + productId + 
                                " (cần " + requiredQuantity + ", chỉ có " + availableCount + ")");
                        }
                        
                        // 1. Tìm CHÍNH XÁC số lượng cần trong 1 query
                        Pageable limit = Pageable.ofSize(requiredQuantity);
                        List<Warehouse> foundItems = warehouseRepository
                            .findByProductIdAndLockedFalseAndIsDeleteFalseOrderByCreatedAtAsc(productId, limit);
                        
                        log.info("Found {} items for product {} (requested: {})", foundItems.size(), productId, requiredQuantity);

                        // 2. Kiểm tra có đủ hàng không
                        if (foundItems.size() < requiredQuantity) {
                            log.warn("Not enough stock for product {}. Required: {}, Found: {}", 
                                productId, requiredQuantity, foundItems.size());
                            // Nếu không đủ, ném lỗi để rollback toàn bộ @Transactional
                            throw new RuntimeException("Không đủ hàng cho sản phẩm: " + productId + 
                                " (cần " + requiredQuantity + ", chỉ có " + foundItems.size() + ")");
                        }

                        // 3. Khóa tất cả các item tìm thấy
                        for (Warehouse item : foundItems) {
                            item.setLocked(true);
                            item.setLockedBy(getCurrentUserId());
                            item.setLockedAt(LocalDateTime.now());
                        }
                        
                        itemsToSave.addAll(foundItems);
                        lockedItems.addAll(foundItems);
                        
                        log.info("Successfully prepared {} items for product {} to be locked", 
                            foundItems.size(), productId);

                    } else {
                        log.warn("Failed to acquire lock for product: {} within timeout", productId);
                        throw new RuntimeException("Failed to acquire lock for product: " + productId);
                    }
                } finally {
                    lock.unlock();
                }
            }

            // 4. Lưu tất cả thay đổi vào CSDL trong 1 lần batch
            if (!itemsToSave.isEmpty()) {
                warehouseRepository.saveAll(itemsToSave);
                log.info("Successfully saved {} warehouse items in batch", itemsToSave.size());
            }

            log.info("Successfully locked {} warehouse items with quantities (OPTIMIZED)", lockedItems.size());
            return lockedItems;
            
        } catch (Exception e) {
            // Nếu có bất kỳ lỗi nào (không đủ hàng, không lấy được lock...),
            // @Transactional sẽ tự động rollback tất cả các thay đổi
            log.error("Failed to lock warehouse items with quantities, rolling back...", e);
            
            // Không cần phải unlock thủ công ở đây vì @Transactional sẽ lo việc đó.
            // Trả về rỗng để báo hiệu thất bại.
            return new ArrayList<>();
        }
    }
    
    /**
     * Unlock warehouse item
     */
    @Transactional
    public void unlockWarehouseItem(Long warehouseId) {
        log.info("Unlocking warehouse item: {}", warehouseId);
        
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new RuntimeException("Warehouse item not found: " + warehouseId));
            
        warehouse.setLocked(false);
        warehouse.setLockedBy(null);
        warehouse.setLockedAt(null);
        warehouseRepository.save(warehouse);
        
        log.info("Successfully unlocked warehouse item: {}", warehouseId);
    }
    
    /**
     * Unlock nhiều warehouse items
     */
    @Transactional
    public void unlockWarehouseItems(List<Long> warehouseIds) {
        log.info("Unlocking {} warehouse items", warehouseIds.size());
        
        for (Long warehouseId : warehouseIds) {
            try {
                unlockWarehouseItem(warehouseId);
            } catch (Exception e) {
                log.error("Failed to unlock warehouse item: {}", warehouseId, e);
            }
        }
    }
    
    /**
     * Mark warehouse item as delivered (delete)
     */
    @Transactional
    public void markAsDelivered(Long warehouseId) {
        log.info("Marking warehouse item as delivered: {}", warehouseId);
        
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new RuntimeException("Warehouse item not found: " + warehouseId));
            
        warehouse.setIsDelete(true);
        warehouse.setDeletedBy("SYSTEM");
        warehouseRepository.save(warehouse);
        
        log.info("Successfully marked warehouse item as delivered: {}", warehouseId);
    }
    
    /**
     * Lấy current user ID (placeholder - cần implement authentication)
     */
    private Long getCurrentUserId() {
        // TODO: Implement proper authentication
        return 1L; // Placeholder
    }
    
    /**
     * Lấy số lượng hàng có sẵn cho product (không bị lock, không bị xóa)
     */
    public long getAvailableStockCount(Long productId) {
        return warehouseRepository.countByProductIdAndLockedFalseAndIsDeleteFalse(productId);
    }
    
    /**
     * Lấy số lượng hàng có sẵn cho productVariant (không bị lock, không bị xóa)
     */
    public long getAvailableStockCountByVariant(Long productVariantId) {
        return warehouseRepository.countByProductVariantIdAndLockedFalseAndIsDeleteFalse(productVariantId);
    }
    
    /**
     * Reserve warehouse items với timeout (tạm thời lock với thời gian hết hạn) - theo productVariantId
     * Sử dụng để tránh race condition khi nhiều user cùng mua
     */
    @Transactional
    public List<Warehouse> reserveWarehouseItemsWithTimeoutByVariant(Map<Long, Integer> productVariantQuantities, Long userId, int timeoutMinutes) {
        List<Warehouse> reservedItems = new ArrayList<>();
        
        log.info("Reserving warehouse items with {} minutes timeout for user: {} (by productVariant)", timeoutMinutes, userId);
        
        try {
            for (Map.Entry<Long, Integer> entry : productVariantQuantities.entrySet()) {
                Long productVariantId = entry.getKey();
                Integer requiredQuantity = entry.getValue();
                
                String lockKey = "warehouse:reserve:variant:" + productVariantId;
                Lock lock = redisLockRegistry.obtain(lockKey);
                
                try {
                    if (lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        log.info("Acquired reservation lock for productVariant: {}", productVariantId);
                        
                        // SỬ DỤNG SELECT FOR UPDATE để tránh race condition
                        // Lock database rows ngay từ đầu để đảm bảo atomicity
                        List<Warehouse> items = warehouseRepository.findAvailableItemsForReservationByVariant(productVariantId, requiredQuantity);
                        
                        if (items.size() < requiredQuantity) {
                            log.warn("Not enough stock for productVariant {}. Required: {}, Available: {}", 
                                productVariantId, requiredQuantity, items.size());
                            throw new RuntimeException("Không đủ hàng cho sản phẩm: " + productVariantId + 
                                " (cần " + requiredQuantity + ", chỉ có " + items.size() + ")");
                        }
                        
                        // Set reservation timeout
                        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);
                        for (Warehouse item : items) {
                            item.setLocked(true);
                            item.setLockedBy(userId);
                            item.setLockedAt(LocalDateTime.now());
                            item.setReservedUntil(expiresAt); // Thêm field này vào Warehouse model
                        }
                        
                        warehouseRepository.saveAll(items);
                        reservedItems.addAll(items);
                        
                        log.info("Successfully reserved {} items for productVariant {} until {}", 
                            items.size(), productVariantId, expiresAt);
                            
                    } else {
                        log.warn("Failed to acquire reservation lock for productVariant: {}", productVariantId);
                        throw new RuntimeException("Không thể đặt chỗ hàng cho sản phẩm: " + productVariantId);
                    }
                } finally {
                    lock.unlock();
                }
            }
            
            log.info("Successfully reserved {} total items with timeout (by productVariant)", reservedItems.size());
            return reservedItems;
            
        } catch (Exception e) {
            // Rollback: release tất cả items đã reserve
            log.error("Failed to reserve warehouse items, releasing all reservations", e);
            for (Warehouse item : reservedItems) {
                try {
                    unlockWarehouseItem(item.getId());
                } catch (Exception rollbackEx) {
                    log.error("Failed to release item during rollback: {}", item.getId(), rollbackEx);
                }
            }
            throw new RuntimeException("Không thể đặt chỗ hàng: " + e.getMessage());
        }
    }
    
    /**
     * Reserve warehouse items với timeout (tạm thời lock với thời gian hết hạn)
     * Sử dụng để tránh race condition khi nhiều user cùng mua
     */
    @Transactional
    public List<Warehouse> reserveWarehouseItemsWithTimeout(Map<Long, Integer> productQuantities, Long userId, int timeoutMinutes) {
        List<Warehouse> reservedItems = new ArrayList<>();
        
        log.info("Reserving warehouse items with {} minutes timeout for user: {}", timeoutMinutes, userId);
        
        try {
            for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
                Long productId = entry.getKey();
                Integer requiredQuantity = entry.getValue();
                
                String lockKey = "warehouse:reserve:" + productId;
                Lock lock = redisLockRegistry.obtain(lockKey);
                
                try {
                    if (lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        log.info("Acquired reservation lock for product: {}", productId);
                        
                        // SỬ DỤNG SELECT FOR UPDATE để tránh race condition
                        // Lock database rows ngay từ đầu để đảm bảo atomicity
                        List<Warehouse> items = warehouseRepository.findAvailableItemsForReservation(productId, requiredQuantity);
                        
                        if (items.size() < requiredQuantity) {
                            log.warn("Not enough stock for product {}. Required: {}, Available: {}", 
                                productId, requiredQuantity, items.size());
                            throw new RuntimeException("Không đủ hàng cho sản phẩm: " + productId + 
                                " (cần " + requiredQuantity + ", chỉ có " + items.size() + ")");
                        }
                        
                        // Set reservation timeout
                        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);
                        for (Warehouse item : items) {
                            item.setLocked(true);
                            item.setLockedBy(userId);
                            item.setLockedAt(LocalDateTime.now());
                            item.setReservedUntil(expiresAt); // Thêm field này vào Warehouse model
                        }
                        
                        warehouseRepository.saveAll(items);
                        reservedItems.addAll(items);
                        
                        log.info("Successfully reserved {} items for product {} until {}", 
                            items.size(), productId, expiresAt);
                            
                    } else {
                        log.warn("Failed to acquire reservation lock for product: {}", productId);
                        throw new RuntimeException("Không thể đặt chỗ hàng cho sản phẩm: " + productId);
                    }
                } finally {
                    lock.unlock();
                }
            }
            
            log.info("Successfully reserved {} total items with timeout", reservedItems.size());
            return reservedItems;
            
        } catch (Exception e) {
            // Rollback: release tất cả items đã reserve
            log.error("Failed to reserve warehouse items, releasing all reservations", e);
            for (Warehouse item : reservedItems) {
                try {
                    unlockWarehouseItem(item.getId());
                } catch (Exception rollbackEx) {
                    log.error("Failed to release item during rollback: {}", item.getId(), rollbackEx);
                }
            }
            throw new RuntimeException("Không thể đặt chỗ hàng: " + e.getMessage());
        }
    }
    
    /**
     * Cron job để release expired reservations
     */
    @Scheduled(fixedRate = 60000) // Mỗi 1 phút
    @Transactional
    public void releaseExpiredReservations() {
        log.info("Checking for expired warehouse reservations...");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Warehouse> expiredItems = warehouseRepository.findByLockedTrueAndReservedUntilBefore(now);
            
            if (!expiredItems.isEmpty()) {
                log.info("Found {} expired warehouse reservations", expiredItems.size());
                
                for (Warehouse item : expiredItems) {
                    item.setLocked(false);
                    item.setLockedBy(null);
                    item.setLockedAt(null);
                    item.setReservedUntil(null);
                }
                
                warehouseRepository.saveAll(expiredItems);
                log.info("Released {} expired warehouse reservations", expiredItems.size());
            }
        } catch (Exception e) {
            log.error("Error releasing expired reservations: {}", e.getMessage(), e);
        }
    }
}
