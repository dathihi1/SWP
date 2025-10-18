package com.badat.study1.repository;

import com.badat.study1.model.Cart;
import com.badat.study1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Phương thức tiêu chuẩn: Tìm Cart theo User, chỉ dùng cho logic tạo/kiểm tra
    Optional<Cart> findByUser(User user);

    /**
     * Phương thức tối ưu cho việc hiển thị giỏ hàng.
     * Sử dụng LEFT JOIN FETCH để tải CartItems (ci) và Product (p) trong một truy vấn duy nhất.
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items ci LEFT JOIN FETCH ci.product p WHERE c.user = :user")
    Optional<Cart> findByUserWithItems(User user);

    // Lưu ý: Đã xóa các phương thức cũ không phù hợp với cấu trúc 1 Cart / 1 User
}
