package com.badat.study1.service;

import com.badat.study1.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public interface ShopService {
    String sellerShopPage(User user, Model model);
    String productManagementPage(User user, Model model);
    String addProductPage(User user, Model model);
    String editProductPage(User user, Long productId, Model model);
    String productVariantManagementPage(User user, Long productId, Model model);
    String addQuantityPage(User user, Long productVariantId, int page, Model model);
    String ordersPage(User user, int page, String status, Long productId, Long productVariantId, String dateFrom, String dateTo, Model model);
    String reviewsPage(User user, int page, Model model);

    ResponseEntity<?> getReviewsByProduct(User user, Long productId);
    ResponseEntity<?> markReviewsAsRead(User user, Long productId);
    String replyToReview(User user, Long reviewId, String sellerReply, RedirectAttributes redirectAttributes);

    String addProduct(User user,
                    String productName,
                    String productCategory,
                    String productSubcategory,
                    String shortDescription,
                    String detailedDescription,
                    org.springframework.web.multipart.MultipartFile productImageFile,
                    Boolean uniqueProducts,
                    String isCropped,
                    RedirectAttributes redirectAttributes);

    String editProduct(User user,
                     Long id,
                     String productName,
                     String status,
                     String shortDescription,
                     String detailedDescription,
                     org.springframework.web.multipart.MultipartFile productImageFile,
                     RedirectAttributes redirectAttributes);

    String addProductVariant(User user, Long productId, String productName, java.math.BigDecimal productPrice, RedirectAttributes redirectAttributes);


    String editProductVariant(User user, Long productId, String productName, java.math.BigDecimal productPrice, RedirectAttributes redirectAttributes);

    String updateProductQuantityFromFile(User user, Long productVariantId, org.springframework.web.multipart.MultipartFile file, RedirectAttributes redirectAttributes);

    ResponseEntity<?> getUploadDetails(User user, Long uploadId);


}


