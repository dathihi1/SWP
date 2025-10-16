package com.badat.study1.repository;

import com.badat.study1.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByBuyerId(Long buyerId);
    List<Transaction> findByBuyerIdAndIsDeleteFalse(Long buyerId);
    List<Transaction> findByProductId(Long productId);
    List<Transaction> findByProductIdAndIsDeleteFalse(Long productId);
    List<Transaction> findByStatus(Transaction.Status status);
}
