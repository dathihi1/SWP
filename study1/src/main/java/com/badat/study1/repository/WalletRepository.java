package com.badat.study1.repository;

import com.badat.study1.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    java.util.Optional<Wallet> findByUserId(Long userId);
}
