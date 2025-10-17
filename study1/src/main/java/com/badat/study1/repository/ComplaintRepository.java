package com.badat.study1.repository;

import com.badat.study1.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByBuyerId(Long buyerId);
    List<Complaint> findByBuyerIdAndIsDeleteFalse(Long buyerId);
    List<Complaint> findByTransactionId(Long transactionId);
    List<Complaint> findByStatus(Complaint.Status status);
}
