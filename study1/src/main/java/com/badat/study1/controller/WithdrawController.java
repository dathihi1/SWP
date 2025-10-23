package com.badat.study1.controller;

import com.badat.study1.dto.request.WithdrawRequestDto;
import com.badat.study1.dto.response.WithdrawRequestResponse;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.model.WithdrawRequest;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.WithdrawRequestRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.service.WithdrawService;
import com.badat.study1.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WithdrawController {
    
    private final WithdrawService withdrawService;
    private final OtpService otpService;
    private final WalletRepository walletRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final ShopRepository shopRepository;
    
    @GetMapping("/admin-simple")
    public String adminSimplePage() {
        return "admin-simple";
    }
    
    @GetMapping("/admin/test-withdraw")
    @ResponseBody
    public String testWithdraw() {
        try {
            // Create a test withdraw request
            WithdrawRequestDto testRequest = WithdrawRequestDto.builder()
                    .amount(new java.math.BigDecimal("100000"))
                    .bankAccountNumber("1234567890")
                    .bankAccountName("Test User")
                    .bankName("Vietcombank")
                    .note("Test withdraw request")
                    .build();
            
            // Find a shop to use for testing
            var shops = shopRepository.findAll();
            if (shops.isEmpty()) {
                return "No shops found. Please create a shop first.";
            }
            
            var shop = shops.get(0);
            
            // Create withdraw request manually
            WithdrawRequest withdrawRequest = WithdrawRequest.builder()
                    .shopId(shop.getId())
                    .amount(testRequest.getAmount())
                    .bankAccountNumber(testRequest.getBankAccountNumber())
                    .bankAccountName(testRequest.getBankAccountName())
                    .bankName(testRequest.getBankName())
                    .note(testRequest.getNote())
                    .status(WithdrawRequest.Status.PENDING)
                    .build();
            
            withdrawRequest = withdrawRequestRepository.save(withdrawRequest);
            
            return "Test withdraw request created with ID: " + withdrawRequest.getId() + 
                   "<br><a href='/admin-simple'>Go to Admin Simple Page</a>";
                   
        } catch (Exception e) {
            return "Error creating test withdraw request: " + e.getMessage();
        }
    }
    
    @GetMapping("/api/admin/withdraw/requests-simple")
    @ResponseBody
    public ResponseEntity<?> getAllPendingWithdrawRequestsSimple() {
        try {
            // Bypass authentication for simple admin page
            List<WithdrawRequestResponse> requests = withdrawService.getAllPendingWithdrawRequestsSimple();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Error getting pending withdraw requests: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/api/admin/withdraw/approve-simple/{requestId}")
    @ResponseBody
    public ResponseEntity<?> approveWithdrawRequestSimple(@PathVariable Long requestId) {
        try {
            withdrawService.approveWithdrawRequestSimple(requestId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Duyệt yêu cầu rút tiền thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error approving withdraw request: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/api/admin/withdraw/reject-simple/{requestId}")
    @ResponseBody
    public ResponseEntity<?> rejectWithdrawRequestSimple(@PathVariable Long requestId) {
        try {
            withdrawService.rejectWithdrawRequestSimple(requestId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Từ chối yêu cầu rút tiền thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rejecting withdraw request: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        
        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);
        
        return "withdraw";
    }
    
    @PostMapping("/api/withdraw/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendWithdrawOtp(@RequestBody Map<String, Object> request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            String email = user.getEmail();
            String purpose = "Yêu cầu rút tiền";
            
            boolean sent = otpService.sendOtp(email, purpose);
            
            if (sent) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Mã OTP đã được gửi đến email của bạn");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Không thể gửi OTP. Vui lòng thử lại sau");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            log.error("Error sending withdraw OTP: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/api/withdraw/request")
    @ResponseBody
    public ResponseEntity<?> createWithdrawRequest(@RequestBody Map<String, Object> request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            // Verify OTP first
            String otp = (String) request.get("otp");
            String email = user.getEmail();
            String purpose = "Yêu cầu rút tiền";
            
            if (!otpService.verifyOtp(email, purpose, otp)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Mã OTP không hợp lệ hoặc đã hết hạn");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create withdraw request DTO
            WithdrawRequestDto requestDto = WithdrawRequestDto.builder()
                    .amount(new BigDecimal(request.get("amount").toString()))
                    .bankAccountNumber((String) request.get("bankAccountNumber"))
                    .bankAccountName((String) request.get("bankAccountName"))
                    .bankName((String) request.get("bankName"))
                    .note((String) request.get("note"))
                    .build();
            
            WithdrawRequestResponse response = withdrawService.createWithdrawRequest(requestDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating withdraw request: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/api/withdraw/cancel/{requestId}")
    @ResponseBody
    public ResponseEntity<?> cancelWithdrawRequest(@PathVariable Long requestId) {
        try {
            withdrawService.cancelWithdrawRequest(requestId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Hủy yêu cầu rút tiền thành công. Số tiền đã được trả về ví");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error canceling withdraw request: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/api/withdraw/requests")
    @ResponseBody
    public ResponseEntity<?> getWithdrawRequests(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String minAmount,
            @RequestParam(required = false) String maxAmount,
            @RequestParam(required = false) String bankAccountNumber,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String bankAccountName) {
        try {
            List<WithdrawRequestResponse> requests = withdrawService.getWithdrawRequestsByUserWithFilters(
                    startDate, endDate, status, minAmount, maxAmount, 
                    bankAccountNumber, bankName, bankAccountName);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Error getting withdraw requests: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/api/admin/withdraw/approve/{requestId}")
    @ResponseBody
    public ResponseEntity<?> approveWithdrawRequest(@PathVariable Long requestId) {
        try {
            withdrawService.approveWithdrawRequest(requestId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Duyệt yêu cầu rút tiền thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error approving withdraw request: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/api/admin/withdraw/reject/{requestId}")
    @ResponseBody
    public ResponseEntity<?> rejectWithdrawRequest(@PathVariable Long requestId) {
        try {
            withdrawService.rejectWithdrawRequest(requestId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Từ chối yêu cầu rút tiền thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rejecting withdraw request: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/api/admin/withdraw/requests")
    @ResponseBody
    public ResponseEntity<?> getAllWithdrawRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String searchName,
            @RequestParam(required = false) String searchAccount,
            @RequestParam(required = false) String searchBank) {
        try {
            List<WithdrawRequestResponse> requests;
            if (status != null && !status.isEmpty()) {
                com.badat.study1.model.WithdrawRequest.Status requestStatus = 
                    com.badat.study1.model.WithdrawRequest.Status.valueOf(status.toUpperCase());
                requests = withdrawService.getWithdrawRequestsByStatus(requestStatus);
            } else {
                requests = withdrawService.getAllPendingWithdrawRequests();
            }
            
            // Apply additional filters
            requests = withdrawService.filterWithdrawRequests(requests, dateFrom, dateTo, searchName, searchAccount, searchBank);
            
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Error getting withdraw requests: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
}
