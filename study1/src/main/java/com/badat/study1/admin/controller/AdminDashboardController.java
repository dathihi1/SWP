package com.badat.study1.admin.controller;

import com.badat.study1.admin.dto.NotificationRequest;
import com.badat.study1.admin.dto.StatsDTO;
import com.badat.study1.admin.service.AdminDashboardService;
import com.badat.study1.admin.service.AdminNotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final AdminNotificationService notificationService;

    public AdminDashboardController(AdminDashboardService dashboardService,
                                    AdminNotificationService notificationService) {
        this.dashboardService = dashboardService;
        this.notificationService = notificationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        StatsDTO stats = dashboardService.getStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    @PostMapping("/notify")
    public String notify(@ModelAttribute NotificationRequest req) {
        switch ((req.getChannel() == null ? "EMAIL" : req.getChannel()).toUpperCase()) {
            case "SMS" -> notificationService.sendSms(req);
            case "INAPP" -> notificationService.sendInApp(req);
            default -> notificationService.sendEmail(req);
        }
        return "redirect:/admin/dashboard?notified";
    }
}
