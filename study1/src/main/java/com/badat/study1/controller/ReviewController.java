package com.badat.study1.controller;

import com.badat.study1.model.Order;
import com.badat.study1.model.Review;
import com.badat.study1.model.User;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.repository.ReviewRepository;
import com.badat.study1.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    @PostMapping("/reviews")
    public ResponseEntity<Map<String, Object>> createReview(@RequestBody Map<String, Object> reviewData) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            Long orderId = Long.valueOf(reviewData.get("orderId").toString());
            Long productId = null;
            if (reviewData.containsKey("productId") && reviewData.get("productId") != null && !reviewData.get("productId").toString().isBlank()) {
                productId = Long.valueOf(reviewData.get("productId").toString());
            }
            Integer rating = Integer.valueOf(reviewData.get("rating").toString());
            String title = reviewData.get("title") != null ? reviewData.get("title").toString() : null;
            String content = reviewData.get("content") != null ? reviewData.get("content").toString() : null;
            
            // Get order to validate
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            // Validate that the order belongs to the current user
            if (!order.getBuyerId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Đơn hàng không tồn tại hoặc không thuộc về bạn"
                ));
            }
            
            // Validate order is COMPLETED
            if (order.getStatus() != Order.Status.COMPLETED) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Chỉ có thể đánh giá đơn hàng đã hoàn thành"
                ));
            }
            
            // Check if review already exists for this order by current user
            List<Review> existingReviews = reviewRepository.findByOrderIdAndIsDeleteFalse(orderId);
            boolean alreadyReviewedByUser = existingReviews.stream()
                .anyMatch(r -> r.getBuyerId().equals(user.getId()));
            if (alreadyReviewedByUser) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Bạn đã đánh giá đơn hàng này rồi"
                ));
            }
            
            // Check if 7 days limit has passed
            java.time.LocalDateTime completedAt = order.getUpdatedAt(); // Assume completed time is when status was set
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            long daysSinceCompleted = java.time.temporal.ChronoUnit.DAYS.between(completedAt, now);
            
            if (daysSinceCompleted > 7) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Bạn chỉ có thể đánh giá trong vòng 7 ngày sau khi hoàn thành đơn hàng"
                ));
            }
            
            Review review = reviewService.createReview(orderId, productId, user.getId(), rating, title, content);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đánh giá đã được gửi thành công",
                "reviewId", review.getId()
            ));
            
        } catch (Exception e) {
            log.error("Error creating review", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Có lỗi xảy ra khi gửi đánh giá"
            ));
        }
    }
}
