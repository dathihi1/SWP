package com.badat.study1.repository;

import com.badat.study1.model.WithdrawRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Long> {
    List<WithdrawRequest> findByShopId(Long shopId);
    List<WithdrawRequest> findByShopIdAndIsDeleteFalse(Long shopId);
    List<WithdrawRequest> findByStatus(WithdrawRequest.Status status);
    List<WithdrawRequest> findByShopIdOrderByCreatedAtDesc(Long shopId);
    List<WithdrawRequest> findByStatusOrderByCreatedAtDesc(WithdrawRequest.Status status);
    List<WithdrawRequest> findByShopIdAndStatus(Long shopId, WithdrawRequest.Status status);
    
    @Query("SELECT wr FROM WithdrawRequest wr WHERE wr.shopId = :shopId " +
           "AND (:startDate IS NULL OR DATE(wr.createdAt) >= :startDate) " +
           "AND (:endDate IS NULL OR DATE(wr.createdAt) <= :endDate) " +
           "AND (:status IS NULL OR wr.status = :status) " +
           "AND (:minAmount IS NULL OR wr.amount >= CAST(:minAmount AS java.math.BigDecimal)) " +
           "AND (:maxAmount IS NULL OR wr.amount <= CAST(:maxAmount AS java.math.BigDecimal)) " +
           "AND (:bankAccountNumber IS NULL OR wr.bankAccountNumber LIKE CONCAT('%', :bankAccountNumber, '%')) " +
           "AND (:bankName IS NULL OR wr.bankName LIKE CONCAT('%', :bankName, '%')) " +
           "AND (:bankAccountName IS NULL OR wr.bankAccountName LIKE CONCAT('%', :bankAccountName, '%')) " +
           "ORDER BY wr.createdAt DESC")
    List<WithdrawRequest> findByShopIdWithFilters(
            @Param("shopId") Long shopId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("status") WithdrawRequest.Status status,
            @Param("minAmount") String minAmount,
            @Param("maxAmount") String maxAmount,
            @Param("bankAccountNumber") String bankAccountNumber,
            @Param("bankName") String bankName,
            @Param("bankAccountName") String bankAccountName);
}
