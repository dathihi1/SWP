package com.badat.study1.repository;

import com.badat.study1.model.Cart;
import com.badat.study1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Phương thức tiêu chuẩn: Tìm Cart theo User.
    // Dùng cho logic kiểm tra và tạo giỏ hàng (CartService)
    Optional<Cart> findByUser(User user);

    /**
     * Phương thức tối ưu cho việc hiển thị giỏ hàng (Controller).
     * Sử dụng LEFT JOIN FETCH để tải CartItems (ci), ProductVariant (p), Product (pr), và Shop (s) trong một truy vấn duy nhất.
     * Tránh lỗi N+1 Selects.
     */
    @Query("SELECT DISTINCT c FROM Cart c " +
           "LEFT JOIN FETCH c.items ci " +
           "LEFT JOIN FETCH ci.productVariant p " +
           "LEFT JOIN FETCH p.product pr " +
           "LEFT JOIN FETCH p.shop s " +
           "WHERE c.user = :user")
    Optional<Cart> findByUserWithItems(User user);

    // ĐÃ XÓA: findByUserIdAndProductId và deleteByUserIdAndProductId
    // vì cấu trúc Cart mới không còn chứa productId trực tiếp.
}
