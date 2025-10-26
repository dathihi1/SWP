package com.badat.study1.repository;

import com.badat.study1.model.ApiCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long> {
    
    // Basic queries
    List<ApiCallLog> findByUserId(Long userId);
    List<ApiCallLog> findByEndpoint(String endpoint);
    List<ApiCallLog> findByStatusCode(Integer statusCode);
    
    // Pagination methods
    Page<ApiCallLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<ApiCallLog> findByEndpointContainingOrderByCreatedAtDesc(String endpoint, Pageable pageable);
    Page<ApiCallLog> findByStatusCodeGreaterThanEqualOrderByCreatedAtDesc(Integer statusCode, Pageable pageable);
    
    // Filter methods with date range
    @Query("SELECT a FROM ApiCallLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) " +
           "AND (:endpoint IS NULL OR a.endpoint LIKE %:endpoint%) " +
           "AND (:method IS NULL OR a.method = :method) " +
           "AND (:statusCode IS NULL OR a.statusCode = :statusCode) " +
           "AND (:fromDate IS NULL OR a.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC")
    Page<ApiCallLog> findWithFilters(@Param("userId") Long userId,
                                     @Param("endpoint") String endpoint,
                                     @Param("method") String method,
                                     @Param("statusCode") Integer statusCode,
                                     @Param("fromDate") LocalDateTime fromDate,
                                     @Param("toDate") LocalDateTime toDate,
                                     Pageable pageable);
    
    // Statistics queries
    @Query("SELECT AVG(a.durationMs) FROM ApiCallLog a WHERE a.createdAt >= :fromDate")
    Double findAverageResponseTime(@Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT COUNT(a) FROM ApiCallLog a WHERE a.statusCode >= 400 AND a.createdAt >= :fromDate")
    Long countErrorsSince(@Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT COUNT(a) FROM ApiCallLog a WHERE a.createdAt >= :fromDate")
    Long countTotalCallsSince(@Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT a.endpoint, COUNT(a) as callCount, AVG(a.durationMs) as avgDuration " +
           "FROM ApiCallLog a WHERE a.createdAt >= :fromDate " +
           "GROUP BY a.endpoint ORDER BY callCount DESC")
    List<Object[]> findTopEndpoints(@Param("fromDate") LocalDateTime fromDate, Pageable pageable);
    
    @Query("SELECT a FROM ApiCallLog a WHERE a.durationMs > :threshold AND a.createdAt >= :fromDate " +
           "ORDER BY a.durationMs DESC")
    List<ApiCallLog> findSlowCalls(@Param("threshold") Integer threshold, 
                                   @Param("fromDate") LocalDateTime fromDate, 
                                   Pageable pageable);
    
    // Get distinct values for filter dropdowns
    @Query("SELECT DISTINCT a.endpoint FROM ApiCallLog a ORDER BY a.endpoint")
    List<String> findDistinctEndpoints();
    
    @Query("SELECT DISTINCT a.method FROM ApiCallLog a ORDER BY a.method")
    List<String> findDistinctMethods();
    
    @Query("SELECT DISTINCT a.statusCode FROM ApiCallLog a ORDER BY a.statusCode")
    List<Integer> findDistinctStatusCodes();
    
    // Cleanup old logs
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
    long countByCreatedAtBefore(LocalDateTime dateTime);
}
