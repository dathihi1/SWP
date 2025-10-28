package com.badat.study1;

import com.badat.study1.model.WalletHistory;
import com.badat.study1.repository.WalletHistoryRepository;
import com.badat.study1.service.PaymentService;
import com.badat.study1.service.WalletHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PaymentSpamPreventionTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WalletHistoryService walletHistoryService;

    @Autowired
    private WalletHistoryRepository walletHistoryRepository;

    @Test
    public void testDuplicateTransactionPrevention() {
        // Test data
        String orderId = "WALLET_1_1234567890";
        Long amount = 100000L;
        String vnpTxnRef = orderId;
        String vnpTransactionNo = "VNP123456789";

        // First call should succeed
        boolean firstResult = paymentService.processPaymentCallback(orderId, amount, vnpTxnRef, vnpTransactionNo);
        assertTrue(firstResult, "First payment should succeed");

        // Second call with same transaction number should be prevented
        boolean secondResult = paymentService.processPaymentCallback(orderId, amount, vnpTxnRef, vnpTransactionNo);
        assertTrue(secondResult, "Second payment should return true (already processed) but not actually process");

        // Verify only one successful record exists
        long successCount = walletHistoryRepository.findAll().stream()
            .filter(h -> h.getTransactionNo() != null && h.getTransactionNo().equals(vnpTransactionNo))
            .filter(h -> h.getType() == WalletHistory.Type.DEPOSIT)
            .filter(h -> h.getStatus() == WalletHistory.Status.SUCCESS)
            .count();
        
        assertEquals(1, successCount, "Should have exactly one successful transaction record");
    }

    @Test
    public void testDuplicateOrderIdPrevention() {
        // Test data
        String orderId = "WALLET_2_1234567891";
        Long amount = 50000L;
        String vnpTxnRef = orderId;
        String vnpTransactionNo1 = "VNP111111111";
        String vnpTransactionNo2 = "VNP222222222";

        // First call should succeed
        boolean firstResult = paymentService.processPaymentCallback(orderId, amount, vnpTxnRef, vnpTransactionNo1);
        assertTrue(firstResult, "First payment should succeed");

        // Second call with different transaction number but same orderId should be prevented
        boolean secondResult = paymentService.processPaymentCallback(orderId, amount, vnpTxnRef, vnpTransactionNo2);
        assertTrue(secondResult, "Second payment should return true (already processed) but not actually process");

        // Verify only one successful record exists for this orderId
        long successCount = walletHistoryRepository.findAll().stream()
            .filter(h -> h.getReferenceId() != null && h.getReferenceId().equals(orderId))
            .filter(h -> h.getType() == WalletHistory.Type.DEPOSIT)
            .filter(h -> h.getStatus() == WalletHistory.Status.SUCCESS)
            .count();
        
        assertEquals(1, successCount, "Should have exactly one successful transaction record for this orderId");
    }
}

