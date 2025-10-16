package com.badat.study1.repository;

import com.badat.study1.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByBankAccount(String bankAccount);
}


