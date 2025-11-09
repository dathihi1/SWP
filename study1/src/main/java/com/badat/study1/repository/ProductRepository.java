package com.badat.study1.repository;

import com.badat.study1.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Find all products by shop ID
    List<Product> findByShopIdAndIsDeleteFalse(Long shopId);
    
    // Find all products by shop ID and status
    List<Product> findByShopIdAndStatusAndIsDeleteFalse(Long shopId, String status);
    
    // Find product by ID and shop ID (to ensure user can only access their own products)
    Optional<Product> findByIdAndShopIdAndIsDeleteFalse(Long id, Long shopId);
    
    // Count products by shop ID
    long countByShopIdAndIsDeleteFalse(Long shopId);
    
    // Find all active products
    List<Product> findByStatusAndIsDeleteFalse(String status);
    
    // Check if product category already exists for a shop
    boolean existsByShopIdAndProductCategoryAndIsDeleteFalse(Long shopId, String productCategory);
    
    // Check if product with same name and category already exists for a shop
    Optional<Product> findByProductNameAndProductCategoryAndShopIdAndIsDeleteFalse(String productName, String productCategory, Long shopId);
    
    // Find all pending products for admin approval
    List<Product> findByStatusAndIsDeleteFalseOrderByCreatedAtDesc(String status);
    
    // Find products by name containing and status
    List<Product> findByProductNameContainingIgnoreCaseAndIsDeleteFalseAndStatus(String productName, String status);
    
    // Find all products by shop ID (including deleted ones for admin operations)
    List<Product> findByShopId(Long shopId);
    
}

