package com.badat.study1.controller;

import com.badat.study1.model.Product;
import com.badat.study1.model.Shop;
import com.badat.study1.model.Stall;
import com.badat.study1.model.UploadHistory;
import com.badat.study1.model.User;
import com.badat.study1.model.Warehouse;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.OrderItemRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import com.badat.study1.repository.UploadHistoryRepository;
import com.badat.study1.repository.WarehouseRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class ShopController {
    private final ShopRepository shopRepository;
    private final StallRepository stallRepository;
    private final ProductRepository productRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderItemRepository orderItemRepository;
    

    public ShopController(ShopRepository shopRepository, StallRepository stallRepository, ProductRepository productRepository, UploadHistoryRepository uploadHistoryRepository, WarehouseRepository warehouseRepository, OrderItemRepository orderItemRepository) {
        this.shopRepository = shopRepository;
        this.stallRepository = stallRepository;
        this.productRepository = productRepository;
        this.uploadHistoryRepository = uploadHistoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @PostMapping("/seller/add-stall")
    public String addStall(@RequestParam String stallName,
                          @RequestParam String businessType,
                          @RequestParam String stallCategory,
                          @RequestParam Double discount,
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
        
        
        // Validate required text fields
        if (stallName == null || stallName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên gian hàng là bắt buộc!");
            return "redirect:/seller/add-stall";
        }
        
        if (businessType == null || businessType.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loại hình kinh doanh là bắt buộc!");
            return "redirect:/seller/add-stall";
        }
        
        if (stallCategory == null || stallCategory.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Loại gian hàng là bắt buộc!");
            return "redirect:/seller/add-stall";
        }
        
        if (discount == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chiết khấu cho sàn là bắt buộc!");
            return "redirect:/seller/add-stall";
        }
        
        if (shortDescription == null || shortDescription.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả ngắn là bắt buộc!");
            return "redirect:/seller/add-stall";
        }
        
        if (detailedDescription == null || detailedDescription.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả chi tiết là bắt buộc!");
            return "redirect:/seller/add-stall";
        }

        // Backend word-count validation
        int shortWordCount = (int) java.util.Arrays.stream(shortDescription.trim().split("\\s+")).filter(s -> !s.isBlank()).count();
        int detailedWordCount = (int) java.util.Arrays.stream(detailedDescription.trim().split("\\s+")).filter(s -> !s.isBlank()).count();
        if (shortWordCount > 150) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả ngắn không được vượt quá 150 từ!");
            return "redirect:/seller/add-stall";
        }
        if (detailedWordCount > 500) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mô tả chi tiết không được vượt quá 500 từ!");
            return "redirect:/seller/add-stall";
        }
        
        // Validate unique products checkbox
        if (uniqueProducts == null || !uniqueProducts) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn phải đồng ý với cam kết 'Sản phẩm không trùng lặp' để tạo gian hàng!");
            return "redirect:/seller/add-stall";
        }
        
        // Validate stall image is required
        if (stallImageFile == null || stallImageFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hình ảnh gian hàng là bắt buộc!");
            return "redirect:/seller/add-stall";
        }
        
        // Validate that image has been cropped
        if (isCropped == null || !isCropped.equals("true")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn phải cắt ảnh trước khi tạo gian hàng!");
            return "redirect:/seller/add-stall";
        }
        
        try {
            // Get user's shop again for remaining validations
            Shop userShop = shopRepository.findByUserId(user.getId())
                    .orElse(null);
            
            if (userShop == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa có shop. Vui lòng tạo shop trước khi tạo gian hàng!");
                return "redirect:/seller/stall-management";
            }
            
            // Check if user already has maximum number of stalls (5) - CHECK SAU
            long currentStallCount = stallRepository.countByShopIdAndIsDeleteFalse(userShop.getId());
            if (currentStallCount >= 5) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "Bạn đã đạt giới hạn tối đa 5 gian hàng. Không thể tạo thêm gian hàng mới!");
                // Đưa người dùng về trang tạo để thấy thông báo thay vì quay về quản lý
                return "redirect:/seller/add-stall";
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
            
            // Handle image upload
            if (stallImageFile != null && !stallImageFile.isEmpty()) {
                try {
                    byte[] imageData = stallImageFile.getBytes();
                    stall.setStallImageData(imageData);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xử lý hình ảnh. Vui lòng thử lại!");
                    return "redirect:/seller/add-stall";
                }
            }
            
            stall.setStatus("PENDING");
            stall.setActive(false);
            stall.setCreatedAt(Instant.now());
            stall.setDelete(false);
            
            // Save to database
            stallRepository.save(stall);
            
            redirectAttributes.addFlashAttribute("successMessage", "Gian hàng đã được tạo thành công và đang chờ duyệt!");
            return "redirect:/seller/stall-management";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tạo gian hàng. Vui lòng thử lại!");
            return "redirect:/seller/add-stall";
        }
    }

    @GetMapping("/stall-image/{stallId}")
    public ResponseEntity<byte[]> getStallImage(@PathVariable Long stallId) {
        try {
            Stall stall = stallRepository.findById(stallId).orElse(null);
            if (stall == null || stall.getStallImageData() == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(stall.getStallImageData().length);
            headers.setCacheControl("max-age=3600"); // Cache for 1 hour

            return new ResponseEntity<>(stall.getStallImageData(), headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/seller/edit-stall/{id}")
    public String editStall(@PathVariable Long id,
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
        
        try {
            // Lấy thông tin gian hàng
            var stallOptional = stallRepository.findById(id);
            if (stallOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy gian hàng!");
                return "redirect:/seller/stall-management";
            }
            
            Stall stall = stallOptional.get();
            
            // Kiểm tra quyền sở hữu gian hàng
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !stall.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền sửa gian hàng này!");
                return "redirect:/seller/stall-management";
            }
            
            // Validate: Chỉ cho phép mở gian hàng khi có hàng trong kho
            if ("OPEN".equals(status)) {
                // Kiểm tra xem gian hàng có sản phẩm nào trong kho không
                // Check if stall has available warehouse items (not locked, not deleted)
                long availableStock = warehouseRepository.countAvailableItemsByStallId(stall.getId());
                boolean hasStock = availableStock > 0;
                
                if (!hasStock) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Không thể mở gian hàng! Gian hàng phải có ít nhất 1 sản phẩm trong kho.");
                    return "redirect:/seller/edit-stall/" + id;
                }
            }
            
            // Validate description lengths
            if (shortDescription == null || shortDescription.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mô tả ngắn là bắt buộc!");
                return "redirect:/seller/edit-stall/" + id;
            }
            if (detailedDescription == null || detailedDescription.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mô tả chi tiết là bắt buộc!");
                return "redirect:/seller/edit-stall/" + id;
            }
            int shortWordCountEdit = (int) java.util.Arrays.stream(shortDescription.trim().split("\\s+")).filter(s -> !s.isBlank()).count();
            int detailedWordCountEdit = (int) java.util.Arrays.stream(detailedDescription.trim().split("\\s+")).filter(s -> !s.isBlank()).count();
            if (shortWordCountEdit > 150) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mô tả ngắn không được vượt quá 150 từ!");
                return "redirect:/seller/edit-stall/" + id;
            }
            if (detailedWordCountEdit > 500) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mô tả chi tiết không được vượt quá 500 từ!");
                return "redirect:/seller/edit-stall/" + id;
            }

            // Cập nhật thông tin gian hàng
            stall.setStallName(stallName);
            
            // Nếu gian hàng bị từ chối, chuyển về trạng thái chờ duyệt khi cập nhật
            if ("REJECTED".equals(stall.getStatus())) {
                stall.setStatus("PENDING");
                stall.setActive(false);
                // Xóa thông tin duyệt cũ
                stall.setApprovedAt(null);
                stall.setApprovedBy(null);
                stall.setApprovalReason(null);
            } else {
                stall.setStatus(status);
            }
            
            stall.setShortDescription(shortDescription);
            stall.setDetailedDescription(detailedDescription);
            
            // Xử lý hình ảnh mới nếu có
            if (stallImageFile != null && !stallImageFile.isEmpty()) {
                try {
                    byte[] imageData = stallImageFile.getBytes();
                    stall.setStallImageData(imageData);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xử lý hình ảnh. Vui lòng thử lại!");
                    return "redirect:/seller/edit-stall/" + id;
                }
            }
            
            // Lưu vào database
            stallRepository.save(stall);
            
            redirectAttributes.addFlashAttribute("successMessage", "Gian hàng đã được cập nhật thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật gian hàng. Vui lòng thử lại!");
        }
        
        return "redirect:/seller/stall-management";
    }

    @PostMapping("/seller/add-product/{stallId}")
    public String addProduct(@PathVariable Long stallId,
                           @RequestParam String productName,
                           @RequestParam BigDecimal productPrice,
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
        
        // Validate required fields
        if (productName == null || productName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên mặt hàng là bắt buộc!");
            return "redirect:/seller/product-management/" + stallId;
        }
        
        if (productPrice == null || productPrice.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giá tiền phải lớn hơn 0!");
            return "redirect:/seller/product-management/" + stallId;
        }
        
        try {
            // Lấy thông tin gian hàng
            var stallOptional = stallRepository.findById(stallId);
            if (stallOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy gian hàng!");
                return "redirect:/seller/stall-management";
            }
            
            Stall stall = stallOptional.get();
            
            // Kiểm tra quyền sở hữu gian hàng
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !stall.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thêm sản phẩm vào gian hàng này!");
                return "redirect:/seller/stall-management";
            }

            Product product;
                // Tạo sản phẩm mới
                String uniqueKey = "PROD_" + System.currentTimeMillis() + "_" + user.getId();
                
                product = Product.builder()
                        .shopId(stall.getShopId())
                        .stallId(stallId)
                        .type("Khác") // Default type
                        .name(productName)
                        .description("") // Default empty description
                        .price(productPrice)
                        .quantity(0) // Default quantity is 0
                        .uniqueKey(uniqueKey)
                        .status(Product.Status.UNAVAILABLE) // Default status is UNAVAILABLE when stock is 0
                        .build();
            
            // Lưu vào database
            productRepository.save(product);
            
            redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm đã được thêm thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi thêm sản phẩm. Vui lòng thử lại!");
        }
        
        return "redirect:/seller/product-management/" + stallId;
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
        
        try {
            // Lấy thông tin sản phẩm
            var productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
                return "redirect:/seller/stall-management";
            }
            
            Product product = productOptional.get();
            
            // Kiểm tra quyền sở hữu sản phẩm
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !product.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật sản phẩm này!");
                return "redirect:/seller/stall-management";
            }
            
            // Cập nhật số lượng
            product.setQuantity(newQuantity);
            
            // Tự động cập nhật trạng thái dựa trên số lượng
            if (newQuantity > 0) {
                product.setStatus(Product.Status.AVAILABLE);
            } else {
                product.setStatus(Product.Status.UNAVAILABLE);
            }
            
            // Lưu vào database
            productRepository.save(product);
            
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật số lượng thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật số lượng. Vui lòng thử lại!");
        }
        
        return "redirect:/seller/product-management/" + productRepository.findById(productId).get().getStallId();
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
        
        // Check if user has SELLER role
        if (!user.getRole().equals(User.Role.SELLER)) {
            return "redirect:/profile";
        }
        
        try {
            // Validate file
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file TXT!");
                return "redirect:/seller/add-quantity/" + productId;
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".txt")) {
                redirectAttributes.addFlashAttribute("errorMessage", "File phải có định dạng TXT!");
                return "redirect:/seller/add-quantity/" + productId;
            }
            
            if (file.getSize() > 1024 * 1024) { // 1MB limit
                redirectAttributes.addFlashAttribute("errorMessage", "File quá lớn! Vui lòng chọn file nhỏ hơn 1MB.");
                return "redirect:/seller/add-quantity/" + productId;
            }
            
            // Lấy thông tin sản phẩm
            var productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
                return "redirect:/seller/stall-management";
            }
            
            Product product = productOptional.get();
            
            // Kiểm tra quyền sở hữu sản phẩm
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !product.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật sản phẩm này!");
                return "redirect:/seller/stall-management";
            }
            
            Shop shop = userShop.get();
            Stall stall = stallRepository.findById(product.getStallId()).orElse(null);
            if (stall == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy gian hàng!");
                return "redirect:/seller/stall-management";
            }
            
            // Read file content
            String content = new String(file.getBytes(), "UTF-8").trim();
            String[] lines = content.split("\\r?\\n");
            
            
            int successCount = 0;
            int failureCount = 0;
            StringBuilder resultDetails = new StringBuilder();
            
            // Determine expected item type based on stall category
            Warehouse.ItemType expectedType = determineItemTypeFromStall(stall.getStallCategory());
            
            // Track processed items in this file to avoid duplicates
            Set<String> processedItemsInFile = new HashSet<>();
            Set<String> processedItemKeys = new HashSet<>(); // Track unique item keys (e.g., CVV codes)
            
            // Process each line
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                
                try {
                    // Parse line format: TYPE|data1|data2|data3
                    String[] parts = line.split("\\|");
                    if (parts.length < 2) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Định dạng không hợp lệ (cần ít nhất 2 phần)\n");
                        continue;
                    }
                    
                    String itemType = parts[0].toUpperCase();
                    String itemData = line; // Store the full line as item data
                    
                    // Check for duplicates within the file (exact line match)
                    if (processedItemsInFile.contains(itemData)) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Item trùng lặp trong file (").append(itemType).append(")\n");
                        continue;
                    }
                    
                    // Check for duplicate item keys within the file
                    String itemKey = null;
                    if (parts.length >= 2) {
                        // For CARD: check Serial code (3rd part)
                        if ("CARD".equals(itemType) && parts.length >= 3) {
                            itemKey = parts[2]; // Serial code
                        }
                        // For EMAIL: check email address (2nd part)
                        else if ("EMAIL".equals(itemType)) {
                            itemKey = parts[1]; // Email address
                        }
                        // For ACCOUNT: check username (2nd part)
                        else if ("ACCOUNT".equals(itemType)) {
                            itemKey = parts[1]; // Username
                        }
                        // For KEY: check key value (2nd part)
                        else if ("KEY".equals(itemType)) {
                            itemKey = parts[1]; // Key value
                        }
                    }
                    
                    if (itemKey != null && processedItemKeys.contains(itemKey)) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": ").append(itemType).append(" với ").append(itemKey).append(" đã tồn tại trong file\n");
                        continue;
                    }
                    
                    // Validate item type matches expected type
                    Warehouse.ItemType type;
                    try {
                        type = Warehouse.ItemType.valueOf(itemType);
                    } catch (IllegalArgumentException e) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Loại sản phẩm không hợp lệ (").append(itemType).append(")\n");
                        continue;
                    }
                    
                    // Check if item type matches expected type for this stall
                    boolean isValidType = (type == expectedType);
                    
                    if (!isValidType) {
                        failureCount++;
                        resultDetails.append("Dòng ").append(i + 1).append(": Loại sản phẩm không khớp với gian hàng\n");
                        continue;
                    }
                    
                    // Kiểm tra xem có warehouse item đã tồn tại với cùng unique key trong toàn bộ hệ thống không
                    List<Warehouse> existingItems = warehouseRepository.findByItemTypeOrderByCreatedAtDesc(type);
                    Warehouse existingItem = null;
                    
                    for (Warehouse item : existingItems) {
                        // Check by unique key instead of full line match
                        boolean isDuplicate = false;
                        if (itemKey != null) {
                            // Extract key from existing item
                            String[] existingParts = item.getItemData().split("\\|");
                            String existingKey = null;
                            
                            if ("CARD".equals(itemType) && existingParts.length >= 3) {
                                existingKey = existingParts[2]; // Serial code
                            } else if ("EMAIL".equals(itemType) && existingParts.length >= 2) {
                                existingKey = existingParts[1]; // Email address
                            } else if ("ACCOUNT".equals(itemType) && existingParts.length >= 2) {
                                existingKey = existingParts[1]; // Username
                            } else if ("KEY".equals(itemType) && existingParts.length >= 2) {
                                existingKey = existingParts[1]; // Key value
                            }
                            
                            isDuplicate = (existingKey != null && existingKey.equals(itemKey));
                        } else {
                            // Fallback to full line match if no key available
                            isDuplicate = item.getItemData().equals(itemData);
                        }
                        
                        if (isDuplicate) {
                            if (item.getIsDelete()) {
                                // Item đã bị xóa mềm - kiểm tra trạng thái locked
                                if (item.getLocked()) {
                                    // Item đã bị xóa mềm và bị khóa - không thể restore
                                    existingItem = item;
                                    break;
                                } else {
                                    // Item đã bị xóa mềm và chưa bị khóa - có thể restore
                                    existingItem = item;
                                    break;
                                }
                            } else {
                                // Item đã tồn tại và chưa được mua - không thể thêm duplicate
                                existingItem = item;
                                break;
                            }
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
                    
                    // Kiểm tra trường hợp item bị xóa mềm và bị khóa
                    if (existingItem != null && existingItem.getIsDelete() && existingItem.getLocked()) {
                        failureCount++;
                        if (itemKey != null) {
                            resultDetails.append("Dòng ").append(i + 1).append(": Item đã bị khóa và không thể khôi phục (").append(itemType).append(" với ").append(itemKey).append(")\n");
                        } else {
                            resultDetails.append("Dòng ").append(i + 1).append(": Item đã bị khóa và không thể khôi phục (").append(itemType).append(")\n");
                        }
                        continue;
                    }
                    
                    // Thêm item vào danh sách đã xử lý để tránh duplicate trong file
                    processedItemsInFile.add(itemData);
                    if (itemKey != null) {
                        processedItemKeys.add(itemKey);
                    }
                    
                    if (existingItem != null && existingItem.getIsDelete() && !existingItem.getLocked()) {
                        // Restore warehouse item đã bị xóa mềm và chưa bị khóa
                        existingItem.setIsDelete(false);
                        existingItem.setDeletedBy(null);
                        warehouseRepository.save(existingItem);
                        if (itemKey != null) {
                            resultDetails.append("Dòng ").append(i + 1).append(": Khôi phục item (").append(itemType).append(" với ").append(itemKey).append(")\n");
                        } else {
                            resultDetails.append("Dòng ").append(i + 1).append(": Khôi phục item (").append(itemType).append(")\n");
                        }
                    } else {
                        // Tạo warehouse item mới
                        Warehouse warehouseItem = Warehouse.builder()
                                .itemType(type)
                                .itemData(itemData)
                                .product(product)
                                .shop(shop)
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
                    
                } catch (Exception e) {
                    failureCount++;
                    resultDetails.append("Dòng ").append(i + 1).append(": Lỗi xử lý - ").append(e.getMessage()).append("\n");
                }
            }
            
            // Update product quantity from warehouse count
            long warehouseCount = productRepository.countWarehouseItemsByProductId(productId);
            product.setQuantity((int) warehouseCount);
            
            // Tự động cập nhật trạng thái dựa trên số lượng
            if (warehouseCount > 0) {
                product.setStatus(Product.Status.AVAILABLE);
            } else {
                product.setStatus(Product.Status.UNAVAILABLE);
            }
            
            // Lưu vào database - bắt buộc phải thành công
            productRepository.save(product);
            
            // Đồng bộ tất cả products trong shop để đảm bảo consistency
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
            } catch (Exception syncException) {
                // Continue execution even if sync fails
            }
            
            // Lưu lịch sử upload với error handling
            try {
                // Tạo thông tin kết quả chi tiết
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
                
                UploadHistory uploadHistory = UploadHistory.builder()
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
                
            } catch (Exception historyException) {
                // Continue execution even if history save fails
            }
            
            
            if (successCount > 0) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Upload thành công! Đã thêm " + successCount + " sản phẩm vào kho. " + 
                    (failureCount > 0 ? "Có " + failureCount + " dòng lỗi." : ""));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Upload thất bại! Không có sản phẩm nào được thêm vào kho. Vui lòng kiểm tra định dạng file.");
            }
            
            // Stay on add-quantity page to show success message
            return "redirect:/seller/add-quantity/" + productId;
            
        } catch (Exception e) {
            
            // Lưu lịch sử upload thất bại
            try {
                var productOptional = productRepository.findById(productId);
                if (productOptional.isPresent()) {
                    Product failedProduct = productOptional.get();
                    
                    // Tạo thông tin kết quả chi tiết cho lỗi
                    StringBuilder detailedResult = new StringBuilder();
                    detailedResult.append("Tên mặt hàng: ").append(failedProduct.getName()).append("\n");
                    detailedResult.append("Tên file: ").append(file.getOriginalFilename()).append("\n");
                    detailedResult.append("Ngày upload: ").append(java.time.LocalDateTime.now().toString()).append("\n");
                    detailedResult.append("Tổng số dòng: 0\n");
                    detailedResult.append("Thành công: 0\n");
                    detailedResult.append("Thất bại: 1\n");
                    detailedResult.append("Trạng thái: THẤT BẠI\n");
                    detailedResult.append("\nChi tiết lỗi:\n").append("Lỗi xử lý file: ").append(e.getMessage());
                    
                    // Get stall for failed product
                    var failedStallOptional = stallRepository.findById(failedProduct.getStallId());
                    if (failedStallOptional.isPresent()) {
                        UploadHistory uploadHistory = UploadHistory.builder()
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
            } catch (Exception ex) {
            }
            
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xử lý file. Vui lòng thử lại!");
            return "redirect:/seller/add-quantity/" + productId;
        }
    }

    @PostMapping("/seller/update-product/{productId}")
    public String updateProduct(@PathVariable Long productId,
                              @RequestParam String productName,
                              @RequestParam BigDecimal productPrice,
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
        
        // Validate required fields
        if (productName == null || productName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên mặt hàng là bắt buộc!");
            return "redirect:/seller/product-management/" + productRepository.findById(productId).get().getStallId();
        }
        
        if (productPrice == null || productPrice.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giá tiền phải lớn hơn 0!");
            return "redirect:/seller/product-management/" + productRepository.findById(productId).get().getStallId();
        }
        
        try {
            // Lấy thông tin sản phẩm
            var productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
                return "redirect:/seller/stall-management";
            }
            
            Product product = productOptional.get();
            
            // Kiểm tra quyền sở hữu sản phẩm
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty() || !product.getShopId().equals(userShop.get().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền sửa sản phẩm này!");
                return "redirect:/seller/stall-management";
            }
            
            // Cập nhật thông tin sản phẩm
            product.setName(productName);
            product.setPrice(productPrice);
            product.setUpdatedAt(java.time.LocalDateTime.now());
            
            // Lưu vào database
            productRepository.save(product);
            
            redirectAttributes.addFlashAttribute("successMessage", "Sản phẩm đã được cập nhật thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật sản phẩm. Vui lòng thử lại!");
        }
        
        return "redirect:/seller/product-management/" + productRepository.findById(productId).get().getStallId();
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
        
        try {
            var uploadOptional = uploadHistoryRepository.findById(uploadId);
            if (uploadOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            UploadHistory upload = uploadOptional.get();
            
            // Check if user has access to this upload
            if (!upload.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }
            
            // Create response object
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
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
        
        try {
            // Lấy tất cả products của user
            var userShop = shopRepository.findByUserId(user.getId());
            if (userShop.isEmpty()) {
                return ResponseEntity.badRequest().body("User shop not found");
            }
            
            var products = productRepository.findByShopIdAndIsDeleteFalse(userShop.get().getId());
            int updatedCount = 0;
            
            for (Product product : products) {
                long warehouseCount = productRepository.countWarehouseItemsByProductId(product.getId());
                product.setQuantity((int) warehouseCount);
                
                // Cập nhật status
                if (warehouseCount > 0) {
                    product.setStatus(Product.Status.AVAILABLE);
                } else {
                    product.setStatus(Product.Status.UNAVAILABLE);
                }
                
                productRepository.save(product);
                updatedCount++;
            }
            
            return ResponseEntity.ok("Đã cập nhật quantity cho " + updatedCount + " sản phẩm từ warehouse");
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * Determine the expected item type based on stall category
     */
    private Warehouse.ItemType determineItemTypeFromStall(String stallCategory) {
        if (stallCategory == null) {
            return Warehouse.ItemType.KEY; // Default fallback
        }
        
        String category = stallCategory.toLowerCase();
        
        // Tài khoản Email gian hàng: chỉ EMAIL
        if (category.contains("tài khoản") || category.contains("account") || category.contains("acc")) {
            return Warehouse.ItemType.EMAIL;
        } 
        // Thẻ cào gian hàng: CARD
        else if (category.contains("thẻ") || category.contains("card") || category.contains("gift") || 
                 category.contains("điện thoại") || category.contains("phone")) {
            return Warehouse.ItemType.CARD;
        } 
        // Key Game gian hàng: KEY
        else if (category.contains("key") || category.contains("game") || category.contains("software")) {
            return Warehouse.ItemType.KEY;
        } 
        else {
            return Warehouse.ItemType.KEY; // Default fallback
        }
    }

}
