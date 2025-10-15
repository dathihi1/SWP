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
        System.out.println("=== VNPay Callback Received ===");
        System.out.println("All parameters: " + params);
        
        // Verify VNPay signature first
        boolean isValidSignature = vnPayUtil.verifyPayment(params);
        System.out.println("Signature valid: " + isValidSignature);
        
        if (!isValidSignature) {
            System.out.println("Invalid payment signature!");
            model.addAttribute("success", false);
            model.addAttribute("message", "Chữ ký thanh toán không hợp lệ!");
            return "payment-result";
        }
        
        String orderId = params.get("vnp_TxnRef");
        String amountStr = params.get("vnp_Amount");
        String responseCode = params.get("vnp_ResponseCode");
        
        System.out.println("Order ID: " + orderId);
        System.out.println("Amount: " + amountStr);
        System.out.println("Response Code: " + responseCode);
        
        // Check if payment was successful (ResponseCode = "00")
        if (!"00".equals(responseCode)) {
            System.out.println("Payment failed with response code: " + responseCode);
            model.addAttribute("success", false);
            model.addAttribute("message", "Thanh toán thất bại! Mã lỗi: " + responseCode);
            return "payment-result";
        }
        
        Long amount = amountStr != null ? Long.parseLong(amountStr) / 100 : 0L;
        System.out.println("Processing payment: " + amount + " VND for order: " + orderId);
        
        boolean success = paymentService.processPaymentCallback(orderId, amount);
        String resultMessage = success ? "Nạp tiền thành công!" : "Xử lý thanh toán thất bại!";
        
        System.out.println("Payment processing result: " + success);
        
        // Set model attributes for template
        model.addAttribute("success", success);
        model.addAttribute("message", resultMessage);
        if (success) {
            model.addAttribute("amount", amount);
        }
        
        return "payment-result";
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