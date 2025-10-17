package com.badat.study1.admin.controller;

import com.badat.study1.admin.service.AdminDisputeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/disputes")
public class AdminDisputeController {
    private final AdminDisputeService disputeService;

    public AdminDisputeController(AdminDisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("disputes", disputeService.listAll());
        return "admin/disputes";
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id,
                          @RequestParam String resolution,
                          @RequestParam(required = false) String note) {
        disputeService.resolve(id, resolution, note);
        return "redirect:/admin/disputes";
    }
}
