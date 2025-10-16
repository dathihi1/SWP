package com.badat.study1.repository;

import com.badat.study1.model.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {
    List<PaymentLog> findByUserId(Long userId);
    List<PaymentLog> findByUserIdAndIsDeleteFalse(Long userId);
    List<PaymentLog> findByStatus(PaymentLog.Status status);
}
