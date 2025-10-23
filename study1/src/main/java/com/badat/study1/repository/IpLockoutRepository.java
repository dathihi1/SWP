package com.badat.study1.repository;

import com.badat.study1.model.IpLockout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IpLockoutRepository extends JpaRepository<IpLockout, Long> {
    
    Optional<IpLockout> findByIpAddressAndIsActiveTrue(String ipAddress);
    
    List<IpLockout> findByIpAddressOrderByCreatedAtDesc(String ipAddress);
    
    @Query("SELECT il FROM IpLockout il WHERE il.ipAddress = :ipAddress AND il.isActive = true AND (il.lockedUntil IS NULL OR il.lockedUntil > :now)")
    Optional<IpLockout> findActiveLockoutByIpAddress(@Param("ipAddress") String ipAddress, @Param("now") LocalDateTime now);
    
    @Query("SELECT il FROM IpLockout il WHERE il.isActive = true AND il.lockedUntil < :now")
    List<IpLockout> findExpiredLockouts(@Param("now") LocalDateTime now);
    
    @Query("UPDATE IpLockout il SET il.isActive = false, il.unlockedAt = :unlockedAt WHERE il.ipAddress = :ipAddress AND il.isActive = true")
    int deactivateLockoutsByIpAddress(@Param("ipAddress") String ipAddress, @Param("unlockedAt") LocalDateTime unlockedAt);
    
    @Query("SELECT COUNT(il) FROM IpLockout il WHERE il.ipAddress = :ipAddress AND il.createdAt >= :since")
    Long countByIpAddressAndCreatedAtAfter(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
}

