package com.badat.study1.controller;

import com.badat.study1.model.OrderItem;
import com.badat.study1.model.User;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyticsController {

    private final OrderItemRepository orderItemRepository;
    private final StallRepository stallRepository;
    private final ShopRepository shopRepository;

    @GetMapping("/seller/sales")
    public ResponseEntity<Map<String, Object>> sales(@RequestParam(value = "month", required = false) String month,
                                                     Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        LocalDate start;
        LocalDate end;
        if (month != null && month.matches("\\d{4}-\\d{2}")) {
            YearMonth ym = YearMonth.parse(month);
            start = ym.atDay(1);
            end = ym.atEndOfMonth();
        } else {
            end = LocalDate.now();
            start = end.minusDays(29);
        }

        LocalDateTime startDateTime = start.atStartOfDay();
        List<OrderItem> orderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(user.getId());
        
        // Filter order items by date range
        List<OrderItem> orderItemsInRange = orderItems.stream()
                .filter(orderItem -> orderItem.getCreatedAt().isAfter(startDateTime))
                .collect(Collectors.toList());
        
        // Filter completed order items for revenue calculation
        List<OrderItem> completed = orderItemsInRange.stream()
                .filter(orderItem -> orderItem.getStatus() == OrderItem.Status.COMPLETED)
                .collect(Collectors.toList());

        // Prepare labels per day
        List<String> labels = new ArrayList<>();
        List<BigDecimal> totals = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        BigDecimal revenueSum = BigDecimal.ZERO;
        long ordersSum = 0L;

        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            labels.add(day.toString());
            
            // Revenue: chỉ tính từ COMPLETED order items
            BigDecimal dayTotal = completed.stream()
                    .filter(orderItem -> !orderItem.getCreatedAt().toLocalDate().isBefore(day) && !orderItem.getCreatedAt().toLocalDate().isAfter(day))
                    .map(orderItem -> orderItem.getTotalAmount() != null ? orderItem.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Order count: đếm số order_id riêng biệt từ order items
            long dayCount = orderItemsInRange.stream()
                    .filter(orderItem -> !orderItem.getCreatedAt().toLocalDate().isBefore(day) && !orderItem.getCreatedAt().toLocalDate().isAfter(day))
                    .map(OrderItem::getOrderId)
                    .distinct()
                    .count();
            
            totals.add(dayTotal);
            counts.add(dayCount);
            revenueSum = revenueSum.add(dayTotal);
            ordersSum += dayCount;
        }

        // Calculate pending amount (PENDING order items limited by selected date range)
        BigDecimal pendingSum = orderItems.stream()
                .filter(orderItem -> orderItem.getStatus() == OrderItem.Status.PENDING)
                .filter(orderItem -> orderItem.getCreatedAt().isAfter(startDateTime))
                .filter(orderItem -> {
                    LocalDate od = orderItem.getCreatedAt().toLocalDate();
                    return !od.isBefore(start) && !od.isAfter(end);
                })
                .map(orderItem -> orderItem.getTotalAmount() != null ? orderItem.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> body = new HashMap<>();
        body.put("labels", labels);
        body.put("totals", totals);
        body.put("counts", counts);
        body.put("revenueSum", revenueSum);
        body.put("ordersSum", ordersSum);
        body.put("pendingSum", pendingSum);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/seller/stalls")
    public ResponseEntity<List<Map<String, Object>>> stalls(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        // Tìm shop của user hiện tại
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        
        // Chỉ lấy gian hàng của shop hiện tại
        var stalls = stallRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId()).stream()
                .map(stall -> {
                    Map<String, Object> stallData = new HashMap<>();
                    stallData.put("id", stall.getId());
                    stallData.put("name", stall.getStallName());
                    return stallData;
                })
                .toList();
        return ResponseEntity.ok(stalls);
    }

    @GetMapping("/seller/stall-sales")
    public ResponseEntity<Map<String, Object>> stallSales(@RequestParam(value = "month", required = false) String month,
                                                          @RequestParam(value = "stallId", required = false) Long stallId,
                                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        LocalDate start;
        LocalDate end;
        if (month != null && month.matches("\\d{4}-\\d{2}")) {
            YearMonth ym = YearMonth.parse(month);
            start = ym.atDay(1);
            end = ym.atEndOfMonth();
        } else {
            end = LocalDate.now();
            start = end.minusDays(29);
        }

        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyMap());
        }

        // security: validate stall belongs to seller's shop if provided
        if (stallId != null) {
            var stall = stallRepository.findByIdAndShopIdAndIsDeleteFalse(stallId, userShop.get().getId());
            if (stall.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyMap());
            }
        }

        LocalDateTime startDt = start.atStartOfDay();
        List<OrderItem> orderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(user.getId());
        
        // Filter order items by date range and stall
        List<OrderItem> orderItemsInRange = orderItems.stream()
                .filter(orderItem -> orderItem.getCreatedAt().isAfter(startDt))
                .filter(orderItem -> stallId == null || stallId.equals(orderItem.getStallId()))
                .collect(Collectors.toList());
        
        // Filter completed order items for revenue calculation
        List<OrderItem> filtered = orderItemsInRange.stream()
                .filter(orderItem -> orderItem.getStatus() == OrderItem.Status.COMPLETED)
                .collect(Collectors.toList());

        List<String> labels = new ArrayList<>();
        List<BigDecimal> totals = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        BigDecimal revenueSum = BigDecimal.ZERO;
        long ordersSum = 0L;
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            labels.add(day.toString());
            
            // Revenue: chỉ tính từ COMPLETED order items
            BigDecimal dayTotal = filtered.stream()
                    .filter(orderItem -> !orderItem.getCreatedAt().toLocalDate().isBefore(day) && !orderItem.getCreatedAt().toLocalDate().isAfter(day))
                    .map(orderItem -> orderItem.getTotalAmount() != null ? orderItem.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Order count: đếm số order_id riêng biệt từ order items
            long dayCount = orderItemsInRange.stream()
                    .filter(orderItem -> !orderItem.getCreatedAt().toLocalDate().isBefore(day) && !orderItem.getCreatedAt().toLocalDate().isAfter(day))
                    .map(OrderItem::getOrderId)
                    .distinct()
                    .count();
            
            totals.add(dayTotal);
            counts.add(dayCount);
            revenueSum = revenueSum.add(dayTotal);
            ordersSum += dayCount;
        }

        // pending sum for selected stall (or all stalls) limited by selected date range
        BigDecimal pendingSum = orderItems.stream()
                .filter(orderItem -> orderItem.getStatus() == OrderItem.Status.PENDING)
                .filter(orderItem -> orderItem.getCreatedAt().isAfter(startDt))
                .filter(orderItem -> stallId == null || stallId.equals(orderItem.getStallId()))
                .filter(orderItem -> {
                    LocalDate od = orderItem.getCreatedAt().toLocalDate();
                    return !od.isBefore(start) && !od.isAfter(end);
                })
                .map(orderItem -> orderItem.getTotalAmount() != null ? orderItem.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> body = new HashMap<>();
        body.put("labels", labels);
        body.put("totals", totals);
        body.put("counts", counts);
        body.put("revenueSum", revenueSum);
        body.put("ordersSum", ordersSum);
        body.put("pendingSum", pendingSum);
        return ResponseEntity.ok(body);
    }
}


