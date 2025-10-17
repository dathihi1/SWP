package com.badat.study1.admin.controller;

import com.badat.study1.admin.service.AdminModerationService;
import com.badat.study1.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserRepository userRepository;
    private final AdminModerationService moderationService;

    public AdminUserController(UserRepository userRepository,
                               AdminModerationService moderationService) {
        this.userRepository = userRepository;
        this.moderationService = moderationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam boolean active) {
        moderationService.activateUser(id, active);
        return "redirect:/admin/users";
    }
}
