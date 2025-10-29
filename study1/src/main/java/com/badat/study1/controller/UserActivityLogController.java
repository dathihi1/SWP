package com.badat.study1.controller;

import com.badat.study1.dto.response.UserActivityLogResponse;
import com.badat.study1.model.User;
import com.badat.study1.model.UserActivityLog;
import com.badat.study1.service.UserActivityLogService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserActivityLogController {
    
    private final UserActivityLogService userActivityLogService;
    
    @GetMapping("/activity-history")
    public String getActivityHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return "redirect:/login";
            }
            
            User currentUser = (User) authentication.getPrincipal();
            log.info("Loading activity history for user: {}, page: {}", currentUser.getUsername(), page);
            
            // Validate and normalize pagination (1-based input -> 0-based Pageable)
            int safeSize = com.badat.study1.util.PaginationValidator.validateSize(size);
            int safePageOneBased = com.badat.study1.util.PaginationValidator.validateOneBasedPage(page);
            int safePageIndex = com.badat.study1.util.PaginationValidator.toZeroBased(safePageOneBased);
            Pageable pageable = PageRequest.of(safePageIndex, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            // Parse dates
            LocalDate from = null;
            LocalDate to = null;
            if (fromDate != null && !fromDate.isEmpty()) {
                from = LocalDate.parse(fromDate, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (toDate != null && !toDate.isEmpty()) {
                to = LocalDate.parse(toDate, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            
            // Normalize action parameter (convert empty string to null)
            String normalizedAction = (action != null && action.trim().isEmpty()) ? null : action;
            
            log.debug("User activity history filter - userId: {}, action: '{}' -> '{}', success: {}, fromDate: '{}', toDate: '{}'", 
                currentUser.getId(), action, normalizedAction, success, fromDate, toDate);
            
            // Get user activities
            Page<UserActivityLog> activities = userActivityLogService.getUserActivities(
                currentUser.getId(), normalizedAction, success, from, to, pageable);

            // Clamp page if out of bounds, then re-query once with adjusted page
            int totalPages = activities.getTotalPages();
            int clampedIndex = com.badat.study1.util.PaginationValidator.validatePageAgainstTotal(safePageIndex, totalPages);
            if (clampedIndex != safePageIndex) {
                pageable = PageRequest.of(clampedIndex, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
                activities = userActivityLogService.getUserActivities(
                    currentUser.getId(), normalizedAction, success, from, to, pageable);
            }
            
            log.info("Found {} activities for user {} (total: {}, page: {})", 
                activities.getNumberOfElements(), currentUser.getUsername(), 
                activities.getTotalElements(), activities.getNumber());
            
            // Convert to DTO with parsed User-Agent information
            List<UserActivityLogResponse> activityResponses = activities.getContent().stream()
                    .map(UserActivityLogResponse::fromEntity)
                    .collect(Collectors.toList());
            
            // Create a custom Page with DTOs
            Page<UserActivityLogResponse> activityPage = new org.springframework.data.domain.PageImpl<>(
                activityResponses, 
                pageable, 
                activities.getTotalElements()
            );
            
            // Prepare model data
            model.addAttribute("activities", activityPage);
            model.addAttribute("selectedAction", action);
            model.addAttribute("selectedSuccess", success);
            model.addAttribute("selectedFromDate", fromDate);
            model.addAttribute("selectedToDate", toDate);
            model.addAttribute("selectedSize", safeSize);
            
            // Add user info for navbar
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("userRole", currentUser.getRole().name());
            model.addAttribute("walletBalance", 0.0); // You can get this from wallet service if needed
            
            // Add action mappings for display
            Map<String, String> actionMappings = new HashMap<>();
            actionMappings.put("LOGIN", "Đăng nhập");
            actionMappings.put("LOGOUT", "Đăng xuất");
            actionMappings.put("REGISTER", "Đăng ký tài khoản");
            actionMappings.put("OTP_VERIFY", "Xác minh OTP");
            actionMappings.put("PROFILE_UPDATE", "Cập nhật thông tin");
            actionMappings.put("PASSWORD_CHANGE", "Đổi mật khẩu");
            actionMappings.put("ADD_TO_CART", "Thêm vào giỏ hàng");
            actionMappings.put("UPDATE_CART", "Cập nhật giỏ hàng");
            actionMappings.put("REMOVE_FROM_CART", "Xóa khỏi giỏ hàng");
            actionMappings.put("CLEAR_CART", "Xóa giỏ hàng");
            actionMappings.put("VIEW_PRODUCT", "Xem sản phẩm");
            actionMappings.put("CREATE_ORDER", "Tạo đơn hàng");
            actionMappings.put("CANCEL_ORDER", "Hủy đơn hàng");
            actionMappings.put("PAYMENT_SUCCESS", "Thanh toán thành công");
            actionMappings.put("PAYMENT_FAILED", "Thanh toán thất bại");
            actionMappings.put("CREATE_REVIEW", "Tạo đánh giá");
            actionMappings.put("UPDATE_REVIEW", "Cập nhật đánh giá");
            actionMappings.put("DELETE_REVIEW", "Xóa đánh giá");
            
            model.addAttribute("actionMappings", actionMappings);
            model.addAttribute("availableActions", actionMappings.keySet());
            
            log.info("Found {} activities for user {}", activityPage.getTotalElements(), currentUser.getUsername());
            
            return "customer/activity-history";
            
        } catch (Exception e) {
            log.error("Error loading activity history: {}", e.getMessage(), e);
            // Always provide a safe empty page to avoid template nulls
            Pageable fallbackPageable = PageRequest.of(0, com.badat.study1.util.PaginationValidator.getDefaultSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<UserActivityLogResponse> emptyPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(), fallbackPageable, 0);
            model.addAttribute("activities", emptyPage);
            model.addAttribute("selectedSize", com.badat.study1.util.PaginationValidator.getDefaultSize());
            model.addAttribute("error", "Có lỗi xảy ra khi tải lịch sử hoạt động");
            return "customer/activity-history";
        }
    }
}