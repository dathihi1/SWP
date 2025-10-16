package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.model.WalletHistory;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.service.WalletHistoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ViewController {
    private final WalletRepository walletRepository;
    private final WalletHistoryService walletHistoryService;

    public ViewController(WalletRepository walletRepository, WalletHistoryService walletHistoryService) {
        this.walletRepository = walletRepository;
        this.walletHistoryService = walletHistoryService;
    }

    @GetMapping("/")
    public String homePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("walletBalance", BigDecimal.ZERO); // Default value
        
        if (isAuthenticated) {
            // Lấy User object từ authentication principal
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

    @GetMapping("/seller/register")
    public String sellerRegisterPage(Model model) {
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
        
        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);
        
        // Lấy wallet history của user
        List<WalletHistory> allWalletHistory = walletRepository.findByUserId(user.getId())
                .map(wallet -> walletHistoryService.getWalletHistoryByWalletId(wallet.getId()))
                .orElse(List.of());
        
        // Filter theo điều kiện
        List<WalletHistory> filteredHistory = allWalletHistory.stream()
                .filter(history -> {
                    // Filter theo ngày bắt đầu
                    if (fromDate != null && !fromDate.trim().isEmpty()) {
                        try {
                            LocalDate filterFromDate = LocalDate.parse(fromDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            if (history.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(filterFromDate)) {
                                return false;
                            }
                        } catch (Exception e) {
                            // Nếu format ngày không đúng, bỏ qua filter này
                        }
                    }
                    
                    // Filter theo ngày kết thúc
                    if (toDate != null && !toDate.trim().isEmpty()) {
                        try {
                            LocalDate filterToDate = LocalDate.parse(toDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            if (history.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(filterToDate)) {
                                return false;
                            }
                        } catch (Exception e) {
                            // Nếu format ngày không đúng, bỏ qua filter này
                        }
                    }
                    
                    // Filter theo loại giao dịch
                    if (transactionType != null && !transactionType.trim().isEmpty() && !transactionType.equals("ALL")) {
                        if (!history.getType().name().equals(transactionType)) {
                            return false;
                        }
                    }
                    
                    // Filter theo trạng thái
                    if (transactionStatus != null && !transactionStatus.trim().isEmpty() && !transactionStatus.equals("ALL")) {
                        if (!history.getStatus().name().equals(transactionStatus)) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        // Phân trang
        int pageSize = 5;
        int totalPages = (int) Math.ceil((double) filteredHistory.size() / pageSize);
        
        // Đảm bảo page hợp lệ
        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;
        
        // Tính toán offset
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, filteredHistory.size());
        
        // Lấy dữ liệu cho trang hiện tại
        List<WalletHistory> walletHistory = filteredHistory.subList(startIndex, endIndex);
        
        model.addAttribute("walletHistory", walletHistory);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasNextPage", page < totalPages);
        model.addAttribute("hasPrevPage", page > 1);
        model.addAttribute("nextPage", page + 1);
        model.addAttribute("prevPage", page - 1);
        
        // Thêm filter parameters để giữ lại giá trị trong form
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("transactionType", transactionType);
        model.addAttribute("transactionStatus", transactionStatus);
        
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
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        
        // Lấy số dư ví
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);
        
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
        
        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
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
        
        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }
        
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        
        return "seller/products";
    }
}


