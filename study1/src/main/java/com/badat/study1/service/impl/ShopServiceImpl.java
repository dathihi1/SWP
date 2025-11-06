package com.badat.study1.service.impl;

import com.badat.study1.model.Product;
import com.badat.study1.model.ProductVariant;
import com.badat.study1.model.User;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ProductVariantRepository;
import com.badat.study1.repository.ReviewRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.UploadHistoryRepository;
import com.badat.study1.repository.WarehouseRepository;
import com.badat.study1.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import com.badat.study1.model.OrderItem;
import com.badat.study1.model.Order;
import com.badat.study1.dto.OrderSummary;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Objects;

@Service
public class ShopServiceImpl implements ShopService {
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;

    public ShopServiceImpl(ShopRepository shopRepository,
                           ProductRepository productRepository,
                           ProductVariantRepository productVariantRepository,
                           UploadHistoryRepository uploadHistoryRepository,
                           WarehouseRepository warehouseRepository,
                           OrderItemRepository orderItemRepository,
                           ReviewRepository reviewRepository) {
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.uploadHistoryRepository = uploadHistoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.orderItemRepository = orderItemRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public String productManagementOverview(User user, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());

        shopRepository.findByUserId(user.getId()).ifPresent(shop -> {
            var products = productRepository.findByShopIdAndIsDeleteFalse(shop.getId());
            products.forEach(product -> {
                var productVariants = productVariantRepository.findByProductIdAndIsDeleteFalse(product.getId());
                int totalStock = (int) warehouseRepository.countAvailableItemsByProductId(product.getId());
                product.setProductVariantCount(totalStock);
                if (!productVariants.isEmpty()) {
                    var availableProductVariants = productVariants.stream()
                            .filter(pv -> pv.getQuantity() != null && pv.getQuantity() > 0)
                            .collect(Collectors.toList());
                    if (!availableProductVariants.isEmpty()) {
                        BigDecimal minPrice = availableProductVariants.stream().map(ProductVariant::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                        BigDecimal maxPrice = availableProductVariants.stream().map(ProductVariant::getPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                        NumberFormat viNumber = NumberFormat.getNumberInstance(Locale.US);
                        viNumber.setGroupingUsed(true);
                        String minStr = viNumber.format(minPrice.setScale(0, RoundingMode.HALF_UP));
                        String maxStr = viNumber.format(maxPrice.setScale(0, RoundingMode.HALF_UP));
                        if (minPrice.equals(maxPrice)) {
                            product.setPriceRange(minStr + " VND");
                        } else {
                            product.setPriceRange(minStr + " VND - " + maxStr + " VND");
                        }
                    } else {
                        product.setPriceRange("-");
                    }
                } else {
                    product.setPriceRange("-");
                }
            });
            model.addAttribute("products", products);
            long totalProducts = productRepository.countByShopIdAndIsDeleteFalse(shop.getId());
            model.addAttribute("totalProducts", totalProducts);
        });

        return "seller/product-management";
    }

    @Override
    public String addProductPage(User user, Model model) {
        boolean hasShop = shopRepository.findByUserId(user.getId()).isPresent();
        if (!hasShop) {
            return "redirect:/seller/product-management";
        }
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        return "seller/add-product";
    }

    @Override
    public String editProductPage(User user, Long productId, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        var product = productRepository.findById(productId);
        if (product.isEmpty()) {
            return "redirect:/seller/product-management";
        }
        shopRepository.findByUserId(user.getId()).ifPresent(shop -> {
            if (!product.get().getShopId().equals(shop.getId())) {
                // silently ignore, redirect handled below
            }
        });
        model.addAttribute("stall", product.get());
        return "seller/edit-product";
    }

    @Override
    public String productManagementPage(User user, Long stallId, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        var productOptional = productRepository.findById(stallId);
        if (productOptional.isEmpty()) {
            return "redirect:/seller/product-management";
        }
        Product product = productOptional.get();
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty() || !product.getShopId().equals(userShop.get().getId())) {
            return "redirect:/seller/product-management";
        }
        model.addAttribute("stall", product);
        var productVariants = productVariantRepository.findByProductIdAndIsDeleteFalse(stallId);
        for (ProductVariant pv : productVariants) {
            long stock = warehouseRepository.countByProductVariantIdAndLockedFalseAndIsDeleteFalse(pv.getId());
            pv.setQuantity((int) stock);
            pv.setStatus(stock > 0 ? ProductVariant.Status.AVAILABLE : ProductVariant.Status.UNAVAILABLE);
        }
        model.addAttribute("products", productVariants);
        return "seller/product-variant-management";
    }

    @Override
    public String addQuantityPage(User user, Long productId, int page, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        var productVariantOptional = productVariantRepository.findById(productId);
        if (productVariantOptional.isEmpty()) {
            return "redirect:/seller/product-management";
        }
        var productVariant = productVariantOptional.get();
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return "redirect:/seller/product-management";
        }
        if (!productVariant.getShopId().equals(userShop.get().getId())) {
            return "redirect:/seller/product-management";
        }
        var product = productRepository.findById(productVariant.getProductId());
        if (product.isEmpty()) {
            return "redirect:/seller/product-management";
        }
        model.addAttribute("productVariant", productVariant);
        model.addAttribute("product", product.get());
        model.addAttribute("stall", product.get());
        int validatedPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validatedPage, 5);
        Page<com.badat.study1.model.UploadHistory> uploadHistoryPage = uploadHistoryRepository.findByProductVariantIdOrderByCreatedAtDesc(productId, pageable);
        if (validatedPage >= uploadHistoryPage.getTotalPages() && uploadHistoryPage.getTotalPages() > 0) {
            validatedPage = uploadHistoryPage.getTotalPages() - 1;
            pageable = PageRequest.of(validatedPage, 5);
            uploadHistoryPage = uploadHistoryRepository.findByProductVariantIdOrderByCreatedAtDesc(productId, pageable);
        }
        model.addAttribute("recentUploads", uploadHistoryPage.getContent());
        model.addAttribute("currentPage", validatedPage);
        model.addAttribute("totalPages", uploadHistoryPage.getTotalPages());
        model.addAttribute("totalElements", uploadHistoryPage.getTotalElements());
        model.addAttribute("hasNext", uploadHistoryPage.hasNext());
        model.addAttribute("hasPrevious", uploadHistoryPage.hasPrevious());
        return "seller/add-quantity";
    }

    @Override
    public String ordersPage(User user, int page, String status, Long stallId, Long productId, String dateFrom, String dateTo, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return "redirect:/seller/product-management";
        }
        var products = productRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
        int validatedPage = Math.max(0, page);
        List<OrderItem> allOrderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(user.getId());
        List<OrderItem> filteredOrderItems = allOrderItems.stream()
                .filter(orderItem -> status == null || status.isEmpty() || orderItem.getStatus().name().equals(status.toUpperCase()))
                .filter(orderItem -> stallId == null || stallId.equals(orderItem.getWarehouse().getProduct().getId()))
                .filter(orderItem -> productId == null || productId.equals(orderItem.getProductId()))
                .filter(orderItem -> {
                    if (dateFrom == null || dateFrom.isEmpty()) return true;
                return orderItem.getCreatedAt().toLocalDate().isAfter(LocalDate.parse(dateFrom).minusDays(1));
                })
                .filter(orderItem -> {
                    if (dateTo == null || dateTo.isEmpty()) return true;
                return orderItem.getCreatedAt().toLocalDate().isBefore(LocalDate.parse(dateTo).plusDays(1));
                })
            .collect(Collectors.toList());
        // Group items by order
        Map<Order, List<OrderItem>> byOrder = new LinkedHashMap<>();
        for (OrderItem oi : filteredOrderItems) {
            byOrder.computeIfAbsent(oi.getOrder(), k -> new ArrayList<>()).add(oi);
        }
        // Build per-order summaries
        List<OrderSummary> groupedOrders = new ArrayList<>();
        for (var entry : byOrder.entrySet()) {
            Order order = entry.getKey();
            List<OrderItem> items = entry.getValue();
            BigDecimal totalAmount = items.stream()
                    .map(OrderItem::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int totalQuantity = items.stream()
                    .map(OrderItem::getQuantity)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            String firstProductName = items.isEmpty() ? "" : (items.get(0).getProductVariantName() != null ? items.get(0).getProductVariantName() : "N/A");
            String stallName = items.isEmpty() ? "" : (items.get(0).getWarehouse().getProduct() != null ? items.get(0).getWarehouse().getProduct().getProductName() : "N/A");
            BigDecimal firstUnitPrice = items.isEmpty() ? BigDecimal.ZERO : items.get(0).getUnitPrice();
            var firstStatus = items.isEmpty() ? null : items.get(0).getStatus();
            LocalDateTime createdAt = items.isEmpty() ? null : items.get(0).getCreatedAt();
            var summary = OrderSummary.builder()
                    .order(order)
                    .items(items)
                    .totalAmount(totalAmount)
                    .totalQuantity(totalQuantity)
                    .firstProductName(firstProductName)
                    .stallName(stallName)
                    .unitPrice(firstUnitPrice)
                    .status(firstStatus)
                    .createdAt(createdAt)
                    .build();
            groupedOrders.add(summary);
        }
        // Pagination on grouped orders
        int pageSize = 10;
        int totalElements = groupedOrders.size();
        int totalPages = (totalElements + pageSize - 1) / pageSize;
        if (validatedPage >= totalPages && totalPages > 0) {
            validatedPage = totalPages - 1;
        }
        int start = validatedPage * pageSize;
        int end = Math.min(start + pageSize, totalElements);
        List<OrderSummary> pageContent = groupedOrders.subList(start, end);
        model.addAttribute("stalls", products);
        model.addAttribute("products", products);
        model.addAttribute("orders", pageContent);
        // Filters
        model.addAttribute("orderStatuses", OrderItem.Status.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedStallId", stallId);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedDateFrom", dateFrom);
        model.addAttribute("selectedDateTo", dateTo);
        // Pagination
        model.addAttribute("currentPage", validatedPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("hasNext", validatedPage < totalPages - 1);
        model.addAttribute("hasPrevious", validatedPage > 0);
        return "seller/orders";
    }

    @Override
    public String reviewsPage(User user, int page, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        // Load stats
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return "redirect:/seller/product-management";
        }
        var products = productRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
        var stallStats = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (var product : products) {
            var productReviews = reviewRepository.findByProductIdAndIsDeleteFalse(product.getId());
            double averageRating = 0.0;
            int reviewCount = productReviews.size();
            if (reviewCount > 0) {
                averageRating = productReviews.stream().mapToInt(com.badat.study1.model.Review::getRating).average().orElse(0.0);
            }
            int unreadCount = (int) productReviews.stream().filter(review -> !review.getIsRead()).count();
            var stallStat = new java.util.HashMap<String, Object>();
            stallStat.put("stall", product);
            stallStat.put("averageRating", Math.round(averageRating * 10.0) / 10.0);
            stallStat.put("reviewCount", reviewCount);
            stallStat.put("unreadCount", unreadCount);
            stallStats.add(stallStat);
        }
        Pageable pageable = PageRequest.of(0, 1000);
        Page<com.badat.study1.model.Review> reviewsPage = reviewRepository.findBySellerIdAndShopIdAndIsDeleteFalse(user.getId(), userShop.get().getId(), pageable);
        model.addAttribute("reviews", reviewsPage.getContent());
        model.addAttribute("stallStats", stallStats);
        return "seller/reviews";
    }

    @Override
    public String sellerShopPage(User user, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        return "seller/gross-sales";
    }

    @Override
    public ResponseEntity<?> getReviewsByStall(User user, Long stallId) {
        var product = productRepository.findById(stallId);
        if (product.isEmpty()) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Product not found"));
        }
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Shop not found"));
        }
        if (!product.get().getShopId().equals(userShop.get().getId())) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Access denied"));
        }
        var reviews = reviewRepository.findByProductIdAndIsDeleteFalse(stallId);
        var reviewDTOs = reviews.stream().map(review -> {
            var dto = new java.util.HashMap<String, Object>();
            dto.put("id", review.getId());
            dto.put("rating", review.getRating());
            dto.put("content", review.getContent());
            dto.put("replyContent", review.getReplyContent());
            dto.put("createdAt", review.getCreatedAt());
            dto.put("isRead", review.getIsRead());
            var buyerInfo = new java.util.HashMap<String, Object>();
            buyerInfo.put("username", review.getBuyer().getUsername());
            dto.put("buyer", buyerInfo);
            var productInfo = new java.util.HashMap<String, Object>();
            productInfo.put("name", review.getProductVariant() != null ? review.getProductVariant().getName() : "N/A");
            dto.put("product", productInfo);
            return dto;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(reviewDTOs);
    }

    @Override
    public ResponseEntity<?> markReviewsAsRead(User user, Long stallId) {
        var product = productRepository.findById(stallId);
        if (product.isEmpty()) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Product not found"));
        }
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Shop not found"));
        }
        if (!product.get().getShopId().equals(userShop.get().getId())) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Access denied"));
        }
        var reviews = reviewRepository.findByProductIdAndIsDeleteFalse(stallId);
        int updated = 0;
        for (var review : reviews) {
            if (!review.getIsRead()) {
                review.setIsRead(true);
                reviewRepository.save(review);
                updated++;
            }
        }
        return ResponseEntity.ok(java.util.Map.of("message", "Reviews marked as read", "count", updated));
    }

    @Override
    public String replyToReview(User user, Long reviewId, String sellerReply, RedirectAttributes redirectAttributes) {
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy shop của bạn!");
            return "redirect:/seller/reviews";
        }
        try {
            var reviewOptional = reviewRepository.findById(reviewId);
            if (reviewOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đánh giá!");
                return "redirect:/seller/reviews";
            }
            var review = reviewOptional.get();
            if (!review.getSellerId().equals(user.getId()) || !review.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền trả lời đánh giá này!");
                return "redirect:/seller/reviews";
            }
            review.setReplyContent(sellerReply);
            review.setReplyAt(java.time.LocalDateTime.now());
            reviewRepository.save(review);
            redirectAttributes.addFlashAttribute("successMessage", "Đã trả lời đánh giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi trả lời đánh giá. Vui lòng thử lại!");
        }
        return "redirect:/seller/reviews";
    }

    @Override
    public String addProduct(User user,
                           String stallName,
                           String stallCategory,
                           String productSubcategory,
                           String shortDescription,
                           String detailedDescription,
                           org.springframework.web.multipart.MultipartFile stallImageFile,
                           Boolean uniqueProducts,
                           String isCropped,
                           RedirectAttributes redirectAttributes) {

        try {
            var userShop = shopRepository.findByUserId(user.getId()).orElse(null);
            if (userShop == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa có shop. Vui lòng tạo shop trước khi tạo sản phẩm!");
                return "redirect:/seller/product-management";
            }
            Product product = new Product();
            product.setShopId(userShop.getId());
            product.setProductName(stallName);
            product.setProductCategory(stallCategory);
            product.setShortDescription(shortDescription);
            product.setDetailedDescription(detailedDescription);
            product.setProductSubcategory(productSubcategory);
            if (stallImageFile != null && !stallImageFile.isEmpty()) {
                byte[] imageData = stallImageFile.getBytes();
                product.setProductImageData(imageData);
            }
            product.setStatus("OPEN");
            product.setCreatedAt(java.time.Instant.now());
            product.setDelete(false);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm đã được tạo thành công!");
            return "redirect:/seller/product-management";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tạo gian hàng. Vui lòng thử lại!");
            return "redirect:/seller/add-product";
        }
    }

    @Override
    public String editProduct(User user,
                            Long id,
                            String stallName,
                            String status,
                            String shortDescription,
                            String detailedDescription,
                            org.springframework.web.multipart.MultipartFile stallImageFile,
                            RedirectAttributes redirectAttributes) {
        try {
            var productOptional = productRepository.findById(id);
            if (productOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy gian hàng!");
                return "redirect:/seller/product-management";
            }
            Product product = productOptional.get();
            var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty() || !product.getShopId().equals(userShop.get().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền sửa gian hàng này!");
            return "redirect:/seller/product-management";
        }
            // No stock gating: allow opening product regardless of current warehouse quantity
            // Apply updates (controller already validated no-op and readonly tampering)
            product.setProductName(stallName);
            product.setStatus(status);
            product.setShortDescription(shortDescription);
            product.setDetailedDescription(detailedDescription);
            if (stallImageFile != null && !stallImageFile.isEmpty()) {
                try {
                    byte[] imageData = stallImageFile.getBytes();
                    product.setProductImageData(imageData);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xử lý hình ảnh. Vui lòng thử lại!");
                    return "redirect:/seller/edit-product/" + id;
                }
            }

            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm đã được cập nhật thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật sản phẩm. Vui lòng thử lại!");
        }
        return "redirect:/seller/product-management";
    }

    @Override
    public String addProduct(User user, Long stallId, String productName, java.math.BigDecimal productPrice, RedirectAttributes redirectAttributes) {
        try {
            var productOptional = productRepository.findById(stallId);
            if (productOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy gian hàng!");
                return "redirect:/seller/product-management";
            }
            Product parentProduct = productOptional.get();
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !parentProduct.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thêm sản phẩm vào gian hàng này!");
                return "redirect:/seller/product-management";
            }
            ProductVariant productVariant;
            String uniqueKey = "PROD_" + System.currentTimeMillis() + "_" + user.getId();
            String subcategoryValue = parentProduct.getProductSubcategory() != null ? parentProduct.getProductSubcategory() : "Khác";
            productVariant = ProductVariant.builder()
                    .shopId(parentProduct.getShopId())
                    .productId(stallId)
                    .subcategory(subcategoryValue)
                    .name(productName)
                    .price(productPrice)
                    .quantity(0)
                    .uniqueKey(uniqueKey)
                    .status(ProductVariant.Status.UNAVAILABLE)
                    .build();
            productVariantRepository.save(productVariant);
            redirectAttributes.addFlashAttribute("successMessage", "Biến thể đã được thêm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi thêm sản phẩm. Vui lòng thử lại!");
        }
        return "redirect:/seller/product-variant-management/" + stallId;
    }

    @Override
    public String updateProduct(User user, Long productId, String productName, java.math.BigDecimal productPrice, RedirectAttributes redirectAttributes) {
        try {
            var productVariantOptional = productVariantRepository.findById(productId);
            if (productVariantOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
                return "redirect:/seller/product-management";
            }
            ProductVariant productVariant = productVariantOptional.get();
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !productVariant.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền sửa sản phẩm này!");
                return "redirect:/seller/product-management";
            }
            productVariant.setName(productName);
            productVariant.setPrice(productPrice);
            productVariant.setUpdatedAt(java.time.LocalDateTime.now());
            productVariantRepository.save(productVariant);
            redirectAttributes.addFlashAttribute("successMessage", "Biến thể đã được cập nhật thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật sản phẩm. Vui lòng thử lại!");
        }
        Long parentProductId = productVariantRepository.findById(productId).map(ProductVariant::getProductId).orElse(null);
        return "redirect:/seller/product-variant-management/" + (parentProductId != null ? parentProductId : 0);
    }

    @Override
    public String updateProductQuantityFromFile(User user, Long productId, org.springframework.web.multipart.MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file TXT!");
                return "redirect:/seller/add-quantity/" + productId;
            }
            if (!file.getOriginalFilename().toLowerCase().endsWith(".txt")) {
                redirectAttributes.addFlashAttribute("errorMessage", "File phải có định dạng TXT!");
                return "redirect:/seller/add-quantity/" + productId;
            }
            if (file.getSize() > 1024 * 1024) {
                redirectAttributes.addFlashAttribute("errorMessage", "File quá lớn! Vui lòng chọn file nhỏ hơn 1MB.");
                return "redirect:/seller/add-quantity/" + productId;
            }
            var productVariantOptional = productVariantRepository.findById(productId);
            if (productVariantOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
                return "redirect:/seller/product-management";
            }
            ProductVariant productVariant = productVariantOptional.get();
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty()) {
                return "redirect:/seller/product-management";
            }
            Product parentProduct = productRepository.findById(productVariant.getProductId()).orElse(null);
            if (parentProduct == null || !parentProduct.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật kho cho gian hàng này!");
                return "redirect:/seller/product-management";
            }
            String content = new String(file.getBytes(), "UTF-8").trim();
            String[] lines = content.split("\\r?\\n");
            int successCount = 0;
            int failureCount = 0;
            StringBuilder resultDetails = new StringBuilder();
            java.util.Set<String> processedItemKeys = new java.util.HashSet<>();
            // Load existing identifiers (scoped by same subcategory) for this variant for duplicate check in DB
            var existingItems = warehouseRepository.findByProductVariantIdAndIsDeleteFalse(productVariant.getId());
            final String currentSubcategory = productVariant.getSubcategory();
            java.util.Set<String> existingKeys = new java.util.HashSet<>();
            for (var it : existingItems) {
                // ensure same subcategory when checking duplicates in DB
                if (it.getItemSubcategory() != null && currentSubcategory != null
                        && it.getItemSubcategory().equalsIgnoreCase(currentSubcategory)) {
                    String[] ep = it.getItemData().split("\\|");
                    String key = extractIdentifier(currentSubcategory, ep);
                    if (key != null) existingKeys.add(key);
                }
            }
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                try {
                    String[] parts = line.split("\\|");
                    // Yêu cầu đúng 2 phần theo định dạng đã công bố
                    if (parts.length != 2) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Định dạng không hợp lệ.\n");
                        continue;
                    }
                    // Không chấp nhận format cũ có tiền tố loại (CARD|, EMAIL|, KEY|)
                    String firstTokenUpper = parts[0].trim().toUpperCase();
                    if ("CARD".equals(firstTokenUpper) || "EMAIL".equals(firstTokenUpper) || "KEY".equals(firstTokenUpper)) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Định dạng không hợp lệ.\n");
                        continue;
                    }
                    String itemData = line;
                    String itemKey = extractIdentifier(productVariant.getSubcategory(), parts);
                    // Kiểm tra khóa định danh bắt buộc theo subcategory
                    if (itemKey == null || itemKey.isBlank()) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Định dạng không hợp lệ.\n");
                        continue;
                    }
                    if (itemKey != null && processedItemKeys.contains(itemKey)) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Định dạng không hợp lệ.\n");
                        continue;
                    }
                    boolean existsInDb = (itemKey != null && existingKeys.contains(itemKey));
                    if (existsInDb) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Định dạng không hợp lệ.\n");
                        continue;
                    }
                    if (itemKey != null) processedItemKeys.add(itemKey);
                        com.badat.study1.model.Warehouse warehouseItem = com.badat.study1.model.Warehouse.builder()
                            .itemSubcategory(productVariant.getSubcategory())
                                .itemData(itemData)
                                .productVariant(productVariant)
                                .shop(userShop.get())
                                .product(parentProduct)
                                .user(user)
                                .build();
                        warehouseRepository.save(warehouseItem);
                    successCount++;
                } catch (Exception ex) {
                    failureCount++;
                    resultDetails.append("Dòng ").append(i + 1).append(": Định dạng không hợp lệ.\n");
                }
            }
            long warehouseCount = warehouseRepository.countByProductVariantIdAndLockedFalseAndIsDeleteFalse(productId);
            productVariant.setQuantity((int) warehouseCount);
            if (warehouseCount > 0) {
                productVariant.setStatus(ProductVariant.Status.AVAILABLE);
            } else {
                productVariant.setStatus(ProductVariant.Status.UNAVAILABLE);
            }
            productVariantRepository.save(productVariant);
            try {
                if (userShop.isPresent()) {
                    var allProductVariants = productVariantRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
                    for (ProductVariant shopProductVariant : allProductVariants) {
                        long productVariantWarehouseCount = warehouseRepository.countByProductVariantIdAndLockedFalseAndIsDeleteFalse(shopProductVariant.getId());
                        shopProductVariant.setQuantity((int) productVariantWarehouseCount);
                        if (productVariantWarehouseCount > 0) {
                            shopProductVariant.setStatus(ProductVariant.Status.AVAILABLE);
                        } else {
                            shopProductVariant.setStatus(ProductVariant.Status.UNAVAILABLE);
                        }
                        productVariantRepository.save(shopProductVariant);
                    }
                }
            } catch (Exception ignore) {}
            try {
                StringBuilder detailedResult = new StringBuilder();
                detailedResult.append("Tên biến thể: ").append(productVariant.getName()).append("\n");
                detailedResult.append("Tên file: ").append(file.getOriginalFilename()).append("\n");
                detailedResult.append("Ngày upload: ").append(java.time.LocalDateTime.now().toString()).append("\n");
                detailedResult.append("Tổng số dòng: ").append(lines.length).append("\n");
                detailedResult.append("Thành công: ").append(successCount).append("\n");
                detailedResult.append("Thất bại: ").append(failureCount).append("\n");
                detailedResult.append("Trạng thái: ").append(successCount > 0 ? "THÀNH CÔNG" : "THẤT BẠI").append("\n");
                if (resultDetails.length() > 0) {
                    detailedResult.append("\nChi tiết:\n").append(resultDetails.toString());
                }
                com.badat.study1.model.UploadHistory uploadHistory = com.badat.study1.model.UploadHistory.builder()
                        .fileName(file.getOriginalFilename())
                        .productName(productVariant.getName())
                        .isSuccess(successCount > 0)
                        .result(successCount > 0 ? "SUCCESS" : "FAILED")
                        .status(successCount > 0 ? "COMPLETED" : "FAILED")
                        .totalItems(lines.length)
                        .successCount(successCount)
                        .failureCount(failureCount)
                        .resultDetails(detailedResult.toString())
                        .productVariant(productVariant)
                        .product(parentProduct)
                        .user(user)
                        .build();
                uploadHistoryRepository.save(uploadHistory);
            } catch (Exception ignore) {}
            if (successCount > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Upload thành công! Đã thêm " + successCount + " sản phẩm vào kho. " + (failureCount > 0 ? "Có " + failureCount + " dòng lỗi." : ""));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Upload thất bại! Không có sản phẩm nào được thêm vào kho. Vui lòng kiểm tra định dạng file.");
            }
            return "redirect:/seller/add-quantity/" + productId;
        } catch (Exception e) {
            try {
                var productVariantOptional = productVariantRepository.findById(productId);
                if (productVariantOptional.isPresent()) {
                    ProductVariant failedProductVariant = productVariantOptional.get();
                    StringBuilder detailedResult = new StringBuilder();
                    detailedResult.append("Tên biến thể: ").append(failedProductVariant.getName()).append("\n");
                    detailedResult.append("Tên file: ").append(file.getOriginalFilename()).append("\n");
                    detailedResult.append("Ngày upload: ").append(java.time.LocalDateTime.now().toString()).append("\n");
                    detailedResult.append("Tổng số dòng: 0\n");
                    detailedResult.append("Thành công: 0\n");
                    detailedResult.append("Thất bại: 1\n");
                    detailedResult.append("Trạng thái: THẤT BẠI\n");
                    detailedResult.append("\nChi tiết lỗi:\n").append("Lỗi xử lý file: ").append(e.getMessage());
                    var failedParentProductOptional = productRepository.findById(failedProductVariant.getProductId());
                    if (failedParentProductOptional.isPresent()) {
                        com.badat.study1.model.UploadHistory uploadHistory = com.badat.study1.model.UploadHistory.builder()
                                .fileName(file.getOriginalFilename())
                                .productName(failedProductVariant.getName())
                                .isSuccess(false)
                                .result("FAILED")
                                .status("FAILED")
                                .totalItems(0)
                                .successCount(0)
                                .failureCount(1)
                                .resultDetails(detailedResult.toString())
                                .productVariant(failedProductVariant)
                                .product(failedParentProductOptional.get())
                                .user(user)
                                .build();
                        uploadHistoryRepository.save(uploadHistory);
                    }
                }
            } catch (Exception ignored) {}
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xử lý file. Vui lòng thử lại!");
            return "redirect:/seller/add-quantity/" + productId;
        }
    }

    @Override
    public ResponseEntity<?> getUploadDetails(User user, Long uploadId) {
        try {
            var uploadOptional = uploadHistoryRepository.findById(uploadId);
            if (uploadOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            var upload = uploadOptional.get();
            if (!upload.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Access denied");
            }
            var response = new java.util.HashMap<String, Object>();
            response.put("fileName", upload.getFileName());
            response.put("createdAt", upload.getCreatedAt().toString());
            response.put("totalItems", upload.getTotalItems());
            response.put("successCount", upload.getSuccessCount());
            response.put("failureCount", upload.getFailureCount());
            response.put("resultDetails", upload.getResultDetails());
            response.put("isSuccess", upload.getIsSuccess());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    private String extractIdentifier(String subcategory, String[] parts) {
        if (parts == null) return null;
        String sub = subcategory == null ? "" : subcategory.toLowerCase();
        // Gmail: email|password -> key = email
        if (sub.contains("gmail")) {
            String email = parts.length >= 2 ? parts[0].trim() : null;
            if (email == null || !email.contains("@")) return null;
            return email;
        }
        // Thẻ cào: code|serial -> key = serial
        if (sub.contains("thẻ") || sub.contains("card")) {
            return parts.length >= 2 ? parts[1].trim() : null;
        }
        // Tài khoản: username|password -> key = username
        if (sub.contains("tài khoản") || sub.contains("account") || sub.contains("acc")) {
            return parts.length >= 2 ? parts[0].trim() : null;
        }
        // Default fallback: first part as identifier
        return parts.length >= 2 ? parts[0].trim() : null;
        }

}


