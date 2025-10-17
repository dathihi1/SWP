package com.badat.study1.controller;

import com.badat.study1.model.Shop;
import com.badat.study1.model.Stall;
import com.badat.study1.model.User;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;

@Slf4j
@Controller
public class ShopController {
    private final ShopRepository shopRepository;
    private final StallRepository stallRepository;

    public ShopController(ShopRepository shopRepository, StallRepository stallRepository) {
        this.shopRepository = shopRepository;
        this.stallRepository = stallRepository;
    }

    @PostMapping("/seller/add-stall")
    public String addStall(@RequestParam String stallName,
                          @RequestParam String businessType,
                          @RequestParam String stallCategory,
                          @RequestParam Double discount,
                          @RequestParam String shortDescription,
                          @RequestParam String detailedDescription,
                          @RequestParam(required = false) String stallImageUrl,
                          @RequestParam(required = false) Boolean uniqueProducts,
                          RedirectAttributes redirectAttributes) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        
        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }
        
        // Validate unique products checkbox
        if (uniqueProducts == null || !uniqueProducts) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn phải đồng ý với cam kết 'Sản phẩm không trùng lặp' để tạo gian hàng!");
            return "redirect:/seller/add-stall";
        }
        
        try {
            // Get user's shop
            Shop userShop = shopRepository.findByUserId(user.getId())
                    .orElse(null);
            
            if (userShop == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa có shop. Vui lòng tạo shop trước khi tạo gian hàng!");
                return "redirect:/seller/shop-management";
            }
            
            // Check if user already has maximum number of stalls (5)
            long currentStallCount = stallRepository.countByShopIdAndIsDeleteFalse(userShop.getId());
            if (currentStallCount >= 5) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "Bạn đã đạt giới hạn tối đa 5 gian hàng. Không thể tạo thêm gian hàng mới!");
                return "redirect:/seller/shop-management";
            }
            
            // Create new stall
            Stall stall = new Stall();
            stall.setShopId(userShop.getId());
            stall.setStallName(stallName);
            stall.setBusinessType(businessType);
            stall.setStallCategory(stallCategory);
            stall.setDiscountPercentage(discount);
            stall.setShortDescription(shortDescription);
            stall.setDetailedDescription(detailedDescription);
            stall.setStallImageUrl(stallImageUrl);
            stall.setStatus("OPEN");
            stall.setCreatedAt(Instant.now());
            stall.setDelete(false);
            
            // Save to database
            stallRepository.save(stall);
            
            log.info("Stall created successfully for user {}: {}", user.getUsername(), stallName);
            redirectAttributes.addFlashAttribute("successMessage", "Gian hàng đã được tạo thành công và đang chờ duyệt!");
            
        } catch (Exception e) {
            log.error("Error creating stall for user {}: {}", user.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tạo gian hàng. Vui lòng thử lại!");
        }
        
        return "redirect:/seller/shop-management";
    }
}
