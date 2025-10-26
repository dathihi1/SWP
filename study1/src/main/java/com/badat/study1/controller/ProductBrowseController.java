package com.badat.study1.controller;

import com.badat.study1.model.Product;
import com.badat.study1.model.Review;
import com.badat.study1.model.Shop;
import com.badat.study1.model.Stall;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ReviewRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductBrowseController {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;
    private final StallRepository stallRepository;
    private final WalletRepository walletRepository;
    private final WarehouseRepository warehouseRepository;

    @GetMapping("/products")
    public String listProducts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "shop", required = false) String shopName,
            Model model) {
        List<Stall> stalls;
        if (query != null && !query.isBlank()) {
            stalls = stallRepository.findByStallNameContainingIgnoreCaseAndIsDeleteFalseAndStatus(
                    query.trim(), "OPEN");
        } else {
            stalls = stallRepository.findByStatusAndIsDeleteFalse("OPEN");
        }
        
        // Optional in-memory filters to avoid adding more repo methods
        if (type != null && !type.isBlank()) {
            String filterType = type.trim();
            stalls = stalls.stream()
                    .filter(s -> filterType.equalsIgnoreCase(s.getStallCategory()))
                    .toList();
        }
        // Get shop information for all stalls
        Map<Long, Shop> shopMap = stalls.stream()
                .map(Stall::getShopId)
                .distinct()
                .collect(Collectors.toMap(
                        shopId -> shopId,
                        shopId -> shopRepository.findById(shopId).orElse(null)
                ));

        if (shopName != null && !shopName.isBlank()) {
            String filterShop = shopName.trim().toLowerCase();
            stalls = stalls.stream()
                    .filter(s -> {
                        Shop shop = shopMap.get(s.getShopId());
                        return shop != null && shop.getShopName() != null
                                && shop.getShopName().toLowerCase().contains(filterShop);
                    })
                    .toList();
        }

        // Compute average rating per stall (0 if none)
        Map<Long, Double> stallRatings = stalls.stream().collect(Collectors.toMap(
                Stall::getId,
                s -> {
                    Long stallId = s.getId();
                    List<Review> reviews = reviewRepository.findByStallIdAndIsDeleteFalse(stallId);
                    return reviews.stream()
                            .map(Review::getRating)
                            .filter(r -> r != null && r > 0)
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0.0);
                }
        ));

        // Compute product count per stall from warehouse (available items only)
        Map<Long, Integer> productCounts = stalls.stream().collect(Collectors.toMap(
                Stall::getId,
                s -> {
                    Long stallId = s.getId();
                    long warehouseCount = warehouseRepository.countAvailableItemsByStallId(stallId);
                    return (int) warehouseCount;
                }
        ));
        // Add authentication attributes
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                                !authentication.getName().equals("anonymousUser");

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("walletBalance", BigDecimal.ZERO); // Default value

        if (isAuthenticated) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                model.addAttribute("username", user.getUsername());
                model.addAttribute("userRole", user.getRole().name());

                BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                        .map(Wallet::getBalance)
                        .orElse(BigDecimal.ZERO);
                model.addAttribute("walletBalance", walletBalance);
            } else {
                model.addAttribute("username", authentication.getName());
                model.addAttribute("userRole", "USER");
            }
        }

        model.addAttribute("stalls", stalls);
        model.addAttribute("shopMap", shopMap);
        model.addAttribute("productCounts", productCounts);
        model.addAttribute("q", query);
        model.addAttribute("type", type);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("shop", shopName);
        model.addAttribute("stallRatings", stallRatings);
        return "products/list";
    }

    @GetMapping({"/products/{id}", "/product/{id}"})
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .filter(p -> Boolean.FALSE.equals(p.getIsDelete()) && p.getStatus() == Product.Status.AVAILABLE)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        List<Review> reviews = reviewRepository.findByProductIdAndIsDeleteFalse(id);
        double avgRating = reviews.stream()
                .map(Review::getRating)
                .filter(r -> r != null && r > 0)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        
        // Add authentication attributes
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                                !authentication.getName().equals("anonymousUser");

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("walletBalance", BigDecimal.ZERO); // Default value

        if (isAuthenticated) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                model.addAttribute("username", user.getUsername());
                model.addAttribute("userRole", user.getRole().name());

                BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                        .map(Wallet::getBalance)
                        .orElse(BigDecimal.ZERO);
                model.addAttribute("walletBalance", walletBalance);
            } else {
                model.addAttribute("username", authentication.getName());
                model.addAttribute("userRole", "USER");
            }
        }
        
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        return "products/detail";
    }

    @GetMapping("/stall/{id}")
    public String stallDetail(@PathVariable Long id, Model model) {
        // Get stall information
        Stall stall = stallRepository.findById(id)
                .filter(s -> !s.isDelete() && "OPEN".equals(s.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Stall not found or not available"));

        // Get products in this stall
        List<Product> products = productRepository.findByStallIdAndIsDeleteFalse(id)
                .stream()
                .filter(p -> p.getStatus() == Product.Status.AVAILABLE)
                .toList();

        // Get shop information
        Shop shop = shopRepository.findById(stall.getShopId()).orElse(null);

        // Get stall reviews and rating
        List<Review> reviews = reviewRepository.findByStallIdAndIsDeleteFalse(id);
        double avgRating = reviews.stream()
                .map(Review::getRating)
                .filter(r -> r != null && r > 0)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        // Add authentication attributes
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                                !authentication.getName().equals("anonymousUser");

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("walletBalance", BigDecimal.ZERO); // Default value

        if (isAuthenticated) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                model.addAttribute("username", user.getUsername());
                model.addAttribute("userRole", user.getRole().name());

                BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                        .map(Wallet::getBalance)
                        .orElse(BigDecimal.ZERO);
                model.addAttribute("walletBalance", walletBalance);
            } else {
                model.addAttribute("username", authentication.getName());
                model.addAttribute("userRole", "USER");
            }
        }

        model.addAttribute("stall", stall);
        model.addAttribute("products", products);
        model.addAttribute("shop", shop);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        return "stall/detail";
    }
}


