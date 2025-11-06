package com.badat.study1.service;

import com.badat.study1.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public interface ShopService {
    String sellerShopPage(User user, Model model);
    String productManagementOverview(User user, Model model);
    String addProductPage(User user, Model model);
    String editProductPage(User user, Long productId, Model model);
    String productManagementPage(User user, Long stallId, Model model);
    String addQuantityPage(User user, Long productId, int page, Model model);
    String ordersPage(User user, int page, String status, Long stallId, Long productId, String dateFrom, String dateTo, Model model);
    String reviewsPage(User user, int page, Model model);

    ResponseEntity<?> getReviewsByStall(User user, Long stallId);
    ResponseEntity<?> markReviewsAsRead(User user, Long stallId);
    String replyToReview(User user, Long reviewId, String sellerReply, RedirectAttributes redirectAttributes);

    String addProduct(User user,
                    String stallName,
                    String stallCategory,
                    String productSubcategory,
                    String shortDescription,
                    String detailedDescription,
                    org.springframework.web.multipart.MultipartFile stallImageFile,
                    Boolean uniqueProducts,
                    String isCropped,
                    RedirectAttributes redirectAttributes);

    String editProduct(User user,
                     Long id,
                     String stallName,
                     String status,
                     String shortDescription,
                     String detailedDescription,
                     org.springframework.web.multipart.MultipartFile stallImageFile,
                     RedirectAttributes redirectAttributes);

    String addProduct(User user, Long stallId, String productName, java.math.BigDecimal productPrice, RedirectAttributes redirectAttributes);


    String updateProduct(User user, Long productId, String productName, java.math.BigDecimal productPrice, RedirectAttributes redirectAttributes);

    String updateProductQuantityFromFile(User user, Long productId, org.springframework.web.multipart.MultipartFile file, RedirectAttributes redirectAttributes);

    ResponseEntity<?> getUploadDetails(User user, Long uploadId);


}


