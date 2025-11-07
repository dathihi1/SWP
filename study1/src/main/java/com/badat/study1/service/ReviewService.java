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
    public Review createReview(Long orderId, Long productId, Long buyerId, 
                              Integer rating, String title, String content) {
        
        // Get order details
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Get seller and shop info from order
        Long sellerId = order.getSellerId();
        Long shopId = order.getShopId();
        Long productIdFromOrder = order.getProductId();
        
        // Resolve product variant for order-level review when productId is not provided
        if (productId == null) {
            java.util.List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
            if (orderItems == null || orderItems.isEmpty() || orderItems.get(0).getProductVariantId() == null) {
                throw new RuntimeException("Không tìm thấy biến thể trong đơn hàng để đánh giá");
            }
            productId = orderItems.get(0).getProductVariantId();
        }

        // Create review
        Review review = Review.builder()
            .orderId(orderId)
            .productVariantId(productId)
            .buyerId(buyerId)
            .sellerId(sellerId)
            .shopId(shopId)
            .productId(productIdFromOrder)
            .rating(rating)
            .title(title)
            .content(content)
            .isRead(false)
            .build();
        
        return reviewRepository.save(review);
    }
}
