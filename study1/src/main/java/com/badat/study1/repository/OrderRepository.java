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
    
    List<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    
    Page<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
    
    List<Order> findByShopIdOrderByCreatedAtDesc(Long shopId);
    
    List<Order> findByStallIdOrderByCreatedAtDesc(Long stallId);
    
    List<Order> findByStatusOrderByCreatedAtDesc(Order.Status status);
    
    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, Order.Status status);
    
    List<Order> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, Order.Status status);
    
    Page<Order> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, Order.Status status, Pageable pageable);
    
    // Secure seller order methods - validate both seller and shop
    List<Order> findBySellerIdAndShopIdOrderByCreatedAtDesc(Long sellerId, Long shopId);
    Page<Order> findBySellerIdAndShopIdOrderByCreatedAtDesc(Long sellerId, Long shopId, Pageable pageable);
    List<Order> findBySellerIdAndShopIdAndStatusOrderByCreatedAtDesc(Long sellerId, Long shopId, Order.Status status);
    Page<Order> findBySellerIdAndShopIdAndStatusOrderByCreatedAtDesc(Long sellerId, Long shopId, Order.Status status, Pageable pageable);
    
    Optional<Order> findByOrderCode(String orderCode);
    
    @Query("SELECT o FROM Order o WHERE o.buyerId = :buyerId AND o.createdAt >= :startDate ORDER BY o.createdAt DESC")
    List<Order> findByBuyerIdAndCreatedAtAfter(@Param("buyerId") Long buyerId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT o FROM Order o WHERE o.sellerId = :sellerId AND o.createdAt >= :startDate ORDER BY o.createdAt DESC")
    List<Order> findBySellerIdAndCreatedAtAfter(@Param("sellerId") Long sellerId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.buyerId = :buyerId AND o.status = :status")
    Long countByBuyerIdAndStatus(@Param("buyerId") Long buyerId, @Param("status") Order.Status status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.sellerId = :sellerId AND o.status = :status")
    Long countBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") Order.Status status);
    
    List<Order> findByOrderCodeContaining(String orderCode);
}
