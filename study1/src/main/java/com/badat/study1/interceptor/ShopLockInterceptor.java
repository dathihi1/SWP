package com.badat.study1.interceptor;

import com.badat.study1.model.Shop;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.lang.NonNull;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;



@Slf4j
@Component
@RequiredArgsConstructor
public class ShopLockInterceptor implements HandlerInterceptor {
    
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // Chỉ áp dụng cho các URL bắt đầu với /seller
        String requestURI = request.getRequestURI();
        if (!requestURI.startsWith("/seller")) {
            return true;
        }
        
        // Kiểm tra xem user có đăng nhập không
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            return true; // Cho phép truy cập nếu chưa đăng nhập (sẽ được xử lý bởi security)
        }
        
        try {
            String username = authentication.getName();
            // Tìm user theo username
            var user = userRepository.findByUsername(username);
            if (user.isEmpty()) {
                return true; // Cho phép truy cập nếu không tìm thấy user
            }

            // Tìm shop theo userId
            var shop = shopRepository.findByUserId(user.get().getId());

            Shop shopEntity = shop.get();
            if (shopEntity.getStatus() == Shop.Status.INACTIVE) {
                // Hiển thị thông báo trực tiếp
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(loadLockedTemplate());
                return false;
            }
            return true;
            
        } catch (Exception e) {
            return true; // Cho phép truy cập nếu có lỗi
        }
    }
    
    private String loadLockedTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/shop-locked.html");
            try (InputStream is = resource.getInputStream()) {
                return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            // Fallback nội tuyến phòng trường hợp không tải được file
            return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Shop locked</title></head><body><h1>Shop đã bị khóa</h1><p>Vui lòng liên hệ quản trị viên.</p></body></html>";
        }
    }
}
