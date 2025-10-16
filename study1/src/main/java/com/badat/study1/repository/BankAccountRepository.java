package com.badat.study1.repository;

import com.badat.study1.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    List<BankAccount> findByUserId(Long userId);
    List<BankAccount> findByIsDeleteFalse();
}
