package com.badat.study1.repository;

import com.badat.study1.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    
    /**
     * Find warehouse items by product ID
     */
    List<Warehouse> findByProductIdOrderByCreatedAtDesc(Long productId);
    
    /**
     * Find warehouse items by shop ID
     */
    List<Warehouse> findByShopIdOrderByCreatedAtDesc(Long shopId);
    
    /**
     * Find warehouse items by stall ID
     */
    List<Warehouse> findByStallIdOrderByCreatedAtDesc(Long stallId);
    
    /**
     * Find warehouse items by user ID
     */
    List<Warehouse> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find warehouse items by item type
     */
    List<Warehouse> findByItemTypeOrderByCreatedAtDesc(Warehouse.ItemType itemType);
    
    /**
     * Count warehouse items by product ID
     */
    long countByProductId(Long productId);
    
    /**
     * Count warehouse items by shop ID
     */
    long countByShopId(Long shopId);
    
    /**
     * Count warehouse items by stall ID
     */
    long countByStallId(Long stallId);
    
    /**
     * Count warehouse items by user ID
     */
    long countByUserId(Long userId);
    
    /**
     * Count warehouse items by item type
     */
    long countByItemType(Warehouse.ItemType itemType);
    
    /**
     * Find warehouse items by product ID and item type
     */
    List<Warehouse> findByProductIdAndItemTypeOrderByCreatedAtDesc(Long productId, Warehouse.ItemType itemType);
    
    /**
     * Find recent warehouse items for a specific product (last 50 records)
     */
    @Query(value = "SELECT * FROM warehouse WHERE product_id = :productId ORDER BY created_at DESC LIMIT 50", nativeQuery = true)
    List<Warehouse> findRecentByProductId(@Param("productId") Long productId);
    
    /**
     * Find first available warehouse item for product (not locked, not deleted)
     */
    Optional<Warehouse> findFirstByProductIdAndLockedFalseAndIsDeleteFalse(Long productId);
}
