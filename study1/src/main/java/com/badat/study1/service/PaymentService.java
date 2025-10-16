package com.badat.study1.service;

import com.badat.study1.dto.request.PaymentRequest;
import com.badat.study1.dto.response.PaymentResponse;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.model.WalletHistory;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.WalletHistoryRepository;
import com.badat.study1.util.VNPayUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@Service
public class PaymentService {
    
    private final VNPayUtil vnPayUtil;
    private final WalletRepository walletRepository;
    private final WalletHistoryService walletHistoryService;
    
    public PaymentService(VNPayUtil vnPayUtil, WalletRepository walletRepository, WalletHistoryRepository walletHistoryRepository, WalletHistoryService walletHistoryService) {
        this.vnPayUtil = vnPayUtil;
        this.walletRepository = walletRepository;
        this.walletHistoryService = walletHistoryService;
    }
    
    public PaymentResponse createPaymentUrl(PaymentRequest request) {
        return createPaymentUrl(request, null);
    }
    
    public PaymentResponse createPaymentUrl(PaymentRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            // Generate unique order ID
            String orderId = "WALLET_" + user.getId() + "_" + System.currentTimeMillis();
            
            // Create payment URL
            String paymentUrl = vnPayUtil.createPaymentUrl(
                request.getAmount(),
                request.getOrderInfo(),
                orderId,
                httpRequest
            );

            // Create PENDING wallet history immediately
            try {
                Wallet wallet = walletRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + user.getId()));
                String description = "Deposit via VNPay - Pending";
                walletHistoryService.saveHistory(
                    wallet.getId(),
                    java.math.BigDecimal.valueOf(request.getAmount()),
                    orderId,
                    null,
                    WalletHistory.Type.DEPOSIT,
                    WalletHistory.Status.PENDING,
                    description
                );
            } catch (Exception historyInitEx) {
                System.out.println("Warning: failed to create pending wallet history: " + historyInitEx.getMessage());
            }
            
            return PaymentResponse.builder()
                    .paymentUrl(paymentUrl)
                    .orderId(orderId)
                    .message("Payment URL created successfully")
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            return PaymentResponse.builder()
                    .message("Error creating payment URL: " + e.getMessage())
                    .success(false)
                    .build();
        }
    }
    
    @Transactional
    public boolean processPaymentCallback(String orderId, Long amount, String vnpTxnRef, String vnpTransactionNo) {
        try {
            // Extract user ID from orderId (format: WALLET_{userId}_{timestamp})
            if (orderId == null || !orderId.startsWith("WALLET_")) {
                System.out.println("Invalid orderId format: " + orderId);
                return false;
            }
            
            String[] parts = orderId.split("_");
            if (parts.length < 2) {
                System.out.println("Invalid orderId format: " + orderId);
                return false;
            }
            
            Long userId = Long.parseLong(parts[1]);
            System.out.println("Processing payment for user ID: " + userId + ", amount: " + amount);
            
            // Find wallet by user ID
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
            
            // Add amount to wallet balance
            BigDecimal currentBalance = wallet.getBalance();
            BigDecimal newBalance = currentBalance.add(BigDecimal.valueOf(amount));
            wallet.setBalance(newBalance);
            
            walletRepository.save(wallet);

            // Save wallet history in a separate transaction to avoid rollback coupling
            try {
                // Update existing PENDING record to SUCCESS
                String description = "Deposit via VNPay - TransactionNo: " + (vnpTransactionNo == null ? "" : vnpTransactionNo);
                walletHistoryService.saveHistory(
                    wallet.getId(),
                    BigDecimal.valueOf(amount),
                    vnpTxnRef,
                    vnpTransactionNo,
                    WalletHistory.Type.DEPOSIT,
                    WalletHistory.Status.SUCCESS,
                    description
                );
            } catch (Exception historyEx) {
                // Log but do not fail the overall deposit processing
                System.out.println("Warning: failed to update wallet history: " + historyEx.getMessage());
                historyEx.printStackTrace();
            }
            
            System.out.println("Payment processed successfully. New balance: " + newBalance);
            return true;
            
        } catch (Exception e) {
            System.out.println("Error processing payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void handleFailedPayment(String orderId, Long amount, String vnpTxnRef, String vnpTransactionNo, String responseCode) {
        try {
            if (orderId == null || !orderId.startsWith("WALLET_")) {
                return; // cannot map to wallet
            }
            String[] parts = orderId.split("_");
            if (parts.length < 2) {
                return;
            }
            Long userId = Long.parseLong(parts[1]);
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet == null) {
                return;
            }
            String desc = "Deposit failed via VNPay - Code: " + responseCode + " - TransactionNo: " + (vnpTransactionNo == null ? "" : vnpTransactionNo);
            walletHistoryService.saveHistory(
                wallet.getId(),
                amount == null ? java.math.BigDecimal.ZERO : java.math.BigDecimal.valueOf(amount),
                vnpTxnRef,
                vnpTransactionNo,
                WalletHistory.Type.DEPOSIT,
                WalletHistory.Status.FAILED,
                desc
            );
        } catch (Exception ex) {
            System.out.println("Warning: failed to record failed payment history: " + ex.getMessage());
        }
    }
}