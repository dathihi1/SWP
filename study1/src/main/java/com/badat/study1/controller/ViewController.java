package com.badat.study1.controller;

import com.badat.study1.model.Stall;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.model.WalletHistory;
import com.badat.study1.model.Product;
import com.badat.study1.model.Review;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.UploadHistoryRepository;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.ReviewRepository;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.service.WalletHistoryService;
import com.badat.study1.service.AuditLogService;
import com.badat.study1.dto.response.AuditLogResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Base64;
import java.time.LocalDate;
import java.time.ZoneId;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
@Controller
public class ViewController {
    private final WalletRepository walletRepository;
    private final ShopRepository shopRepository;
    private final StallRepository stallRepository;
    private final ProductRepository productRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    private final WalletHistoryService walletHistoryService;
    private final AuditLogService auditLogService;

    public ViewController(WalletRepository walletRepository, ShopRepository shopRepository, StallRepository stallRepository, ProductRepository productRepository, UploadHistoryRepository uploadHistoryRepository, WalletHistoryService walletHistoryService, AuditLogService auditLogService, UserRepository userRepository, ReviewRepository reviewRepository, OrderItemRepository orderItemRepository, OrderRepository orderRepository) {
        this.walletRepository = walletRepository;
        this.shopRepository = shopRepository;
        this.stallRepository = stallRepository;
        this.productRepository = productRepository;
        this.uploadHistoryRepository = uploadHistoryRepository;
        this.walletHistoryService = walletHistoryService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
    }

    // Inject common attributes (auth info and wallet balance) for all views
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !String.valueOf(authentication.getName()).equals("anonymousUser");

        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            // Get UserDetails from authentication principal
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                model.addAttribute("username", user.getUsername());
                model.addAttribute("userRole", user.getRole().name());

                BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                        .map(Wallet::getBalance)
                        .orElse(BigDecimal.ZERO);
                model.addAttribute("walletBalance", walletBalance);
            } else {
                // Fallback for other types of principals
                model.addAttribute("username", authentication.getName());
                model.addAttribute("userRole", "USER");
                model.addAttribute("walletBalance", BigDecimal.ZERO);
            }
        }
    }

    @GetMapping("/")
    public String homePage(Model model) {
        log.info("Homepage requested");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                    !authentication.getName().equals("anonymousUser");

            model.addAttribute("isAuthenticated", isAuthenticated);
            model.addAttribute("walletBalance", BigDecimal.ZERO); // Default value

            if (isAuthenticated) {
                // Get User object from authentication principal
                Object principal = authentication.getPrincipal();
                if (principal instanceof User) {
                    User user = (User) principal;

                    model.addAttribute("username", user.getUsername());
                    model.addAttribute("authorities", authentication.getAuthorities());
                    model.addAttribute("userRole", user.getRole().name());
                    // Default submitSuccess to false to avoid null in template conditions
                    model.addAttribute("submitSuccess", false);

                    // Lấy số dư ví
                    try {
                        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                                .map(Wallet::getBalance)
                                .orElse(BigDecimal.ZERO);
                        model.addAttribute("walletBalance", walletBalance);
                    } catch (Exception e) {
                        log.error("Error getting wallet balance for user {}: {}", user.getId(), e.getMessage());
                        model.addAttribute("walletBalance", BigDecimal.ZERO);
                    }
                } else {
                    // Fallback for other types of principals
                    model.addAttribute("username", authentication.getName());
                    model.addAttribute("authorities", authentication.getAuthorities());
                    model.addAttribute("userRole", "USER");
                    model.addAttribute("submitSuccess", false);
                    model.addAttribute("walletBalance", BigDecimal.ZERO);
                }
            }
        } catch (Exception e) {
            log.error("Error in homePage method: {}", e.getMessage(), e);
            // Set default values to prevent template errors
            model.addAttribute("isAuthenticated", false);
            model.addAttribute("walletBalance", BigDecimal.ZERO);
            model.addAttribute("username", "Guest");
            model.addAttribute("userRole", "USER");
        }

        // Load top 8 stalls with highest product counts for homepage preview
        try {
            var activeStalls = stallRepository.findByStatusAndIsDeleteFalse("OPEN");
            List<Map<String, Object>> stallCards = new ArrayList<>();
            
            // Calculate product counts for all stalls
            List<Map<String, Object>> stallsWithCounts = new ArrayList<>();
            for (Stall stall : activeStalls) {
                Map<String, Object> vm = new HashMap<>();
                vm.put("stallId", stall.getId());
                vm.put("stallName", stall.getStallName());
                vm.put("stallCategory", stall.getStallCategory());

                // Compute product count by summing quantities of products in the stall
                var products = productRepository.findByStallIdAndIsDeleteFalse(stall.getId());
                int totalStock = products.stream()
                        .mapToInt(p -> p.getQuantity() != null ? p.getQuantity() : 0)
                        .sum();
                vm.put("productCount", totalStock);
                
                // Calculate price range from available products
                if (!products.isEmpty()) {
                    var availableProducts = products.stream()
                            .filter(product -> product.getQuantity() != null && product.getQuantity() > 0)
                            .collect(Collectors.toList());
                    
                    if (!availableProducts.isEmpty()) {
                        BigDecimal minPrice = availableProducts.stream()
                                .map(Product::getPrice)
                                .min(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);
                        
                        BigDecimal maxPrice = availableProducts.stream()
                                .map(Product::getPrice)
                                .max(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);
                        
                        NumberFormat viNumber = NumberFormat.getNumberInstance(Locale.US);
                        viNumber.setGroupingUsed(true);
                        String minStr = viNumber.format(minPrice.setScale(0, RoundingMode.HALF_UP));
                        String maxStr = viNumber.format(maxPrice.setScale(0, RoundingMode.HALF_UP));
                        if (minPrice.equals(maxPrice)) {
                            vm.put("priceRange", minStr + " VND");
                        } else {
                            vm.put("priceRange", minStr + " VND - " + maxStr + " VND");
                        }
                    } else {
                        vm.put("priceRange", "Hết hàng");
                    }
                } else {
                    vm.put("priceRange", "Chưa có sản phẩm");
                }

                // Resolve shop name
                shopRepository.findById(stall.getShopId())
                        .ifPresent(shop -> vm.put("shopName", shop.getShopName()));
                if (!vm.containsKey("shopName")) {
                    vm.put("shopName", "Unknown Shop");
                }

                // Calculate average rating for the stall
                try {
                    var reviews = reviewRepository.findByStallIdAndIsDeleteFalse(stall.getId());
                    if (!reviews.isEmpty()) {
                        double avgRating = reviews.stream()
                                .mapToInt(Review::getRating)
                                .average()
                                .orElse(0.0);
                        vm.put("averageRating", Math.round(avgRating * 10.0) / 10.0); // Round to 1 decimal place
                        vm.put("reviewCount", reviews.size());
                    } else {
                        vm.put("averageRating", 0.0);
                        vm.put("reviewCount", 0);
                    }
                } catch (Exception e) {
                    log.warn("Error calculating average rating for stall {}: {}", stall.getId(), e.getMessage());
                    vm.put("averageRating", 0.0);
                    vm.put("reviewCount", 0);
                }

                // Always set imageBase64 key, even if null
                if (stall.getStallImageData() != null && stall.getStallImageData().length > 0) {
                    String base64 = Base64.getEncoder().encodeToString(stall.getStallImageData());
                    vm.put("imageBase64", base64);
                } else {
                    vm.put("imageBase64", null);
                }

                stallsWithCounts.add(vm);
            }
            
            // Sort by product count descending and take top 8
            stallCards = stallsWithCounts.stream()
                    .sorted((a, b) -> Integer.compare((Integer) b.get("productCount"), (Integer) a.get("productCount")))
                    .limit(8)
                    .collect(Collectors.toList());
                    
            model.addAttribute("stalls", stallCards);
        } catch (Exception ignored) {}

        log.info("Returning home template");
        return "home";
    }

    @GetMapping("/index")
    public String indexPage() {
        return "redirect:/";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam(value = "email", required = false) String email, Model model) {
        if (email != null) {
            model.addAttribute("email", email);
        }
        return "verify-otp";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "email", required = false) String email,
                                   @RequestParam(value = "otp", required = false) String otp,
                                   Model model) {
        if (email != null) {
            model.addAttribute("email", email);
        }
        if (otp != null) {
            model.addAttribute("otp", otp);
        }
        return "reset-password";
    }

    @GetMapping("/seller/register")
    public String sellerRegisterPage(Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login?required=1";
        }

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("walletBalance", BigDecimal.ZERO); // Default value

        User user = (User) authentication.getPrincipal();
		// Require phone and full name before accessing seller registration
		boolean missingPhone = (user.getPhone() == null || user.getPhone().trim().isEmpty());
		boolean missingFullName = (user.getFullName() == null || user.getFullName().trim().isEmpty());
		
		log.info("Seller registration check - User: {}, Phone: {}, FullName: {}, MissingPhone: {}, MissingFullName: {}", 
				user.getUsername(), user.getPhone(), user.getFullName(), missingPhone, missingFullName);
		
		if (missingPhone || missingFullName) {
			log.info("Redirecting to profile - missing required info");
			redirectAttributes.addFlashAttribute("infoRequired",
					"Vui lòng cập nhật đầy đủ Họ và tên và Số điện thoại trước khi đăng ký bán hàng.");
			return "redirect:/profile";
		}
        model.addAttribute("username", user.getUsername());
        model.addAttribute("authorities", authentication.getAuthorities());
        model.addAttribute("userRole", user.getRole().name());
        // Default submitSuccess to false to avoid null in template conditions
        model.addAttribute("submitSuccess", false);

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        // Đã có shop => đang chờ duyệt nếu role chưa là SELLER
        boolean hasShop = shopRepository.findByUserId(user.getId()).isPresent();
        boolean isSeller = user.getRole() == User.Role.SELLER;
        model.addAttribute("pendingReview", hasShop && !isSeller);
        model.addAttribute("alreadySeller", isSeller);

        return "seller/register";
    }

    @GetMapping("/terms")
    public String termsPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("walletBalance", BigDecimal.ZERO); // Default value

        if (isAuthenticated) {
            User user = (User) authentication.getPrincipal();
            model.addAttribute("username", user.getUsername());
            model.addAttribute("authorities", authentication.getAuthorities());
            model.addAttribute("userRole", user.getRole().name());

            // Lấy số dư ví
            BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                    .map(Wallet::getBalance)
                    .orElse(BigDecimal.ZERO);
            model.addAttribute("walletBalance", walletBalance);
        }

        return "terms";
    }

    @GetMapping("/faqs")
    public String faqsPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("walletBalance", BigDecimal.ZERO); // Default value

        if (isAuthenticated) {
            User user = (User) authentication.getPrincipal();
            model.addAttribute("username", user.getUsername());
            model.addAttribute("authorities", authentication.getAuthorities());
            model.addAttribute("userRole", user.getRole().name());

            // Lấy số dư ví
            BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                    .map(Wallet::getBalance)
                    .orElse(BigDecimal.ZERO);
            model.addAttribute("walletBalance", walletBalance);
        }

        return "faqs";
    }


    @GetMapping("/payment-history")
    public String paymentHistoryPage(Model model,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(required = false) String fromDate,
                                     @RequestParam(required = false) String toDate,
                                     @RequestParam(required = false) String transactionType,
                                     @RequestParam(required = false) String transactionStatus) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("user", user); // Thêm object user vào model

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        // Load wallet history for current user, apply filters and pagination
        final int currentPageParam = page;
        walletRepository.findByUserId(user.getId()).ifPresent(wallet -> {
            List<WalletHistory> all = walletHistoryService.getWalletHistoryByWalletId(wallet.getId());

            List<WalletHistory> filtered = all.stream()
                    .filter(h -> {
                        // fromDate (HTML5 yyyy-MM-dd)
                        if (fromDate != null && !fromDate.trim().isEmpty()) {
                            try {
                                LocalDate fd = LocalDate.parse(fromDate);
                                if (h.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(fd)) {
                                    return false;
                                }
                            } catch (Exception ignored) {}
                        }
                        // toDate
                        if (toDate != null && !toDate.trim().isEmpty()) {
                            try {
                                LocalDate td = LocalDate.parse(toDate);
                                if (h.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(td)) {
                                    return false;
                                }
                            } catch (Exception ignored) {}
                        }
                        // type
                        if (transactionType != null && !transactionType.trim().isEmpty() && !"ALL".equals(transactionType)) {
                            if (!h.getType().name().equals(transactionType)) return false;
                        }
                        // status
                        if (transactionStatus != null && !transactionStatus.trim().isEmpty() && !"ALL".equals(transactionStatus)) {
                            if (!h.getStatus().name().equals(transactionStatus)) return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            int pageSize = 5;
            int totalPages = (int) Math.ceil((double) filtered.size() / pageSize);
            int safePage = currentPageParam;
            if (safePage < 1) safePage = 1;
            if (safePage > totalPages && totalPages > 0) safePage = totalPages;
            int startIndex = (safePage - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, filtered.size());
            List<WalletHistory> pageData = filtered.subList(startIndex, endIndex);

            model.addAttribute("walletHistory", pageData);
            model.addAttribute("currentPage", safePage);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("hasNextPage", safePage < totalPages);
            model.addAttribute("hasPrevPage", safePage > 1);
            model.addAttribute("nextPage", safePage + 1);
            model.addAttribute("prevPage", safePage - 1);

            // keep filter params
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            model.addAttribute("transactionType", transactionType);
            model.addAttribute("transactionStatus", transactionStatus);
        });

        return "customer/payment-history";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("user", user); // Thêm object user vào model

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        return "customer/change-password";
    }

    @GetMapping("/orders")
    public String ordersPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("user", user); // Thêm object user vào model

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        return "customer/orders";
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        // Refresh user from database to get latest avatarUrl
        User freshUser = userRepository.findById(user.getId()).orElse(user);
        model.addAttribute("username", freshUser.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", freshUser.getRole().name());
        model.addAttribute("user", freshUser); // Thêm object user vào model

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(freshUser.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        // Thêm thông tin ngày đăng ký
        model.addAttribute("userCreatedAt", freshUser.getCreatedAt());

        // Thêm thông tin đơn hàng (tạm thời set 0, có thể cập nhật sau)
        model.addAttribute("totalOrders", 0);

        // Lấy lịch sử hoạt động gần đây (5 hoạt động mới nhất)
        model.addAttribute("recentActivities", auditLogService.getRecentUserAuditLogs(freshUser.getId(), 5));

        return "customer/profile";
    }

    @GetMapping("/activity-history")
    public String activityHistoryPage(Model model, 
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(required = false) String action,
                                    @RequestParam(required = false) Boolean success,
                                    @RequestParam(required = false) String fromDate,
                                    @RequestParam(required = false) String toDate,
                                    @RequestParam(required = false) String technicalAction,
                                    @RequestParam(required = false) String ipFilter) {
        try {
            log.info("Activity history page requested for page: {}, action: {}, success: {}", page, action, success);
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                    !authentication.getName().equals("anonymousUser");

            if (!isAuthenticated) {
                log.warn("Unauthenticated access to activity history page");
                return "redirect:/login";
            }

            User user = (User) authentication.getPrincipal();
            log.info("Loading activity history for user: {}", user.getUsername());
            
            model.addAttribute("username", user.getUsername());
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("userRole", user.getRole().name());
            model.addAttribute("user", user);

            // Lấy số dư ví với error handling
            try {
                BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                        .map(Wallet::getBalance)
                        .orElse(BigDecimal.ZERO);
                model.addAttribute("walletBalance", walletBalance);
                log.debug("Wallet balance for user {}: {}", user.getId(), walletBalance);
            } catch (Exception e) {
                log.error("Error getting wallet balance for user {}: {}", user.getId(), e.getMessage());
                model.addAttribute("walletBalance", BigDecimal.ZERO);
            }

            // Use technicalAction if provided, otherwise use action
            String finalAction = (technicalAction != null && !technicalAction.isEmpty()) ? technicalAction : action;
            
            // Lấy lịch sử hoạt động với retry logic và error handling
            Page<AuditLogResponse> activities = null;
            int maxRetries = 3;
            int retryCount = 0;
            
            while (activities == null && retryCount < maxRetries) {
                try {
                    log.info("Attempting to get audit logs for user {} (attempt {})", user.getId(), retryCount + 1);
                    activities = auditLogService.getUserAuditLogsWithFilters(
                            user.getId(), page, 5, finalAction, success, fromDate, toDate);
                    
                    if (activities != null) {
                        log.info("Successfully retrieved {} activities for user {}", activities.getTotalElements(), user.getId());
                    }
                    break;
                } catch (Exception e) {
                    retryCount++;
                    log.error("Attempt {} failed to get audit logs for user {}: {}", retryCount, user.getId(), e.getMessage());
                    
                    if (retryCount >= maxRetries) {
                        log.error("All retry attempts failed for user {}", user.getId());
                        activities = Page.empty();
                    } else {
                        // Wait before retry
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("Thread interrupted while retrying for user {}", user.getId());
                            activities = Page.empty();
                            break;
                        }
                    }
                }
            }
            
            if (activities == null) {
                activities = Page.empty();
            }
            
            model.addAttribute("activities", activities);
            model.addAttribute("currentPage", page);
            
            // Filter parameters
            model.addAttribute("selectedAction", finalAction);
            model.addAttribute("selectedSuccess", success);
            model.addAttribute("selectedFromDate", fromDate);
            model.addAttribute("selectedToDate", toDate);
            model.addAttribute("selectedIp", ipFilter);
            
            // Available actions for filter with user-friendly names
            Map<String, String> actionMappings = new HashMap<>();
            actionMappings.put("LOGIN", "Đăng nhập");
            actionMappings.put("LOGOUT", "Đăng xuất");
            actionMappings.put("ACCOUNT_LOCKED", "Khóa tài khoản");
            actionMappings.put("ACCOUNT_UNLOCKED", "Mở khóa tài khoản");
            actionMappings.put("PASSWORD_CHANGE", "Đổi mật khẩu");
            actionMappings.put("PROFILE_UPDATE", "Cập nhật thông tin");
            actionMappings.put("REGISTER", "Đăng ký tài khoản");
            actionMappings.put("OTP_VERIFY", "Xác minh OTP");
            
            model.addAttribute("actionMappings", actionMappings);
            model.addAttribute("availableActions", actionMappings.keySet());

            log.info("Activity history page loaded successfully for user {}", user.getUsername());
            return "customer/activity-history";
            
        } catch (Exception e) {
            log.error("Critical error in activityHistoryPage: {}", e.getMessage(), e);
            // Return error page or redirect to home
            return "redirect:/";
        }
    }

    @GetMapping("/cart")
    public String cartPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("user", user); // Thêm object user vào model

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        return "customer/cart";
    }


    @GetMapping("/seller/gross-sales")
    public String sellerShopPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        return "seller/shop";
    }

    @GetMapping("/seller/products")
    public String sellerProductsPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has ADMIN role (for now, only ADMIN can access seller pages)
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        return "seller/shop";
    }


    @GetMapping("/seller/stall-management")
    public String sellerShopManagementPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        // Lấy danh sách gian hàng của user và tính tổng kho
        shopRepository.findByUserId(user.getId()).ifPresent(shop -> {
            var stalls = stallRepository.findByShopIdAndIsDeleteFalse(shop.getId());

            // Tính tổng kho và khoảng giá cho mỗi gian hàng
            stalls.forEach(stall -> {
                // Lấy tất cả sản phẩm trong gian hàng này
                var products = productRepository.findByStallIdAndIsDeleteFalse(stall.getId());

                // Tính tổng quantity của tất cả sản phẩm trong gian hàng
                int totalStock = products.stream()
                        .mapToInt(product -> product.getQuantity() != null ? product.getQuantity() : 0)
                        .sum();

                stall.setProductCount(totalStock);
                
                // Tính khoảng giá từ sản phẩm còn hàng
                if (!products.isEmpty()) {
                    var availableProducts = products.stream()
                            .filter(product -> product.getQuantity() != null && product.getQuantity() > 0)
                            .collect(Collectors.toList());
                    
                    if (!availableProducts.isEmpty()) {
                        BigDecimal minPrice = availableProducts.stream()
                                .map(Product::getPrice)
                                .min(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);
                        
                        BigDecimal maxPrice = availableProducts.stream()
                                .map(Product::getPrice)
                                .max(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);
                        
                        NumberFormat viNumber = NumberFormat.getNumberInstance(Locale.US);
                        viNumber.setGroupingUsed(true);
                        String minStr = viNumber.format(minPrice.setScale(0, RoundingMode.HALF_UP));
                        String maxStr = viNumber.format(maxPrice.setScale(0, RoundingMode.HALF_UP));
                        if (minPrice.equals(maxPrice)) {
                            stall.setPriceRange(minStr + " VND");
                        } else {
                            stall.setPriceRange(minStr + " VND - " + maxStr + " VND");
                        }
                    } else {
                        stall.setPriceRange("Hết hàng");
                    }
                } else {
                    stall.setPriceRange("Chưa có sản phẩm");
                }
            });

            model.addAttribute("stalls", stalls);

            // Lấy tổng số sản phẩm trong shop
            long totalProducts = productRepository.countByShopIdAndIsDeleteFalse(shop.getId());
            model.addAttribute("totalProducts", totalProducts);
        });

        return "seller/stall-management";
    }

    @GetMapping("/seller/add-stall")
    public String sellerAddStallPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has ADMIN role (for now, only ADMIN can access seller pages)
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        // Check if user has a shop
        boolean hasShop = shopRepository.findByUserId(user.getId()).isPresent();
        if (!hasShop) {
            return "redirect:/seller/stall-management";
        }

        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);
        
        return "seller/add-stall";
    }

    @GetMapping("/seller/edit-stall/{id}")
    public String sellerEditStallPage(@PathVariable Long id, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        // Lấy thông tin gian hàng
        var stall = stallRepository.findById(id);
        if (stall.isEmpty()) {
            return "redirect:/seller/stall-management";
        }

        // Kiểm tra quyền sở hữu gian hàng
        shopRepository.findByUserId(user.getId()).ifPresent(shop -> {
            if (!stall.get().getShopId().equals(shop.getId())) {
                return; // Không có quyền sửa gian hàng này
            }
        });

        model.addAttribute("stall", stall.get());

        return "seller/edit-stall";
    }

    @GetMapping("/seller/product-management/{stallId}")
    public String sellerProductManagementPage(@PathVariable Long stallId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        // Lấy thông tin gian hàng
        var stallOptional = stallRepository.findById(stallId);
        if (stallOptional.isEmpty()) {
            return "redirect:/seller/stall-management";
        }

        Stall stall = stallOptional.get();

        // Kiểm tra quyền sở hữu gian hàng
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty() || !stall.getShopId().equals(userShop.get().getId())) {
            return "redirect:/seller/stall-management";
        }

        model.addAttribute("stall", stall);

        // Lấy danh sách sản phẩm của gian hàng
        var products = productRepository.findByStallIdAndIsDeleteFalse(stallId);
        model.addAttribute("products", products);

        return "seller/product-management";
    }

    @GetMapping("/seller/add-quantity/{productId}")
    public String sellerAddQuantityPage(@PathVariable Long productId, 
                                       @RequestParam(defaultValue = "0") int page,
                                       Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());

        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);

        // Lấy thông tin sản phẩm
        var productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            return "redirect:/seller/stall-management";
        }

        var product = productOptional.get();

        // Kiểm tra quyền sở hữu sản phẩm
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return "redirect:/seller/stall-management";
        }

        var stallOptional = stallRepository.findById(product.getStallId());
        if (stallOptional.isEmpty() || !stallOptional.get().getShopId().equals(userShop.get().getId())) {
            return "redirect:/seller/stall-management";
        }

        model.addAttribute("product", product);
        model.addAttribute("stall", stallOptional.get());
        
        // Lấy lịch sử upload gần nhất cho sản phẩm này với pagination (5 bản ghi mỗi trang)
        Pageable pageable = PageRequest.of(page, 5);
        Page<com.badat.study1.model.UploadHistory> uploadHistoryPage = uploadHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        
        model.addAttribute("recentUploads", uploadHistoryPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", uploadHistoryPage.getTotalPages());
        model.addAttribute("totalElements", uploadHistoryPage.getTotalElements());
        model.addAttribute("hasNext", uploadHistoryPage.hasNext());
        model.addAttribute("hasPrevious", uploadHistoryPage.hasPrevious());
        
        return "seller/add-quantity";
    }

    @GetMapping("/seller/orders")
    public String sellerOrdersPage(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) Long stallId,
                                   @RequestParam(required = false) Long productId,
                                   @RequestParam(required = false) String dateFrom,
                                   @RequestParam(required = false) String dateTo,
                                   Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        // Get seller's shop
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return "redirect:/seller/stall-management";
        }

        // Get seller's stalls and products for filter dropdowns
        var stalls = stallRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
        var products = productRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
        
        // Get orders for this seller with pagination (10 orders per page)
        Pageable pageable = PageRequest.of(page, 10);
        List<com.badat.study1.model.Order> allOrders = orderRepository.findBySellerIdOrderByCreatedAtDesc(user.getId());
        
        // Populate transient fields from first OrderItem for each Order
        for (com.badat.study1.model.Order order : allOrders) {
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                com.badat.study1.model.OrderItem firstItem = order.getOrderItems().get(0);
                order.setProduct(firstItem.getProduct());
                // Đếm số lượng order_item thay vì lấy quantity của item đầu tiên
                order.setQuantity(order.getOrderItems().size());
                order.setUnitPrice(firstItem.getUnitPrice());
            }
        }
        
        // Apply filters
        List<com.badat.study1.model.Order> filteredOrders = allOrders.stream()
                .filter(order -> status == null || status.isEmpty() || order.getStatus().name().equals(status.toUpperCase()))
                .filter(order -> stallId == null || stallId.equals(order.getStallId()))
                // Note: productId filter removed since Order doesn't have direct productId field
                // If product filtering is needed, it should be done through OrderItem relationship
                .filter(order -> {
                    if (dateFrom == null || dateFrom.isEmpty()) return true;
                    return order.getCreatedAt().toLocalDate().isAfter(java.time.LocalDate.parse(dateFrom).minusDays(1));
                })
                .filter(order -> {
                    if (dateTo == null || dateTo.isEmpty()) return true;
                    return order.getCreatedAt().toLocalDate().isBefore(java.time.LocalDate.parse(dateTo).plusDays(1));
                })
                .collect(java.util.stream.Collectors.toList());
        
        // Create pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredOrders.size());
        List<com.badat.study1.model.Order> pageContent = filteredOrders.subList(start, end);
        
        Page<com.badat.study1.model.Order> ordersPage = new org.springframework.data.domain.PageImpl<>(
            pageContent, 
            pageable, 
            filteredOrders.size()
        );
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("totalElements", ordersPage.getTotalElements());
        model.addAttribute("hasNext", ordersPage.hasNext());
        model.addAttribute("hasPrevious", ordersPage.hasPrevious());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedStallId", stallId);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedDateFrom", dateFrom);
        model.addAttribute("selectedDateTo", dateTo);
        model.addAttribute("orderStatuses", com.badat.study1.model.Order.Status.values());
        model.addAttribute("stalls", stalls);
        model.addAttribute("products", products);

        return "seller/orders";
    }

    @GetMapping("/seller/reviews")
    public String sellerReviewsPage(@RequestParam(defaultValue = "0") int page,
                                   Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        // Get seller's shop
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return "redirect:/seller/stall-management";
        }

        // Get seller's stalls with review statistics
        var stalls = stallRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
        var stallStats = new java.util.ArrayList<java.util.Map<String, Object>>();
        
        for (var stall : stalls) {
            var stallReviews = reviewRepository.findByStallIdAndIsDeleteFalse(stall.getId());
            
            double averageRating = 0.0;
            int reviewCount = stallReviews.size();
            
            if (reviewCount > 0) {
                averageRating = stallReviews.stream()
                    .mapToInt(com.badat.study1.model.Review::getRating)
                    .average()
                    .orElse(0.0);
            }
            
            // Calculate unread count for this stall
            int unreadCount = (int) stallReviews.stream()
                .filter(review -> !review.getIsRead())
                .count();
            
            var stallStat = new java.util.HashMap<String, Object>();
            stallStat.put("stall", stall);
            stallStat.put("averageRating", Math.round(averageRating * 10.0) / 10.0); // Round to 1 decimal
            stallStat.put("reviewCount", reviewCount);
            stallStat.put("unreadCount", unreadCount);
            stallStats.add(stallStat);
        }
        
        // Get reviews for this seller with pagination (10 reviews per page)
        // SECURITY: Validate both seller_id and shop_id to ensure reviews belong to this seller's shop
        Pageable pageable = PageRequest.of(page, 10);
        Page<com.badat.study1.model.Review> reviewsPage = reviewRepository.findBySellerIdAndShopIdAndIsDeleteFalse(user.getId(), userShop.get().getId(), pageable);

        model.addAttribute("reviews", reviewsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviewsPage.getTotalPages());
        model.addAttribute("totalElements", reviewsPage.getTotalElements());
        model.addAttribute("hasNext", reviewsPage.hasNext());
        model.addAttribute("hasPrevious", reviewsPage.hasPrevious());
        model.addAttribute("stallStats", stallStats);

        return "seller/reviews";
    }

    @GetMapping("/api/seller/reviews/stall/{stallId}")
    @ResponseBody
    public ResponseEntity<?> getReviewsByStall(@PathVariable Long stallId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        try {
            // Verify that the stall belongs to this seller
            var stall = stallRepository.findById(stallId);
            if (stall.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Stall not found"));
            }

            // Get seller's shop
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Shop not found"));
            }

            // Verify stall belongs to seller's shop
            if (!stall.get().getShopId().equals(userShop.get().getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            // Get reviews for this stall
            var reviews = reviewRepository.findByStallIdAndIsDeleteFalse(stallId);
            
            // Convert to DTO format for JSON response
            var reviewDTOs = reviews.stream().map(review -> {
                var dto = new java.util.HashMap<String, Object>();
                dto.put("id", review.getId());
                dto.put("rating", review.getRating());
                dto.put("content", review.getContent());
                dto.put("replyContent", review.getReplyContent());
                dto.put("createdAt", review.getCreatedAt());
                dto.put("isRead", review.getIsRead());
                
                // Add buyer info
                var buyerInfo = new java.util.HashMap<String, Object>();
                buyerInfo.put("username", review.getBuyer().getUsername());
                dto.put("buyer", buyerInfo);
                
                // Add product info
                var productInfo = new java.util.HashMap<String, Object>();
                productInfo.put("name", review.getProduct().getName());
                dto.put("product", productInfo);
                
                return dto;
            }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(reviewDTOs);

    } catch (Exception e) {
        log.error("Error fetching reviews for stall {}: {}", stallId, e.getMessage());
        return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
    }
}

@PostMapping("/api/seller/reviews/mark-read")
@ResponseBody
public ResponseEntity<?> markReviewsAsRead(@RequestParam Long stallId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
            !authentication.getName().equals("anonymousUser");

    if (!isAuthenticated) {
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }

    User user = (User) authentication.getPrincipal();

    // Check if user has SELLER role
    if (!user.getRole().equals(User.Role.SELLER)) {
        return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }

    try {
        // Verify that the stall belongs to this seller
        var stall = stallRepository.findById(stallId);
        if (stall.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Stall not found"));
        }

        // Get seller's shop
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Shop not found"));
        }

        // Verify stall belongs to seller's shop
        if (!stall.get().getShopId().equals(userShop.get().getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        // Mark all reviews for this stall as read
        var reviews = reviewRepository.findByStallIdAndIsDeleteFalse(stallId);
        for (var review : reviews) {
            if (!review.getIsRead()) {
                review.setIsRead(true);
                reviewRepository.save(review);
            }
        }

        return ResponseEntity.ok(Map.of("message", "Reviews marked as read", "count", reviews.size()));

    } catch (Exception e) {
        log.error("Error marking reviews as read for stall {}: {}", stallId, e.getMessage());
        return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
    }
}

    @PostMapping("/seller/reviews/{reviewId}/reply")
    public String replyToReview(@PathVariable Long reviewId,
                                @RequestParam String sellerReply,
                                RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        // Get seller's shop for validation
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy shop của bạn!");
            return "redirect:/seller/reviews";
        }

        try {
            var reviewOptional = reviewRepository.findById(reviewId);
            if (reviewOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đánh giá!");
                return "redirect:/seller/reviews";
            }

            var review = reviewOptional.get();
            
            // SECURITY: Check if seller owns this review AND it belongs to their shop
            if (!review.getSellerId().equals(user.getId()) || !review.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền trả lời đánh giá này!");
                return "redirect:/seller/reviews";
            }

            // Update seller reply
            review.setReplyContent(sellerReply);
            review.setReplyAt(java.time.LocalDateTime.now());
            reviewRepository.save(review);

            redirectAttributes.addFlashAttribute("successMessage", "Đã trả lời đánh giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi trả lời đánh giá. Vui lòng thử lại!");
        }

        return "redirect:/seller/reviews";
    }

}
