package com.badat.study1.controller;

import com.badat.study1.model.Shop;
import com.badat.study1.model.User;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

@Controller
public class SellerController {

    private final ShopRepository shopRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public SellerController(ShopRepository shopRepository,
                            WalletRepository walletRepository,
                            UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/seller/register")
    public String submitSellerRegistration(@RequestParam("ownerName") String ownerName,
                                           @RequestParam("identity") String identity,
                                           @RequestParam("bankAccountName") String bankAccountName,
                                           @RequestParam(value = "agree", required = false) String agree,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName());
        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Prepare header bar data
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("walletBalance", walletRepository.findByUserId(user.getId()).map(w -> w.getBalance()).orElse(null));

        // Server-side validation for checkbox agreement
        if (agree == null || !agree.equals("on")) {
            redirectAttributes.addFlashAttribute("submitError", "Bạn phải đồng ý với Điều khoản & Chính sách để tiếp tục.");
            return "redirect:/seller/register";
        }

        // Server-side validation for shop name uniqueness
        if (shopRepository.findByShopName(ownerName.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("submitError", "Tên shop đã tồn tại. Vui lòng chọn tên khác.");
            return "redirect:/seller/register";
        }

        // If shop already exists, just show success info-like state
        if (shopRepository.findByUserId(user.getId()).isPresent()) {
            redirectAttributes.addFlashAttribute("submitSuccess", true);
            return "redirect:/seller/register";
        }

        // Check duplicate CCCD across shops
        if (shopRepository.findByCccd(identity.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("submitError", "Số CCCD đã tồn tại.");
            return "redirect:/seller/register";
        }

        // Save shop
        Shop shop = new Shop();
        shop.setUserId(user.getId());
        // Use ownerName as default shop name
        shop.setShopName(ownerName);
        shop.setCccd(identity);
        shop.setBankAccountName(bankAccountName == null ? null : bankAccountName.trim());
        // set required not-null fields per DB
        shop.setCreatedAt(Instant.now());
        shop.setIsDelete(false);
        shopRepository.save(shop);

        // Update user role to SELLER immediately
        if (user.getRole() != User.Role.SELLER) {
            user.setRole(User.Role.SELLER);
            userRepository.save(user);
        }

        // Render register page with success notice
        redirectAttributes.addFlashAttribute("submitSuccess", true);
        return "redirect:/seller/register";
    }
}


