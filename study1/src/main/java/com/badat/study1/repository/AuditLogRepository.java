package com.badat.study1.repository;

import com.badat.study1.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByIsDeleteFalse();
    
    // Pagination methods
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<AuditLog> findByUserIdAndActionOrderByCreatedAtDesc(Long userId, String action, Pageable pageable);
    Page<AuditLog> findByUserIdAndSuccessOrderByCreatedAtDesc(Long userId, Boolean success, Pageable pageable);
    
    // Filter methods with date range
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:success IS NULL OR a.success = :success) " +
           "AND (:fromDate IS NULL OR a.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByUserIdWithFilters(@Param("userId") Long userId,
                                         @Param("action") String action,
                                         @Param("success") Boolean success,
                                         @Param("fromDate") LocalDateTime fromDate,
                                         @Param("toDate") LocalDateTime toDate,
                                         Pageable pageable);
}
