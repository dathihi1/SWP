package com.badat.study1.controller;

import com.badat.study1.model.Product;
import com.badat.study1.model.Review;
import com.badat.study1.model.Shop;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ReviewRepository;
import com.badat.study1.repository.ShopRepository;
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
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductBrowseController {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;
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
        // Get all shops
        List<Shop> shops = shopRepository.findAll();
        
        // Optional in-memory filters
        if (query != null && !query.isBlank()) {
            String filterQuery = query.trim().toLowerCase();
            shops = shops.stream()
                    .filter(s -> s.getShopName() != null && s.getShopName().toLowerCase().contains(filterQuery))
                    .toList();
        }

        if (type != null && !type.isBlank()) {
            String filterType = type.trim();
            shops = shops.stream()
                    .filter(s -> s.getDescription() != null && s.getDescription().toLowerCase().contains(filterType))
                    .toList();
        }

        if (shopName != null && !shopName.isBlank()) {
            String filterShop = shopName.trim().toLowerCase();
            shops = shops.stream()
                    .filter(s -> s.getShopName() != null && s.getShopName().toLowerCase().contains(filterShop))
                    .toList();
        }

        // Compute average rating per shop (0 if none)
        Map<Long, Double> shopRatings = shops.stream().collect(Collectors.toMap(
                Shop::getId,
                s -> {
                    Long shopId = s.getId();
                    List<Review> reviews = reviewRepository.findByShopIdAndIsDeleteFalse(shopId);
                    return reviews.stream()
                            .map(Review::getRating)
                            .filter(r -> r != null && r > 0)
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0.0);
                }
        ));

        // Compute product count per shop
        Map<Long, Integer> productCounts = shops.stream().collect(Collectors.toMap(
                Shop::getId,
                s -> {
                    Long shopId = s.getId();
                    List<Product> products = productRepository.findByShopIdAndIsDeleteFalse(shopId);
                    return products.size();
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

        model.addAttribute("shops", shops);
        model.addAttribute("productCounts", productCounts);
        model.addAttribute("q", query);
        model.addAttribute("type", type);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("shop", shopName);
        model.addAttribute("shopRatings", shopRatings);
        return "products/list";
    }



    @GetMapping("/shop/{shopId}/products")
    public String shopProducts(@PathVariable Long shopId, Model model) {
        try {
            // Get shop information
            Shop shop = shopRepository.findById(shopId)
                    .orElseThrow(() -> new IllegalArgumentException("Shop not found with ID: " + shopId));

            // Get all products from this shop
            List<Product> products = productRepository.findByShopIdAndIsDeleteFalse(shopId)
                    .stream()
                    .filter(p -> p.getStatus() == Product.Status.AVAILABLE)
                    .toList();
            
            // Log for debugging
            System.out.println("Shop ID: " + shopId + ", Shop Name: " + shop.getShopName() + ", Products Count: " + products.size());
        
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
        
            model.addAttribute("shop", shop);
            model.addAttribute("products", products);
            return "shop/products";
        } catch (Exception e) {
            // Log error and return error page
            System.err.println("Error loading shop products for shop ID " + shopId + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tải sản phẩm của shop: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/product/{productId}")
    public String productDetail(@PathVariable Long productId, Model model) {
        try {
            // Get product information
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Get shop information
            Shop shop = shopRepository.findById(product.getShopId())
                    .orElseThrow(() -> new IllegalArgumentException("Shop not found"));

            // Get product reviews and rating
            List<Review> reviews = reviewRepository.findByProductIdAndIsDeleteFalse(productId);
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
        model.addAttribute("shop", shop);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
            return "product/detail";
        } catch (Exception e) {
            // Log error and return error page
            System.err.println("Error in productDetail: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tải thông tin sản phẩm: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/api/products/{productId}/sub-products")
    @ResponseBody
    public List<Map<String, Object>> getSubProducts(@PathVariable Long productId) {
        try {
            System.out.println("=== DEBUG: Loading sub-products for product ID: " + productId + " ===");
            
            // Get main product
            Product mainProduct = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            
            System.out.println("Main product found: " + mainProduct.getName() + " (Type: " + mainProduct.getType() + ")");

            // Get warehouse items for this product
            List<Map<String, Object>> subProducts = new ArrayList<>();
            
            // Query warehouse items for this product using WarehouseRepository
            List<com.badat.study1.model.Warehouse> warehouseItems = warehouseRepository
                    .findByProductIdAndLockedFalseAndIsDeleteFalseOrderByCreatedAtAsc(productId, 
                            org.springframework.data.domain.PageRequest.of(0, 50));
            
            System.out.println("Found " + warehouseItems.size() + " warehouse items for product " + productId);
            
            if (!warehouseItems.isEmpty()) {
                // Return actual warehouse items as sub-products with dynamic names
                for (com.badat.study1.model.Warehouse warehouse : warehouseItems) {
                    System.out.println("Processing warehouse item ID: " + warehouse.getId() + ", Type: " + warehouse.getItemType());
                    
                    Map<String, Object> subProduct = new HashMap<>();
                    
                    // Generate dynamic name based on warehouse data
                    String dynamicName = generateSubProductName(warehouse, mainProduct);
                    System.out.println("Generated name: " + dynamicName);
                    
                    // Calculate dynamic price based on warehouse data
                    BigDecimal dynamicPrice = calculateSubProductPrice(warehouse, mainProduct);
                    System.out.println("Calculated price: " + dynamicPrice);
                    
                    // Get stock count for this warehouse item
                    long stockCount = getStockCountForWarehouseItem(warehouse.getId());
                    System.out.println("Stock count: " + stockCount);
                    
                    subProduct.put("id", warehouse.getId());
                    subProduct.put("name", dynamicName);
                    subProduct.put("description", "Sản phẩm từ kho - " + warehouse.getItemType());
                    subProduct.put("price", dynamicPrice);
                    subProduct.put("quantity", stockCount);
                    subProduct.put("warehouse_id", warehouse.getId());
                    subProduct.put("item_type", warehouse.getItemType().name());
                    subProduct.put("item_data", warehouse.getItemData());
                    subProduct.put("created_at", warehouse.getCreatedAt());
                    subProducts.add(subProduct);
                }
                System.out.println("Successfully processed " + subProducts.size() + " sub-products");
            } else {
                System.out.println("No warehouse items found for product " + productId);
                // If no warehouse items, show message
                Map<String, Object> noItems = new HashMap<>();
                noItems.put("id", 0);
                noItems.put("name", "Không có sản phẩm trong kho");
                noItems.put("description", "Sản phẩm này hiện không có trong kho");
                noItems.put("price", mainProduct.getPrice());
                noItems.put("quantity", 0);
                noItems.put("warehouse_id", 0);
                subProducts.add(noItems);
            }
            
            System.out.println("=== DEBUG: Returning " + subProducts.size() + " sub-products ===");
            return subProducts;
        } catch (Exception e) {
            // Return error message
            List<Map<String, Object>> errorList = new ArrayList<>();
            Map<String, Object> errorItem = new HashMap<>();
            errorItem.put("id", 0);
            errorItem.put("name", "Lỗi tải dữ liệu");
            errorItem.put("description", "Không thể tải sản phẩm con: " + e.getMessage());
            errorItem.put("price", 0);
            errorItem.put("quantity", 0);
            errorItem.put("warehouse_id", 0);
            errorList.add(errorItem);
            return errorList;
        }
    }
    
    private String generateSubProductName(com.badat.study1.model.Warehouse warehouse, Product mainProduct) {
        try {
            // Parse warehouse item data
            String itemData = warehouse.getItemData();
            if (itemData != null && !itemData.isEmpty()) {
                // Try to parse JSON data
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(itemData);
                    
                    String productName = mainProduct.getName();
                    String itemType = warehouse.getItemType().name();
                    
                    if (itemType.equals("EMAIL")) {
                        String email = jsonNode.has("email") ? jsonNode.get("email").asText() : "";
                        String storageUsed = jsonNode.has("storage_used") ? jsonNode.get("storage_used").asText() : "";
                        String storageTotal = jsonNode.has("storage_total") ? jsonNode.get("storage_total").asText() : "";
                        String createdDate = jsonNode.has("created_date") ? jsonNode.get("created_date").asText() : "";
                        
                        // Calculate age in months
                        String ageText = "";
                        if (createdDate != null && !createdDate.isEmpty()) {
                            try {
                                java.time.LocalDate created = java.time.LocalDate.parse(createdDate);
                                java.time.LocalDate now = java.time.LocalDate.now();
                                long months = java.time.temporal.ChronoUnit.MONTHS.between(created, now);
                                if (months <= 1) ageText = "1 tháng";
                                else if (months <= 3) ageText = "3 tháng";
                                else if (months <= 6) ageText = "6 tháng";
                                else if (months <= 12) ageText = "1 năm";
                                else ageText = "VIP";
                            } catch (Exception e) {
                                ageText = "3 tháng";
                            }
                        } else {
                            // Use age_months from item_data if available
                            String ageMonths = jsonNode.has("age_months") ? jsonNode.get("age_months").asText() : "";
                            if (!ageMonths.isEmpty()) {
                                try {
                                    int months = Integer.parseInt(ageMonths);
                                    if (months <= 1) ageText = "1 tháng";
                                    else if (months <= 3) ageText = "3 tháng";
                                    else if (months <= 6) ageText = "6 tháng";
                                    else if (months <= 12) ageText = "1 năm";
                                    else ageText = "VIP";
                                } catch (Exception e) {
                                    ageText = "3 tháng";
                                }
                            } else {
                                ageText = "3 tháng";
                            }
                        }
                        
                        // Add tier based on storage usage or tier from item_data
                        String tierText = "";
                        if (jsonNode.has("tier")) {
                            tierText = jsonNode.get("tier").asText();
                        } else if (storageUsed != null && storageTotal != null) {
                            try {
                                double used = Double.parseDouble(storageUsed.replace("GB", ""));
                                double total = Double.parseDouble(storageTotal.replace("GB", ""));
                                double percentage = (used / total) * 100;
                                
                                if (percentage < 30) tierText = "Pro";
                                else if (percentage < 60) tierText = "Standard";
                                else tierText = "Basic";
                            } catch (Exception e) {
                                tierText = "Standard";
                            }
                        } else {
                            tierText = "Standard";
                        }
                        
                        if (email.contains("gmail")) {
                            return "Gmail " + ageText + " - " + tierText;
                        } else if (email.contains("yahoo")) {
                            return "Yahoo Mail " + ageText + " - " + tierText;
                        } else if (email.contains("outlook")) {
                            return "Outlook " + ageText + " - " + tierText;
                        } else {
                            return "Email " + ageText + " - " + tierText;
                        }
                    } else if (itemType.equals("ACCOUNT")) {
                        String username = jsonNode.has("username") ? jsonNode.get("username").asText() : "";
                        String followers = jsonNode.has("followers") ? jsonNode.get("followers").asText() : "";
                        String level = jsonNode.has("level") ? jsonNode.get("level").asText() : "";
                        String licenseType = jsonNode.has("license_type") ? jsonNode.get("license_type").asText() : "";
                        String tier = jsonNode.has("tier") ? jsonNode.get("tier").asText() : "";
                        String appsIncluded = jsonNode.has("apps_included") ? jsonNode.get("apps_included").asText() : "";
                        
                        // For Office 365, Adobe, etc.
                        if (licenseType != null && !licenseType.isEmpty()) {
                            String validUntil = jsonNode.has("valid_until") ? jsonNode.get("valid_until").asText() : "";
                            String cloudStorage = jsonNode.has("cloud_storage") ? jsonNode.get("cloud_storage").asText() : "";
                            
                            // Calculate duration from valid_until
                            String durationText = "";
                            if (validUntil != null && !validUntil.isEmpty()) {
                                try {
                                    java.time.LocalDate validDate = java.time.LocalDate.parse(validUntil);
                                    java.time.LocalDate now = java.time.LocalDate.now();
                                    long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(now, validDate);
                                    
                                    if (daysUntilExpiry > 300) durationText = " - 1 năm";
                                    else if (daysUntilExpiry > 150) durationText = " - 6 tháng";
                                    else if (daysUntilExpiry > 60) durationText = " - 3 tháng";
                                    else durationText = " - 1 tháng";
                                } catch (Exception e) {
                                    durationText = " - 1 năm";
                                }
                            } else {
                                durationText = " - 1 năm";
                            }
                            
                            if ("Personal".equals(licenseType)) {
                                return "Office 365 Personal" + durationText;
                            } else if ("Business".equals(licenseType)) {
                                return "Office 365 Business" + durationText;
                            } else if ("Team".equals(licenseType)) {
                                return "Adobe Team" + durationText + (cloudStorage != null ? " (" + cloudStorage + ")" : "");
                            } else if ("Enterprise".equals(licenseType)) {
                                return "Adobe Enterprise" + durationText + (cloudStorage != null ? " (" + cloudStorage + ")" : "");
                            } else if ("Individual".equals(licenseType)) {
                                return "Adobe Individual" + durationText + (cloudStorage != null ? " (" + cloudStorage + ")" : "");
                            }
                        }
                        
                        // For gaming accounts
                        if (level != null && !level.isEmpty()) {
                            String ucBalance = jsonNode.has("uc_balance") ? jsonNode.get("uc_balance").asText() : "";
                            String royalPass = jsonNode.has("royal_pass") ? jsonNode.get("royal_pass").asText() : "";
                            String region = jsonNode.has("region") ? jsonNode.get("region").asText() : "";
                            String championsOwned = jsonNode.has("champions_owned") ? jsonNode.get("champions_owned").asText() : "";
                            String rpBalance = jsonNode.has("rp_balance") ? jsonNode.get("rp_balance").asText() : "";
                            
                            if (username.contains("pubg") || username.contains("PUBG")) {
                                String passText = "true".equals(royalPass) ? " + Royal Pass" : "";
                                String ucText = ucBalance != null && !ucBalance.isEmpty() ? " (" + ucBalance + " UC)" : "";
                                String regionText = region != null && !region.isEmpty() ? " [" + region + "]" : "";
                                return "PUBG Level " + level + " - " + tier + passText + ucText + regionText;
                            } else if (username.contains("lol") || username.contains("LoL")) {
                                String rpText = rpBalance != null && !rpBalance.isEmpty() ? " (" + rpBalance + " RP)" : "";
                                String championsText = championsOwned != null && !championsOwned.isEmpty() ? " [" + championsOwned + " champs]" : "";
                                String regionText = region != null && !region.isEmpty() ? " [" + region + "]" : "";
                                return "LoL Level " + level + " - " + tier + rpText + championsText + regionText;
                            }
                        }
                        
                        // For social media accounts
                        if (followers != null && !followers.isEmpty()) {
                            try {
                                int followerCount = Integer.parseInt(followers);
                                String posts = jsonNode.has("posts") ? jsonNode.get("posts").asText() : "";
                                String verificationBadge = jsonNode.has("verification_badge") ? jsonNode.get("verification_badge").asText() : "";
                                String category = jsonNode.has("category") ? jsonNode.get("category").asText() : "";
                                String following = jsonNode.has("following") ? jsonNode.get("following").asText() : "";
                                
                                String verifiedText = "true".equals(verificationBadge) ? " ✓" : "";
                                String postsText = posts != null && !posts.isEmpty() ? " (" + posts + " posts)" : "";
                                String categoryText = category != null && !category.isEmpty() ? " [" + category + "]" : "";
                                String followingText = following != null && !following.isEmpty() ? " (" + following + " following)" : "";
                                
                                if (username.contains("instagram") || username.contains("Instagram")) {
                                    if (followerCount < 1000) return "Instagram Basic" + verifiedText + postsText + categoryText + followingText;
                                    else if (followerCount < 10000) return "Instagram Standard" + verifiedText + postsText + categoryText + followingText;
                                    else if (followerCount < 50000) return "Instagram Pro" + verifiedText + postsText + categoryText + followingText;
                                    else return "Instagram VIP" + verifiedText + postsText + categoryText + followingText;
                                } else if (username.contains("tiktok") || username.contains("TikTok")) {
                                    if (followerCount < 10000) return "TikTok Basic" + verifiedText + postsText + categoryText + followingText;
                                    else if (followerCount < 50000) return "TikTok Standard" + verifiedText + postsText + categoryText + followingText;
                                    else if (followerCount < 100000) return "TikTok Pro" + verifiedText + postsText + categoryText + followingText;
                                    else return "TikTok VIP" + verifiedText + postsText + categoryText + followingText;
                                } else if (username.contains("facebook") || username.contains("Facebook")) {
                                    if (followerCount < 1000) return "Facebook Basic" + verifiedText + postsText + categoryText;
                                    else if (followerCount < 10000) return "Facebook Standard" + verifiedText + postsText + categoryText;
                                    else if (followerCount < 50000) return "Facebook Pro" + verifiedText + postsText + categoryText;
                                    else return "Facebook VIP" + verifiedText + postsText + categoryText;
                                }
                            } catch (Exception e) {
                                // Keep default
                            }
                        }
                        
                        // Fallback for other accounts
                        return "Account " + (tier != null ? tier : "Standard");
                    } else if (itemType.equals("KEY")) {
                        String key = jsonNode.has("key") ? jsonNode.get("key").asText() : "";
                        String activationType = jsonNode.has("activation_type") ? jsonNode.get("activation_type").asText() : "";
                        String validUntil = jsonNode.has("valid_until") ? jsonNode.get("valid_until").asText() : "";
                        String version = jsonNode.has("version") ? jsonNode.get("version").asText() : "";
                        String region = jsonNode.has("region") ? jsonNode.get("region").asText() : "";
                        
                        // Calculate validity period
                        String validityText = "";
                        if (validUntil != null && !validUntil.isEmpty()) {
                            try {
                                java.time.LocalDate validDate = java.time.LocalDate.parse(validUntil);
                                java.time.LocalDate now = java.time.LocalDate.now();
                                long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(now, validDate);
                                
                                if (daysUntilExpiry > 365) validityText = " - Lifetime";
                                else if (daysUntilExpiry > 180) validityText = " - 1 năm";
                                else if (daysUntilExpiry > 90) validityText = " - 6 tháng";
                                else validityText = " - 3 tháng";
                            } catch (Exception e) {
                                validityText = " - Lifetime";
                            }
                        } else {
                            validityText = " - Lifetime";
                        }
                        
                        String regionText = region != null && !region.isEmpty() ? " (" + region + ")" : "";
                        
                        if (key.contains("W269N") || key.contains("MH37W") || key.contains("NPPR9")) {
                            if ("Retail".equals(activationType)) {
                                return "Windows Key Retail" + validityText + regionText;
                            } else if ("OEM".equals(activationType)) {
                                return "Windows Key OEM" + validityText + regionText;
                            } else {
                                return "Windows Key " + activationType + validityText + regionText;
                            }
                        } else {
                            return "Software Key " + activationType + validityText + regionText;
                        }
                    }
                } catch (Exception e) {
                    // If JSON parsing fails, use simple naming
                }
            }
            
            // Fallback: Simple naming based on warehouse ID and type with unique suffix
            String uniqueSuffix = "";
            try {
                // Add warehouse ID as suffix to make it unique
                uniqueSuffix = " #" + warehouse.getId();
            } catch (Exception e) {
                uniqueSuffix = " #" + System.currentTimeMillis();
            }
            return mainProduct.getName() + uniqueSuffix + " (" + warehouse.getItemType() + ")";
            
        } catch (Exception e) {
            return mainProduct.getName() + " - Item #" + warehouse.getId();
        }
    }
    
    private BigDecimal calculateSubProductPrice(com.badat.study1.model.Warehouse warehouse, Product mainProduct) {
        try {
            String itemData = warehouse.getItemData();
            if (itemData != null && !itemData.isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(itemData);
                    
                    String itemType = warehouse.getItemType().name();
                    BigDecimal basePrice = mainProduct.getPrice();
                    
                    if (itemType.equals("EMAIL")) {
                        String storageUsed = jsonNode.has("storage_used") ? jsonNode.get("storage_used").asText() : "";
                        String createdDate = jsonNode.has("created_date") ? jsonNode.get("created_date").asText() : "";
                        
                        // Price multiplier based on age
                        double ageMultiplier = 1.0;
                        if (createdDate != null && !createdDate.isEmpty()) {
                            try {
                                java.time.LocalDate created = java.time.LocalDate.parse(createdDate);
                                java.time.LocalDate now = java.time.LocalDate.now();
                                long months = java.time.temporal.ChronoUnit.MONTHS.between(created, now);
                                
                                if (months <= 1) ageMultiplier = 0.8; // 1 tháng - rẻ hơn
                                else if (months <= 3) ageMultiplier = 1.0; // 3 tháng - giá gốc
                                else if (months <= 6) ageMultiplier = 1.2; // 6 tháng - đắt hơn
                                else if (months <= 12) ageMultiplier = 1.5; // 1 năm - rất đắt
                                else ageMultiplier = 2.0; // VIP - siêu đắt
                            } catch (Exception e) {
                                ageMultiplier = 1.0;
                            }
                        }
                        
                        // Price multiplier based on storage usage
                        double storageMultiplier = 1.0;
                        if (storageUsed != null && !storageUsed.isEmpty()) {
                            try {
                                double used = Double.parseDouble(storageUsed.replace("GB", ""));
                                if (used < 5) storageMultiplier = 0.7; // Ít dùng - rẻ
                                else if (used < 10) storageMultiplier = 1.0; // Trung bình - giá gốc
                                else storageMultiplier = 1.3; // Nhiều dùng - đắt
                            } catch (Exception e) {
                                storageMultiplier = 1.0;
                            }
                        }
                        
                        return basePrice.multiply(BigDecimal.valueOf(ageMultiplier * storageMultiplier));
                        
                    } else if (itemType.equals("ACCOUNT")) {
                        String followers = jsonNode.has("followers") ? jsonNode.get("followers").asText() : "";
                        String level = jsonNode.has("level") ? jsonNode.get("level").asText() : "";
                        String licenseType = jsonNode.has("license_type") ? jsonNode.get("license_type").asText() : "";
                        String validUntil = jsonNode.has("valid_until") ? jsonNode.get("valid_until").asText() : "";
                        
                        double accountMultiplier = 1.0;
                        
                        // Price based on license type (for Office 365, Adobe, etc.)
                        if (licenseType != null && !licenseType.isEmpty()) {
                            if ("Personal".equals(licenseType)) accountMultiplier = 1.0;
                            else if ("Business".equals(licenseType)) accountMultiplier = 1.5;
                            else if ("Team".equals(licenseType)) accountMultiplier = 2.0;
                            else if ("Enterprise".equals(licenseType)) accountMultiplier = 3.0;
                        }
                        
                        // Price based on followers (for social media accounts)
                        if (followers != null && !followers.isEmpty()) {
                            try {
                                int followerCount = Integer.parseInt(followers);
                                if (followerCount < 1000) accountMultiplier = 0.5;
                                else if (followerCount < 10000) accountMultiplier = 1.0;
                                else if (followerCount < 50000) accountMultiplier = 1.5;
                                else accountMultiplier = 2.0;
                            } catch (Exception e) {
                                accountMultiplier = 1.0;
                            }
                        }
                        
                        // Price based on level (for gaming accounts)
                        if (level != null && !level.isEmpty()) {
                            try {
                                int levelNum = Integer.parseInt(level);
                                if (levelNum < 10) accountMultiplier *= 0.5;
                                else if (levelNum < 30) accountMultiplier *= 1.0;
                                else accountMultiplier *= 1.5;
                            } catch (Exception e) {
                                // Keep current multiplier
                            }
                        }
                        
                        // Price based on validity (for licenses)
                        if (validUntil != null && !validUntil.isEmpty()) {
                            try {
                                java.time.LocalDate validDate = java.time.LocalDate.parse(validUntil);
                                java.time.LocalDate now = java.time.LocalDate.now();
                                long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(now, validDate);
                                
                                if (daysUntilExpiry > 365) accountMultiplier *= 1.5; // Còn lâu
                                else if (daysUntilExpiry > 180) accountMultiplier *= 1.2; // Còn khá lâu
                                else accountMultiplier *= 0.8; // Sắp hết hạn
                            } catch (Exception e) {
                                // Keep current multiplier
                            }
                        }
                        
                        return basePrice.multiply(BigDecimal.valueOf(accountMultiplier));
                        
                    } else if (itemType.equals("KEY")) {
                        String activationType = jsonNode.has("activation_type") ? jsonNode.get("activation_type").asText() : "";
                        String validUntil = jsonNode.has("valid_until") ? jsonNode.get("valid_until").asText() : "";
                        
                        double keyMultiplier = 1.0;
                        
                        // Price based on activation type
                        if ("Retail".equals(activationType)) keyMultiplier = 1.5;
                        else if ("OEM".equals(activationType)) keyMultiplier = 0.8;
                        else keyMultiplier = 1.0;
                        
                        // Price based on validity
                        if (validUntil != null && !validUntil.isEmpty()) {
                            try {
                                java.time.LocalDate validDate = java.time.LocalDate.parse(validUntil);
                                java.time.LocalDate now = java.time.LocalDate.now();
                                long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(now, validDate);
                                
                                if (daysUntilExpiry > 365) keyMultiplier *= 1.5; // Còn lâu
                                else if (daysUntilExpiry > 180) keyMultiplier *= 1.2; // Còn khá lâu
                                else keyMultiplier *= 0.8; // Sắp hết hạn
                            } catch (Exception e) {
                                // Keep current multiplier
                            }
                        }
                        
                        return basePrice.multiply(BigDecimal.valueOf(keyMultiplier));
                    }
                    
                    // Add random variation for other item types
                    if (itemType.equals("CARD") || itemType.equals("OTHER")) {
                        // Add some random variation to make prices different
                        double randomMultiplier = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2
                        return basePrice.multiply(BigDecimal.valueOf(randomMultiplier));
                    }
                } catch (Exception e) {
                    // If JSON parsing fails, use base price
                }
            }
            
            // Fallback: Use base price with small random variation
            double randomMultiplier = 0.9 + (Math.random() * 0.2); // 0.9 to 1.1
            return mainProduct.getPrice().multiply(BigDecimal.valueOf(randomMultiplier));
            
        } catch (Exception e) {
            return mainProduct.getPrice();
        }
    }
    
    private long getStockCountForWarehouseItem(Long warehouseId) {
        try {
            // Query actual stock count from database
            // Check if warehouse item exists and is available
            com.badat.study1.model.Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
            if (warehouse == null || warehouse.getIsDelete() || warehouse.getLocked()) {
                return 0;
            }
            
            // For now, return stock based on warehouse ID to ensure variety
            // In real implementation, you would query actual stock from database
            long stockCount = (warehouseId % 5) + 1; // 1-5 items based on ID
            return stockCount;
        } catch (Exception e) {
            return 0;
        }
    }
}


