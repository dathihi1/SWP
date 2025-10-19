package com.badat.study1.controller;

import com.badat.study1.dto.request.CartPaymentRequest;
import com.badat.study1.dto.response.PaymentResponse;
import com.badat.study1.dto.response.PaymentStatusResponse;
import com.badat.study1.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/improved-payment")
@RequiredArgsConstructor
public class ImprovedPaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * Xử lý thanh toán giỏ hàng với queue system
     */
    @PostMapping("/process-cart")
    public ResponseEntity<PaymentResponse> processCartPayment(@RequestBody CartPaymentRequest request) {
        try {
            log.info("Processing cart payment for amount: {}", request.getTotalAmount());
            
            PaymentResponse response = paymentService.processCartPayment(request);
            
            if (response.isSuccess()) {
                log.info("Cart payment queued successfully");
                return ResponseEntity.ok(response);
            } else {
                log.warn("Cart payment failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error processing cart payment", e);
            return ResponseEntity.internalServerError()
                .body(PaymentResponse.builder()
                    .message("Internal server error: " + e.getMessage())
                    .success(false)
                    .build());
        }
    }
    
    /**
     * Lấy trạng thái payment
     */
    @GetMapping("/status/{paymentId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable Long paymentId) {
        try {
            log.info("Getting payment status for ID: {}", paymentId);
            
            PaymentStatusResponse response = paymentService.getPaymentStatus(paymentId);
            
            if (response.isSuccess()) {
                log.info("Payment status retrieved successfully");
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to get payment status: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error getting payment status", e);
            return ResponseEntity.internalServerError()
                .body(PaymentStatusResponse.builder()
                    .message("Internal server error: " + e.getMessage())
                    .success(false)
                    .build());
        }
    }
}
