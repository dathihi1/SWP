package com.badat.study1.controller;



import com.badat.study1.model.Product;
import com.badat.study1.model.ProductVariant;
import com.badat.study1.model.User;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ProductVariantRepository;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.UploadHistoryRepository;
import com.badat.study1.repository.WarehouseRepository;
import com.badat.study1.repository.ReviewRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import java.math.BigDecimal;


@Controller
public class ShopController {
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final com.badat.study1.service.ShopService shopService;
    

    public ShopController(ShopRepository shopRepository, ProductRepository productRepository, ProductVariantRepository productVariantRepository, UploadHistoryRepository uploadHistoryRepository, WarehouseRepository warehouseRepository, OrderItemRepository orderItemRepository, ReviewRepository reviewRepository, com.badat.study1.service.ShopService shopService) {
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.uploadHistoryRepository = uploadHistoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.shopService = shopService;
    }

    @PostMapping("/seller/add-product")
    public String addProduct(@RequestParam String stallName,
                          @RequestParam String stallCategory,
                          @RequestParam(required = false, name = "productSubcategory") String productSubcategory,
                          @RequestParam String shortDescription,
                          @RequestParam String detailedDescription,
                          @RequestParam(required = false) MultipartFile stallImageFile,
                          @RequestParam(required = false) Boolean uniqueProducts,
                          @RequestParam(required = false) String isCropped,
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
        
        if (stallName == null || stallName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên sản phẩm là bắt buộc!");
            return "redirect:/seller/add-product";
        }
        if (stallCategory == null || stallCategory.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loại sản phẩm là bắt buộc!");
            return "redirect:/seller/add-product";
        }
        if (shortDescription == null || shortDescription.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả ngắn là bắt buộc!");
            return "redirect:/seller/add-product";
        }
        if (detailedDescription == null || detailedDescription.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả chi tiết là bắt buộc!");
            return "redirect:/seller/add-product";
        }
        // Validate specific category when applicable
        String stallCategoryTrim = stallCategory.trim();
        String subcategoryTrim = productSubcategory == null ? "" : productSubcategory.trim();
        if (!"Khác".equalsIgnoreCase(stallCategoryTrim)) {
            if (subcategoryTrim.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn loại cụ thể cho sản phẩm!");
                return "redirect:/seller/add-product";
            }
            // Enforce allowed subcategories by category
            java.util.Set<String> allowed;
            if ("Email".equalsIgnoreCase(stallCategoryTrim)) {
                allowed = new java.util.HashSet<>(java.util.Arrays.asList("Gmail"));
            } else if ("Tài khoản".equalsIgnoreCase(stallCategoryTrim)) {
                allowed = new java.util.HashSet<>(java.util.Arrays.asList("Facebook","Twitter","Telegram","Instagram","Discord","Quizlet","Khác"));
            } else if ("Thẻ cào (Game, Điện thoại)".equalsIgnoreCase(stallCategoryTrim)) {
                allowed = new java.util.HashSet<>(java.util.Arrays.asList("Garena","Zing","Vcoin","Gate","Khác"));
            } else {
                allowed = java.util.Collections.emptySet();
            }
            if (!allowed.isEmpty() && !allowed.contains(subcategoryTrim)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Loại cụ thể không hợp lệ cho loại sản phẩm đã chọn!");
                return "redirect:/seller/add-product";
            }
        }
        int shortWordCount = (int) java.util.Arrays.stream(shortDescription.trim().split("\\s+")).filter(s -> !s.isBlank()).count();
        int detailedWordCount = (int) java.util.Arrays.stream(detailedDescription.trim().split("\\s+")).filter(s -> !s.isBlank()).count();
        if (shortWordCount > 150) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả ngắn không được vượt quá 150 từ!");
            return "redirect:/seller/add-product";
        }
        if (detailedWordCount > 500) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả chi tiết không được vượt quá 500 từ!");
            return "redirect:/seller/add-product";
        }
        if (uniqueProducts == null || !uniqueProducts) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn phải đồng ý với cam kết 'Sản phẩm không trùng lặp' để tạo gian hàng!");
            return "redirect:/seller/add-product";
        }
        if (stallImageFile == null || stallImageFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hình ảnh sản phẩm là bắt buộc!");
            return "redirect:/seller/add-product";
        }
        if (isCropped == null || !isCropped.equals("true")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn phải cắt ảnh trước khi tạo sản phẩm!");
            return "redirect:/seller/add-product";
        }
        
        return shopService.addProduct(user, stallName, stallCategory, productSubcategory, shortDescription, detailedDescription, stallImageFile, uniqueProducts, isCropped, redirectAttributes);
    }

    @GetMapping("/seller/gross-sales")
    public String sellerShopPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        return shopService.sellerShopPage(user, model);
    }


    @GetMapping("/seller/product-management")
    public String sellerShopManagementPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();

        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        return shopService.productManagementOverview(user, model);
    }
            
    @GetMapping("/seller/add-product")
    public String sellerAddProductPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        return shopService.addProductPage(user, model);
    }

    @GetMapping("/seller/edit-product/{id}")
    public String sellerEditProductPage(@PathVariable Long id, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        return shopService.editProductPage(user, id, model);
    }

    @GetMapping("/seller/product-management/{stallId}")
    public String sellerProductManagementPage(@PathVariable Long stallId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        return shopService.productManagementPage(user, stallId, model);
            }
            
    @GetMapping("/seller/add-quantity/{productId}")
    public String sellerAddQuantityPage(@PathVariable Long productId,
                                       @RequestParam(defaultValue = "0") int page,
                                       Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        return shopService.addQuantityPage(user, productId, page, model);
    }

    @GetMapping("/seller/orders")
    public String sellerOrdersPage(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) Long stallId,
                                   @RequestParam(required = false) Long productId,
                                   @RequestParam(required = false) String dateFrom,
                                   @RequestParam(required = false) String dateTo,
                                   Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        return shopService.ordersPage(user, page, status, stallId, productId, dateFrom, dateTo, model);
    }

    @GetMapping("/seller/reviews")
    public String sellerReviewsPage(@RequestParam(defaultValue = "0") int page,
                                   Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        return shopService.reviewsPage(user, page, model);
    }

    @GetMapping("/api/seller/reviews/stall/{stallId}")
    @ResponseBody
    public ResponseEntity<?> getReviewsByStall(@PathVariable Long stallId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Unauthorized"));
        }

        User user = (User) authentication.getPrincipal();

        if (!user.getRole().equals(User.Role.SELLER)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Forbidden"));
        }

        try {
            return shopService.getReviewsByStall(user, stallId);
                } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/api/seller/reviews/mark-read")
    @ResponseBody
    public ResponseEntity<?> markReviewsAsRead(@RequestParam Long stallId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Unauthorized"));
        }

        User user = (User) authentication.getPrincipal();

        if (!user.getRole().equals(User.Role.SELLER)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Forbidden"));
        }

        try {
            return shopService.markReviewsAsRead(user, stallId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/seller/reviews/{reviewId}/reply")
    public String replyToReview(@PathVariable Long reviewId,
                                @RequestParam String sellerReply,
                                RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }

        return shopService.replyToReview(user, reviewId, sellerReply, redirectAttributes);
    }

    @GetMapping("/stall-image/{stallId}")
    public ResponseEntity<byte[]> getStallImage(@PathVariable Long stallId) {
        try {
            Product product = productRepository.findById(stallId).orElse(null);
            if (product == null || product.getProductImageData() == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(product.getProductImageData().length);
            // Disable caching to ensure latest image shows after update
            headers.setCacheControl("no-store, no-cache, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            return new ResponseEntity<>(product.getProductImageData(), headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/seller/edit-product/{id}")
    public String editProduct(@PathVariable Long id,
                          @RequestParam String stallName,
                          @RequestParam String status,
                          @RequestParam String shortDescription,
                          @RequestParam String detailedDescription,
                          @RequestParam(required = false) MultipartFile stallImageFile,
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
        
        // Controller-level validations and readonly protection
            var productOptional = productRepository.findById(id);
            if (productOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
                return "redirect:/seller/product-management";
            }
            Product product = productOptional.get();
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !product.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền sửa sản phẩm này!");
                return "redirect:/seller/product-management";
            }
            
        String stallNameTrim = stallName == null ? "" : stallName.trim();
        String shortDescTrim = shortDescription == null ? "" : shortDescription.trim();
        String detailedDescTrim = detailedDescription == null ? "" : detailedDescription.trim();

        if (stallNameTrim.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên sản phẩm là bắt buộc!");
            return "redirect:/seller/edit-product/" + id;
        }
        if (shortDescTrim.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả ngắn là bắt buộc!");
                    return "redirect:/seller/edit-product/" + id;
                }
        if (detailedDescTrim.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả chi tiết là bắt buộc!");
            return "redirect:/seller/edit-product/" + id;
        }

        int shortWordCount = (int) java.util.Arrays.stream(shortDescTrim.split("\\s+")).filter(s -> !s.isBlank()).count();
        int detailedWordCount = (int) java.util.Arrays.stream(detailedDescTrim.split("\\s+")).filter(s -> !s.isBlank()).count();
        if (shortWordCount > 150) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả ngắn không được vượt quá 150 từ!");
            return "redirect:/seller/edit-product/" + id;
        }
        if (detailedWordCount > 500) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả chi tiết không được vượt quá 500 từ!");
            return "redirect:/seller/edit-product/" + id;
        }

        // Validate status input: only OPEN or CLOSED are allowed
        String incomingStatus = status == null ? "" : status.trim();
        if (!("OPEN".equals(incomingStatus) || "CLOSED".equals(incomingStatus))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ!");
            return "redirect:/seller/edit-product/" + id;
        }
            
        // No-op change detection: if all fields are unchanged and no new image is uploaded, block update
        String targetStatus = incomingStatus;
        boolean sameName = stallNameTrim.equals(product.getProductName());
        boolean sameStatus = targetStatus.equals(product.getStatus());
        boolean sameShort = shortDescTrim.equals(product.getShortDescription());
        boolean sameDetailed = detailedDescTrim.equals(product.getDetailedDescription());
        boolean noNewImage = (stallImageFile == null || stallImageFile.isEmpty());
        if (sameName && sameStatus && sameShort && sameDetailed && noNewImage) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không có thay đổi nào để cập nhật hoặc trường không được phép thay đổi!");
            return "redirect:/seller/edit-product/" + id;
        }

        return shopService.editProduct(user, id, stallNameTrim, incomingStatus, shortDescTrim, detailedDescTrim, stallImageFile, redirectAttributes);
    }

    @PostMapping("/seller/add-product/{stallId}")
    public String addProduct(@PathVariable Long stallId,
                           @RequestParam String productName,
                           @RequestParam(required = false) BigDecimal productPrice,
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
        
        if (productName == null || productName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên mặt hàng là bắt buộc!");
            return "redirect:/seller/product-management/" + stallId;
        }
        if (productPrice == null || productPrice.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giá tiền phải lớn hơn 0!");
            return "redirect:/seller/product-management/" + stallId;
        }
        
        return shopService.addProduct(user, stallId, productName, productPrice, redirectAttributes);
    }

    @PostMapping("/seller/update-product-quantity/{productId}")
    public String updateProductQuantity(@PathVariable Long productId,
                                      @RequestParam Integer newQuantity,
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
        
        return shopService.updateProductQuantity(user, productId, newQuantity, redirectAttributes);
    }

    @PostMapping("/seller/update-product-quantity-file/{productId}")
    public String updateProductQuantityFromFile(@PathVariable Long productId,
                                             @RequestParam("file") MultipartFile file,
                                             RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }
        
        return shopService.updateProductQuantityFromFile(user, productId, file, redirectAttributes);
    }

    @PostMapping("/seller/update-product/{productId}")
    public String updateProduct(@PathVariable Long productId,
                              @RequestParam String productName,
                              @RequestParam(required = false) BigDecimal productPrice,
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
        
        if (productName == null || productName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên mặt hàng là bắt buộc!");
            Long parentProductId = productVariantRepository.findById(productId).map(ProductVariant::getProductId).orElse(0L);
            return "redirect:/seller/product-management/" + parentProductId;
        }
        if (productPrice == null || productPrice.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giá tiền phải lớn hơn 0!");
            Long parentProductId = productVariantRepository.findById(productId).map(ProductVariant::getProductId).orElse(0L);
            return "redirect:/seller/product-management/" + parentProductId;
        }
        
        return shopService.updateProduct(user, productId, productName, productPrice, redirectAttributes);
    }

    @GetMapping("/seller/upload-details/{uploadId}")
    @ResponseBody
    public ResponseEntity<?> getUploadDetails(@PathVariable Long uploadId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        
        User user = (User) authentication.getPrincipal();
        
        return shopService.getUploadDetails(user, uploadId);
    }

    // Endpoint để cập nhật quantity cho tất cả products từ warehouse
    @PostMapping("/seller/update-product-quantities")
    @ResponseBody
    public ResponseEntity<?> updateProductQuantities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                !authentication.getName().equals("anonymousUser");
        
        if (!isAuthenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        
        User user = (User) authentication.getPrincipal();
        
        return shopService.updateProductQuantities(user);
    }

    

}
