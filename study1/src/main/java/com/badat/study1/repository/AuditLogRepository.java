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

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
           "AND a.category = 'USER_ACTION' " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:success IS NULL OR a.success = :success) " +
           "AND (:fromDate IS NULL OR a.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findUserViewWithFilters(@Param("userId") Long userId,
                                         @Param("action") String action,
                                         @Param("success") Boolean success,
                                         @Param("fromDate") LocalDateTime fromDate,
                                         @Param("toDate") LocalDateTime toDate,
                                         Pageable pageable);

    // New method to get all user audit logs without category restriction
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:success IS NULL OR a.success = :success) " +
           "AND (:fromDate IS NULL OR a.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findAllUserAuditLogsWithFilters(@Param("userId") Long userId,
                                                  @Param("action") String action,
                                                  @Param("success") Boolean success,
                                                  @Param("fromDate") LocalDateTime fromDate,
                                                  @Param("toDate") LocalDateTime toDate,
                                                  Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:success IS NULL OR a.success = :success) " +
           "AND (:fromDate IS NULL OR a.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findAdminViewWithFilters(@Param("userId") Long userId,
                                            @Param("action") String action,
                                            @Param("success") Boolean success,
                                            @Param("fromDate") LocalDateTime fromDate,
                                            @Param("toDate") LocalDateTime toDate,
                                            Pageable pageable);
    
    // New methods for category-based filtering
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
           "AND a.category = :category " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:success IS NULL OR a.success = :success) " +
           "AND (:fromDate IS NULL OR a.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByUserIdAndCategoryWithFilters(@Param("userId") Long userId,
                                                      @Param("category") AuditLog.Category category,
                                                      @Param("action") String action,
                                                      @Param("success") Boolean success,
                                                      @Param("fromDate") LocalDateTime fromDate,
                                                      @Param("toDate") LocalDateTime toDate,
                                                      Pageable pageable);
    
    // Get all categories for a user
    @Query("SELECT DISTINCT a.category FROM AuditLog a WHERE a.userId = :userId")
    List<AuditLog.Category> findDistinctCategoriesByUserId(@Param("userId") Long userId);
    
    // Get distinct actions and categories for admin filter dropdowns
    @Query("SELECT DISTINCT a.action FROM AuditLog a ORDER BY a.action")
    List<String> findDistinctActions();
    
    @Query("SELECT DISTINCT a.category FROM AuditLog a ORDER BY a.category")
    List<String> findDistinctCategories();
    
    // Admin filter method for all audit logs
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:action IS NULL OR :action = '' OR a.action = :action) AND " +
           "(:category IS NULL OR :category = '' OR a.category = :categoryEnum) AND " +
           "(:success IS NULL OR :success = '' OR a.success = :successBoolean) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findAdminAuditLogsWithFilters(@Param("action") String action,
                                                @Param("category") String category,
                                                @Param("categoryEnum") AuditLog.Category categoryEnum,
                                                @Param("success") String success,
                                                @Param("successBoolean") Boolean successBoolean,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);
    
    // Dashboard statistics methods
    long countByCreatedAtAfter(LocalDateTime dateTime);
    long countByActionAndSuccess(String action, Boolean success);
    long countByCategory(AuditLog.Category category);
}
