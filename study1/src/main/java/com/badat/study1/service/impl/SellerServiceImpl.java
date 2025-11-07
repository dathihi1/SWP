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
                                           String shortDescription,
                                           MultipartFile cccdFront,
                                           MultipartFile cccdBack,
                                           String agree,
                                           User user,
                                           RedirectAttributes redirectAttributes) {
        // Validate tên shop phải duy nhất (case-insensitive, chỉ tính shop chưa bị xóa)
        String trimmedShopName = ownerName.trim();
        Optional<Shop> existingShop = shopRepository.findByShopNameIgnoreCaseAndIsDelete(trimmedShopName, false);
        if (existingShop.isPresent() && !existingShop.get().getUserId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("submitError", "Tên shop đã tồn tại. Vui lòng chọn tên khác.");
            return "redirect:/seller/register";
        }

        try {
        // Check if user already has a shop (for resubmission)
        Optional<Shop> existingUserShop = shopRepository.findByUserId(user.getId());
        Shop shop;
        if (existingUserShop.isPresent()) {
            // Update existing shop
            shop = existingUserShop.get();
            shop.setShopName(ownerName);
            shop.setShortDescription(shortDescription);
            shop.setStatus(Shop.Status.PENDING); // Reset to PENDING when resubmitting
            shop.setRejectionReason(null); // Clear rejection reason when resubmitting
        } else {
            // Create new shop
            shop = new Shop();
            shop.setUserId(user.getId());
            shop.setShopName(ownerName);
            shop.setShortDescription(shortDescription);
            shop.setStatus(Shop.Status.PENDING);
            shop.setCreatedAt(Instant.now());
            shop.setIsDelete(false);
        }
            
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
            
        shop.setUpdatedAt(Instant.now());
            
            // Save shop with image data to database
            Shop savedShop = shopRepository.save(shop);
            log.info("Shop registration saved successfully. Shop ID: {}, User ID: {}, Status: PENDING (waiting for approval)", 
                    savedShop.getId(), user.getId());

            // Do NOT automatically add SELLER role - wait for admin approval
        redirectAttributes.addFlashAttribute("submitSuccess", true);
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
        Optional<Shop> userShop = shopRepository.findByUserId(user.getId());
        boolean hasShop = userShop.isPresent();
        
        // Check shop status
        boolean isRejected = false;
        boolean isPending = false;
        Shop shop = null;
        
        if (hasShop && userShop.isPresent()) {
            shop = userShop.get();
            isPending = shop.getStatus() == Shop.Status.PENDING;
            isRejected = shop.getStatus() == Shop.Status.INACTIVE;
            model.addAttribute("shop", shop); // Add shop info for history view
        }
        
        // If shop is INACTIVE (rejected), allow re-registration (don't treat as alreadySeller)
        // Only treat as alreadySeller if user has SELLER role OR shop is ACTIVE
        boolean alreadySeller = isSeller || (hasShop && shop != null && shop.getStatus() == Shop.Status.ACTIVE);
        
        model.addAttribute("alreadySeller", alreadySeller);
        model.addAttribute("isPendingApproval", isPending);
        model.addAttribute("isRejected", isRejected);

        // Chỉ thiết lập submitSuccess mặc định khi đã có shop/SELLER và không bị từ chối
        if (alreadySeller && !model.containsAttribute("submitSuccess")) {
            model.addAttribute("submitSuccess", false);
        }

        return "seller/register";
    }
}


