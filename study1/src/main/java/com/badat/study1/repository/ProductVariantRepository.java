package com.badat.study1.repository;

import com.badat.study1.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    
    List<ProductVariant> findByProductIdAndStatusAndIsDeleteFalse(Long productId, ProductVariant.Status status);
    
    List<ProductVariant> findByProductIdAndIsDeleteFalse(Long productId);
    
    Optional<ProductVariant> findByIdAndIsDeleteFalse(Long id);
    
    Optional<ProductVariant> findByUniqueKeyAndIsDeleteFalse(String uniqueKey);
    
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'AVAILABLE' AND pv.isDelete = false ORDER BY pv.createdAt ASC")
    List<ProductVariant> findAvailableVariantsByProductId(@Param("productId") Long productId);
}
