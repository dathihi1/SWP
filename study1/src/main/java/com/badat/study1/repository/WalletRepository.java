package com.badat.study1.repository;

import com.badat.study1.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByUserIdAndIsDeleteFalse(Long userId);
    List<Wallet> findByIsDeleteFalse();
}
