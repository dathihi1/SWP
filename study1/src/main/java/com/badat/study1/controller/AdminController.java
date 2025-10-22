package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.WithdrawRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {
    
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;
    
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
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStalls", totalStalls);
        model.addAttribute("pendingWithdrawals", pendingWithdrawals);
        
        // Get recent activities (placeholder for now)
        model.addAttribute("recentActivities", java.util.Collections.emptyList());
        
        return "admin/dashboard";
    }
    
    @GetMapping("/admin/users")
    public String adminUsers(Model model) {
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
        
        // Get all users
        model.addAttribute("users", userRepository.findAll());
        
        return "admin/users";
    }
    
    @GetMapping("/admin/stalls")
    public String adminStalls(Model model) {
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
        
        // Get all shops
        model.addAttribute("stalls", shopRepository.findAll());
        
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
