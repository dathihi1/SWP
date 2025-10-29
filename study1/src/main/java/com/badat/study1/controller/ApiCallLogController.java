package com.badat.study1.controller;

import com.badat.study1.dto.response.ApiCallLogResponse;
import com.badat.study1.model.ApiCallLog;
import com.badat.study1.repository.ApiCallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ApiCallLogController {
    
    private final ApiCallLogRepository apiCallLogRepository;
    
    @GetMapping("/api-calls")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getApiCallLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            // Parse dates
            LocalDateTime fromDateTime = null;
            LocalDateTime toDateTime = null;
            
            if (fromDate != null && !fromDate.trim().isEmpty()) {
                fromDateTime = LocalDateTime.parse(fromDate + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            
            if (toDate != null && !toDate.trim().isEmpty()) {
                toDateTime = LocalDateTime.parse(toDate + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            
            // Query with filters
            Page<ApiCallLog> logs = apiCallLogRepository.findWithFilters(
                    userId, endpoint, method, statusCode, fromDateTime, toDateTime, pageable);
            
            // Convert to response DTOs
            Page<ApiCallLogResponse> response = logs.map(ApiCallLogResponse::fromApiCallLog);
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", response.getContent());
            result.put("totalElements", response.getTotalElements());
            result.put("totalPages", response.getTotalPages());
            result.put("currentPage", response.getNumber());
            result.put("size", response.getSize());
            result.put("first", response.isFirst());
            result.put("last", response.isLast());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting API call logs: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/api-calls/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getApiStatistics(
            @RequestParam(defaultValue = "7") int days) {
        
        try {
            LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
            
            // Get statistics
            Double avgResponseTime = apiCallLogRepository.findAverageResponseTime(fromDate);
            Long errorCount = apiCallLogRepository.countErrorsSince(fromDate);
            Long totalCalls = apiCallLogRepository.countTotalCallsSince(fromDate);
            
            // Get top endpoints
            Pageable topEndpointsPageable = PageRequest.of(0, 10);
            List<Object[]> topEndpoints = apiCallLogRepository.findTopEndpoints(fromDate, topEndpointsPageable);
            
            // Get slow calls
            Pageable slowCallsPageable = PageRequest.of(0, 10);
            List<ApiCallLog> slowCalls = apiCallLogRepository.findSlowCalls(1000, fromDate, slowCallsPageable);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("avgResponseTime", avgResponseTime != null ? Math.round(avgResponseTime) : 0);
            statistics.put("errorCount", errorCount);
            statistics.put("totalCalls", totalCalls);
            statistics.put("errorRate", totalCalls > 0 ? Math.round((double) errorCount / totalCalls * 100) : 0);
            statistics.put("topEndpoints", topEndpoints);
            statistics.put("slowCalls", slowCalls.stream().map(ApiCallLogResponse::fromApiCallLog).toList());
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("Error getting API statistics: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/api-calls/endpoints")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDistinctEndpoints() {
        try {
            List<String> endpoints = apiCallLogRepository.findDistinctEndpoints();
            return ResponseEntity.ok(Map.of("endpoints", endpoints));
        } catch (Exception e) {
            log.error("Error getting distinct endpoints: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/api-calls/methods")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDistinctMethods() {
        try {
            List<String> methods = apiCallLogRepository.findDistinctMethods();
            return ResponseEntity.ok(Map.of("methods", methods));
        } catch (Exception e) {
            log.error("Error getting distinct methods: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/api-calls/status-codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDistinctStatusCodes() {
        try {
            List<Integer> statusCodes = apiCallLogRepository.findDistinctStatusCodes();
            return ResponseEntity.ok(Map.of("statusCodes", statusCodes));
        } catch (Exception e) {
            log.error("Error getting distinct status codes: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}
















