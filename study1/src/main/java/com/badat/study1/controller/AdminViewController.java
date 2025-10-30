package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.model.Stall;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.repository.OrderItemRepository;
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
    private final StallRepository stallRepository;
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
        long totalStalls = shopRepository.count();
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
        List<Map<String, Object>> topSellers = revenueBySeller.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .map(e -> {
                Long sellerId = e.getKey();
                var userOpt = userRepository.findById(sellerId);
                String sellerName = userOpt.map(User::getUsername).orElse("Seller " + sellerId);
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("sellerId", sellerId);
                m.put("sellerName", sellerName);
                m.put("revenue", e.getValue());
                m.put("orders", (long) ordersBySellerDistinct.getOrDefault(sellerId, java.util.Collections.emptySet()).size());
                return m;
            })
            .collect(Collectors.toList());
        model.addAttribute("topSellers", topSellers);

        Map<Long, BigDecimal> revenueByStall = new java.util.HashMap<>();
        Map<Long, java.util.Set<Long>> ordersByStallDistinct = new java.util.HashMap<>();
        for (var item : completedItems) {
            Long stallId = item.getStallId();
            revenueByStall.put(stallId, revenueByStall.getOrDefault(stallId, BigDecimal.ZERO)
                .add(item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO));
            ordersByStallDistinct.computeIfAbsent(stallId, k -> new java.util.HashSet<>()).add(item.getOrderId());
        }
        List<Map<String, Object>> topStalls = revenueByStall.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .map(e -> {
                Long stallId = e.getKey();
                var stallOpt = stallRepository.findById(stallId);
                String stallName = stallOpt.map(Stall::getStallName).orElse("Stall " + stallId);
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("stallId", stallId);
                m.put("stallName", stallName);
                m.put("revenue", e.getValue());
                m.put("orders", (long) ordersByStallDistinct.getOrDefault(stallId, java.util.Collections.emptySet()).size());
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
        model.addAttribute("pendingStalls", stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("PENDING"));
        model.addAttribute("approvedStalls", stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("CLOSED"));
        model.addAttribute("rejectedStalls", stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("REJECTED"));
        List<Stall> historyStalls = new java.util.ArrayList<>();
        historyStalls.addAll(stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("CLOSED"));
        historyStalls.addAll(stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("OPEN"));
        historyStalls.addAll(stallRepository.findByStatusAndIsDeleteFalseOrderByCreatedAtDesc("REJECTED"));
        historyStalls.sort((a, b) -> {
            if (a.getApprovedAt() == null && b.getApprovedAt() == null) return 0;
            if (a.getApprovedAt() == null) return 1;
            if (b.getApprovedAt() == null) return -1;
            return b.getApprovedAt().compareTo(a.getApprovedAt());
        });
        model.addAttribute("historyStalls", historyStalls);
        java.util.List<User> sellers = userRepository.findByRole(User.Role.SELLER);
        for (User seller : sellers) {
            if (seller.getStatus() == null) {
                seller.setStatus(User.Status.ACTIVE);
                userRepository.save(seller);
            }
        }
        model.addAttribute("sellers", sellers);
        List<Map<String, Object>> sellerRevenue = sellers.stream()
            .map(seller -> {
                java.util.List<com.badat.study1.model.OrderItem> completedOrderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(seller.getId())
                    .stream()
                    .filter(orderItem -> orderItem.getStatus() == com.badat.study1.model.OrderItem.Status.COMPLETED)
                    .collect(Collectors.toList());
                BigDecimal totalRevenue = completedOrderItems.stream()
                    .map(orderItem -> orderItem.getTotalAmount() != null ? orderItem.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                long totalOrders = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(seller.getId())
                    .stream()
                    .map(com.badat.study1.model.OrderItem::getOrderId)
                    .distinct()
                    .count();
                long completedOrdersCount = completedOrderItems.stream()
                    .map(com.badat.study1.model.OrderItem::getOrderId)
                    .distinct()
                    .count();
                var shop = shopRepository.findByUserId(seller.getId()).orElse(null);
                String shopName = shop != null ? shop.getShopName() : "Chưa có shop";
                long stallsCount = shop != null ? stallRepository.countByShopIdAndIsDeleteFalse(shop.getId()) : 0;
                Map<String, Object> sellerData = new java.util.HashMap<>();
                sellerData.put("seller", seller);
                sellerData.put("shop", shop);
                sellerData.put("shopName", shopName);
                sellerData.put("totalRevenue", totalRevenue);
                sellerData.put("totalOrders", totalOrders);
                sellerData.put("completedOrders", completedOrdersCount);
                sellerData.put("stallsCount", stallsCount);
                return sellerData;
            })
            .collect(Collectors.toList());
        model.addAttribute("sellerRevenue", sellerRevenue);
        List<Map<String, Object>> topSellers = sellerRevenue.stream()
            .sorted((a, b) -> ((BigDecimal) b.get("totalRevenue")).compareTo((BigDecimal) a.get("totalRevenue")))
            .limit(5)
            .collect(Collectors.toList());
        model.addAttribute("topSellers", topSellers);
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
}





