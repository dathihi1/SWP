package com.badat.study1.repository;

import com.badat.study1.model.WalletHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletHoldRepository extends JpaRepository<WalletHold, Long> {
    
    List<WalletHold> findByUserIdAndStatus(Long userId, WalletHold.Status status);
    
    List<WalletHold> findByUserIdAndOrderIdAndStatus(Long userId, String orderId, WalletHold.Status status);
    
    List<WalletHold> findByStatusAndExpiresAtBefore(WalletHold.Status status, Instant expiresAt);
    
    Optional<WalletHold> findByOrderId(String orderId);
    
    @Query("SELECT wh FROM WalletHold wh WHERE wh.userId = :userId AND wh.status = :status AND wh.expiresAt > :now")
    List<WalletHold> findActiveHoldsByUser(@Param("userId") Long userId, 
                                          @Param("status") WalletHold.Status status, 
                                          @Param("now") Instant now);
    
    // Methods for counting by status
    long countByStatus(WalletHold.Status status);
    
    // Method for counting expired holds
    long countByStatusAndExpiresAtBefore(WalletHold.Status status, Instant expiresAt);
}
