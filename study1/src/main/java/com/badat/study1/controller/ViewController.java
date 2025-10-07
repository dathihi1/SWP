package com.badat.study1.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String homePage() {
        return "guest/home";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "customer/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "guest/register";
    }

    @GetMapping("/seller/register")
    public String sellerRegisterPage() {
        return "seller/register";
    }

    @GetMapping("/terms")
    public String termsPage() {
        return "customer/terms";
    }

    @GetMapping("/faqs")
    public String faqsPage() {
        return "customer/faqs";
    }
}


