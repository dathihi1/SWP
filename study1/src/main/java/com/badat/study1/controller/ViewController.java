package com.badat.study1.controller;

import com.badat.study1.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ViewController {

    @GetMapping("/")
    public String homePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        model.addAttribute("isAuthenticated", isAuthenticated);
        if (isAuthenticated) {
            User user = (User) authentication.getPrincipal();
            // Sử dụng getRealUsername() để hiển thị tên đăng nhập, không phải email
            model.addAttribute("username", user.getRealUsername());
            model.addAttribute("userRole", user.getRole().name());
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

    @GetMapping("/verify")
    public String verifyPage(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "verify"; // Trả về file verify.html
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


