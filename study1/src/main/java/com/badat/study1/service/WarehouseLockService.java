package com.badat.study1.service;

import com.badat.study1.model.Warehouse;
import com.badat.study1.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

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
                Warehouse lockedItem = lockWarehouseItem(productId);
                lockedItems.add(lockedItem);
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
            throw e;
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
}
