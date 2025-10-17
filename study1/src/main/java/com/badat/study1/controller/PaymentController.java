package com.badat.study1.controller;

import com.badat.study1.dto.request.PaymentRequest;
import com.badat.study1.dto.response.PaymentResponse;
import com.badat.study1.service.PaymentService;
import com.badat.study1.util.VNPayUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class PaymentController {
    
    private final PaymentService paymentService;
    private final VNPayUtil vnPayUtil;
    
    public PaymentController(PaymentService paymentService, VNPayUtil vnPayUtil) {
        this.paymentService = paymentService;
        this.vnPayUtil = vnPayUtil;
    }
    
    @GetMapping("/payment")
    public String paymentPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        model.addAttribute("isAuthenticated", true);
        return "payment";
    }
    
    @PostMapping("/payment/create")
    @ResponseBody
    public PaymentResponse createPayment(@RequestBody PaymentRequest request, HttpServletRequest httpRequest) {
        return paymentService.createPaymentUrl(request, httpRequest);
    }
    
    @GetMapping("/payment/return")
    public String paymentReturn(@RequestParam Map<String, String> params, Model model) {
        try {
        System.out.println("=== VNPay Callback Received ===");
        System.out.println("All parameters: " + params);
        
        // Verify VNPay signature first
        boolean isValidSignature;
        try {
            isValidSignature = vnPayUtil.verifyPayment(params);
        } catch (Exception ex) {
            System.out.println("Error verifying signature: " + ex.getMessage());
            ex.printStackTrace();
            model.addAttribute("success", false);
            model.addAttribute("message", "Không thể xác thực chữ ký thanh toán: " + ex.getMessage());
            model.addAttribute("txnRef", params.get("vnp_TxnRef"));
            model.addAttribute("transactionNo", params.get("vnp_TransactionNo"));
            return "payment-result";
        }
        System.out.println("Signature valid: " + isValidSignature);
        
        if (!isValidSignature) {
            System.out.println("Invalid payment signature!");
            model.addAttribute("success", false);
            model.addAttribute("message", "Chữ ký thanh toán không hợp lệ!");
            model.addAttribute("txnRef", params.get("vnp_TxnRef"));
            model.addAttribute("transactionNo", params.get("vnp_TransactionNo"));
            return "payment-result";
        }
        
        String orderId = params.get("vnp_TxnRef");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String amountStr = params.get("vnp_Amount");
        String responseCode = params.get("vnp_ResponseCode");
        
        System.out.println("Order ID: " + orderId);
        System.out.println("Amount: " + amountStr);
        System.out.println("Response Code: " + responseCode);
        
        // Check if payment was successful (ResponseCode = "00")
        if (!"00".equals(responseCode)) {
            System.out.println("Payment failed with response code: " + responseCode);
            try {
                paymentService.handleFailedPayment(orderId, amountStr != null ? Long.parseLong(amountStr) / 100 : 0L, orderId, vnpTransactionNo, responseCode);
            } catch (Exception logEx) {
                System.out.println("Warn: could not save failed history: " + logEx.getMessage());
            }
            model.addAttribute("success", false);
            model.addAttribute("message", "Thanh toán thất bại! Mã lỗi: " + responseCode);
            model.addAttribute("txnRef", orderId);
            model.addAttribute("transactionNo", vnpTransactionNo);
            return "payment-result";
        }

        try {
            Long amount = amountStr != null ? Long.parseLong(amountStr) / 100 : 0L;
            System.out.println("Processing payment: " + amount + " VND for order: " + orderId);

            boolean success = paymentService.processPaymentCallback(orderId, amount, orderId, vnpTransactionNo);
            String resultMessage = success ? "Nạp tiền thành công!" : "Xử lý thanh toán thất bại!";

            System.out.println("Payment processing result: " + success);

            // Set model attributes for template
            model.addAttribute("success", success);
            model.addAttribute("message", resultMessage);
            if (success) {
                model.addAttribute("amount", amount);
            }
            model.addAttribute("txnRef", orderId);
            model.addAttribute("transactionNo", vnpTransactionNo);

            return "payment-result";
        } catch (Exception ex) {
            System.out.println("Error in paymentReturn: " + ex.getMessage());
            ex.printStackTrace();
            model.addAttribute("success", false);
            model.addAttribute("message", "Đã xảy ra lỗi khi xử lý giao dịch: " + ex.getMessage());
            model.addAttribute("txnRef", orderId);
            model.addAttribute("transactionNo", vnpTransactionNo);
            return "payment-result";
        }
        } catch (Exception fatalEx) {
            System.out.println("Fatal error in paymentReturn: " + fatalEx.getMessage());
            fatalEx.printStackTrace();
            model.addAttribute("success", false);
            model.addAttribute("message", "Lỗi không mong muốn: " + fatalEx.getMessage());
            model.addAttribute("txnRef", params != null ? params.get("vnp_TxnRef") : null);
            model.addAttribute("transactionNo", params != null ? params.get("vnp_TransactionNo") : null);
            return "payment-result";
        }
    }
    
    @GetMapping("/debug-hash")
    @ResponseBody
    public String debugHash(HttpServletRequest request) {
        try {
            // Test với dữ liệu cố định
            String testUrl = vnPayUtil.createPaymentUrl(100000L, "Test payment", "TEST123", request);
            return "<h3>Debug Hash</h3><p><a href='" + testUrl + "' target='_blank'>" + testUrl + "</a></p>";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-callback")
    public String testCallback(Model model) {
        // Test callback với dữ liệu giả
        model.addAttribute("success", true);
        model.addAttribute("message", "Test callback thành công!");
        model.addAttribute("amount", 100000L);
        return "payment-result";
    }
}