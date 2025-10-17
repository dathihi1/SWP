package com.badat.study1.controller;

import com.badat.study1.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyticsController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/seller/sales")
    public ResponseEntity<Map<String, Object>> sales(@RequestParam(value = "month", required = false) String month) {
        LocalDate from;
        LocalDate to;
        if (month != null && month.matches("\\d{4}-\\d{2}")) {
            // month format YYYY-MM
            int y = Integer.parseInt(month.substring(0, 4));
            int m = Integer.parseInt(month.substring(5, 7));
            from = LocalDate.of(y, m, 1);
            to = from.withDayOfMonth(from.lengthOfMonth());
        } else {
            to = LocalDate.now();
            from = to.minusDays(29);
        }

        List<Object[]> rows = transactionRepository.aggregateBetween(from, to);

        Map<LocalDate, Map<String, Number>> byDate = new HashMap<>();
        for (Object[] r : rows) {
            LocalDate d = ((java.sql.Date) r[0]).toLocalDate();
            Number total = (Number) r[1];
            Number cnt = (Number) r[2];
            Map<String, Number> v = new HashMap<>();
            v.put("total", total);
            v.put("count", cnt);
            byDate.put(d, v);
        }

        List<String> labels = new ArrayList<>();
        List<Number> totals = new ArrayList<>();
        List<Number> counts = new ArrayList<>();
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        Number revenueSum = 0;
        Number ordersSum = 0;
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            labels.add(d.toString());
            Map<String, Number> v = byDate.getOrDefault(d, Collections.emptyMap());
            Number total = v.getOrDefault("total", 0);
            Number cnt = v.getOrDefault("count", 0);
            totals.add(total);
            counts.add(cnt);
            revenueSum = revenueSum.doubleValue() + ((Number) total).doubleValue();
            ordersSum = ordersSum.longValue() + ((Number) cnt).longValue();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("labels", labels);
        body.put("totals", totals);
        body.put("counts", counts);
        body.put("revenueSum", revenueSum);
        body.put("ordersSum", ordersSum);
        return ResponseEntity.ok(body);
    }
}


