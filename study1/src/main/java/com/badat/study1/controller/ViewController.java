package com.badat.study1.controller;

import com.badat.study1.model.AuditLog;
import com.badat.study1.model.Stall;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.model.WalletHistory;
import com.badat.study1.model.Product;
import com.badat.study1.model.Review;
import com.badat.study1.model.ApiCallLog;
import com.badat.study1.model.UserActivityLog;
import com.badat.study1.repository.AuditLogRepository;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.UploadHistoryRepository;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.ReviewRepository;
import com.badat.study1.repository.OrderRepository;
import com.badat.study1.repository.ApiCallLogRepository;
import com.badat.study1.repository.UserActivityLogRepository;
import com.badat.study1.service.WalletHistoryService;
import com.badat.study1.service.AuditLogService;
import com.badat.study1.service.UserService;
import java.time.LocalDateTime;
import com.badat.study1.dto.response.AuditLogResponse;
import com.badat.study1.util.PaginationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
@RequiredArgsConstructor
public class ViewController {
    private final WalletRepository walletRepository;
    private final ShopRepository shopRepository;
    private final StallRepository stallRepository;
    private final ProductRepository productRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApiCallLogRepository apiCallLogRepository;
    private final UserActivityLogRepository userActivityLogRepository;

    private final WalletHistoryService walletHistoryService;
    private final AuditLogService auditLogService;
    private final UserService userService;

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
        } catch (Exception e) {
            log.error("Error loading stalls for homepage: {}", e.getMessage(), e);
            // Add empty stalls list to prevent template errors
            model.addAttribute("stalls", new ArrayList<>());
        }

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
        
        // Simple pagination validation - validate before creating PageRequest
        int validatedPage = page;
        if (page < 0) {
            validatedPage = 0;
        }
        
        // Lấy lịch sử upload gần nhất cho sản phẩm này với pagination (5 bản ghi mỗi trang)
        Pageable pageable = PageRequest.of(validatedPage, 5);
        Page<com.badat.study1.model.UploadHistory> uploadHistoryPage = uploadHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        
        // Check if page exceeds total pages
        if (validatedPage >= uploadHistoryPage.getTotalPages() && uploadHistoryPage.getTotalPages() > 0) {
            validatedPage = uploadHistoryPage.getTotalPages() - 1;
            pageable = PageRequest.of(validatedPage, 5);
            uploadHistoryPage = uploadHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        }
        
        model.addAttribute("recentUploads", uploadHistoryPage.getContent());
        model.addAttribute("currentPage", validatedPage);
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
        
        // Simple pagination validation - validate before creating PageRequest
        int validatedPage = page;
        if (page < 0) {
            validatedPage = 0;
        }
        
        // Get order items for this seller with pagination (10 items per page)
        Pageable pageable = PageRequest.of(validatedPage, 10);
        List<com.badat.study1.model.OrderItem> allOrderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(user.getId());
        
        // Apply filters
        List<com.badat.study1.model.OrderItem> filteredOrderItems = allOrderItems.stream()
                .filter(orderItem -> status == null || status.isEmpty() || orderItem.getStatus().name().equals(status.toUpperCase()))
                .filter(orderItem -> stallId == null || stallId.equals(orderItem.getWarehouse().getStall().getId()))
                .filter(orderItem -> productId == null || productId.equals(orderItem.getProductId()))
                .filter(orderItem -> {
                    if (dateFrom == null || dateFrom.isEmpty()) return true;
                    return orderItem.getCreatedAt().toLocalDate().isAfter(java.time.LocalDate.parse(dateFrom).minusDays(1));
                })
                .filter(orderItem -> {
                    if (dateTo == null || dateTo.isEmpty()) return true;
                    return orderItem.getCreatedAt().toLocalDate().isBefore(java.time.LocalDate.parse(dateTo).plusDays(1));
                })
                .collect(java.util.stream.Collectors.toList());
        
        // Calculate total pages for validation
        int totalPages = (filteredOrderItems.size() + pageable.getPageSize() - 1) / pageable.getPageSize();
        
        // Check if page exceeds total pages
        if (validatedPage >= totalPages && totalPages > 0) {
            validatedPage = totalPages - 1;
            pageable = PageRequest.of(validatedPage, 10);
        }
        
        // Create pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredOrderItems.size());
        List<com.badat.study1.model.OrderItem> pageContent = filteredOrderItems.subList(start, end);
        
        Page<com.badat.study1.model.OrderItem> orderItemsPage = new org.springframework.data.domain.PageImpl<>(
            pageContent, 
            pageable, 
            filteredOrderItems.size()
        );
        model.addAttribute("orders", orderItemsPage.getContent());
        model.addAttribute("currentPage", validatedPage);
        model.addAttribute("totalPages", orderItemsPage.getTotalPages());
        model.addAttribute("totalElements", orderItemsPage.getTotalElements());
        model.addAttribute("hasNext", orderItemsPage.hasNext());
        model.addAttribute("hasPrevious", orderItemsPage.hasPrevious());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedStallId", stallId);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedDateFrom", dateFrom);
        model.addAttribute("selectedDateTo", dateTo);
        model.addAttribute("orderStatuses", com.badat.study1.model.OrderItem.Status.values());
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

    @GetMapping("/admin/users")
    public String adminUsers(Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "25") int size,
                           @RequestParam(required = false) String search,
                           @RequestParam(required = false) String role,
                           @RequestParam(required = false) String status,
                           @RequestParam(defaultValue = "id") String sortBy,
                           @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return "redirect:/login";
            }

            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().name().equals("ADMIN")) {
                return "redirect:/";
            }

            // Create sort object
            Sort sort = Sort.by(sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // Get users with filters
            Page<User> usersPage = userService.getUsersWithFilters(search, role, status, pageable);

            model.addAttribute("users", usersPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", usersPage.getTotalPages());
            model.addAttribute("totalElements", usersPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("search", search);
            model.addAttribute("role", role);
            model.addAttribute("status", status);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("userRole", currentUser.getRole().name());

            return "admin/users";

        } catch (Exception e) {
            log.error("Error in adminUsers: {}", e.getMessage(), e);
            return "redirect:/";
        }
    }

    @GetMapping("/admin/users/{id}/detail")
    public String adminUserDetail(@PathVariable Long id, Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "25") int size,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String dateFrom,
                                @RequestParam(required = false) String dateTo,
                                @RequestParam(required = false) BigDecimal minAmount) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return "redirect:/";
        }

        // Get user details
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return "redirect:/admin/users";
        }

        // Get user's wallet
        Wallet wallet = walletRepository.findByUserId(id).orElse(null);
        BigDecimal walletBalance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;

        // Get user's orders with filters
        // Pageable pageable = PageRequest.of(page, size); // Not used in mock implementation
        
        // Mock order data for now - you should implement real order filtering
        List<Map<String, Object>> orders = new ArrayList<>();
        
        // Apply filters to mock data
        if (status != null && !status.isEmpty()) {
            // Filter by status
            orders = orders.stream()
                .filter(order -> order.get("status").equals(status))
                .collect(java.util.stream.Collectors.toList());
        }
        
        if (dateFrom != null && !dateFrom.isEmpty()) {
            try {
                java.time.LocalDate fromDate = java.time.LocalDate.parse(dateFrom);
                orders = orders.stream()
                    .filter(order -> {
                        java.time.LocalDate orderDate = (java.time.LocalDate) order.get("orderDate");
                        return orderDate.isAfter(fromDate) || orderDate.isEqual(fromDate);
                    })
                    .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                log.warn("Invalid dateFrom format: {}", dateFrom);
            }
        }
        
        if (dateTo != null && !dateTo.isEmpty()) {
            try {
                java.time.LocalDate toDate = java.time.LocalDate.parse(dateTo);
                orders = orders.stream()
                    .filter(order -> {
                        java.time.LocalDate orderDate = (java.time.LocalDate) order.get("orderDate");
                        return orderDate.isBefore(toDate) || orderDate.isEqual(toDate);
                    })
                    .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                log.warn("Invalid dateTo format: {}", dateTo);
            }
        }
        
        if (minAmount != null) {
            orders = orders.stream()
                .filter(order -> {
                    BigDecimal orderAmount = (BigDecimal) order.get("totalAmount");
                    return orderAmount.compareTo(minAmount) >= 0;
                })
                .collect(java.util.stream.Collectors.toList());
        }
        
        // Mock data for demonstration - replace with actual order queries
        Map<String, Object> order1 = new HashMap<>();
        order1.put("id", 1L);
        order1.put("productName", "Gmail Premium 2024");
        order1.put("productType", "Email");
        order1.put("productImage", "/images/products/email.jpg");
        order1.put("quantity", 1);
        order1.put("totalPrice", new BigDecimal("50000"));
        order1.put("status", "COMPLETED");
        order1.put("createdAt", java.time.LocalDateTime.now().minusDays(5));
        orders.add(order1);

        Map<String, Object> order2 = new HashMap<>();
        order2.put("id", 2L);
        order2.put("productName", "Windows 11 Pro Key");
        order2.put("productType", "Software");
        order2.put("productImage", "/images/products/software.jpg");
        order2.put("quantity", 1);
        order2.put("totalPrice", new BigDecimal("200000"));
        order2.put("status", "PENDING");
        order2.put("createdAt", java.time.LocalDateTime.now().minusDays(2));
        orders.add(order2);

        // Calculate stats
        BigDecimal totalSpent = orders.stream()
            .map(o -> (BigDecimal) o.get("totalPrice"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalOrders = orders.size();

        model.addAttribute("user", user);
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("userCreatedAt", user.getCreatedAt());
        model.addAttribute("orders", orders);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalItems", orders.size());
        model.addAttribute("totalPages", Math.max(1, (int) Math.ceil((double) orders.size() / size)));
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("userRole", currentUser.getRole().name());
        
        return "admin/user-detail";
    }

    @GetMapping("/admin/audit-logs")
    public String adminAuditLogs(Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "25") int size,
                               @RequestParam(required = false) String action,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String success,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return "redirect:/";
        }

        // Validate pagination parameters
        int validatedPage = PaginationValidator.validatePage(page);
        int validatedSize = PaginationValidator.validateSize(size);
        
        // Redirect if parameters were invalid
        if (validatedPage != page || validatedSize != size) {
            String redirectUrl = "/admin/audit-logs?page=" + validatedPage + "&size=" + validatedSize;
            if (action != null) redirectUrl += "&action=" + action;
            if (category != null) redirectUrl += "&category=" + category;
            if (success != null) redirectUrl += "&success=" + success;
            if (startDate != null) redirectUrl += "&startDate=" + startDate;
            if (endDate != null) redirectUrl += "&endDate=" + endDate;
            return "redirect:" + redirectUrl;
        }

        // Get audit logs with filters
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Parse date parameters
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                startDateTime = LocalDateTime.parse(startDate);
            } catch (Exception e) {
                log.warn("Invalid startDate format: {}", startDate);
            }
        }
        
        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                endDateTime = LocalDateTime.parse(endDate);
            } catch (Exception e) {
                log.warn("Invalid endDate format: {}", endDate);
            }
        }
        
        // Parse success parameter to Boolean
        Boolean successBoolean = null;
        if (success != null && !success.trim().isEmpty()) {
            try {
                successBoolean = Boolean.valueOf(success);
            } catch (Exception e) {
                log.warn("Invalid success format: {}", success);
            }
        }
        
        // Parse category to enum if provided
        AuditLog.Category categoryEnum = null;
        if (category != null && !category.trim().isEmpty()) {
            try {
                categoryEnum = AuditLog.Category.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid category format: {}", category);
            }
        }
        
        // Get audit logs with filters
        Page<AuditLog> auditLogsPage = auditLogRepository.findAdminAuditLogsWithFilters(
            action, category, categoryEnum, success, successBoolean, startDateTime, endDateTime, pageable);
        
        log.info("Audit logs query - Page: {}, Size: {}, Total elements: {}, Total pages: {}", 
                page, size, auditLogsPage.getTotalElements(), auditLogsPage.getTotalPages());
        
        // Get unique actions and categories for filter dropdowns
        List<String> actions = auditLogRepository.findDistinctActions();
        List<String> categories = auditLogRepository.findDistinctCategories();
        
        model.addAttribute("auditLogs", auditLogsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auditLogsPage.getTotalPages());
        model.addAttribute("totalElements", auditLogsPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("actions", actions);
        model.addAttribute("categories", categories);
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("userRole", currentUser.getRole().name());
        
        // Add filter parameters to model for pagination
        model.addAttribute("action", action);
        model.addAttribute("category", category);
        model.addAttribute("success", success);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        return "admin/audit-logs";
    }

    @GetMapping("/admin/audit-logs/export")
    public ResponseEntity<?> exportAuditLogs(@RequestParam(required = false) String action,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) Boolean success,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        try {
            // Get all audit logs (no pagination for export)
            List<AuditLog> auditLogs = auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            
            // Create CSV content
            StringBuilder csv = new StringBuilder();
            csv.append("ID,Thời gian,Hành động,Danh mục,Trạng thái,User ID,IP Address,Chi tiết\n");
            
            for (AuditLog log : auditLogs) {
                csv.append(String.format("%d,%s,%s,%s,%s,%s,%s,\"%s\"\n",
                    log.getId(),
                    log.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    log.getAction(),
                    log.getCategory().name(),
                    log.getSuccess() ? "Thành công" : "Thất bại",
                    log.getUserId() != null ? log.getUserId().toString() : "",
                    log.getIpAddress(),
                    log.getDetails().replace("\"", "\"\"") // Escape quotes
                ));
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "audit-logs-" + 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + ".csv");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString().getBytes("UTF-8"));
                
        } catch (Exception e) {
            log.error("Error exporting audit logs: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Clear the access token cookie
            boolean secure = request.isSecure();
            ResponseCookie clearCookie = ResponseCookie.from("accessToken", "")
                    .httpOnly(true)
                    .secure(secure)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(0)
                    .build();
            
            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
            
            // Clear Spring Security context
            SecurityContextHolder.clearContext();
            
            log.info("User logged out successfully");
            return "redirect:/login?logout=true";
            
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            return "redirect:/login?error=logout_failed";
        }
    }

    // ==================== ADMIN API LOGS & USER ACTIVITY LOGS ====================
    
    @GetMapping("/admin/api-logs")
    public String adminApiLogs(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) Long userId,
                              @RequestParam(required = false) String endpoint,
                              @RequestParam(required = false) String method,
                              @RequestParam(required = false) Integer statusCode,
                              @RequestParam(required = false) String fromDate,
                              @RequestParam(required = false) String toDate) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return "redirect:/?error=access_denied";
        }

        try {
            // Validate pagination parameters
            int validatedPage = PaginationValidator.validatePage(page);
            int validatedSize = PaginationValidator.validateSize(size);
            
            // Redirect if parameters were invalid
            if (validatedPage != page || validatedSize != size) {
                String redirectUrl = "/admin/api-logs?page=" + validatedPage + "&size=" + validatedSize;
                if (userId != null) redirectUrl += "&userId=" + userId;
                if (endpoint != null && !endpoint.trim().isEmpty()) redirectUrl += "&endpoint=" + endpoint;
                if (method != null && !method.trim().isEmpty()) redirectUrl += "&method=" + method;
                if (statusCode != null) redirectUrl += "&statusCode=" + statusCode;
                if (fromDate != null) redirectUrl += "&fromDate=" + fromDate;
                if (toDate != null) redirectUrl += "&toDate=" + toDate;
                return "redirect:" + redirectUrl;
            }
            
            // Parse dates
            LocalDateTime fromDateTime = null;
            LocalDateTime toDateTime = null;
            
            if (fromDate != null && !fromDate.trim().isEmpty()) {
                fromDateTime = LocalDateTime.parse(fromDate + " 00:00:00", 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            
            if (toDate != null && !toDate.trim().isEmpty()) {
                toDateTime = LocalDateTime.parse(toDate + " 23:59:59", 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            log.info("Admin API logs query - userId: {}, endpoint: '{}', method: '{}', statusCode: {}, fromDate: {}, toDate: {}, page: {}, size: {}", 
                userId, endpoint, method, statusCode, fromDateTime, toDateTime, page, size);
            
            // Normalize empty strings to null for query
            String normalizedEndpoint = (endpoint != null && !endpoint.trim().isEmpty()) ? endpoint : null;
            String normalizedMethod = (method != null && !method.trim().isEmpty()) ? method : null;
            
            // Get API logs with filters
            Page<com.badat.study1.model.ApiCallLog> apiLogs = apiCallLogRepository.findWithFilters(
                userId, normalizedEndpoint, normalizedMethod, statusCode, fromDateTime, toDateTime, pageable);
            
            log.info("Found {} API logs (total: {}, page: {}, totalPages: {})", 
                apiLogs.getNumberOfElements(), apiLogs.getTotalElements(), apiLogs.getNumber(), apiLogs.getTotalPages());
            
            // Debug: Try simple query to see if data exists
            long totalLogs = apiCallLogRepository.count();
            log.info("Total API logs in database: {}", totalLogs);
            
            // If page is beyond total pages, redirect to last page
            if (page >= apiLogs.getTotalPages() && apiLogs.getTotalPages() > 0) {
                log.warn("Requested page {} is beyond total pages {}, redirecting to page {}", 
                    page, apiLogs.getTotalPages(), apiLogs.getTotalPages() - 1);
                String redirectUrl = "/admin/api-logs?page=" + (apiLogs.getTotalPages() - 1) + "&size=" + size;
                if (userId != null) redirectUrl += "&userId=" + userId;
                if (endpoint != null && !endpoint.trim().isEmpty()) redirectUrl += "&endpoint=" + endpoint;
                if (method != null && !method.trim().isEmpty()) redirectUrl += "&method=" + method;
                if (statusCode != null) redirectUrl += "&statusCode=" + statusCode;
                if (fromDate != null) redirectUrl += "&fromDate=" + fromDate;
                if (toDate != null) redirectUrl += "&toDate=" + toDate;
                return "redirect:" + redirectUrl;
            }
            
            // Get statistics
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            Double avgResponseTime = apiCallLogRepository.findAverageResponseTime(weekAgo);
            Long errorCount = apiCallLogRepository.countErrorsSince(weekAgo);
            Long totalCalls = apiCallLogRepository.countTotalCallsSince(weekAgo);
            
            // Get distinct values for filter dropdowns
            List<String> endpoints = apiCallLogRepository.findDistinctEndpoints();
            List<String> methods = apiCallLogRepository.findDistinctMethods();
            List<Integer> statusCodes = apiCallLogRepository.findDistinctStatusCodes();
            
            model.addAttribute("apiLogs", apiLogs);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", apiLogs.getTotalPages());
            model.addAttribute("totalElements", apiLogs.getTotalElements());
            model.addAttribute("numberOfElements", apiLogs.getNumberOfElements());
            model.addAttribute("pageSize", size);
            
            // Filter values (use normalized values)
            model.addAttribute("selectedUserId", userId);
            model.addAttribute("selectedEndpoint", normalizedEndpoint);
            model.addAttribute("selectedMethod", normalizedMethod);
            model.addAttribute("selectedStatusCode", statusCode);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            
            // Filter options
            model.addAttribute("endpoints", endpoints);
            model.addAttribute("methods", methods);
            model.addAttribute("statusCodes", statusCodes);
            
            // Statistics
            model.addAttribute("avgResponseTime", avgResponseTime != null ? Math.round(avgResponseTime) : 0);
            model.addAttribute("errorCount", errorCount);
            model.addAttribute("totalCalls", totalCalls);
            model.addAttribute("errorRate", totalCalls > 0 ? Math.round((double) errorCount / totalCalls * 100) : 0);
            
            return "admin/api-logs";
            
        } catch (Exception e) {
            log.error("Error loading admin API logs: {}", e.getMessage(), e);
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu API logs");
            return "admin/api-logs";
        }
    }

    @GetMapping("/admin/user-activity-logs")
    public String adminUserActivityLogs(Model model,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) String action,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String fromDate,
                                       @RequestParam(required = false) String toDate) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getRole().equals(User.Role.ADMIN)) {
            return "redirect:/?error=access_denied";
        }

        try {
            // Validate pagination parameters
            int validatedPage = PaginationValidator.validatePage(page);
            int validatedSize = PaginationValidator.validateSize(size);
            
            // Redirect if parameters were invalid
            if (validatedPage != page || validatedSize != size) {
                String redirectUrl = "/admin/user-activity-logs?page=" + validatedPage + "&size=" + validatedSize;
                if (userId != null) redirectUrl += "&userId=" + userId;
                if (action != null) redirectUrl += "&action=" + action;
                if (category != null) redirectUrl += "&category=" + category;
                if (fromDate != null) redirectUrl += "&fromDate=" + fromDate;
                if (toDate != null) redirectUrl += "&toDate=" + toDate;
                return "redirect:" + redirectUrl;
            }
            
            // Parse dates
            LocalDateTime fromDateTime = null;
            LocalDateTime toDateTime = null;
            
            if (fromDate != null && !fromDate.trim().isEmpty()) {
                fromDateTime = LocalDateTime.parse(fromDate + " 00:00:00", 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            
            if (toDate != null && !toDate.trim().isEmpty()) {
                toDateTime = LocalDateTime.parse(toDate + " 23:59:59", 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            // Get user activity logs with filters
            UserActivityLog.Category categoryEnum = null;
            if (category != null && !category.isEmpty()) {
                try {
                    categoryEnum = UserActivityLog.Category.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid category: {}", category);
                }
            }
            
            log.info("Admin user activity logs query - userId: {}, action: {}, category: {}, fromDate: {}, toDate: {}, page: {}, size: {}", 
                userId, action, category, fromDateTime, toDateTime, page, size);
            
            Page<UserActivityLog> userActivityLogs = userActivityLogRepository.findAdminViewWithFilters(
                userId, action, categoryEnum, fromDateTime, toDateTime, pageable);
            
            log.info("Found {} user activity logs (total: {}, page: {}, totalPages: {})", 
                userActivityLogs.getNumberOfElements(), userActivityLogs.getTotalElements(), userActivityLogs.getNumber(), userActivityLogs.getTotalPages());
            
            // If page is beyond total pages, redirect to last page
            if (page >= userActivityLogs.getTotalPages() && userActivityLogs.getTotalPages() > 0) {
                log.warn("Requested page {} is beyond total pages {}, redirecting to page {}", 
                    page, userActivityLogs.getTotalPages(), userActivityLogs.getTotalPages() - 1);
                String redirectUrl = "/admin/user-activity-logs?page=" + (userActivityLogs.getTotalPages() - 1) + "&size=" + size;
                if (userId != null) redirectUrl += "&userId=" + userId;
                if (action != null) redirectUrl += "&action=" + action;
                if (category != null) redirectUrl += "&category=" + category;
                if (fromDate != null) redirectUrl += "&fromDate=" + fromDate;
                if (toDate != null) redirectUrl += "&toDate=" + toDate;
                return "redirect:" + redirectUrl;
            }
            
            // Get distinct values for filter dropdowns
            List<String> actions = userActivityLogRepository.findDistinctActions();
            List<UserActivityLog.Category> categories = userActivityLogRepository.findDistinctCategories();
            
            // Convert categories to strings for template
            List<String> categoryStrings = categories.stream()
                .map(c -> c.name())
                .toList();
            
            model.addAttribute("userActivityLogs", userActivityLogs);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", userActivityLogs.getTotalPages());
            model.addAttribute("totalElements", userActivityLogs.getTotalElements());
            model.addAttribute("numberOfElements", userActivityLogs.getNumberOfElements());
            model.addAttribute("pageSize", size);
            
            // Filter values
            model.addAttribute("selectedUserId", userId);
            model.addAttribute("selectedAction", action);
            model.addAttribute("selectedCategory", category);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            
            // Filter options
            model.addAttribute("actions", actions);
            model.addAttribute("categories", categoryStrings);
            
            // Add user info
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("userRole", currentUser.getRole().name());
            model.addAttribute("user", currentUser);
            
            return "admin/user-activity-logs";
            
        } catch (Exception e) {
            log.error("Error loading admin user activity logs: {}", e.getMessage(), e);
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu user activity logs");
            return "admin/user-activity-logs";
        }
    }
}
