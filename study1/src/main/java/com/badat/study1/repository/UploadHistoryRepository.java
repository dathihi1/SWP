package com.badat.study1.repository;

import com.badat.study1.model.UploadHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadHistoryRepository extends JpaRepository<UploadHistory, Long> {
    
    /**
     * Find upload history by user ID
     */
    List<UploadHistory> findByUserId(Long userId);
    
    /**
     * Find upload history by user ID, ordered by creation date descending
     */
    List<UploadHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find upload history by user ID with pagination
     */
    Page<UploadHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find upload history by product ID, ordered by creation date descending
     */
    List<UploadHistory> findByProductIdOrderByCreatedAtDesc(Long productId);
    
    /**
     * Find recent upload history for a specific product (last 10 records)
     */
    @Query("SELECT uh FROM UploadHistory uh WHERE uh.product.id = :productId ORDER BY uh.createdAt DESC")
    List<UploadHistory> findRecentByProductId(@Param("productId") Long productId, Pageable pageable);
    
    /**
     * Find recent upload history for a specific product (last 10 records) - simple version
     */
    @Query(value = "SELECT * FROM upload_history WHERE product_id = :productId ORDER BY created_at DESC LIMIT 10", nativeQuery = true)
    List<UploadHistory> findRecentByProductIdSimple(@Param("productId") Long productId);
    
    /**
     * Count total uploads by user
     */
    long countByUserId(Long userId);
    
    /**
     * Count successful uploads by user
     */
    long countByUserIdAndIsSuccessTrue(Long userId);
    
    /**
     * Count failed uploads by user
     */
    long countByUserIdAndIsSuccessFalse(Long userId);
}
