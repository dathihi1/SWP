package com.badat.study1.repository;

import com.badat.study1.model.WalletHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletHistoryRepository extends JpaRepository<WalletHistory, Long> {
    List<WalletHistory> findByWalletId(Long walletId);
    List<WalletHistory> findByWalletIdAndIsDeleteFalse(Long walletId);
    List<WalletHistory> findByType(WalletHistory.Type type);
    List<WalletHistory> findByReferenceId(Long referenceId);
}
