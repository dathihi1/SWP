package com.badat.study1.admin.controller;

import com.badat.study1.admin.service.AdminModerationService;
import com.badat.study1.repository.ShopRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/admin/shops")
public class AdminShopController {
    private final ShopRepository shopRepository;
    private final AdminModerationService moderationService;

    public AdminShopController(ShopRepository shopRepository,
                               AdminModerationService moderationService) {
        this.shopRepository = shopRepository;
        this.moderationService = moderationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("shops", shopRepository.findAll());
        return "admin/shops";
    }

    @PostMapping("/{id}/hide")
    public String hide(@PathVariable Long id, @RequestParam boolean hidden) {
        moderationService.hideShop(id, hidden);
        return "redirect:/admin/shops";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        moderationService.deleteShop(id);
        return "redirect:/admin/shops";
    }
}
