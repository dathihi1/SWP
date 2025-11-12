package com.badat.study1.controller;

import com.badat.study1.model.OrderItem;
import com.badat.study1.model.User;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.ProductRepository;
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
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;

    @GetMapping("/seller/sales")
    public ResponseEntity<Map<String, Object>> sales(@RequestParam(value = "month", required = false) String month,
                                                     Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        LocalDateRange range = resolveRange(month);
        LocalDate start = range.start();
        LocalDate end = range.end();
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay().minusNanos(1);

        List<OrderItem> orderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(user.getId());

        List<OrderItem> orderItemsInRange = orderItems.stream()
                .filter(orderItem -> orderItem != null && orderItem.getCreatedAt() != null)
                .filter(orderItem -> isWithinRange(orderItem.getCreatedAt(), startDateTime, endDateTime))
                .collect(Collectors.toList());

        if (orderItemsInRange.isEmpty()) {
            return ResponseEntity.ok(buildEmptyResponse(start, end));
        }

        List<OrderItem> completed = orderItemsInRange.stream()
                .filter(orderItem -> orderItem.getStatus() == OrderItem.Status.COMPLETED)
                .collect(Collectors.toList());

        Map<String, Object> body = buildSalesResponse(start, end, orderItemsInRange, completed);
        BigDecimal pendingSum = orderItemsInRange.stream()
                .filter(orderItem -> orderItem.getStatus() == OrderItem.Status.PENDING)
                .map(orderItem -> orderItem.getTotalAmount() != null ? orderItem.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
        
        // Chỉ lấy sản phẩm của shop hiện tại
        var products = productRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId()).stream()
                .map(product -> {
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("id", product.getId());
                    productData.put("name", product.getProductName());
                    return productData;
                })
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/seller/stall-sales")
    public ResponseEntity<Map<String, Object>> stallSales(@RequestParam(value = "month", required = false) String month,
                                                          @RequestParam(value = "stallId", required = false) Long stallId,
                                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        LocalDateRange range = resolveRange(month);
        LocalDate start = range.start();
        LocalDate end = range.end();
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay().minusNanos(1);

        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return ResponseEntity.ok(buildEmptyResponse(start, end));
        }

        if (stallId != null) {
            var product = productRepository.findByIdAndShopIdAndIsDeleteFalse(stallId, userShop.get().getId());
            if (product.isEmpty()) {
                return ResponseEntity.ok(buildEmptyResponse(start, end));
            }
        }

        List<OrderItem> orderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(user.getId());

        List<OrderItem> orderItemsInRange = orderItems.stream()
                .filter(orderItem -> orderItem != null && orderItem.getCreatedAt() != null)
                .filter(orderItem -> stallId == null || stallId.equals(orderItem.getProductId()))
                .filter(orderItem -> isWithinRange(orderItem.getCreatedAt(), startDateTime, endDateTime))
                .collect(Collectors.toList());

        if (orderItemsInRange.isEmpty()) {
            return ResponseEntity.ok(buildEmptyResponse(start, end));
        }

        List<OrderItem> completed = orderItemsInRange.stream()
                .filter(orderItem -> orderItem.getStatus() == OrderItem.Status.COMPLETED)
                .collect(Collectors.toList());

        Map<String, Object> body = buildSalesResponse(start, end, orderItemsInRange, completed);

        BigDecimal pendingSum = orderItemsInRange.stream()
                .filter(orderItem -> orderItem.getStatus() == OrderItem.Status.PENDING)
                .map(orderItem -> orderItem.getTotalAmount() != null ? orderItem.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        body.put("pendingSum", pendingSum);

        return ResponseEntity.ok(body);
    }

    private boolean isWithinRange(LocalDateTime createdAt, LocalDateTime start, LocalDateTime end) {
        return (createdAt.isAfter(start) || createdAt.isEqual(start))
                && (createdAt.isBefore(end) || createdAt.isEqual(end));
    }

    private LocalDateRange resolveRange(String month) {
        if (month != null && month.matches("\\d{4}-\\d{2}")) {
            YearMonth ym = YearMonth.parse(month);
            return new LocalDateRange(ym.atDay(1), ym.atEndOfMonth());
        }
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);
        return new LocalDateRange(start, end);
    }

    private Map<String, Object> buildSalesResponse(LocalDate start,
                                                   LocalDate end,
                                                   List<OrderItem> orderItemsInRange,
                                                   List<OrderItem> completedItems) {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> totals = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        BigDecimal revenueSum = BigDecimal.ZERO;
        long ordersSum = 0L;

        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            labels.add(day.toString());

            BigDecimal dayTotal = completedItems.stream()
                    .filter(orderItem -> isSameDay(orderItem.getCreatedAt(), day))
                    .map(orderItem -> orderItem.getTotalAmount() != null ? orderItem.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long dayCount = orderItemsInRange.stream()
                    .filter(orderItem -> isSameDay(orderItem.getCreatedAt(), day))
                    .map(OrderItem::getOrderId)
                    .distinct()
                    .count();

            totals.add(dayTotal);
            counts.add(dayCount);
            revenueSum = revenueSum.add(dayTotal);
            ordersSum += dayCount;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("labels", labels);
        body.put("totals", totals);
        body.put("counts", counts);
        body.put("revenueSum", revenueSum);
        body.put("ordersSum", ordersSum);
        body.put("pendingSum", BigDecimal.ZERO);
        return body;
    }

    private Map<String, Object> buildEmptyResponse(LocalDate start, LocalDate end) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        List<String> labels = new ArrayList<>();
        List<BigDecimal> totals = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            labels.add(day.toString());
            totals.add(BigDecimal.ZERO);
            counts.add(0L);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("labels", labels);
        body.put("totals", totals);
        body.put("counts", counts);
        body.put("revenueSum", BigDecimal.ZERO);
        body.put("ordersSum", 0L);
        body.put("pendingSum", BigDecimal.ZERO);
        return body;
    }

    private boolean isSameDay(LocalDateTime dateTime, LocalDate day) {
        return dateTime != null && dateTime.toLocalDate().isEqual(day);
    }

    private record LocalDateRange(LocalDate start, LocalDate end) {}
}


