package com.badat.study1.repository;

import com.badat.study1.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);
    List<Review> findByProductIdAndIsDeleteFalse(Long productId);
    List<Review> findByUserId(Long userId);
    List<Review> findByUserIdAndIsDeleteFalse(Long userId);
    List<Review> findByRating(Integer rating);
}
