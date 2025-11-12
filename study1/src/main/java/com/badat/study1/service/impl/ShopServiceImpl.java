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
import java.nio.charset.StandardCharsets;
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
    public String productManagementPage(User user, Model model) {
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
        model.addAttribute("product", product.get());
        return "seller/edit-product";
    }

    @Override
    public String productVariantManagementPage(User user, Long productId, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        var productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            return "redirect:/seller/product-management";
        }
        Product product = productOptional.get();
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty() || !product.getShopId().equals(userShop.get().getId())) {
            return "redirect:/seller/product-management";
        }
        model.addAttribute("product", product);
        var productVariants = productVariantRepository.findByProductIdAndIsDeleteFalse(productId);
        for (ProductVariant pv : productVariants) {
            long stock = warehouseRepository.countByProductVariantIdAndLockedFalseAndIsDeleteFalse(pv.getId());
            pv.setQuantity((int) stock);
            pv.setStatus(stock > 0 ? ProductVariant.Status.AVAILABLE : ProductVariant.Status.UNAVAILABLE);
        }
        model.addAttribute("products", productVariants);
        return "seller/product-variant-management";
    }

    @Override
    public String addQuantityPage(User user, Long productVariantId, int page, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        var productVariantOptional = productVariantRepository.findById(productVariantId);
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
        int validatedPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validatedPage, 5);
        Page<com.badat.study1.model.UploadHistory> uploadHistoryPage = uploadHistoryRepository.findByProductVariantIdOrderByCreatedAtDesc(productVariantId, pageable);
        if (validatedPage >= uploadHistoryPage.getTotalPages() && uploadHistoryPage.getTotalPages() > 0) {
            validatedPage = uploadHistoryPage.getTotalPages() - 1;
            pageable = PageRequest.of(validatedPage, 5);
            uploadHistoryPage = uploadHistoryRepository.findByProductVariantIdOrderByCreatedAtDesc(productVariantId, pageable);
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
    public String ordersPage(User user, int page, String status, Long productId, Long productVariantId, String dateFrom, String dateTo, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return "redirect:/seller/product-management";
        }
        var products = productRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
        var productVariants = productVariantRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
        int validatedPage = Math.max(0, page);
        List<OrderItem> allOrderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(user.getId());
        List<OrderItem> filteredOrderItems = allOrderItems.stream()
                .filter(orderItem -> status == null || status.isEmpty() || orderItem.getStatus().name().equals(status.toUpperCase()))
                .filter(orderItem -> productId == null || productId.equals(orderItem.getWarehouse().getProduct().getId()))
                .filter(orderItem -> productVariantId == null || productVariantId.equals(orderItem.getProductVariantId()))
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
            String firstProductVariantName = items.isEmpty() ? "" : (items.get(0).getProductVariantName() != null ? items.get(0).getProductVariantName() : "N/A");
            String productName = items.isEmpty() ? "" : (items.get(0).getWarehouse().getProduct() != null ? items.get(0).getWarehouse().getProduct().getProductName() : "N/A");
            BigDecimal firstUnitPrice = items.isEmpty() ? BigDecimal.ZERO : items.get(0).getUnitPrice();
            var firstStatus = items.isEmpty() ? null : items.get(0).getStatus();
            LocalDateTime createdAt = items.isEmpty() ? null : items.get(0).getCreatedAt();
            var summary = OrderSummary.builder()
                    .order(order)
                    .items(items)
                    .totalAmount(totalAmount)
                    .totalQuantity(totalQuantity)
                    .firstProductVariantName(firstProductVariantName)
                    .productName(productName)
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
        model.addAttribute("products", products);
        model.addAttribute("productVariants", productVariants);
        model.addAttribute("orders", pageContent);
        // Filters
        model.addAttribute("orderStatuses", OrderItem.Status.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedProductVariantId", productVariantId);
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
        var productStats = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (var product : products) {
            var productReviews = reviewRepository.findByProductIdAndIsDeleteFalse(product.getId());
            double averageRating = 0.0;
            int reviewCount = productReviews.size();
            if (reviewCount > 0) {
                averageRating = productReviews.stream().mapToInt(com.badat.study1.model.Review::getRating).average().orElse(0.0);
            }
            int unreadCount = (int) productReviews.stream().filter(review -> !review.getIsRead()).count();
            var productStat = new java.util.HashMap<String, Object>();
            productStat.put("product", product);
            productStat.put("averageRating", Math.round(averageRating * 10.0) / 10.0);
            productStat.put("reviewCount", reviewCount);
            productStat.put("unreadCount", unreadCount);
            productStats.add(productStat);
        }
        Pageable pageable = PageRequest.of(0, 1000);
        Page<com.badat.study1.model.Review> reviewsPage = reviewRepository.findBySellerIdAndShopIdAndIsDeleteFalse(user.getId(), userShop.get().getId(), pageable);
        model.addAttribute("reviews", reviewsPage.getContent());
        model.addAttribute("productStats", productStats);
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
    public ResponseEntity<?> getReviewsByProduct(User user, Long productId) {
        // Logic nghiệp vụ: kiểm tra product thuộc shop của user
        var product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Không tìm thấy sản phẩm"));
        }
        var userShop = shopRepository.findByUserId(user.getId()).orElse(null);
        if (userShop == null) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Không tìm thấy shop"));
        }
        if (!product.getShopId().equals(userShop.getId())) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Không có quyền truy cập"));
        }
        var reviews = reviewRepository.findByProductIdAndIsDeleteFalse(productId);
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
            var productVariantInfo = new java.util.HashMap<String, Object>();
            productVariantInfo.put("name", review.getProductVariant() != null ? review.getProductVariant().getName() : "N/A");
            dto.put("productVariant", productVariantInfo);
            return dto;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(reviewDTOs);
    }

    @Override
    public ResponseEntity<?> markReviewsAsRead(User user, Long productId) {
        // Logic nghiệp vụ: kiểm tra product thuộc shop của user
        var product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Không tìm thấy sản phẩm"));
        }
        var userShop = shopRepository.findByUserId(user.getId()).orElse(null);
        if (userShop == null) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Không tìm thấy shop"));
        }
        if (!product.getShopId().equals(userShop.getId())) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Không có quyền truy cập"));
        }
        var reviews = reviewRepository.findByProductIdAndIsDeleteFalse(productId);
        int updated = 0;
        for (var review : reviews) {
            if (!review.getIsRead()) {
                review.setIsRead(true);
                reviewRepository.save(review);
                updated++;
            }
        }
        return ResponseEntity.ok(java.util.Map.of("message", "Đã đánh dấu đánh giá là đã đọc", "count", updated));
    }

    @Override
    public String replyToReview(User user, Long reviewId, String sellerReply, RedirectAttributes redirectAttributes) {
        // Logic nghiệp vụ: kiểm tra user có quyền trả lời review này không
        var userShop = shopRepository.findByUserId(user.getId()).orElse(null);
        if (userShop == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy shop của bạn!");
            return "redirect:/seller/reviews";
        }
        var reviewOptional = reviewRepository.findById(reviewId);
        if (reviewOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đánh giá!");
            return "redirect:/seller/reviews";
        }
        var review = reviewOptional.get();
        if (!review.getSellerId().equals(user.getId()) || !review.getShopId().equals(userShop.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền trả lời đánh giá này!");
            return "redirect:/seller/reviews";
        }
        try {
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
                           String productName,
                           String productCategory,
                           String productSubcategory,
                           String shortDescription,
                           String detailedDescription,
                           org.springframework.web.multipart.MultipartFile productImageFile,
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
            product.setProductName(productName);
            product.setProductCategory(productCategory);
            product.setShortDescription(shortDescription);
            product.setDetailedDescription(detailedDescription);
            product.setProductSubcategory(productSubcategory);
            if (productImageFile != null && !productImageFile.isEmpty()) {
                byte[] imageData = productImageFile.getBytes();
                product.setProductImageData(imageData);
            }
            product.setStatus("OPEN");
            product.setCreatedAt(java.time.Instant.now());
            product.setDelete(false);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm đã được tạo thành công!");
            return "redirect:/seller/product-management";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tạo sản phẩm. Vui lòng thử lại!");
            return "redirect:/seller/add-product";
        }
    }

    @Override
    public String editProduct(User user,
                            Long id,
                            String productName,
                            String status,
                            String shortDescription,
                            String detailedDescription,
                            org.springframework.web.multipart.MultipartFile productImageFile,
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
            product.setProductName(productName);
            product.setStatus(status);
            product.setShortDescription(shortDescription);
            product.setDetailedDescription(detailedDescription);
            if (productImageFile != null && !productImageFile.isEmpty()) {
                try {
                    byte[] imageData = productImageFile.getBytes();
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
    public String addProductVariant(User user, Long productId, String productName, java.math.BigDecimal productPrice, RedirectAttributes redirectAttributes) {
        try {
            // Logic nghiệp vụ: tạo product variant
            var productOptional = productRepository.findById(productId);
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
                    .productId(productId)
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
        return "redirect:/seller/product-variant-management/" + productId;
    }

    @Override
    public String editProductVariant(User user, Long productId, String productName, java.math.BigDecimal productPrice, RedirectAttributes redirectAttributes) {
        try {
            // Logic nghiệp vụ: cập nhật product variant
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
    public String updateProductQuantityFromFile(User user, Long productVariantId, org.springframework.web.multipart.MultipartFile file, RedirectAttributes redirectAttributes) {
        final String originalFileName = file != null ? file.getOriginalFilename() : null;
        final String safeFileName = (originalFileName != null && !originalFileName.isBlank()) ? originalFileName : "unknown";
        try {
            // Logic nghiệp vụ: xử lý file và cập nhật kho
            var productVariantOptional = productVariantRepository.findById(productVariantId);
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
            if (file == null || file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "File upload không hợp lệ!");
                return "redirect:/seller/add-quantity/" + productVariantId;
            }
            String content = new String(file.getBytes(), StandardCharsets.UTF_8)
                    .replace("\uFEFF", "")
                    .trim();
            String[] lines = content.split("\\r?\\n");
            int successCount = 0;
            int failureCount = 0;
            StringBuilder invalidLineDetails = new StringBuilder();
            java.util.Set<String> processedItemKeys = new java.util.HashSet<>();
            // Load existing identifiers (scoped by same subcategory) for this variant for duplicate check in DB
            var existingItems = warehouseRepository.findByProductVariantIdAndIsDeleteFalse(productVariant.getId());
            final String currentSubcategory = productVariant.getSubcategory();
            InventoryFormat expectedFormat = determineFormat(currentSubcategory);
            java.util.Set<String> existingKeys = new java.util.HashSet<>();
            for (var it : existingItems) {
                // ensure same subcategory when checking duplicates in DB
                if (it.getItemSubcategory() != null && currentSubcategory != null
                        && it.getItemSubcategory().equalsIgnoreCase(currentSubcategory)) {
                    String[] ep = it.getItemData().split("\\|");
                    String key = extractIdentifier(currentSubcategory, ep);
                    String normalized = normalizeIdentifier(key);
                    if (normalized != null) existingKeys.add(normalized);
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
                        appendInvalidLine(invalidLineDetails, i + 1, "Định dạng không hợp lệ");
                        continue;
                    }
                    String[] sanitizedParts = new String[] {
                            cleanPart(parts[0]),
                            cleanPart(parts[1])
                    };
                    if (sanitizedParts[0] == null || sanitizedParts[1] == null) {
                        failureCount++;
                        appendInvalidLine(invalidLineDetails, i + 1, "Thiếu dữ liệu bắt buộc");
                        continue;
                    }
                    if (!validateLineFormat(expectedFormat, sanitizedParts)) {
                        failureCount++;
                        appendInvalidLine(invalidLineDetails, i + 1, "Dữ liệu không đúng định dạng cho loại sản phẩm");
                        continue;
                    }
                    String itemData = sanitizedParts[0] + "|" + sanitizedParts[1];
                    String itemKey = extractIdentifier(productVariant.getSubcategory(), sanitizedParts);
                    String normalizedKey = normalizeIdentifier(itemKey);
                    // Kiểm tra khóa định danh bắt buộc theo subcategory
                    if (normalizedKey == null) {
                        failureCount++;
                        appendInvalidLine(invalidLineDetails, i + 1, "Không có khóa định danh hợp lệ");
                        continue;
                    }
                    if (processedItemKeys.contains(normalizedKey)) {
                        failureCount++;
                        appendInvalidLine(invalidLineDetails, i + 1, "Dòng trùng lặp trong file");
                        continue;
                    }
                    boolean existsInDb = existingKeys.contains(normalizedKey);
                    if (existsInDb) {
                        failureCount++;
                        appendInvalidLine(invalidLineDetails, i + 1, "Đã tồn tại trong hệ thống");
                        continue;
                    }
                    processedItemKeys.add(normalizedKey);
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
                    String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định";
                    appendInvalidLine(invalidLineDetails, i + 1, "Lỗi xử lý - " + errorMessage);
                }
            }
            long warehouseCount = warehouseRepository.countByProductVariantIdAndLockedFalseAndIsDeleteFalse(productVariantId);
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
                com.badat.study1.model.UploadHistory uploadHistory = com.badat.study1.model.UploadHistory.builder()
                        .fileName(safeFileName)
                        .productName(productVariant.getName())
                        .isSuccess(successCount > 0)
                        .result(successCount > 0 ? "SUCCESS" : "FAILED")
                        .status(successCount > 0 ? "COMPLETED" : "FAILED")
                        .totalItems(lines.length)
                        .successCount(successCount)
                        .failureCount(failureCount)
                        .resultDetails(invalidLineDetails.length() > 0 ? invalidLineDetails.toString() : null)
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
            return "redirect:/seller/add-quantity/" + productVariantId;
        } catch (Exception e) {
            try {
                var productVariantOptional = productVariantRepository.findById(productVariantId);
                if (productVariantOptional.isPresent()) {
                    ProductVariant failedProductVariant = productVariantOptional.get();
                    StringBuilder detailedError = new StringBuilder();
                    detailedError.append("Lỗi xử lý file: ").append(e.getMessage());
                    var failedParentProductOptional = productRepository.findById(failedProductVariant.getProductId());
                    if (failedParentProductOptional.isPresent()) {
                        com.badat.study1.model.UploadHistory uploadHistory = com.badat.study1.model.UploadHistory.builder()
                                .fileName(safeFileName)
                                .productName(failedProductVariant.getName())
                                .isSuccess(false)
                                .result("FAILED")
                                .status("FAILED")
                                .totalItems(0)
                                .successCount(0)
                                .failureCount(1)
                                .resultDetails(detailedError.toString())
                                .productVariant(failedProductVariant)
                                .product(failedParentProductOptional.get())
                                .user(user)
                                .build();
                        uploadHistoryRepository.save(uploadHistory);
                    }
                }
            } catch (Exception ignored) {}
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xử lý file. Vui lòng thử lại!");
            return "redirect:/seller/add-quantity/" + productVariantId;
        }
    }

    @Override
    public ResponseEntity<?> getUploadDetails(User user, Long uploadId) {
        try {
            // Logic nghiệp vụ: kiểm tra quyền truy cập upload details
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

    private String normalizeIdentifier(String key) {
        String cleaned = cleanPart(key);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private String cleanPart(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace("\uFEFF", "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void appendInvalidLine(StringBuilder builder, int lineNumber, String message) {
        if (builder == null || message == null) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append("Dòng ").append(lineNumber).append(": ").append(message);
    }

    private String extractIdentifier(String subcategory, String[] parts) {
        if (parts == null || parts.length < 2) return null;
        String sub = subcategory == null ? "" : subcategory.toLowerCase(Locale.ROOT);
        String first = cleanPart(parts[0]);
        String second = cleanPart(parts[1]);
        if (first == null && second == null) {
            return null;
        }
        // Gmail: email|password -> key = email
        if (sub.contains("gmail")) {
            if (first == null || !first.contains("@")) return null;
            return first;
        }
        // Thẻ cào: code|serial -> key = serial
        if (sub.contains("thẻ") || sub.contains("card")) {
            return second;
        }
        // Tài khoản: username|password -> key = username
        if (sub.contains("tài khoản") || sub.contains("account") || sub.contains("acc")) {
            return first;
        }
        // Default fallback: first part as identifier
        return first;
    }

    private InventoryFormat determineFormat(String subcategory) {
        String sub = subcategory == null ? "" : subcategory.toLowerCase(Locale.ROOT);
        if (sub.contains("gmail")) {
            return InventoryFormat.EMAIL;
        }
        if (sub.contains("thẻ") || sub.contains("card")) {
            return InventoryFormat.CARD;
        }
        if (sub.contains("tài khoản") || sub.contains("account") || sub.contains("acc")) {
            return InventoryFormat.ACCOUNT;
        }
        return InventoryFormat.GENERIC;
    }

    private boolean validateLineFormat(InventoryFormat format, String[] parts) {
        if (parts == null || parts.length < 2) {
            return false;
        }
        String first = parts[0];
        String second = parts[1];
        return switch (format) {
            case EMAIL -> isValidEmail(first);
            case CARD -> isValidCardCode(first) && isValidCardSerial(second);
            case ACCOUNT -> isValidAccount(first);
            case GENERIC -> true;
        };
    }

    private boolean isValidEmail(String value) {
        if (value == null) {
            return false;
        }
        int atIndex = value.indexOf('@');
        if (atIndex <= 0 || atIndex == value.length() - 1) {
            return false;
        }
        if (!value.contains(".")) {
            return false;
        }
        return !value.contains(" ");
    }

    private boolean isValidCardCode(String value) {
        return value != null && value.matches("^[A-Za-z0-9]{6,}$");
    }

    private boolean isValidCardSerial(String value) {
        return value != null && value.matches("^[A-Za-z0-9-]{6,}$");
    }

    private boolean isValidAccount(String value) {
        return value != null && value.length() >= 3;
    }

    private enum InventoryFormat {
        EMAIL,
        CARD,
        ACCOUNT,
        GENERIC
    }
}


