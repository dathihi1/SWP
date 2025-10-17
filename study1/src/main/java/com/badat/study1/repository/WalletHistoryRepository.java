package com.badat.study1.repository;

import com.badat.study1.model.WalletHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletHistoryRepository extends JpaRepository<WalletHistory, Long> {
    java.util.Optional<WalletHistory> findFirstByReferenceId(String referenceId);
    List<WalletHistory> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    List<WalletHistory> findByWalletIdAndIsDeleteFalseOrderByCreatedAtDesc(Long walletId);
    java.util.Optional<WalletHistory> findByWalletIdAndReferenceIdAndTypeAndStatus(Long walletId, String referenceId, WalletHistory.Type type, WalletHistory.Status status);
}


