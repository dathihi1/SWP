package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Slf4j
@Controller
public class ViewController {
    private final WalletRepository walletRepository;

    public ViewController(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
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
    public String paymentHistoryPage(Model model) {
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


