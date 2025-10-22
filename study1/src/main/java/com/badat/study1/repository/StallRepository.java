package com.badat.study1.repository;

import com.badat.study1.model.Stall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StallRepository extends JpaRepository<Stall, Long> {
    
    // Find all stalls by shop ID
    List<Stall> findByShopIdAndIsDeleteFalse(Long shopId);
    
    // Find all stalls by shop ID and status
    List<Stall> findByShopIdAndStatusAndIsDeleteFalse(Long shopId, String status);
    
    // Find stall by ID and shop ID (to ensure user can only access their own stalls)
    Optional<Stall> findByIdAndShopIdAndIsDeleteFalse(Long id, Long shopId);
    
    // Count stalls by shop ID
    long countByShopIdAndIsDeleteFalse(Long shopId);
    
    // Find all active stalls
    List<Stall> findByStatusAndIsDeleteFalse(String status);
    
    // Check if stall category already exists for a shop
    boolean existsByShopIdAndStallCategoryAndIsDeleteFalse(Long shopId, String stallCategory);
    
    // Check if stall with same name and category already exists for a shop
    Optional<Stall> findByStallNameAndStallCategoryAndShopIdAndIsDeleteFalse(String stallName, String stallCategory, Long shopId);
    
    // Find all pending stalls for admin approval
    List<Stall> findByStatusAndIsDeleteFalseOrderByCreatedAtDesc(String status);
    
    // Find stalls by name containing and status
    List<Stall> findByStallNameContainingIgnoreCaseAndIsDeleteFalseAndStatus(String stallName, String status);
    
}
