package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.model.WalletHistory;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.service.WalletHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Controller
public class ViewController {
    private final WalletRepository walletRepository;
    private final WalletHistoryService walletHistoryService;

    public ViewController(WalletRepository walletRepository, WalletHistoryService walletHistoryService) {
        this.walletRepository = walletRepository;
        this.walletHistoryService = walletHistoryService;
    }

    // Inject common attributes (auth info and wallet balance) for all views
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !String.valueOf(authentication.getName()).equals("anonymousUser");

        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            User user = (User) authentication.getPrincipal();
            model.addAttribute("username", user.getUsername());
            model.addAttribute("userRole", user.getRole().name());

            BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                    .map(Wallet::getBalance)
                    .orElse(BigDecimal.ZERO);
            model.addAttribute("walletBalance", walletBalance);
        }
    }

    @GetMapping("/")
    public String homePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Home page - Authentication: {}, Name: {}", authentication, 
                 authentication != null ? authentication.getName() : "null");
        
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        log.debug("Is authenticated: {}", isAuthenticated);
        
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("walletBalance", BigDecimal.ZERO); // Default value
        
        if (isAuthenticated) {
            // Lấy User object từ authentication principal
            User user = (User) authentication.getPrincipal();
            log.debug("User: {}, Role: {}", user.getUsername(), user.getRole());
            
            model.addAttribute("username", user.getUsername());
            model.addAttribute("authorities", authentication.getAuthorities());
            model.addAttribute("userRole", user.getRole().name());
            
            // Lấy số dư ví
            BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                    .map(Wallet::getBalance)
                    .orElse(BigDecimal.ZERO);
            model.addAttribute("walletBalance", walletBalance);
        }
        
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

    @GetMapping("/seller/register")
    public String sellerRegisterPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        model.addAttribute("isAuthenticated", isAuthenticated);
        if (isAuthenticated) {
            User user = (User) authentication.getPrincipal();
            model.addAttribute("username", user.getUsername());
            model.addAttribute("authorities", authentication.getAuthorities());
        }
        
        return "seller/register";
    }

    @GetMapping("/terms")
    public String termsPage() {
        return "terms";
    }

    @GetMapping("/faqs")
    public String faqsPage() {
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
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        
        return "customer/profile";
    }

    @GetMapping("/seller/store")
    public String sellerStorePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        
        // Check if user has ADMIN role (for now, only ADMIN can access seller pages)
        if (!user.getRole().equals(User.Role.ADMIN)) {
            return "redirect:/profile";
        }
        
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        
        return "seller/store";
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
        if (!user.getRole().equals(User.Role.ADMIN)) {
            return "redirect:/profile";
        }
        
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        
        return "seller/products";
    }
}


