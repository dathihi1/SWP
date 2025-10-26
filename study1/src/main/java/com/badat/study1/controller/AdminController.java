package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.model.Stall;
import com.badat.study1.model.Order;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.repository.WithdrawRequestRepository;
import com.badat.study1.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {
    
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final StallRepository stallRepository;
    private final OrderRepository orderRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final AuditLogRepository auditLogRepository;
    
    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        
        // Check if user is admin
        if (!user.getRole().name().equals("ADMIN")) {
            return "redirect:/";
        }
        
        // Add common attributes
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("user", user);
        
        // Get dashboard statistics
        long totalUsers = userRepository.count();
        long totalStalls = shopRepository.count();
        long pendingWithdrawals = withdrawRequestRepository.findByStatus(com.badat.study1.model.WithdrawRequest.Status.PENDING).size();
        
        // Get audit log statistics
        long totalAuditLogs = auditLogRepository.count();
        long todayAuditLogs = auditLogRepository.countByCreatedAtAfter(java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0));
        long failedLogins = auditLogRepository.countByActionAndSuccess("LOGIN", false);
        long securityEvents = auditLogRepository.countByCategory(com.badat.study1.model.AuditLog.Category.SECURITY_EVENT);
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStalls", totalStalls);
        model.addAttribute("pendingWithdrawals", pendingWithdrawals);
        model.addAttribute("totalAuditLogs", totalAuditLogs);
        model.addAttribute("todayAuditLogs", todayAuditLogs);
        model.addAttribute("failedLogins", failedLogins);
        model.addAttribute("securityEvents", securityEvents);
        
        return "admin/dashboard";
    }
    
    @GetMapping("/admin/stalls")
    public String adminStalls(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        
        // Check if user is admin
        if (!user.getRole().name().equals("ADMIN")) {
            return "redirect:/";
        }
        
        // Add common attributes
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("user", user);
        
        // Get all stalls with different statuses
        model.addAttribute("pendingStalls", stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("PENDING"));
        model.addAttribute("approvedStalls", stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("CLOSED"));
        model.addAttribute("rejectedStalls", stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("REJECTED"));
        
        // Combine approved and rejected stalls for history
        List<Stall> historyStalls = new java.util.ArrayList<>();
        historyStalls.addAll(stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("CLOSED"));
        historyStalls.addAll(stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("OPEN"));
        historyStalls.addAll(stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("REJECTED"));
        // Sort by approval date (most recent first)
        historyStalls.sort((a, b) -> {
            if (a.getApprovedAt() == null && b.getApprovedAt() == null) return 0;
            if (a.getApprovedAt() == null) return 1;
            if (b.getApprovedAt() == null) return -1;
            return b.getApprovedAt().compareTo(a.getApprovedAt());
        });
        model.addAttribute("historyStalls", historyStalls);
        
        // Get all sellers (users with SELLER role)
        List<User> sellers = userRepository.findByRole(User.Role.SELLER);
        
        // Đảm bảo tất cả sellers có status được set
        for (User seller : sellers) {
            if (seller.getStatus() == null) {
                seller.setStatus(User.Status.ACTIVE);
                userRepository.save(seller);
            }
        }
        
        model.addAttribute("sellers", sellers);
        
        // Calculate revenue for each seller
        List<Map<String, Object>> sellerRevenue = sellers.stream()
            .map(seller -> {
                // Get all completed orders for this seller
                List<Order> completedOrders = orderRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId())
                    .stream()
                    .filter(order -> order.getStatus() == Order.Status.COMPLETED)
                    .collect(Collectors.toList());
                
                // Calculate total revenue
                BigDecimal totalRevenue = completedOrders.stream()
                    .map(order -> order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                // Count total orders (all statuses)
                long totalOrders = orderRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId()).size();
                
                // Count completed orders
                long completedOrdersCount = completedOrders.size();
                
                // Get seller's shop info
                var shop = shopRepository.findByUserId(seller.getId()).orElse(null);
                String shopName = shop != null ? shop.getShopName() : "Chưa có shop";
                
                // Get seller's stalls count
                long stallsCount = shop != null ? 
                    stallRepository.countByShopIdAndIsDeleteFalse(shop.getId()) : 0;
                
                Map<String, Object> sellerData = new java.util.HashMap<>();
                sellerData.put("seller", seller);
                sellerData.put("shop", shop); // Add shop object to access shop.createdAt
                sellerData.put("shopName", shopName);
                sellerData.put("totalRevenue", totalRevenue);
                sellerData.put("totalOrders", totalOrders);
                sellerData.put("completedOrders", completedOrdersCount);
                sellerData.put("stallsCount", stallsCount);
                
                return sellerData;
            })
            .collect(Collectors.toList());
        
        model.addAttribute("sellerRevenue", sellerRevenue);
        
        // Get top 5 sellers by revenue
        List<Map<String, Object>> topSellers = sellerRevenue.stream()
            .sorted((a, b) -> ((BigDecimal) b.get("totalRevenue")).compareTo((BigDecimal) a.get("totalRevenue")))
            .limit(5)
            .collect(Collectors.toList());
        
        model.addAttribute("topSellers", topSellers);
        
        // Calculate total platform revenue
        BigDecimal totalPlatformRevenue = sellerRevenue.stream()
            .map(data -> (BigDecimal) data.get("totalRevenue"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        model.addAttribute("totalPlatformRevenue", totalPlatformRevenue);
        
        return "admin/stalls";
    }
    
    @GetMapping("/admin/withdraw-requests")
    public String adminWithdrawRequests(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        
        // Check if user is admin
        if (!user.getRole().name().equals("ADMIN")) {
            return "redirect:/";
        }
        
        // Add common attributes
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("user", user);
        
        // Get all withdraw requests (will be filtered by status in frontend)
        model.addAttribute("withdrawRequests", withdrawRequestRepository.findAll());
        
        return "admin/withdraw-requests";
    }
    
    @PostMapping("/admin/stalls/approve")
    public String approveStall(@RequestParam Long stallId, 
                              @RequestParam(required = false) String reason,
                              @RequestParam(required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        
        // Check if user is admin
        if (!user.getRole().name().equals("ADMIN")) {
            return "redirect:/";
        }
        
        try {
            Stall stall = stallRepository.findById(stallId).orElse(null);
            if (stall == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy gian hàng!");
                return "redirect:/admin/stalls";
            }
            
            stall.setStatus("CLOSED");
            stall.setActive(false);
            stall.setApprovedAt(Instant.now());
            stall.setApprovedBy(user.getId());
            stall.setApprovalReason(reason);
            
            stallRepository.save(stall);
            
            redirectAttributes.addFlashAttribute("successMessage", "Gian hàng đã được duyệt thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi duyệt gian hàng!");
        }
        
        if ("shops".equals(redirect)) {
            return "redirect:/admin/stalls?redirect=shops";
        } else if ("pending".equals(redirect)) {
            return "redirect:/admin/stalls?redirect=pending";
        }
        return "redirect:/admin/stalls";
    }
    
    @PostMapping("/admin/stalls/reject")
    public String rejectStall(@RequestParam Long stallId, 
                             @RequestParam String reason,
                             @RequestParam(required = false) String redirect,
                             RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        
        // Check if user is admin
        if (!user.getRole().name().equals("ADMIN")) {
            return "redirect:/";
        }
        
        try {
            Stall stall = stallRepository.findById(stallId).orElse(null);
            if (stall == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy gian hàng!");
                return "redirect:/admin/stalls";
            }
            
            stall.setStatus("REJECTED");
            stall.setActive(false);
            stall.setApprovedAt(Instant.now());
            stall.setApprovedBy(user.getId());
            stall.setApprovalReason(reason);
            
            stallRepository.save(stall);
            
            redirectAttributes.addFlashAttribute("successMessage", "Gian hàng đã bị từ chối!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi từ chối gian hàng!");
        }
        
        if ("shops".equals(redirect)) {
            return "redirect:/admin/stalls?redirect=shops";
        } else if ("pending".equals(redirect)) {
            return "redirect:/admin/stalls?redirect=pending";
        }
        return "redirect:/admin/stalls";
    }
    
    @GetMapping("/admin/stalls/{id}/image")
    public ResponseEntity<byte[]> getStallImage(@PathVariable("id") Long stallId) {
        try {
            Stall stall = stallRepository.findById(stallId).orElse(null);
            if (stall == null || stall.getStallImageData() == null || stall.getStallImageData().length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            byte[] imageBytes = stall.getStallImageData();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setCacheControl("max-age=3600, must-revalidate");

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/api/admin/sellers/{sellerId}/toggle-lock")
    public ResponseEntity<?> toggleSellerLock(@PathVariable Long sellerId) {
        try {
            // Find shop by sellerId (userId)
            var shop = shopRepository.findByUserId(sellerId);
            if (shop.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy cửa hàng"));
            }
            
            var shopEntity = shop.get();
            var currentStatus = shopEntity.getStatus();
            var newStatus = currentStatus == com.badat.study1.model.Shop.Status.ACTIVE 
                ? com.badat.study1.model.Shop.Status.INACTIVE 
                : com.badat.study1.model.Shop.Status.ACTIVE;
            
            // Update shop status
            shopEntity.setStatus(newStatus);
            shopEntity.setUpdatedAt(Instant.now());
            shopRepository.save(shopEntity);
            
            // Update all stalls of this shop
            List<Stall> stalls = stallRepository.findByShopId(shopEntity.getId());
            boolean newActiveStatus = newStatus == com.badat.study1.model.Shop.Status.ACTIVE;
            
            for (Stall stall : stalls) {
                stall.setActive(newActiveStatus);
                // If shop is being locked (inactive), close all stalls
                if (newStatus == com.badat.study1.model.Shop.Status.INACTIVE) {
                    stall.setStatus("CLOSED");
                } else {
                    // If shop is being unlocked (active), keep current status or set to OPEN
                    if ("CLOSED".equals(stall.getStatus())) {
                        stall.setStatus("OPEN");
                    }
                }
                stallRepository.save(stall);
            }
            
            String action = newStatus == com.badat.study1.model.Shop.Status.INACTIVE ? "khóa" : "mở khóa";
            String stallAction = newStatus == com.badat.study1.model.Shop.Status.INACTIVE ? "đóng" : "mở";
            String message = String.format("Đã %s cửa hàng và %s %d gian hàng thành công!", action, stallAction, stalls.size());
            
            log.info("Admin {} {} shop {} and {} stalls", 
                SecurityContextHolder.getContext().getAuthentication().getName(),
                action, shopEntity.getId(), stalls.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message,
                "shopStatus", newStatus.name(),
                "stallsUpdated", stalls.size()
            ));
            
        } catch (Exception e) {
            log.error("Error toggling seller lock for sellerId {}: {}", sellerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Có lỗi xảy ra khi thực hiện thao tác"));
        }
    }
    
}
