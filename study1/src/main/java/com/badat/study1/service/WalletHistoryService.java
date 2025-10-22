package com.badat.study1.service;

import com.badat.study1.model.WalletHistory;
import com.badat.study1.repository.WalletHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletHistoryService {

	private final WalletHistoryRepository walletHistoryRepository;

	public WalletHistoryService(WalletHistoryRepository walletHistoryRepository) {
		this.walletHistoryRepository = walletHistoryRepository;
	}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveDepositHistory(Long walletId, BigDecimal amount, String vnpTxnRef, String vnpTransactionNo) {
        WalletHistory history = WalletHistory.builder()
            .walletId(walletId)
            .type(WalletHistory.Type.DEPOSIT)
            .amount(amount)
            .referenceId(vnpTxnRef)
            .description("Deposit via VNPay - TransactionNo: " + (vnpTransactionNo == null ? "" : vnpTransactionNo))
            .isDelete(false)
            .createdBy("system")
            .createdAt(java.time.Instant.now())
            .status(WalletHistory.Status.SUCCESS)
            .build();

        try {
            walletHistoryRepository.save(history);
        } catch (Exception ex) {
            System.out.println("WalletHistory save failed. Details:");
            System.out.println("  walletId=" + walletId);
            System.out.println("  amount=" + amount);
            System.out.println("  referenceId=" + vnpTxnRef);
            System.out.println("  transactionNo=" + vnpTransactionNo);
            System.out.println("  message=" + ex.getMessage());
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            System.out.println("  rootCause=" + root.getClass().getName() + ": " + root.getMessage());
            throw ex;
        }
    }

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveHistory(Long walletId,
	                       BigDecimal amount,
	                       String vnpTxnRef,
	                       String vnpTransactionNo,
	                       WalletHistory.Type type,
	                       WalletHistory.Status status,
	                       String description) {
        try {
            // Always create new history record for seller/admin payouts
            // Only reuse existing record for deposit transactions
            WalletHistory history;
            if (type == WalletHistory.Type.DEPOSIT) {
                history = walletHistoryRepository.findFirstByReferenceId(vnpTxnRef)
                    .orElseGet(() -> WalletHistory.builder()
                        .walletId(walletId)
                        .type(type)
                        .amount(amount)
                        .referenceId(vnpTxnRef)
                        .isDelete(false)
                        .createdBy("system")
                        .createdAt(java.time.Instant.now())
                        .build());
                
                history.setDescription(description);
                history.setStatus(status);
                history.setUpdatedAt(java.time.Instant.now());
            } else {
                // For SALE_SUCCESS, COMMISSION, etc. - always create new record
                history = WalletHistory.builder()
                    .walletId(walletId)
                    .type(type)
                    .amount(amount)
                    .referenceId(vnpTxnRef)
                    .description(description)
                    .isDelete(false)
                    .createdBy("system")
                    .createdAt(java.time.Instant.now())
                    .status(status)
                    .build();
            }

            walletHistoryRepository.save(history);
		} catch (Exception ex) {
			System.out.println("WalletHistory save failed. Details:");
			System.out.println("  walletId=" + walletId);
			System.out.println("  amount=" + amount);
			System.out.println("  referenceId=" + vnpTxnRef);
			System.out.println("  transactionNo=" + vnpTransactionNo);
			System.out.println("  status=" + status + ", type=" + type);
			System.out.println("  message=" + ex.getMessage());
			Throwable root = ex;
			while (root.getCause() != null && root.getCause() != root) {
				root = root.getCause();
			}
			System.out.println("  rootCause=" + root.getClass().getName() + ": " + root.getMessage());
			throw ex;
		}
	}

	public List<WalletHistory> getWalletHistoryByWalletId(Long walletId) {
		return walletHistoryRepository.findByWalletIdAndIsDeleteFalseOrderByCreatedAtDesc(walletId);
	}

	/**
	 * Update wallet history status for existing purchase record
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateWalletHistoryStatus(Long walletId, String referenceId, WalletHistory.Type type, WalletHistory.Status newStatus, String description) {
		try {
			// Find existing wallet history by walletId, referenceId, and type
			WalletHistory existingHistory = walletHistoryRepository
				.findByWalletIdAndReferenceIdAndTypeAndStatus(walletId, referenceId, type, WalletHistory.Status.PENDING)
				.orElseThrow(() -> new RuntimeException("Wallet history not found for update"));
			
			// Update status and description
			existingHistory.setStatus(newStatus);
			existingHistory.setDescription(description);
			existingHistory.setUpdatedAt(java.time.Instant.now());
			
			walletHistoryRepository.save(existingHistory);
			
		} catch (Exception ex) {
			System.out.println("WalletHistory update failed. Details:");
			System.out.println("  walletId=" + walletId);
			System.out.println("  referenceId=" + referenceId);
			System.out.println("  type=" + type);
			System.out.println("  newStatus=" + newStatus);
			System.out.println("  message=" + ex.getMessage());
			throw ex;
		}
	}
}


