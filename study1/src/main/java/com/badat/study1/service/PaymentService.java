package com.badat.study1.service;

import com.badat.study1.dto.request.CartPaymentRequest;
import com.badat.study1.dto.request.PaymentRequest;
import com.badat.study1.dto.response.PaymentResponse;
import com.badat.study1.dto.response.PaymentStatusResponse;
import com.badat.study1.model.PaymentQueue;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.model.WalletHistory;
import com.badat.study1.model.Warehouse;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.WarehouseRepository;
import com.badat.study1.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final VNPayUtil vnPayUtil;
    private final WalletRepository walletRepository;
    private final WalletHistoryService walletHistoryService;
    private final PaymentQueueService paymentQueueService;
    private final WarehouseRepository warehouseRepository;
    private final CartService cartService;
    
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
            
            // ANTI-SPAM: Check if this transaction has already been processed successfully
            if (vnpTransactionNo != null && !vnpTransactionNo.trim().isEmpty()) {
                boolean alreadyProcessed = walletHistoryService.existsByTransactionNoAndTypeAndStatus(
                    vnpTransactionNo, WalletHistory.Type.DEPOSIT, WalletHistory.Status.SUCCESS);
                if (alreadyProcessed) {
                    System.out.println("Transaction already processed: " + vnpTransactionNo);
                    return true; // Return true to avoid showing error to user
                }
            }
            
            // ANTI-SPAM: Check if this orderId has already been processed successfully
            boolean orderAlreadyProcessed = walletHistoryService.existsByReferenceIdAndTypeAndStatus(
                orderId, WalletHistory.Type.DEPOSIT, WalletHistory.Status.SUCCESS);
            if (orderAlreadyProcessed) {
                System.out.println("Order already processed: " + orderId);
                return true; // Return true to avoid showing error to user
            }
            
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
    
    /**
     * Xử lý thanh toán giỏ hàng với queue system
     */
    public PaymentResponse processCartPayment(CartPaymentRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            // Sử dụng cart items từ request (chỉ những items được chọn)
            CartService.CartPaymentInfo cartInfo;
            if (request.getCartItems() != null && !request.getCartItems().isEmpty()) {
                log.info("Processing {} selected cart items from request", request.getCartItems().size());
                
                // Sử dụng items được chọn từ request và tìm warehouseId thực tế
                BigDecimal totalAmount = BigDecimal.ZERO;
                List<Map<String, Object>> processedCartItems = new ArrayList<>();
                
                for (Map<String, Object> item : request.getCartItems()) {
                    BigDecimal price = new BigDecimal(item.get("price").toString());
                    Integer quantity = Integer.valueOf(item.get("quantity").toString());
                    totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));
                    
                    // Tìm warehouseId thực tế cho product
                    Long productId = Long.valueOf(item.get("productId").toString());
                    Long actualWarehouseId = productId; // Default fallback
                    
                    try {
                        // Tìm warehouse đầu tiên có sẵn cho product này
                        Optional<Warehouse> warehouse = warehouseRepository.findFirstByProductIdAndLockedFalseAndIsDeleteFalse(productId);
                        if (warehouse.isPresent()) {
                            actualWarehouseId = warehouse.get().getId();
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get warehouse for product {}, using productId as fallback", productId);
                    }
                    
                    // Tạo cart item với warehouseId thực tế
                    Map<String, Object> processedItem = new HashMap<>(item);
                    processedItem.put("warehouseId", actualWarehouseId);
                    processedCartItems.add(processedItem);
                }
                
                cartInfo = CartService.CartPaymentInfo.builder()
                    .cartId(0L) // Không cần cartId cho payment
                    .totalItems(processedCartItems.size())
                    .totalAmount(totalAmount)
                    .cartItems(processedCartItems)
                    .build();
            } else {
                // Fallback: lấy tất cả cart items nếu request không có items
                cartInfo = cartService.getCartPaymentInfo();
            }
            
            // Validate cart items
            if (cartInfo.getCartItems() == null || cartInfo.getCartItems().isEmpty()) {
                return PaymentResponse.builder()
                    .message("Cart is empty")
                    .success(false)
                    .build();
            }
            
            // Validate total amount
            if (cartInfo.getTotalAmount() == null || cartInfo.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return PaymentResponse.builder()
                    .message("Invalid total amount")
                    .success(false)
                    .build();
            }
            
            // Fast-fail: Check wallet balance BEFORE enqueueing payment
            Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + user.getId()));
            if (wallet.getBalance() == null || wallet.getBalance().compareTo(cartInfo.getTotalAmount()) < 0) {
                return PaymentResponse.builder()
                    .message("Số dư ví không đủ để thanh toán")
                    .success(false)
                    .build();
            }
            
            // Fast-fail: Check stock availability BEFORE enqueueing payment
            List<String> outOfStockProducts = checkStockAvailability(cartInfo.getCartItems());
            if (!outOfStockProducts.isEmpty()) {
                return PaymentResponse.builder()
                    .message("Sản phẩm hết hàng: " + String.join(", ", outOfStockProducts))
                    .success(false)
                    .build();
            }
            
            // Enqueue payment for processing
            Long paymentId = paymentQueueService.enqueuePayment(
                user.getId(), 
                cartInfo.getCartItems(), 
                cartInfo.getTotalAmount()
            );
            
            return PaymentResponse.builder()
                .message("Payment queued for processing")
                .success(true)
                .paymentId(paymentId)
                .build();
                
        } catch (Exception e) {
            return PaymentResponse.builder()
                .message("Error processing cart payment: " + e.getMessage())
                .success(false)
                .build();
        }
    }
    
    /**
     * Lấy trạng thái payment
     */
    public PaymentStatusResponse getPaymentStatus(Long paymentId) {
        try {
            PaymentQueue payment = paymentQueueService.getPaymentStatus(paymentId);
            return PaymentStatusResponse.fromEntity(payment);
        } catch (Exception e) {
            return PaymentStatusResponse.builder()
                .message("Error retrieving payment status: " + e.getMessage())
                .success(false)
                .build();
        }
    }
    
    /**
     * Kiểm tra tình trạng tồn kho cho các sản phẩm trong giỏ hàng
     */
    private List<String> checkStockAvailability(List<Map<String, Object>> cartItems) {
        List<String> outOfStockProducts = new ArrayList<>();
        
        for (Map<String, Object> cartItem : cartItems) {
            try {
                Long productId = Long.valueOf(cartItem.get("productId").toString());
                Integer requestedQuantity = Integer.valueOf(cartItem.get("quantity").toString());
                String productName = cartItem.get("name").toString();
                
                // Đếm số lượng warehouse items có sẵn cho sản phẩm này
                long availableStock = warehouseRepository.countByProductIdAndLockedFalseAndIsDeleteFalse(productId);
                
                if (availableStock < requestedQuantity) {
                    outOfStockProducts.add(productName + " (cần " + requestedQuantity + ", còn " + availableStock + ")");
                }
                
            } catch (Exception e) {
                outOfStockProducts.add("Lỗi kiểm tra tồn kho cho sản phẩm: " + cartItem.get("name"));
            }
        }
        
        return outOfStockProducts;
    }
}