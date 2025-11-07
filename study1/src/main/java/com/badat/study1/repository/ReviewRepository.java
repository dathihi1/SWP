package com.badat.study1.repository;

import com.badat.study1.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);
    List<Review> findByProductIdAndIsDeleteFalse(Long productId);
    List<Review> findByBuyerId(Long buyerId);
    List<Review> findByBuyerIdAndIsDeleteFalse(Long buyerId);
    List<Review> findByRating(Integer rating);
    
    // Seller review methods
    List<Review> findBySellerIdAndIsDeleteFalse(Long sellerId);
    Page<Review> findBySellerIdAndIsDeleteFalse(Long sellerId, Pageable pageable);
    List<Review> findBySellerIdAndRatingAndIsDeleteFalse(Long sellerId, Integer rating);
    Page<Review> findBySellerIdAndRatingAndIsDeleteFalse(Long sellerId, Integer rating, Pageable pageable);
    List<Review> findBySellerIdAndReplyContentIsNullAndIsDeleteFalse(Long sellerId);
    
    // Secure seller review methods - validate both seller and shop
    @EntityGraph(attributePaths = {"buyer", "product"})
    List<Review> findBySellerIdAndShopIdAndIsDeleteFalse(Long sellerId, Long shopId);
    @EntityGraph(attributePaths = {"buyer", "product"})
    Page<Review> findBySellerIdAndShopIdAndIsDeleteFalse(Long sellerId, Long shopId, Pageable pageable);
    
    // Additional review methods
    List<Review> findByShopIdAndIsDeleteFalse(Long shopId);
    @EntityGraph(attributePaths = {"buyer", "productVariant"})
    List<Review> findByProductVariantIdAndIsDeleteFalse(Long productVariantId);
    @EntityGraph(attributePaths = {"buyer", "product"})
    List<Review> findByOrderIdAndIsDeleteFalse(Long orderId);
}
