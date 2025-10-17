package com.badat.study1.admin.controller;

import com.badat.study1.admin.service.AdminModerationService;
import com.badat.study1.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final ProductRepository productRepository;
    private final AdminModerationService moderationService;

    public AdminProductController(ProductRepository productRepository,
                                  AdminModerationService moderationService) {
        this.productRepository = productRepository;
        this.moderationService = moderationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "admin/products";
    }

    @PostMapping("/{id}/hide")
    public String hide(@PathVariable Long id, @RequestParam boolean hidden) {
        moderationService.hideProduct(id, hidden);
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        moderationService.deleteProduct(id);
        return "redirect:/admin/products";
    }
}
