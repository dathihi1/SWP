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
    
    // Method để lấy orders theo danh sách IDs - sử dụng @Query
    @Query("SELECT o FROM Order o WHERE o.id IN :orderIds")
    List<Order> findAllById(@Param("orderIds") List<Long> orderIds);
    
    // Method để lấy orders với OrderItem details
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.productVariant p LEFT JOIN FETCH oi.warehouse w WHERE o.id = :orderId")
    Optional<Order> findByIdWithOrderItems(@Param("orderId") Long orderId);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.productVariant p LEFT JOIN FETCH oi.warehouse w LEFT JOIN FETCH w.user u WHERE o.buyerId = :buyerId ORDER BY o.createdAt DESC")
    List<Order> findByBuyerIdWithOrderItems(@Param("buyerId") Long buyerId);
    
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.productVariant p " +
           "LEFT JOIN FETCH oi.warehouse w " +
           "LEFT JOIN FETCH w.user u " +
           "WHERE o.buyerId = :buyerId " +
           "AND (:startDate IS NULL OR DATE(o.createdAt) >= :startDate) " +
           "AND (:endDate IS NULL OR DATE(o.createdAt) <= :endDate) " +
           "AND (:searchSeller IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :searchSeller, '%'))) " +
           "AND (:searchProduct IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchProduct, '%'))) " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'createdAt_desc' THEN o.createdAt END DESC, " +
           "CASE WHEN :sortBy = 'createdAt_asc' THEN o.createdAt END ASC, " +
           "CASE WHEN :sortBy = 'totalAmount_desc' THEN o.totalAmount END DESC, " +
           "CASE WHEN :sortBy = 'totalAmount_asc' THEN o.totalAmount END ASC, " +
           "CASE WHEN :sortBy = 'status' THEN o.status END ASC, " +
           "o.createdAt DESC")
    List<Order> findByBuyerIdWithFilters(@Param("buyerId") Long buyerId, 
                                         @Param("startDate") String startDate, 
                                         @Param("endDate") String endDate, 
                                         @Param("searchSeller") String searchSeller, 
                                         @Param("searchProduct") String searchProduct, 
                                         @Param("sortBy") String sortBy);

    /**
     * Find orders by status within a date range (inclusive)
     */
    List<Order> findByStatusAndCreatedAtBetween(Order.Status status, LocalDateTime start, LocalDateTime end);
}
