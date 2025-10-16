package com.badat.study1.repository;

import com.badat.study1.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query(value = "SELECT DATE(t.created_at) as d, SUM(t.amount) as total, COUNT(*) as cnt " +
            "FROM `transaction` t " +
            "WHERE DATE(t.created_at) BETWEEN :fromDate AND :toDate " +
            "GROUP BY DATE(t.created_at) " +
            "ORDER BY d", nativeQuery = true)
    List<Object[]> aggregateBetween(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}


