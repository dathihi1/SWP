package com.badat.study1.repository;

import com.badat.study1.model.PaymentQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PaymentQueueRepository extends JpaRepository<PaymentQueue, Long> {
    
    List<PaymentQueue> findByStatusOrderByCreatedAtAsc(PaymentQueue.Status status);
    
    List<PaymentQueue> findByUserIdAndStatus(Long userId, PaymentQueue.Status status);
    
    List<PaymentQueue> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentQueue.Status status);
    
    @Query("SELECT pq FROM PaymentQueue pq WHERE pq.status = :status AND pq.createdAt <= :before ORDER BY pq.createdAt ASC")
    List<PaymentQueue> findPendingPaymentsBefore(@Param("status") PaymentQueue.Status status, 
                                                @Param("before") Instant before);
    
    // Methods for counting by status
    long countByStatus(PaymentQueue.Status status);
    
    // Methods for finding by status and processed time
    List<PaymentQueue> findByStatusAndProcessedAtAfter(PaymentQueue.Status status, Instant processedAt);
}
