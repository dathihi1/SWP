package com.badat.study1.repository;

import com.badat.study1.model.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    
    List<SecurityEvent> findByIpAddressOrderByCreatedAtDesc(String ipAddress);
    
    List<SecurityEvent> findByEmailOrderByCreatedAtDesc(String email);
    
    List<SecurityEvent> findByEventTypeOrderByCreatedAtDesc(SecurityEvent.EventType eventType);
    
    @Query("SELECT se FROM SecurityEvent se WHERE se.ipAddress = :ipAddress AND se.createdAt >= :since ORDER BY se.createdAt DESC")
    List<SecurityEvent> findByIpAddressAndCreatedAtAfterOrderByCreatedAtDesc(
            @Param("ipAddress") String ipAddress, 
            @Param("since") LocalDateTime since);
    
    @Query("SELECT se FROM SecurityEvent se WHERE se.email = :email AND se.createdAt >= :since ORDER BY se.createdAt DESC")
    List<SecurityEvent> findByEmailAndCreatedAtAfterOrderByCreatedAtDesc(
            @Param("email") String email, 
            @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(se) FROM SecurityEvent se WHERE se.ipAddress = :ipAddress AND se.eventType = :eventType AND se.createdAt >= :since")
    Long countByIpAddressAndEventTypeAndCreatedAtAfter(
            @Param("ipAddress") String ipAddress, 
            @Param("eventType") SecurityEvent.EventType eventType,
            @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(se) FROM SecurityEvent se WHERE se.email = :email AND se.eventType = :eventType AND se.createdAt >= :since")
    Long countByEmailAndEventTypeAndCreatedAtAfter(
            @Param("email") String email, 
            @Param("eventType") SecurityEvent.EventType eventType,
            @Param("since") LocalDateTime since);
    
    Page<SecurityEvent> findByEventTypeOrderByCreatedAtDesc(SecurityEvent.EventType eventType, Pageable pageable);
    
    @Query("SELECT se FROM SecurityEvent se WHERE se.createdAt >= :since ORDER BY se.createdAt DESC")
    Page<SecurityEvent> findByCreatedAtAfterOrderByCreatedAtDesc(@Param("since") LocalDateTime since, Pageable pageable);
}

