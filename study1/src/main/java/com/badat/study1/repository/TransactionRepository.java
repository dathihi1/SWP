package com.badat.study1.repository;

import com.badat.study1.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Tìm giao dịch theo người mua
    List<Transaction> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    
    // Tìm giao dịch theo người bán
    List<Transaction> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    
    // Tìm giao dịch theo shop
    List<Transaction> findByShopIdOrderByCreatedAtDesc(Long shopId);
    
    // Tìm giao dịch theo stall
    List<Transaction> findByStallIdOrderByCreatedAtDesc(Long stallId);
    
    // Tìm giao dịch theo product
    List<Transaction> findByProductIdOrderByCreatedAtDesc(Long productId);
    
    // Tìm giao dịch theo trạng thái
    List<Transaction> findByStatusOrderByCreatedAtDesc(Transaction.Status status);
    
    // Tìm giao dịch theo mã giao dịch
    Optional<Transaction> findByTransactionCode(String transactionCode);
    
    // Đếm số giao dịch thành công của một seller
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.sellerId = :sellerId AND t.status = 'SUCCESS'")
    long countSuccessfulTransactionsBySeller(@Param("sellerId") Long sellerId);
    
    // Tính tổng doanh thu của một seller
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.sellerId = :sellerId AND t.status = 'SUCCESS'")
    Double calculateTotalRevenueBySeller(@Param("sellerId") Long sellerId);
    
    // Tìm giao dịch đang chờ xử lý
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactions();
    
    // Tìm giao dịch theo khoảng thời gian
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsByDateRange(@Param("startDate") java.time.Instant startDate, 
                                                  @Param("endDate") java.time.Instant endDate);
    
    // Tìm giao dịch của một warehouse item (để tránh bán trùng)
    @Query("SELECT t FROM Transaction t WHERE t.warehouseItemId = :warehouseItemId AND t.status = 'SUCCESS'")
    Optional<Transaction> findSuccessfulTransactionByWarehouseItem(@Param("warehouseItemId") Long warehouseItemId);
    
    // Aggregate transactions between dates (for analytics)
    @Query("SELECT DATE(t.createdAt) as date, SUM(t.amount) as total, COUNT(t.id) as count " +
           "FROM Transaction t " +
           "WHERE t.status = 'SUCCESS' AND DATE(t.createdAt) BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(t.createdAt) " +
           "ORDER BY DATE(t.createdAt)")
    List<Object[]> aggregateBetween(@Param("startDate") java.time.LocalDate startDate, 
                                    @Param("endDate") java.time.LocalDate endDate);
    
    // Aggregate transactions by stall between dates (for stall analytics)
    @Query("SELECT DATE(t.createdAt) as date, SUM(t.amount) as total, COUNT(t.id) as count " +
           "FROM Transaction t " +
           "WHERE t.status = 'SUCCESS' AND DATE(t.createdAt) BETWEEN :startDate AND :endDate " +
           "AND (:stallId IS NULL OR t.stallId = :stallId) " +
           "GROUP BY DATE(t.createdAt) " +
           "ORDER BY DATE(t.createdAt)")
    List<Object[]> aggregateByStallBetween(@Param("startDate") java.time.LocalDate startDate, 
                                          @Param("endDate") java.time.LocalDate endDate,
                                          @Param("stallId") Long stallId);
}