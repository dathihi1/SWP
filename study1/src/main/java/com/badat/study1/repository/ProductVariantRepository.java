package com.badat.study1.repository;

import com.badat.study1.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByShopId(Long shopId);
    List<ProductVariant> findByShopIdAndIsDeleteFalse(Long shopId);
    List<ProductVariant> findByIsDeleteFalse();
    List<ProductVariant> findByStatus(ProductVariant.Status status);
    List<ProductVariant> findByType(String type);
    List<ProductVariant> findByNameContainingIgnoreCase(String name);
    Optional<ProductVariant> findByUniqueKey(String uniqueKey);

	// Filter helpers for browsing
	List<ProductVariant> findByIsDeleteFalseAndStatus(ProductVariant.Status status);
	List<ProductVariant> findByNameContainingIgnoreCaseAndIsDeleteFalseAndStatus(String name, ProductVariant.Status status);
	
	// Count methods
	long countByShopIdAndIsDeleteFalse(Long shopId);
	
	// Product methods
	List<ProductVariant> findByProductIdAndIsDeleteFalse(Long productId);
	
	// Recovery methods
	Optional<ProductVariant> findByNameAndPriceAndShopIdAndIsDeleteTrue(String name, BigDecimal price, Long shopId);
	
	// Warehouse quantity methods
    @Query("SELECT COUNT(w) FROM Warehouse w WHERE w.productVariant.id = :productVariantId AND w.isDelete = false AND w.locked = false")
	long countWarehouseItemsByProductVariantId(@Param("productVariantId") Long productVariantId);
	
    @Query("SELECT COUNT(w) FROM Warehouse w WHERE w.productVariant.id = :productVariantId AND w.itemType = :itemType AND w.isDelete = false AND w.locked = false")
	long countWarehouseItemsByProductVariantIdAndType(@Param("productVariantId") Long productVariantId, @Param("itemType") String itemType);
}

