package com.badat.study1.controller;

import com.badat.study1.dto.request.WithdrawRequestDto;
import com.badat.study1.dto.response.WithdrawRequestResponse;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.service.WithdrawService;
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
    private final WalletRepository walletRepository;
    
    @GetMapping("/admin-simple")
    public String adminSimplePage() {
        return "admin-simple";
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
    
    @PostMapping("/api/withdraw/request")
    @ResponseBody
    public ResponseEntity<?> createWithdrawRequest(@RequestBody WithdrawRequestDto requestDto) {
        try {
            WithdrawRequestResponse response = withdrawService.createWithdrawRequest(requestDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating withdraw request: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/api/withdraw/requests")
    @ResponseBody
    public ResponseEntity<?> getWithdrawRequests() {
        try {
            List<WithdrawRequestResponse> requests = withdrawService.getWithdrawRequestsByUser();
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
    public ResponseEntity<?> getAllPendingWithdrawRequests() {
        try {
            List<WithdrawRequestResponse> requests = withdrawService.getAllPendingWithdrawRequests();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Error getting pending withdraw requests: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/admin/withdraw-requests")
    public String adminWithdrawRequestsPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        // Add common attributes
        model.addAttribute("isAuthenticated", true);
        
        return "admin/withdraw-requests";
    }
}
