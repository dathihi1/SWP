package com.badat.study1.repository;

import com.badat.study1.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Tìm tất cả OrderItem theo Order ID
     */
    List<OrderItem> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    /**
     * Tìm tất cả OrderItem theo Order ID với fetch join
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "LEFT JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH oi.warehouse w " +
           "WHERE oi.orderId = :orderId " +
           "ORDER BY oi.createdAt ASC")
    List<OrderItem> findByOrderIdWithDetails(@Param("orderId") Long orderId);

    /**
     * Tìm OrderItem theo Order ID và Product ID
     */
    Optional<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);

    /**
     * Tìm tất cả OrderItem theo Product ID
     */
    List<OrderItem> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Tìm tất cả OrderItem theo Warehouse ID
     */
    List<OrderItem> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    /**
     * Đếm số lượng OrderItem theo Order ID
     */
    long countByOrderId(Long orderId);

    /**
     * Tìm tất cả OrderItem theo trạng thái
     */
    List<OrderItem> findByStatusOrderByCreatedAtDesc(OrderItem.Status status);

    /**
     * Tìm tất cả OrderItem theo Order ID và trạng thái
     */
    List<OrderItem> findByOrderIdAndStatusOrderByCreatedAtAsc(Long orderId, OrderItem.Status status);

    /**
     * Đếm số lượng OrderItem theo Order ID và trạng thái
     */
    long countByOrderIdAndStatus(Long orderId, OrderItem.Status status);

    /**
     * Tìm tất cả OrderItem theo Warehouse User ID
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "LEFT JOIN FETCH oi.warehouse w " +
           "LEFT JOIN FETCH w.user u " +
           "WHERE u.id = :sellerId " +
           "ORDER BY oi.createdAt DESC")
    List<OrderItem> findByWarehouseUserOrderByCreatedAtDesc(@Param("sellerId") Long sellerId);

    /**
     * Đếm số lượng OrderItem theo Warehouse User ID và trạng thái
     */
    @Query("SELECT COUNT(oi) FROM OrderItem oi " +
           "LEFT JOIN oi.warehouse w " +
           "LEFT JOIN w.user u " +
           "WHERE u.id = :sellerId AND oi.status = :status")
    long countByWarehouseUserAndStatus(@Param("sellerId") Long sellerId, @Param("status") OrderItem.Status status);

    /**
     * Tìm OrderItem theo Warehouse ID và trạng thái
     */
    List<OrderItem> findByWarehouseIdAndStatusOrderByCreatedAtDesc(Long warehouseId, OrderItem.Status status);

    /**
     * Find completed OrderItems within a date range (inclusive) ordered by createdAt asc
     */
    List<OrderItem> findByStatusAndCreatedAtBetweenOrderByCreatedAtAsc(OrderItem.Status status, LocalDateTime start, LocalDateTime end);

    /**
     * Find completed OrderItems after a start date (inclusive) ordered by createdAt asc
     */
    List<OrderItem> findByStatusAndCreatedAtAfterOrderByCreatedAtAsc(OrderItem.Status status, LocalDateTime start);
}
