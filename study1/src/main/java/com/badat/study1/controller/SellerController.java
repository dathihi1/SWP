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
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

@Controller
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping("/seller/register")
    public String submitSellerRegistration(@RequestParam("ownerName") String ownerName,
                                           @RequestParam("shopOwnerName") String shopOwnerName,
                                           @RequestParam("cccdFront") MultipartFile cccdFront,
                                           @RequestParam("cccdBack") MultipartFile cccdBack,
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

        // Validate tên shop
        if (ownerName == null || ownerName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("submitError", "Vui lòng nhập tên shop.");
            return "redirect:/seller/register";
        }
        String ownerNameTrim = ownerName.trim();

        // Validate tên chủ shop
        if (shopOwnerName == null || shopOwnerName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("submitError", "Vui lòng nhập tên chủ shop.");
            return "redirect:/seller/register";
        }
        String shopOwnerNameTrim = shopOwnerName.trim();

        // Validate ảnh CCCD mặt trước
        if (cccdFront == null || cccdFront.isEmpty()) {
            redirectAttributes.addFlashAttribute("submitError", "Vui lòng upload ảnh CCCD mặt trước.");
            return "redirect:/seller/register";
        }
        String frontFileName = cccdFront.getOriginalFilename();
        if (frontFileName == null || frontFileName.isEmpty()) {
            redirectAttributes.addFlashAttribute("submitError", "Vui lòng upload ảnh CCCD mặt trước.");
            return "redirect:/seller/register";
        }
        // Kiểm tra file có phải là ảnh không
        String frontContentType = cccdFront.getContentType();
        if (frontContentType == null || !frontContentType.startsWith("image/")) {
            redirectAttributes.addFlashAttribute("submitError", "File CCCD mặt trước phải là file ảnh.");
            return "redirect:/seller/register";
        }

        // Validate ảnh CCCD mặt sau
        if (cccdBack == null || cccdBack.isEmpty()) {
            redirectAttributes.addFlashAttribute("submitError", "Vui lòng upload ảnh CCCD mặt sau.");
            return "redirect:/seller/register";
        }
        String backFileName = cccdBack.getOriginalFilename();
        if (backFileName == null || backFileName.isEmpty()) {
            redirectAttributes.addFlashAttribute("submitError", "Vui lòng upload ảnh CCCD mặt sau.");
            return "redirect:/seller/register";
        }
        // Kiểm tra file có phải là ảnh không
        String backContentType = cccdBack.getContentType();
        if (backContentType == null || !backContentType.startsWith("image/")) {
            redirectAttributes.addFlashAttribute("submitError", "File CCCD mặt sau phải là file ảnh.");
            return "redirect:/seller/register";
        }

        // Validate đồng ý điều khoản
        if (agree == null || !"on".equals(agree)) {
            redirectAttributes.addFlashAttribute("submitError", "Bạn phải đồng ý với Điều khoản & Chính sách để tiếp tục.");
            return "redirect:/seller/register";
        }

        // Prepare header bar data
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("userRole", user.getRole().name());

        return sellerService.submitSellerRegistration(ownerNameTrim, shopOwnerNameTrim, cccdFront, cccdBack, agree, user, redirectAttributes);
    }

    @GetMapping("/seller/register")
    public String sellerRegisterPage(Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName());

        if (!isAuthenticated) {
            return "redirect:/login?required=1";
        }

        if (authentication == null) {
            return "redirect:/login?required=1";
        }

        User user = (User) authentication.getPrincipal();
        model.addAttribute("authorities", authentication.getAuthorities());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("walletBalance", BigDecimal.ZERO);
        return sellerService.getSellerRegisterPage(user, model, redirectAttributes);
    }
}


