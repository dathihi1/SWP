package com.badat.study1.service.impl;

import com.badat.study1.model.Shop;
import com.badat.study1.model.User;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.service.SellerService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.io.IOException;
import java.util.Optional;

@Service
@Slf4j
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
                                           String shopOwnerName,
                                           MultipartFile cccdFront,
                                           MultipartFile cccdBack,
                                           String agree,
                                           User user,
                                           RedirectAttributes redirectAttributes) {
        // Validate tên shop phải duy nhất (case-insensitive, chỉ tính shop chưa bị xóa)
        String trimmedShopName = ownerName.trim();
        Optional<Shop> existingShop = shopRepository.findByShopNameIgnoreCaseAndIsDelete(trimmedShopName, false);
        if (existingShop.isPresent()) {
            redirectAttributes.addFlashAttribute("submitError", "Tên shop đã tồn tại. Vui lòng chọn tên khác.");
            log.warn("Shop name already exists: {} for user: {}", trimmedShopName, user.getId());
            return "redirect:/seller/register";
        }

        try {
        Shop shop = new Shop();
        shop.setUserId(user.getId());
        shop.setShopName(ownerName);
            shop.setShopOwnerName(shopOwnerName);
            
            // Convert and store CCCD front image as byte array
            if (cccdFront != null && !cccdFront.isEmpty()) {
                byte[] frontImageBytes = cccdFront.getBytes();
                shop.setCccdFrontImage(frontImageBytes);
                log.info("CCCD front image converted to byte array. Size: {} bytes for user: {}", 
                        frontImageBytes.length, user.getId());
            }
            
            // Convert and store CCCD back image as byte array
            if (cccdBack != null && !cccdBack.isEmpty()) {
                byte[] backImageBytes = cccdBack.getBytes();
                shop.setCccdBackImage(backImageBytes);
                log.info("CCCD back image converted to byte array. Size: {} bytes for user: {}", 
                        backImageBytes.length, user.getId());
            }
            
        shop.setCreatedAt(Instant.now());
        shop.setIsDelete(false);
            
            // Save shop with image data to database
            Shop savedShop = shopRepository.save(shop);
            log.info("Shop registration saved successfully. Shop ID: {}, User ID: {}", 
                    savedShop.getId(), user.getId());

        if (shopRepository.findByUserId(user.getId()).isPresent()) {
            if (user.getRole() != User.Role.SELLER) {
                user.setRole(User.Role.SELLER);
                userRepository.save(user);
            }
        }
        redirectAttributes.addFlashAttribute("submitSuccess", false);
        return "redirect:/seller/register";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("submitError", "Có lỗi xảy ra khi xử lý ảnh CCCD: " + e.getMessage());
            return "redirect:/seller/register";
        }
    }

    @Override
    public String getSellerRegisterPage(User user, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("userRole", user.getRole().name());

        boolean isSeller = user.getRole() == User.Role.SELLER;
        boolean hasShop = shopRepository.findByUserId(user.getId()).isPresent();
        boolean alreadySeller = isSeller || hasShop;
        model.addAttribute("alreadySeller", alreadySeller);

        // Chỉ thiết lập submitSuccess mặc định khi đã có shop/SELLER
        if (alreadySeller && !model.containsAttribute("submitSuccess")) {
            model.addAttribute("submitSuccess", false);
        }

        return "seller/register";
    }
}


