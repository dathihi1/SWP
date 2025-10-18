package com.badat.study1.controller;

import com.badat.study1.model.Product;
import com.badat.study1.model.Review;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ReviewRepository;
import com.badat.study1.repository.WalletRepository;
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
    private final WalletRepository walletRepository;

    @GetMapping("/products")
    public String listProducts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "shop", required = false) String shopName,
            Model model) {
        List<Product> products;
        if (query != null && !query.isBlank()) {
            products = productRepository.findByNameContainingIgnoreCaseAndIsDeleteFalseAndStatus(
                    query.trim(), Product.Status.ACTIVE);
        } else {
            products = productRepository.findByIsDeleteFalseAndStatus(Product.Status.ACTIVE);
        }
        // Optional in-memory filters to avoid adding more repo methods
        if (type != null && !type.isBlank()) {
            String filterType = type.trim();
            products = products.stream()
                    .filter(p -> filterType.equalsIgnoreCase(p.getType()))
                    .toList();
        }
        if (minPrice != null) {
            products = products.stream()
                    .filter(p -> p.getPrice() != null && p.getPrice().compareTo(minPrice) >= 0)
                    .toList();
        }
        if (maxPrice != null) {
            products = products.stream()
                    .filter(p -> p.getPrice() != null && p.getPrice().compareTo(maxPrice) <= 0)
                    .toList();
        }
        if (shopName != null && !shopName.isBlank()) {
            String filterShop = shopName.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> p.getShop() != null && p.getShop().getShopName() != null
                            && p.getShop().getShopName().toLowerCase().contains(filterShop))
                    .toList();
        }

        // Compute average rating per product (0 if none)
        Map<Long, Double> productRatings = products.stream().collect(Collectors.toMap(
                Product::getId,
                p -> {
                    Long productId = p.getId();
                    List<Review> reviews = reviewRepository.findByProductIdAndIsDeleteFalse(productId);
                    return reviews.stream()
                            .map(Review::getRating)
                            .filter(r -> r != null && r > 0)
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0.0);
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

        model.addAttribute("products", products);
        model.addAttribute("q", query);
        model.addAttribute("type", type);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("shop", shopName);
        model.addAttribute("productRatings", productRatings);
        return "products/list";
    }

    @GetMapping({"/products/{id}", "/product/{id}"})
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .filter(p -> Boolean.FALSE.equals(p.getIsDelete()) && p.getStatus() == Product.Status.ACTIVE)
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
}


