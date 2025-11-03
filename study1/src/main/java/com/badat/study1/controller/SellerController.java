package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.service.SellerService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import java.math.BigDecimal;

@Controller
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
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

        if (authentication == null) {
            return "redirect:/login";
        }
        User user = (User) authentication.getPrincipal();

        boolean missingPhone = (user.getPhone() == null || user.getPhone().trim().isEmpty());
        boolean missingFullName = (user.getFullName() == null || user.getFullName().trim().isEmpty());
        if (missingPhone || missingFullName) {
            redirectAttributes.addFlashAttribute("infoRequired",
                    "Vui lòng cập nhật đầy đủ Họ và tên và Số điện thoại trước khi đăng ký bán hàng.");
            return "redirect:/profile";
        }

        String ownerNameTrim = ownerName == null ? "" : ownerName.trim();
        String identityTrim = identity == null ? "" : identity.trim();
        String bankAccountNameTrim = bankAccountName == null ? "" : bankAccountName.trim();

        if (agree == null || !"on".equals(agree)) {
            redirectAttributes.addFlashAttribute("submitError", "Bạn phải đồng ý với Điều khoản & Chính sách để tiếp tục.");
            return "redirect:/seller/register";
        }
        if (ownerNameTrim.isEmpty()) {
            redirectAttributes.addFlashAttribute("submitError", "Vui lòng nhập tên shop.");
            return "redirect:/seller/register";
        }
        if (identityTrim.isEmpty()) {
            redirectAttributes.addFlashAttribute("submitError", "Vui lòng nhập số CCCD.");
            return "redirect:/seller/register";
        }
        if (bankAccountNameTrim.isEmpty()) {
            redirectAttributes.addFlashAttribute("submitError", "Vui lòng nhập tên tài khoản ngân hàng.");
            return "redirect:/seller/register";
        }

        // Prepare header bar data
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("userRole", user.getRole().name());

        return sellerService.submitSellerRegistration(ownerNameTrim, identityTrim, bankAccountNameTrim, agree, user, redirectAttributes);
    }

    @GetMapping("/seller/register")
    public String sellerRegisterPage(Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName());

        if (!isAuthenticated) {
            return "redirect:/login?required=1";
        }

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("walletBalance", BigDecimal.ZERO);

        if (authentication == null) {
            return "redirect:/login?required=1";
        }
        User user = (User) authentication.getPrincipal();
        model.addAttribute("authorities", authentication.getAuthorities());
        return sellerService.getSellerRegisterPage(user, model, redirectAttributes);
    }
}


