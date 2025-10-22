package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.WithdrawRequestRepository;
import com.badat.study1.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {
    
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
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
    
    @GetMapping("/admin/users")
    public String adminUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            Model model) {
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
        
        // Create sort object
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        // Create pageable object
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get users with pagination and filtering
        Page<User> userPage;
        
        // Build dynamic query based on filters
        if (search != null && !search.trim().isEmpty()) {
            if (role != null && !role.isEmpty() && status != null && !status.isEmpty()) {
                // Search + Role + Status
                userPage = userRepository.findByUsernameContainingIgnoreCaseAndRoleAndStatus(
                    search, User.Role.valueOf(role.toUpperCase()), 
                    User.Status.valueOf(status.toUpperCase()), pageable);
            } else if (role != null && !role.isEmpty()) {
                // Search + Role
                userPage = userRepository.findByUsernameContainingIgnoreCaseAndRole(
                    search, User.Role.valueOf(role.toUpperCase()), pageable);
            } else if (status != null && !status.isEmpty()) {
                // Search + Status
                userPage = userRepository.findByUsernameContainingIgnoreCaseAndStatus(
                    search, User.Status.valueOf(status.toUpperCase()), pageable);
            } else {
                // Search only
                userPage = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                    search, search, search, pageable);
            }
        } else if (role != null && !role.isEmpty() && status != null && !status.isEmpty()) {
            // Role + Status
            userPage = userRepository.findByRoleAndStatus(
                User.Role.valueOf(role.toUpperCase()), 
                User.Status.valueOf(status.toUpperCase()), 
                pageable);
        } else if (role != null && !role.isEmpty()) {
            // Role only
            userPage = userRepository.findByRole(
                User.Role.valueOf(role.toUpperCase()), 
                pageable);
        } else if (status != null && !status.isEmpty()) {
            // Status only
            userPage = userRepository.findByStatus(
                User.Status.valueOf(status.toUpperCase()), 
                pageable);
        } else {
            // Get all users
            userPage = userRepository.findAll(pageable);
        }
        
        // Add pagination attributes
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", userPage.getNumber());
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalElements", userPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("search", search);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        
        // Add filter options
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("statuses", User.Status.values());
        
        return "admin/users";
    }
    
    @GetMapping("/admin/stalls")
    public String adminStalls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String isDelete,
            Model model) {
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
        
        // Create sort object
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        // Create pageable object
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get shops with pagination and filtering
        Page<com.badat.study1.model.Shop> shopPage;
        
        if (search != null && !search.trim().isEmpty()) {
            // Search by shop name, description, address, phone, or email
            shopPage = shopRepository.findByShopNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrAddressContainingIgnoreCase(
                search, search, search, pageable);
        } else if (status != null && !status.isEmpty() && isDelete != null && !isDelete.isEmpty()) {
            // Filter by both status and isDelete
            Boolean deleteStatus = Boolean.parseBoolean(isDelete);
            shopPage = shopRepository.findByStatusAndIsDelete(
                com.badat.study1.model.Shop.Status.valueOf(status.toUpperCase()), 
                deleteStatus, 
                pageable);
        } else if (status != null && !status.isEmpty()) {
            // Filter by status only
            shopPage = shopRepository.findByStatus(
                com.badat.study1.model.Shop.Status.valueOf(status.toUpperCase()), 
                pageable);
        } else if (isDelete != null && !isDelete.isEmpty()) {
            // Filter by isDelete only
            Boolean deleteStatus = Boolean.parseBoolean(isDelete);
            shopPage = shopRepository.findByIsDelete(deleteStatus, pageable);
        } else {
            // Get all shops
            shopPage = shopRepository.findAll(pageable);
        }
        
        // Add pagination attributes
        model.addAttribute("stalls", shopPage.getContent());
        model.addAttribute("currentPage", shopPage.getNumber());
        model.addAttribute("totalPages", shopPage.getTotalPages());
        model.addAttribute("totalElements", shopPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("isDelete", isDelete);
        
        // Add filter options
        model.addAttribute("statuses", com.badat.study1.model.Shop.Status.values());
        
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
}
