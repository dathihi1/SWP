package com.badat.study1.admin.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class AdminAuthController {

    @GetMapping("/admin-login")
    public String adminLogin(Model model) {
        return "admin/login";
    }

    @GetMapping("/admin")
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }
}
