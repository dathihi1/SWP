package com.badat.study1.controller;

import com.badat.study1.model.BankAccount;
import com.badat.study1.model.Shop;
import com.badat.study1.model.User;
import com.badat.study1.repository.BankAccountRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.WalletRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

@Controller
public class SellerController {

    private final ShopRepository shopRepository;
    private final BankAccountRepository bankAccountRepository;
    private final WalletRepository walletRepository;

    public SellerController(ShopRepository shopRepository,
                            BankAccountRepository bankAccountRepository,
                            WalletRepository walletRepository) {
        this.shopRepository = shopRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.walletRepository = walletRepository;
    }

    @PostMapping("/seller/register")
    public String submitSellerRegistration(@RequestParam("ownerName") String ownerName,
                                           @RequestParam("identity") String identity,
                                           @RequestParam("bankAccountNumber") String bankAccountNumber,
                                           @RequestParam("bankName") String bankName,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName());
        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        // Prepare header bar data
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("walletBalance", walletRepository.findByUserId(user.getId()).map(w -> w.getBalance()).orElse(null));

        // If shop already exists, just show success info-like state
        if (shopRepository.findByUserId(user.getId()).isPresent()) {
            redirectAttributes.addFlashAttribute("submitSuccess", true);
            return "redirect:/seller/register";
        }

        // Check duplicate CCCD across shops
        if (shopRepository.findByCccd(identity.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("submitError", "Số CCCD đã tồn tại.");
            return "redirect:/seller/register";
        }

        // Build combined bank_account string: "<number> <name>"
        String combined = (bankAccountNumber == null ? "" : bankAccountNumber.trim()) + " " + (bankName == null ? "" : bankName.trim());
        // Check duplicate only on full combined value
        if (bankAccountRepository.findByBankAccount(combined).isPresent()) {
            redirectAttributes.addFlashAttribute("submitError", "Số tài khoản đã tồn tại.");
            return "redirect:/seller/register";
        }

        // Save bank account
        BankAccount bankAccount = new BankAccount();
        bankAccount.setUserId(user.getId());
        bankAccount.setAccountNumber(bankAccountNumber.trim());
        bankAccount.setBankName((bankName == null || bankName.isBlank()) ? "UNKNOWN" : bankName.trim());
        bankAccount.setBankAccount(combined);
        bankAccount.setCreatedAt(Instant.now());
        bankAccount.setDelete(false);
        try {
            bankAccount = bankAccountRepository.save(bankAccount);
        } catch (DataIntegrityViolationException ex) {
            // Fallback: DB unique constraint (account_number) may still reject
            redirectAttributes.addFlashAttribute("submitError", "Số tài khoản đã tồn tại.");
            return "redirect:/seller/register";
        }

        // Save shop
        Shop shop = new Shop();
        shop.setUserId(user.getId());
        // Use ownerName as default shop name
        shop.setShopName(ownerName);
        shop.setCccd(identity);
        shop.setBankAccountId(bankAccount.getId());
        // set required not-null fields per DB
        shop.setCreatedAtLower(Instant.now());
        shop.setDelete(false);
        shopRepository.save(shop);

        // Render register page with success notice
        redirectAttributes.addFlashAttribute("submitSuccess", true);
        return "redirect:/seller/register";
    }
}


