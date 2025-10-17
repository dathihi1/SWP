package com.badat.study1.admin.controller;

import com.badat.study1.admin.service.AdminWithdrawService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/withdraws")
public class AdminWithdrawController {

    private final AdminWithdrawService withdrawService;

    public AdminWithdrawController(AdminWithdrawService withdrawService) {
        this.withdrawService = withdrawService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("withdraws", withdrawService.listAll());
        return "admin/withdraws";
    }

    @PostMapping("/{id}/status")
    public String setStatus(@PathVariable Long id,
                            @RequestParam String status,
                            @RequestParam(required = false) String note) {
        withdrawService.setStatus(id, status, note);
        return "redirect:/admin/withdraws";
    }
}
