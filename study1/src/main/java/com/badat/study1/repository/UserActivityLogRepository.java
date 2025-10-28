package com.badat.study1.repository;

import com.badat.study1.model.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    
    // Basic queries
    List<UserActivityLog> findByUserId(Long userId);
    List<UserActivityLog> findByAction(String action);
    List<UserActivityLog> findByCategory(UserActivityLog.Category category);
    
    // Pagination methods
    Page<UserActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<UserActivityLog> findByUserIdAndCategoryOrderByCreatedAtDesc(Long userId, UserActivityLog.Category category, Pageable pageable);
    Page<UserActivityLog> findByUserIdAndActionContainingOrderByCreatedAtDesc(Long userId, String action, Pageable pageable);
    
    // Filter methods with date range
    @Query("SELECT u FROM UserActivityLog u WHERE u.userId = :userId " +
           "AND (:action IS NULL OR :action = '' OR u.action = :action) " +
           "AND (:category IS NULL OR u.category = :category) " +
           "AND (:fromDate IS NULL OR u.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR u.createdAt <= :toDate) " +
           "ORDER BY u.createdAt DESC")
    Page<UserActivityLog> findByUserIdWithFilters(@Param("userId") Long userId,
                                                  @Param("action") String action,
                                                  @Param("category") UserActivityLog.Category category,
                                                  @Param("fromDate") LocalDateTime fromDate,
                                                  @Param("toDate") LocalDateTime toDate,
                                                  Pageable pageable);
    
    // Filter methods with success filter
    @Query("SELECT u FROM UserActivityLog u WHERE u.userId = :userId " +
           "AND (:action IS NULL OR :action = '' OR u.action = :action) " +
           "AND (:success IS NULL OR u.success = :success) " +
           "AND (:fromDate IS NULL OR DATE(u.createdAt) >= :fromDate) " +
           "AND (:toDate IS NULL OR DATE(u.createdAt) <= :toDate) " +
           "ORDER BY u.createdAt DESC")
    Page<UserActivityLog> findUserActivitiesWithFilters(@Param("userId") Long userId,
                                                        @Param("action") String action,
                                                        @Param("success") Boolean success,
                                                        @Param("fromDate") java.time.LocalDate fromDate,
                                                        @Param("toDate") java.time.LocalDate toDate,
                                                        Pageable pageable);
    
    // Admin queries
    @Query("SELECT u FROM UserActivityLog u WHERE " +
           "(:userId IS NULL OR u.userId = :userId) " +
           "AND (:action IS NULL OR :action = '' OR u.action = :action) " +
           "AND (:category IS NULL OR u.category = :category) " +
           "AND (:fromDate IS NULL OR u.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR u.createdAt <= :toDate) " +
           "ORDER BY u.createdAt DESC")
    Page<UserActivityLog> findAdminViewWithFilters(@Param("userId") Long userId,
                                                   @Param("action") String action,
                                                   @Param("category") UserActivityLog.Category category,
                                                   @Param("fromDate") LocalDateTime fromDate,
                                                   @Param("toDate") LocalDateTime toDate,
                                                   Pageable pageable);
    
    // Statistics queries
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime dateTime);
    long countByUserIdAndCategory(Long userId, UserActivityLog.Category category);
    long countByUserIdAndAction(Long userId, String action);
    
    // Get distinct actions for filter dropdowns
    @Query("SELECT DISTINCT u.action FROM UserActivityLog u WHERE u.userId = :userId ORDER BY u.action")
    List<String> findDistinctActionsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT u.category FROM UserActivityLog u WHERE u.userId = :userId ORDER BY u.category")
    List<UserActivityLog.Category> findDistinctCategoriesByUserId(@Param("userId") Long userId);
    
    // Get distinct actions for admin (all users)
    @Query("SELECT DISTINCT u.action FROM UserActivityLog u ORDER BY u.action")
    List<String> findDistinctActions();
    
    @Query("SELECT DISTINCT u.category FROM UserActivityLog u ORDER BY u.category")
    List<UserActivityLog.Category> findDistinctCategories();
    
    // Cleanup old logs
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
    long countByCreatedAtBefore(LocalDateTime dateTime);
}
