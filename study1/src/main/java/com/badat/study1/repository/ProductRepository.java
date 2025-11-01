package com.badat.study1.repository;

import com.badat.study1.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByShopId(Long shopId);
    List<Product> findByShopIdAndIsDeleteFalse(Long shopId);
    List<Product> findByIsDeleteFalse();
    List<Product> findByStatus(Product.Status status);
    List<Product> findByType(String type);
    List<Product> findByNameContainingIgnoreCase(String name);
    Optional<Product> findByUniqueKey(String uniqueKey);

	// Filter helpers for browsing
	List<Product> findByIsDeleteFalseAndStatus(Product.Status status);
	List<Product> findByNameContainingIgnoreCaseAndIsDeleteFalseAndStatus(String name, Product.Status status);
	
	// Count methods
	long countByShopIdAndIsDeleteFalse(Long shopId);
	
	// Stall methods
	List<Product> findByStallIdAndIsDeleteFalse(Long stallId);
	
	// Recovery methods
	Optional<Product> findByNameAndPriceAndShopIdAndIsDeleteTrue(String name, BigDecimal price, Long shopId);
	
	// Warehouse quantity methods
    @Query("SELECT COUNT(w) FROM Warehouse w WHERE w.product.id = :productId AND w.isDelete = false AND w.locked = false")
	long countWarehouseItemsByProductId(@Param("productId") Long productId);
	
    @Query("SELECT COUNT(w) FROM Warehouse w WHERE w.product.id = :productId AND w.itemType = :itemType AND w.isDelete = false AND w.locked = false")
	long countWarehouseItemsByProductIdAndType(@Param("productId") Long productId, @Param("itemType") String itemType);
}