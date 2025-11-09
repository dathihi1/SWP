package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.repository.WithdrawRequestRepository;
import com.badat.study1.repository.AuditLogRepository;
import com.badat.study1.model.Product;
import com.badat.study1.model.Shop;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.ArrayList;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminViewController {
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/admin")
    public String adminDashboard(Model model, @RequestParam(value = "range", required = false) String range) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                                !authentication.getName().equals("anonymousUser");
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        User user = (User) authentication.getPrincipal();
        if (!user.getRole().name().equals("ADMIN")) {
            return "redirect:/";
        }
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("user", user);

        long totalUsers = userRepository.count();
        // Count ACTIVE + INACTIVE shops (not PENDING)
        List<Shop> activeShops = shopRepository.findByStatusAndIsDeleteOrderByCreatedAtDesc(Shop.Status.ACTIVE, false);
        List<Shop> inactiveShops = shopRepository.findByStatusAndIsDeleteOrderByCreatedAtDesc(Shop.Status.INACTIVE, false);
        long totalStalls = activeShops.size() + inactiveShops.size();
        long pendingWithdrawals = withdrawRequestRepository.findByStatus(com.badat.study1.model.WithdrawRequest.Status.PENDING).size();
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

        String effectiveRange = (range == null || (!"30d".equals(range) && !"7d".equals(range))) ? "7d" : range;
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = "30d".equals(effectiveRange) ? toDate.minusDays(29) : toDate.minusDays(6);
        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(LocalTime.MAX);
        model.addAttribute("range", effectiveRange);

        var recentActivities = auditLogRepository.findTop10ByOrderByCreatedAtDesc();
        model.addAttribute("recentActivities", recentActivities);

        List<LocalDate> dateKeys = new ArrayList<>();
        for (LocalDate d = fromDate; !d.isAfter(toDate); d = d.plusDays(1)) {
            dateKeys.add(d);
        }
        var completedItems = orderItemRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
            com.badat.study1.model.OrderItem.Status.COMPLETED,
            fromDateTime,
            toDateTime
        );
        Map<String, BigDecimal> revenueByDay = new LinkedHashMap<>();
        dateKeys.forEach(d -> revenueByDay.put(d.toString(), BigDecimal.ZERO));
        for (var item : completedItems) {
            LocalDate key = item.getCreatedAt().toLocalDate();
            String k = key.toString();
            BigDecimal current = revenueByDay.getOrDefault(k, BigDecimal.ZERO);
            BigDecimal amount = item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO;
            revenueByDay.put(k, current.add(amount));
        }
        model.addAttribute("revenueSeries", revenueByDay);

        var completedOrders = orderRepository.findByStatusAndCreatedAtBetween(
            com.badat.study1.model.Order.Status.COMPLETED,
            fromDateTime,
            toDateTime
        );
        Map<String, Long> ordersByDay = new LinkedHashMap<>();
        dateKeys.forEach(d -> ordersByDay.put(d.toString(), 0L));
        for (var order : completedOrders) {
            LocalDate key = order.getCreatedAt().toLocalDate();
            String k = key.toString();
            ordersByDay.put(k, ordersByDay.getOrDefault(k, 0L) + 1);
        }
        model.addAttribute("ordersSeries", ordersByDay);

        Map<Long, BigDecimal> revenueBySeller = new java.util.HashMap<>();
        Map<Long, java.util.Set<Long>> ordersBySellerDistinct = new java.util.HashMap<>();
        for (var item : completedItems) {
            Long sellerId = item.getSellerId();
            revenueBySeller.put(sellerId, revenueBySeller.getOrDefault(sellerId, BigDecimal.ZERO)
                .add(item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO));
            ordersBySellerDistinct.computeIfAbsent(sellerId, k -> new java.util.HashSet<>()).add(item.getOrderId());
        }

        Map<Long, BigDecimal> revenueByProduct = new java.util.HashMap<>();
        Map<Long, java.util.Set<Long>> ordersByProductDistinct = new java.util.HashMap<>();
        for (var item : completedItems) {
            Long productId = item.getProductId();
            revenueByProduct.put(productId, revenueByProduct.getOrDefault(productId, BigDecimal.ZERO)
                .add(item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO));
            ordersByProductDistinct.computeIfAbsent(productId, k -> new java.util.HashSet<>()).add(item.getOrderId());
        }
        List<Map<String, Object>> topStalls = revenueByProduct.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .map(e -> {
                Long productId = e.getKey();
                var productOpt = productRepository.findById(productId);
                String productName = productOpt.map(Product::getProductName).orElse("Product " + productId);
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("stallId", productId);
                m.put("stallName", productName);
                m.put("revenue", e.getValue());
                m.put("orders", (long) ordersByProductDistinct.getOrDefault(productId, java.util.Collections.emptySet()).size());
                return m;
            })
            .collect(Collectors.toList());
        model.addAttribute("topStalls", topStalls);

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
        if (!user.getRole().name().equals("ADMIN")) {
            return "redirect:/";
        }
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("user", user);
        // Get shops waiting for approval (PENDING status)
        model.addAttribute("pendingStalls", shopRepository.findByStatusAndIsDeleteOrderByCreatedAtDesc(Shop.Status.PENDING, false));
        // Get approved shops (ACTIVE status)
        model.addAttribute("approvedStalls", shopRepository.findByStatusAndIsDeleteOrderByCreatedAtDesc(Shop.Status.ACTIVE, false));
        model.addAttribute("rejectedStalls", java.util.Collections.emptyList());
        // History: all shops sorted by createdAt (including PENDING, ACTIVE, INACTIVE)
        List<Shop> historyShops = new java.util.ArrayList<>();
        historyShops.addAll(shopRepository.findByStatusAndIsDeleteOrderByCreatedAtDesc(Shop.Status.ACTIVE, false));
        historyShops.addAll(shopRepository.findByStatusAndIsDeleteOrderByCreatedAtDesc(Shop.Status.INACTIVE, false));
        historyShops.addAll(shopRepository.findByStatusAndIsDeleteOrderByCreatedAtDesc(Shop.Status.PENDING, false));
        historyShops.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        model.addAttribute("historyStalls", historyShops);
        
        // Get all ACTIVE and INACTIVE shops (not PENDING) for shop list
        List<Shop> activeShops = shopRepository.findByStatusAndIsDeleteOrderByCreatedAtDesc(Shop.Status.ACTIVE, false);
        List<Shop> inactiveShops = shopRepository.findByStatusAndIsDeleteOrderByCreatedAtDesc(Shop.Status.INACTIVE, false);
        List<Shop> allShops = new java.util.ArrayList<>();
        allShops.addAll(activeShops);
        allShops.addAll(inactiveShops);
        
        // Build shop list with user info and revenue data
        List<Map<String, Object>> shopList = allShops.stream()
            .map(shop -> {
                // Get user for this shop
                User shopUser = userRepository.findById(shop.getUserId()).orElse(null);
                
                // Calculate revenue and orders for this shop's seller
                BigDecimal totalRevenue = BigDecimal.ZERO;
                long totalOrders = 0;
                long completedOrdersCount = 0;
                
                if (shopUser != null) {
                    java.util.List<com.badat.study1.model.OrderItem> completedOrderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(shopUser.getId())
                        .stream()
                        .filter(orderItem -> orderItem.getStatus() == com.badat.study1.model.OrderItem.Status.COMPLETED)
                        .collect(Collectors.toList());
                    totalRevenue = completedOrderItems.stream()
                        .map(orderItem -> orderItem.getTotalAmount() != null ? orderItem.getTotalAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    totalOrders = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(shopUser.getId())
                        .stream()
                        .map(com.badat.study1.model.OrderItem::getOrderId)
                        .distinct()
                        .count();
                    completedOrdersCount = completedOrderItems.stream()
                        .map(com.badat.study1.model.OrderItem::getOrderId)
                        .distinct()
                        .count();
                }
                
                long productsCount = productRepository.countByShopIdAndIsDeleteFalse(shop.getId());
                
                Map<String, Object> shopData = new java.util.HashMap<>();
                shopData.put("shop", shop);
                shopData.put("user", shopUser);
                shopData.put("shopName", shop.getShopName());
                shopData.put("totalRevenue", totalRevenue);
                shopData.put("totalOrders", totalOrders);
                shopData.put("completedOrders", completedOrdersCount);
                shopData.put("stallsCount", productsCount);
                return shopData;
            })
            .collect(Collectors.toList());
        model.addAttribute("shopList", shopList);
        model.addAttribute("sellerRevenue", shopList); // Keep for backward compatibility with template
        
        // Count ACTIVE + INACTIVE shops for total shops display (not PENDING)
        long totalActiveShops = allShops.size();
        model.addAttribute("totalActiveShops", totalActiveShops);
        BigDecimal totalPlatformRevenue = shopList.stream()
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
        if (!user.getRole().name().equals("ADMIN")) {
            return "redirect:/";
        }
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("user", user);
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
        if (!user.getRole().name().equals("ADMIN")) {
            return "redirect:/";
        }
        try {
            Shop shop = shopRepository.findById(stallId).orElse(null);
            if (shop == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy cửa hàng!");
                return "redirect:/admin/stalls";
            }
            // Activate shop
            shop.setStatus(Shop.Status.ACTIVE);
            shop.setUpdatedAt(Instant.now());
            shopRepository.save(shop);
            
            // Add SELLER role to user
            User shopUser = userRepository.findById(shop.getUserId()).orElse(null);
            if (shopUser != null && shopUser.getRole() != User.Role.SELLER) {
                shopUser.setRole(User.Role.SELLER);
                userRepository.save(shopUser);
                log.info("Added SELLER role to user ID: {} after shop approval", shop.getUserId());
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Duyệt cửa hàng thành công! Người dùng đã được cấp quyền SELLER.");
        } catch (Exception e) {
            log.error("Error approving shop: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi duyệt cửa hàng!");
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
        if (!user.getRole().name().equals("ADMIN")) {
            return "redirect:/";
        }
        try {
            Shop shop = shopRepository.findById(stallId).orElse(null);
            if (shop == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy cửa hàng!");
                return "redirect:/admin/stalls";
            }
            // Reject shop: set status to INACTIVE and save rejection reason
            shop.setStatus(Shop.Status.INACTIVE);
            shop.setRejectionReason(reason);
            shop.setUpdatedAt(Instant.now());
            shopRepository.save(shop);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối cửa hàng.");
        } catch (Exception e) {
            log.error("Error rejecting shop: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi từ chối cửa hàng!");
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
            Shop shop = shopRepository.findById(stallId).orElse(null);
            if (shop == null || shop.getCccdFrontImage() == null || shop.getCccdFrontImage().length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            byte[] imageBytes = shop.getCccdFrontImage();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setCacheControl("max-age=3600, must-revalidate");
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/admin/stalls/{id}/image/back")
    public ResponseEntity<byte[]> getStallImageBack(@PathVariable("id") Long stallId) {
        try {
            Shop shop = shopRepository.findById(stallId).orElse(null);
            if (shop == null || shop.getCccdBackImage() == null || shop.getCccdBackImage().length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            byte[] imageBytes = shop.getCccdBackImage();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setCacheControl("max-age=3600, must-revalidate");
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}









