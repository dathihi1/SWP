package com.badat.study1.service;

import com.badat.study1.dto.request.PaymentRequest;
import com.badat.study1.dto.response.PaymentResponse;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.WalletRepository;
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
    
    public PaymentService(VNPayUtil vnPayUtil, WalletRepository walletRepository) {
        this.vnPayUtil = vnPayUtil;
        this.walletRepository = walletRepository;
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
    public boolean processPaymentCallback(String orderId, Long amount) {
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
            
            System.out.println("Payment processed successfully. New balance: " + newBalance);
            return true;
            
        } catch (Exception e) {
            System.out.println("Error processing payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}