package com.badat.study1.repository;

import com.badat.study1.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    
    Page<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);
    
    List<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    
    List<Order> findByStatusOrderByCreatedAtDesc(Order.Status status);
    
    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, Order.Status status);
    
    Page<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, Order.Status status, Pageable pageable);
    
    Optional<Order> findByOrderCode(String orderCode);
    
    @Query("SELECT o FROM Order o WHERE o.buyerId = :buyerId AND o.createdAt >= :startDate ORDER BY o.createdAt DESC")
    List<Order> findByBuyerIdAndCreatedAtAfter(@Param("buyerId") Long buyerId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.buyerId = :buyerId AND o.status = :status")
    Long countByBuyerIdAndStatus(@Param("buyerId") Long buyerId, @Param("status") Order.Status status);
    
    List<Order> findByOrderCodeContaining(String orderCode);
    
    // Method để lấy orders với OrderItem details
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product p LEFT JOIN FETCH oi.warehouse w WHERE o.id = :orderId")
    Optional<Order> findByIdWithOrderItems(@Param("orderId") Long orderId);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product p LEFT JOIN FETCH oi.warehouse w LEFT JOIN FETCH w.user u WHERE o.buyerId = :buyerId ORDER BY o.createdAt DESC")
    List<Order> findByBuyerIdWithOrderItems(@Param("buyerId") Long buyerId);
}
