package com.badat.study1.controller;

import com.badat.study1.dto.response.AuditLogResponse;
import com.badat.study1.model.User;
import com.badat.study1.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/audit-logs/me")
    public ResponseEntity<Page<AuditLogResponse>> getMyAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false, name = "from") String fromDate,
            @RequestParam(required = false, name = "to") String toDate,
            @RequestParam(required = false) String category) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            return ResponseEntity.status(401).build();
        }
        User user = (User) auth.getPrincipal();
        Page<AuditLogResponse> logs = auditLogService.getUserAuditLogsWithFilters(user.getId(), page, size, action, success, fromDate, toDate, category);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAllAuditLogsForAdmin(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false, name = "from") String fromDate,
            @RequestParam(required = false, name = "to") String toDate) {
        Page<AuditLogResponse> logs = auditLogService.getAdminAuditLogsWithFilters(userId, page, size, action, success, fromDate, toDate);
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/audit-logs/categories")
    public ResponseEntity<List<String>> getAvailableCategories() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            return ResponseEntity.status(401).build();
        }
        User user = (User) auth.getPrincipal();
        List<com.badat.study1.model.AuditLog.Category> categories = auditLogService.getAvailableCategories(user.getId());
        List<String> categoryNames = categories.stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(categoryNames);
    }
}



