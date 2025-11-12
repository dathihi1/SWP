package com.badat.study1.service;

import com.badat.study1.model.Order;
import com.badat.study1.model.OrderItem;
import com.badat.study1.model.Review;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.repository.OrderItemRepository;
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
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Review createReview(Long orderId, Long orderItemId, Long buyerId,
                              Integer rating, String title, String content) {

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
            .orElseThrow(() -> new RuntimeException("Order item not found"));
        if (!orderItem.getOrderId().equals(orderId)) {
            throw new RuntimeException("Order item does not belong to the specified order");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("Order does not belong to the current user");
        }

        Long sellerId = orderItem.getSellerId();
        Long shopId = orderItem.getShopId();
        Long productId = orderItem.getProductId();
        Long productVariantId = orderItem.getProductVariantId();

        Review review = Review.builder()
            .orderId(orderId)
            .orderItemId(orderItemId)
            .productVariantId(productVariantId)
            .buyerId(buyerId)
            .sellerId(sellerId)
            .shopId(shopId)
            .productId(productId)
            .rating(rating)
            .title(title)
            .content(content)
            .isRead(false)
            .build();
        
        return reviewRepository.save(review);
    }
}
