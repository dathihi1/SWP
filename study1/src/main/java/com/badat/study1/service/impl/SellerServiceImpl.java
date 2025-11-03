package com.badat.study1.service.impl;

import com.badat.study1.model.Shop;
import com.badat.study1.model.User;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.service.SellerService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import java.time.Instant;

@Service
public class SellerServiceImpl implements SellerService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    public SellerServiceImpl(ShopRepository shopRepository,
                             UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String submitSellerRegistration(String ownerName,
                                           String identity,
                                           String bankAccountName,
                                           String agree,
                                           User user,
                                           RedirectAttributes redirectAttributes) {
        if (shopRepository.findByShopName(ownerName.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("submitError", "Tên shop đã tồn tại. Vui lòng chọn tên khác.");
            return "redirect:/seller/register";
        }

        if (shopRepository.findByUserId(user.getId()).isPresent()) {
            if (user.getRole() != User.Role.SELLER) {
                user.setRole(User.Role.SELLER);
                userRepository.save(user);
            }
            redirectAttributes.addFlashAttribute("submitSuccess", false);
            return "redirect:/seller/register";
        }

        if (shopRepository.findByCccd(identity.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("submitError", "Số CCCD đã tồn tại.");
            return "redirect:/seller/register";
        }

        Shop shop = new Shop();
        shop.setUserId(user.getId());
        shop.setShopName(ownerName);
        shop.setCccd(identity);
        shop.setBankAccountName(bankAccountName == null ? null : bankAccountName.trim());
        shop.setCreatedAt(Instant.now());
        shop.setIsDelete(false);
        shopRepository.save(shop);

        if (user.getRole() != User.Role.SELLER) {
            user.setRole(User.Role.SELLER);
            userRepository.save(user);
        }

        redirectAttributes.addFlashAttribute("submitSuccess", false);
        return "redirect:/seller/register";
    }

    @Override
    public String getSellerRegisterPage(User user, Model model, RedirectAttributes redirectAttributes) {

        model.addAttribute("username", user.getUsername());
        model.addAttribute("userRole", user.getRole().name());
        if (!model.containsAttribute("submitSuccess")) {
            model.addAttribute("submitSuccess", false);
        }

        boolean isSeller = user.getRole() == User.Role.SELLER;
        model.addAttribute("alreadySeller", isSeller);
        if (!model.containsAttribute("pendingReview")) {
            model.addAttribute("pendingReview", false);
        }

        return "seller/register";
    }
}


