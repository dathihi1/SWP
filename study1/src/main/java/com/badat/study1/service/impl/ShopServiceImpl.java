package com.badat.study1.service.impl;

import com.badat.study1.model.Product;
import com.badat.study1.model.Stall;
import com.badat.study1.model.User;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ReviewRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import com.badat.study1.repository.UploadHistoryRepository;
import com.badat.study1.repository.WarehouseRepository;
import com.badat.study1.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ShopServiceImpl implements ShopService {
    private final ShopRepository shopRepository;
    private final StallRepository stallRepository;
    private final ProductRepository productRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;

    public ShopServiceImpl(ShopRepository shopRepository,
                           StallRepository stallRepository,
                           ProductRepository productRepository,
                           UploadHistoryRepository uploadHistoryRepository,
                           WarehouseRepository warehouseRepository,
                           OrderItemRepository orderItemRepository,
                           ReviewRepository reviewRepository) {
        this.shopRepository = shopRepository;
        this.stallRepository = stallRepository;
        this.productRepository = productRepository;
        this.uploadHistoryRepository = uploadHistoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.orderItemRepository = orderItemRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public String stallManagement(User user, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());

        shopRepository.findByUserId(user.getId()).ifPresent(shop -> {
            var stalls = stallRepository.findByShopIdAndIsDeleteFalse(shop.getId());
            stalls.forEach(stall -> {
                var products = productRepository.findByStallIdAndIsDeleteFalse(stall.getId());
                int totalStock = (int) warehouseRepository.countAvailableItemsByStallId(stall.getId());
                stall.setProductCount(totalStock);
                if (!products.isEmpty()) {
                    var availableProducts = products.stream()
                            .filter(product -> product.getQuantity() != null && product.getQuantity() > 0)
                            .collect(Collectors.toList());
                    if (!availableProducts.isEmpty()) {
                        BigDecimal minPrice = availableProducts.stream().map(Product::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                        BigDecimal maxPrice = availableProducts.stream().map(Product::getPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                        NumberFormat viNumber = NumberFormat.getNumberInstance(Locale.US);
                        viNumber.setGroupingUsed(true);
                        String minStr = viNumber.format(minPrice.setScale(0, RoundingMode.HALF_UP));
                        String maxStr = viNumber.format(maxPrice.setScale(0, RoundingMode.HALF_UP));
                        if (minPrice.equals(maxPrice)) {
                            stall.setPriceRange(minStr + " VND");
                        } else {
                            stall.setPriceRange(minStr + " VND - " + maxStr + " VND");
                        }
                    } else {
                        stall.setPriceRange("Hết hàng");
                    }
                } else {
                    stall.setPriceRange("Chưa có sản phẩm");
                }
            });
            model.addAttribute("stalls", stalls);
            long totalProducts = productRepository.countByShopIdAndIsDeleteFalse(shop.getId());
            model.addAttribute("totalProducts", totalProducts);
        });

        return "seller/stall-management";
    }

    @Override
    public String addStallPage(User user, Model model) {
        boolean hasShop = shopRepository.findByUserId(user.getId()).isPresent();
        if (!hasShop) {
            return "redirect:/seller/stall-management";
        }
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        return "seller/add-stall";
    }

    @Override
    public String editStallPage(User user, Long stallId, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        var stall = stallRepository.findById(stallId);
        if (stall.isEmpty()) {
            return "redirect:/seller/stall-management";
        }
        shopRepository.findByUserId(user.getId()).ifPresent(shop -> {
            if (!stall.get().getShopId().equals(shop.getId())) {
                // silently ignore, redirect handled below
            }
        });
        model.addAttribute("stall", stall.get());
        return "seller/edit-stall";
    }

    @Override
    public String productManagementPage(User user, Long stallId, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        var stallOptional = stallRepository.findById(stallId);
        if (stallOptional.isEmpty()) {
            return "redirect:/seller/stall-management";
        }
        Stall stall = stallOptional.get();
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty() || !stall.getShopId().equals(userShop.get().getId())) {
            return "redirect:/seller/stall-management";
        }
        model.addAttribute("stall", stall);
        var products = productRepository.findByStallIdAndIsDeleteFalse(stallId);
        for (Product p : products) {
            long stock = warehouseRepository.countByProductIdAndLockedFalseAndIsDeleteFalse(p.getId());
            p.setQuantity((int) stock);
            p.setStatus(stock > 0 ? Product.Status.AVAILABLE : Product.Status.UNAVAILABLE);
        }
        model.addAttribute("products", products);
        return "seller/product-management";
    }

    @Override
    public String addQuantityPage(User user, Long productId, int page, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        var productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            return "redirect:/seller/stall-management";
        }
        var product = productOptional.get();
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return "redirect:/seller/stall-management";
        }
        var stallOptional = stallRepository.findById(product.getStallId());
        if (stallOptional.isEmpty() || !stallOptional.get().getShopId().equals(userShop.get().getId())) {
            return "redirect:/seller/stall-management";
        }
        model.addAttribute("product", product);
        model.addAttribute("stall", stallOptional.get());
        int validatedPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validatedPage, 5);
        Page<com.badat.study1.model.UploadHistory> uploadHistoryPage = uploadHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        if (validatedPage >= uploadHistoryPage.getTotalPages() && uploadHistoryPage.getTotalPages() > 0) {
            validatedPage = uploadHistoryPage.getTotalPages() - 1;
            pageable = PageRequest.of(validatedPage, 5);
            uploadHistoryPage = uploadHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
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
            return "redirect:/seller/stall-management";
        }
        var stalls = stallRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
        var products = productRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
        int validatedPage = Math.max(0, page);
        java.util.List<com.badat.study1.model.OrderItem> allOrderItems = orderItemRepository.findByWarehouseUserOrderByCreatedAtDesc(user.getId());
        java.util.List<com.badat.study1.model.OrderItem> filteredOrderItems = allOrderItems.stream()
                .filter(orderItem -> status == null || status.isEmpty() || orderItem.getStatus().name().equals(status.toUpperCase()))
                .filter(orderItem -> stallId == null || stallId.equals(orderItem.getWarehouse().getStall().getId()))
                .filter(orderItem -> productId == null || productId.equals(orderItem.getProductId()))
                .filter(orderItem -> {
                    if (dateFrom == null || dateFrom.isEmpty()) return true;
                    return orderItem.getCreatedAt().toLocalDate().isAfter(java.time.LocalDate.parse(dateFrom).minusDays(1));
                })
                .filter(orderItem -> {
                    if (dateTo == null || dateTo.isEmpty()) return true;
                    return orderItem.getCreatedAt().toLocalDate().isBefore(java.time.LocalDate.parse(dateTo).plusDays(1));
                })
                .collect(java.util.stream.Collectors.toList());
        // Group items by order
        java.util.Map<com.badat.study1.model.Order, java.util.List<com.badat.study1.model.OrderItem>> byOrder = new java.util.LinkedHashMap<>();
        for (com.badat.study1.model.OrderItem oi : filteredOrderItems) {
            byOrder.computeIfAbsent(oi.getOrder(), k -> new java.util.ArrayList<>()).add(oi);
        }
        // Build per-order summaries
        java.util.List<java.util.Map<String, Object>> groupedOrders = new java.util.ArrayList<>();
        for (var entry : byOrder.entrySet()) {
            com.badat.study1.model.Order order = entry.getKey();
            java.util.List<com.badat.study1.model.OrderItem> items = entry.getValue();
            java.math.BigDecimal totalAmount = items.stream()
                    .map(com.badat.study1.model.OrderItem::getTotalAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            int totalQuantity = items.stream()
                    .map(com.badat.study1.model.OrderItem::getQuantity)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            String firstProductName = items.isEmpty() ? "" : items.get(0).getProduct().getName();
            String stallName = items.isEmpty() ? "" : items.get(0).getWarehouse().getStall().getStallName();
            java.math.BigDecimal firstUnitPrice = items.isEmpty() ? java.math.BigDecimal.ZERO : items.get(0).getUnitPrice();
            var firstStatus = items.isEmpty() ? null : items.get(0).getStatus();
            java.time.LocalDateTime createdAt = items.isEmpty() ? null : items.get(0).getCreatedAt();
            java.util.Map<String, Object> summary = new java.util.HashMap<>();
            summary.put("order", order);
            summary.put("items", items);
            summary.put("totalAmount", totalAmount);
            summary.put("totalQuantity", totalQuantity);
            summary.put("firstProductName", firstProductName);
            summary.put("stallName", stallName);
            summary.put("unitPrice", firstUnitPrice);
            summary.put("status", firstStatus);
            summary.put("createdAt", createdAt);
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
        java.util.List<java.util.Map<String, Object>> pageContent = groupedOrders.subList(start, end);
        model.addAttribute("stalls", stalls);
        model.addAttribute("products", products);
        model.addAttribute("orders", pageContent);
        // Filters
        model.addAttribute("orderStatuses", com.badat.study1.model.OrderItem.Status.values());
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
        model.addAttribute("currentPage", page);
        // Load stats
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return "redirect:/seller/stall-management";
        }
        var stalls = stallRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
        var stallStats = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (var stall : stalls) {
            var stallReviews = reviewRepository.findByStallIdAndIsDeleteFalse(stall.getId());
            double averageRating = 0.0;
            int reviewCount = stallReviews.size();
            if (reviewCount > 0) {
                averageRating = stallReviews.stream().mapToInt(com.badat.study1.model.Review::getRating).average().orElse(0.0);
            }
            int unreadCount = (int) stallReviews.stream().filter(review -> !review.getIsRead()).count();
            var stallStat = new java.util.HashMap<String, Object>();
            stallStat.put("stall", stall);
            stallStat.put("averageRating", Math.round(averageRating * 10.0) / 10.0);
            stallStat.put("reviewCount", reviewCount);
            stallStat.put("unreadCount", unreadCount);
            stallStats.add(stallStat);
        }
        Pageable pageable = PageRequest.of(page, 10);
        Page<com.badat.study1.model.Review> reviewsPage = reviewRepository.findBySellerIdAndShopIdAndIsDeleteFalse(user.getId(), userShop.get().getId(), pageable);
        model.addAttribute("reviews", reviewsPage.getContent());
        model.addAttribute("totalPages", reviewsPage.getTotalPages());
        model.addAttribute("totalElements", reviewsPage.getTotalElements());
        model.addAttribute("hasNext", reviewsPage.hasNext());
        model.addAttribute("hasPrevious", reviewsPage.hasPrevious());
        model.addAttribute("stallStats", stallStats);
        return "seller/reviews";
    }

    @Override
    public String sellerShopPage(User user, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        return "seller/shop";
    }

    @Override
    public String sellerProductsPage(User user, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("userRole", user.getRole().name());
        return "seller/shop";
    }

    @Override
    public ResponseEntity<?> getReviewsByStall(User user, Long stallId) {
        var stall = stallRepository.findById(stallId);
        if (stall.isEmpty()) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Stall not found"));
        }
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Shop not found"));
        }
        if (!stall.get().getShopId().equals(userShop.get().getId())) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Access denied"));
        }
        var reviews = reviewRepository.findByStallIdAndIsDeleteFalse(stallId);
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
            productInfo.put("name", review.getProduct().getName());
            dto.put("product", productInfo);
            return dto;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(reviewDTOs);
    }

    @Override
    public ResponseEntity<?> markReviewsAsRead(User user, Long stallId) {
        var stall = stallRepository.findById(stallId);
        if (stall.isEmpty()) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Stall not found"));
        }
        var userShop = shopRepository.findByUserId(user.getId());
        if (userShop.isEmpty()) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Shop not found"));
        }
        if (!stall.get().getShopId().equals(userShop.get().getId())) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Access denied"));
        }
        var reviews = reviewRepository.findByStallIdAndIsDeleteFalse(stallId);
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
    public String addStall(User user,
                           String stallName,
                           String businessType,
                           String stallCategory,
                           Double discount,
                           String shortDescription,
                           String detailedDescription,
                           org.springframework.web.multipart.MultipartFile stallImageFile,
                           Boolean uniqueProducts,
                           String isCropped,
                           RedirectAttributes redirectAttributes) {

        try {
            var userShop = shopRepository.findByUserId(user.getId()).orElse(null);
            if (userShop == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa có shop. Vui lòng tạo shop trước khi tạo gian hàng!");
                return "redirect:/seller/stall-management";
            }
            long currentStallCount = stallRepository.countByShopIdAndIsDeleteFalse(userShop.getId());
            if (currentStallCount >= 5) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã đạt giới hạn tối đa 5 gian hàng. Không thể tạo thêm gian hàng mới!");
                return "redirect:/seller/add-stall";
            }
            Stall stall = new Stall();
            stall.setShopId(userShop.getId());
            stall.setStallName(stallName);
            stall.setBusinessType(businessType);
            stall.setStallCategory(stallCategory);
            stall.setDiscountPercentage(discount);
            stall.setShortDescription(shortDescription);
            stall.setDetailedDescription(detailedDescription);
            if (stallImageFile != null && !stallImageFile.isEmpty()) {
                byte[] imageData = stallImageFile.getBytes();
                stall.setStallImageData(imageData);
            }
            stall.setStatus("PENDING");
            stall.setActive(false);
            stall.setCreatedAt(java.time.Instant.now());
            stall.setDelete(false);
            stallRepository.save(stall);
            redirectAttributes.addFlashAttribute("successMessage", "Gian hàng đã được tạo thành công và đang chờ duyệt!");
            return "redirect:/seller/stall-management";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tạo gian hàng. Vui lòng thử lại!");
            return "redirect:/seller/add-stall";
        }
    }

    @Override
    public String editStall(User user,
                            Long id,
                            String stallName,
                            String status,
                            String shortDescription,
                            String detailedDescription,
                            org.springframework.web.multipart.MultipartFile stallImageFile,
                            RedirectAttributes redirectAttributes) {
        try {
            var stallOptional = stallRepository.findById(id);
            if (stallOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy gian hàng!");
                return "redirect:/seller/stall-management";
            }
            Stall stall = stallOptional.get();
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !stall.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền sửa gian hàng này!");
                return "redirect:/seller/stall-management";
            }
            if ("OPEN".equals(status)) {
                long availableStock = warehouseRepository.countAvailableItemsByStallId(stall.getId());
                if (availableStock <= 0) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không thể mở gian hàng! Gian hàng phải có ít nhất 1 sản phẩm trong kho.");
                    return "redirect:/seller/edit-stall/" + id;
                }
            }
            // Apply updates (controller already validated no-op and readonly tampering)
            stall.setStallName(stallName);
            if ("REJECTED".equals(stall.getStatus())) {
                // Controller enforces PENDING; reset review fields
                stall.setStatus("PENDING");
                stall.setActive(false);
                stall.setApprovedAt(null);
                stall.setApprovedBy(null);
                stall.setApprovalReason(null);
            } else {
                stall.setStatus(status);
            }
            stall.setShortDescription(shortDescription);
            stall.setDetailedDescription(detailedDescription);
            if (stallImageFile != null && !stallImageFile.isEmpty()) {
                try {
                    byte[] imageData = stallImageFile.getBytes();
                    stall.setStallImageData(imageData);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xử lý hình ảnh. Vui lòng thử lại!");
                    return "redirect:/seller/edit-stall/" + id;
                }
            }

            stallRepository.save(stall);
            redirectAttributes.addFlashAttribute("successMessage", "Gian hàng đã được cập nhật thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật gian hàng. Vui lòng thử lại!");
        }
        return "redirect:/seller/stall-management";
    }

    @Override
    public String addProduct(User user, Long stallId, String productName, java.math.BigDecimal productPrice, RedirectAttributes redirectAttributes) {
        try {
            var stallOptional = stallRepository.findById(stallId);
            if (stallOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy gian hàng!");
                return "redirect:/seller/stall-management";
            }
            Stall stall = stallOptional.get();
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !stall.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thêm sản phẩm vào gian hàng này!");
                return "redirect:/seller/stall-management";
            }
            Product product;
            String uniqueKey = "PROD_" + System.currentTimeMillis() + "_" + user.getId();
            product = Product.builder()
                    .shopId(stall.getShopId())
                    .stallId(stallId)
                    .type("Khác")
                    .name(productName)
                    .description("")
                    .price(productPrice)
                    .quantity(0)
                    .uniqueKey(uniqueKey)
                    .status(Product.Status.UNAVAILABLE)
                    .build();
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm đã được thêm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi thêm sản phẩm. Vui lòng thử lại!");
        }
        return "redirect:/seller/product-management/" + stallId;
    }

    @Override
    public String updateProductQuantity(User user, Long productId, Integer newQuantity, RedirectAttributes redirectAttributes) {
        try {
            var productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
                return "redirect:/seller/stall-management";
            }
            Product product = productOptional.get();
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !product.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật sản phẩm này!");
                return "redirect:/seller/stall-management";
            }
            product.setQuantity(newQuantity);
            if (newQuantity != null && newQuantity > 0) {
                product.setStatus(Product.Status.AVAILABLE);
            } else {
                product.setStatus(Product.Status.UNAVAILABLE);
            }
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật số lượng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật số lượng. Vui lòng thử lại!");
        }
        Long stallId = productRepository.findById(productId).get().getStallId();
        return "redirect:/seller/product-management/" + stallId;
    }

    @Override
    public String updateProduct(User user, Long productId, String productName, java.math.BigDecimal productPrice, RedirectAttributes redirectAttributes) {
        try {
            var productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
                return "redirect:/seller/stall-management";
            }
            Product product = productOptional.get();
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !product.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền sửa sản phẩm này!");
                return "redirect:/seller/stall-management";
            }
            product.setName(productName);
            product.setPrice(productPrice);
            product.setUpdatedAt(java.time.LocalDateTime.now());
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm đã được cập nhật thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật sản phẩm. Vui lòng thử lại!");
        }
        return "redirect:/seller/product-management/" + productRepository.findById(productId).get().getStallId();
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
            var productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
                return "redirect:/seller/stall-management";
            }
            Product product = productOptional.get();
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty()) {
                return "redirect:/seller/stall-management";
            }
            Stall stall = stallRepository.findById(product.getStallId()).orElse(null);
            if (stall == null || !stall.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật kho cho gian hàng này!");
                return "redirect:/seller/stall-management";
            }
            String content = new String(file.getBytes(), "UTF-8").trim();
            String[] lines = content.split("\\r?\\n");
            int successCount = 0;
            int failureCount = 0;
            StringBuilder resultDetails = new StringBuilder();
            com.badat.study1.model.Warehouse.ItemType expectedType = determineItemTypeFromStall(stall.getStallCategory());
            java.util.Set<String> processedItemsInFile = new java.util.HashSet<>();
            java.util.Set<String> processedItemKeys = new java.util.HashSet<>();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                try {
                    String[] parts = line.split("\\|");
                    if (parts.length < 2) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Định dạng không hợp lệ (cần ít nhất 2 phần)\n");
                        continue;
                    }
                    String itemType = parts[0].toUpperCase();
                    String itemData = line;
                    if (processedItemsInFile.contains(itemData)) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Item trùng lặp trong file (").append(itemType).append(")\n");
                        continue;
                    }
                    String itemKey = null;
                    if (parts.length >= 2) {
                        if ("CARD".equals(itemType) && parts.length >= 3) {
                            itemKey = parts[2];
                        } else if ("EMAIL".equals(itemType)) {
                            itemKey = parts[1];
                        } else if ("ACCOUNT".equals(itemType)) {
                            itemKey = parts[1];
                        } else if ("KEY".equals(itemType)) {
                            itemKey = parts[1];
                        }
                    }
                    if (itemKey != null && processedItemKeys.contains(itemKey)) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": ").append(itemType).append(" với ").append(itemKey).append(" đã tồn tại trong file\n");
                        continue;
                    }
                    com.badat.study1.model.Warehouse.ItemType type;
                    try {
                        type = com.badat.study1.model.Warehouse.ItemType.valueOf(itemType);
                    } catch (IllegalArgumentException e) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Loại sản phẩm không hợp lệ (").append(itemType).append(")\n");
                        continue;
                    }
                    boolean isValidType = (type == expectedType);
                    if (!isValidType) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Loại sản phẩm không khớp với gian hàng\n");
                        continue;
                    }
                    java.util.List<com.badat.study1.model.Warehouse> existingItems = warehouseRepository.findByItemTypeOrderByCreatedAtDesc(type);
                    com.badat.study1.model.Warehouse existingItem = null;
                    for (com.badat.study1.model.Warehouse item : existingItems) {
                        boolean isDuplicate = false;
                        if (itemKey != null) {
                            String[] existingParts = item.getItemData().split("\\|");
                            String existingKey = null;
                            if ("CARD".equals(itemType) && existingParts.length >= 3) {
                                existingKey = existingParts[2];
                            } else if ("EMAIL".equals(itemType) && existingParts.length >= 2) {
                                existingKey = existingParts[1];
                            } else if ("ACCOUNT".equals(itemType) && existingParts.length >= 2) {
                                existingKey = existingParts[1];
                            } else if ("KEY".equals(itemType) && existingParts.length >= 2) {
                                existingKey = existingParts[1];
                            }
                            isDuplicate = (existingKey != null && existingKey.equals(itemKey));
                        } else {
                            isDuplicate = item.getItemData().equals(itemData);
                        }
                        if (isDuplicate) {
                            existingItem = item;
                            break;
                        }
                    }
                    if (existingItem != null && !existingItem.getIsDelete()) {
                        failureCount++;
                        if (itemKey != null) {
                            resultDetails.append("Dòng ").append(i + 1).append(": Item đã tồn tại trong hệ thống (").append(itemType).append(" với ").append(itemKey).append(")\n");
                        } else {
                            resultDetails.append("Dòng ").append(i + 1).append(": Item đã tồn tại trong hệ thống (").append(itemType).append(")\n");
                        }
                        continue;
                    }
                    if (existingItem != null && existingItem.getIsDelete() && existingItem.getLocked()) {
                        failureCount++;
                        if (itemKey != null) {
                            resultDetails.append("Dòng ").append(i + 1).append(": Item đã bị khóa và không thể khôi phục (").append(itemType).append(" với ").append(itemKey).append(")\n");
                        } else {
                            resultDetails.append("Dòng ").append(i + 1).append(": Item đã bị khóa và không thể khôi phục (").append(itemType).append(")\n");
                        }
                        continue;
                    }
                    processedItemsInFile.add(itemData);
                    if (itemKey != null) {
                        processedItemKeys.add(itemKey);
                    }
                    if (existingItem != null && existingItem.getIsDelete() && !existingItem.getLocked()) {
                        existingItem.setIsDelete(false);
                        existingItem.setDeletedBy(null);
                        warehouseRepository.save(existingItem);
                        if (itemKey != null) {
                            resultDetails.append("Dòng ").append(i + 1).append(": Khôi phục item (").append(itemType).append(" với ").append(itemKey).append(")\n");
                        } else {
                            resultDetails.append("Dòng ").append(i + 1).append(": Khôi phục item (").append(itemType).append(")\n");
                        }
                    } else {
                        com.badat.study1.model.Warehouse warehouseItem = com.badat.study1.model.Warehouse.builder()
                                .itemType(type)
                                .itemData(itemData)
                                .product(product)
                                .shop(userShop.get())
                                .stall(stall)
                                .user(user)
                                .build();
                        warehouseRepository.save(warehouseItem);
                        if (itemKey != null) {
                            resultDetails.append("Dòng ").append(i + 1).append(": Thêm mới item (").append(itemType).append(" với ").append(itemKey).append(")\n");
                        } else {
                            resultDetails.append("Dòng ").append(i + 1).append(": Thêm mới item (").append(itemType).append(")\n");
                        }
                    }
                    successCount++;
                } catch (Exception ex) {
                    failureCount++;
                    resultDetails.append("Dòng ").append(i + 1).append(": Lỗi xử lý - ").append(ex.getMessage()).append("\n");
                }
            }
            long warehouseCount = productRepository.countWarehouseItemsByProductId(productId);
            product.setQuantity((int) warehouseCount);
            if (warehouseCount > 0) {
                product.setStatus(Product.Status.AVAILABLE);
            } else {
                product.setStatus(Product.Status.UNAVAILABLE);
            }
            productRepository.save(product);
            try {
                if (userShop.isPresent()) {
                    var allProducts = productRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
                    for (Product shopProduct : allProducts) {
                        long productWarehouseCount = productRepository.countWarehouseItemsByProductId(shopProduct.getId());
                        shopProduct.setQuantity((int) productWarehouseCount);
                        if (productWarehouseCount > 0) {
                            shopProduct.setStatus(Product.Status.AVAILABLE);
                        } else {
                            shopProduct.setStatus(Product.Status.UNAVAILABLE);
                        }
                        productRepository.save(shopProduct);
                    }
                }
            } catch (Exception ignore) {}
            try {
                StringBuilder detailedResult = new StringBuilder();
                detailedResult.append("Tên mặt hàng: ").append(product.getName()).append("\n");
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
                        .productName(product.getName())
                        .isSuccess(successCount > 0)
                        .result(successCount > 0 ? "SUCCESS" : "FAILED")
                        .status(successCount > 0 ? "COMPLETED" : "FAILED")
                        .totalItems(lines.length)
                        .successCount(successCount)
                        .failureCount(failureCount)
                        .resultDetails(detailedResult.toString())
                        .product(product)
                        .stall(stall)
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
                var productOptional = productRepository.findById(productId);
                if (productOptional.isPresent()) {
                    Product failedProduct = productOptional.get();
                    StringBuilder detailedResult = new StringBuilder();
                    detailedResult.append("Tên mặt hàng: ").append(failedProduct.getName()).append("\n");
                    detailedResult.append("Tên file: ").append(file.getOriginalFilename()).append("\n");
                    detailedResult.append("Ngày upload: ").append(java.time.LocalDateTime.now().toString()).append("\n");
                    detailedResult.append("Tổng số dòng: 0\n");
                    detailedResult.append("Thành công: 0\n");
                    detailedResult.append("Thất bại: 1\n");
                    detailedResult.append("Trạng thái: THẤT BẠI\n");
                    detailedResult.append("\nChi tiết lỗi:\n").append("Lỗi xử lý file: ").append(e.getMessage());
                    var failedStallOptional = stallRepository.findById(failedProduct.getStallId());
                    if (failedStallOptional.isPresent()) {
                        com.badat.study1.model.UploadHistory uploadHistory = com.badat.study1.model.UploadHistory.builder()
                                .fileName(file.getOriginalFilename())
                                .productName(failedProduct.getName())
                                .isSuccess(false)
                                .result("FAILED")
                                .status("FAILED")
                                .totalItems(0)
                                .successCount(0)
                                .failureCount(1)
                                .resultDetails(detailedResult.toString())
                                .product(failedProduct)
                                .stall(failedStallOptional.get())
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

    @Override
    public ResponseEntity<?> updateProductQuantities(User user) {
        try {
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty()) {
                return ResponseEntity.badRequest().body("User shop not found");
            }
            var products = productRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
            int updatedCount = 0;
            for (Product product : products) {
                long warehouseCount = productRepository.countWarehouseItemsByProductId(product.getId());
                product.setQuantity((int) warehouseCount);
                product.setStatus(warehouseCount > 0 ? Product.Status.AVAILABLE : Product.Status.UNAVAILABLE);
                productRepository.save(product);
                updatedCount++;
            }
            return ResponseEntity.ok("Đã cập nhật quantity cho " + updatedCount + " sản phẩm từ warehouse");
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    private com.badat.study1.model.Warehouse.ItemType determineItemTypeFromStall(String stallCategory) {
        if (stallCategory == null) {
            return com.badat.study1.model.Warehouse.ItemType.KEY;
        }
        String category = stallCategory.toLowerCase();
        if (category.contains("tài khoản") || category.contains("account") || category.contains("acc")) {
            return com.badat.study1.model.Warehouse.ItemType.EMAIL;
        } else if (category.contains("thẻ") || category.contains("card") || category.contains("gift") ||
                category.contains("điện thoại") || category.contains("phone")) {
            return com.badat.study1.model.Warehouse.ItemType.CARD;
        } else if (category.contains("key") || category.contains("game") || category.contains("software")) {
            return com.badat.study1.model.Warehouse.ItemType.KEY;
        } else {
            return com.badat.study1.model.Warehouse.ItemType.KEY;
        }
    }
}


