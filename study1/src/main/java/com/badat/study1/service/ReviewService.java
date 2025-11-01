package com.badat.study1.service;

import com.badat.study1.model.Order;
import com.badat.study1.model.Review;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Review createReview(Long orderId, Long productId, Long buyerId, 
                              Integer rating, String title, String content) {
        
        // Get order details
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Get seller and shop info from order
        Long sellerId = order.getSellerId();
        Long shopId = order.getShopId();
        Long stallId = order.getStallId();
        
        // Create review
        Review review = Review.builder()
            .orderId(orderId)
            .productId(productId)
            .buyerId(buyerId)
            .sellerId(sellerId)
            .shopId(shopId)
            .stallId(stallId)
            .rating(rating)
            .title(title)
            .content(content)
            .isRead(false)
            .build();
        
        return reviewRepository.save(review);
    }
}
