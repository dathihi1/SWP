package com.badat.study1.interceptor;

import com.badat.study1.model.Shop;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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
            if (shop.isEmpty()) {
                return true; // Cho phép truy cập nếu user chưa có shop
            }
            
            Shop shopEntity = shop.get();
            if (shopEntity.getStatus() == Shop.Status.INACTIVE) {
                // Hiển thị thông báo trực tiếp
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(generateLockedMessage());
                return false;
            }
            return true;
            
        } catch (Exception e) {
            return true; // Cho phép truy cập nếu có lỗi
        }
    }
    
    private String generateLockedMessage() {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Shop đã bị khóa - MMO Market</title>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css"/>
                <style>
                    .navbar-red {
                        background-color: #dc3545 !important;
                        border-bottom: 3px solid #b02a37 !important;
                        min-height: 80px;
                    }
                    
                    .locked-container {
                        background: white;
                        border-radius: 20px;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        padding: 3rem;
                        text-align: center;
                        max-width: 600px;
                        margin: 2rem auto;
                    }
                    
                    .lock-icon {
                        font-size: 4rem;
                        color: #dc3545;
                        margin-bottom: 1.5rem;
                        animation: pulse 2s infinite;
                    }
                    
                    @keyframes pulse {
                        0% { transform: scale(1); }
                        50% { transform: scale(1.1); }
                        100% { transform: scale(1); }
                    }
                    
                    .title {
                        color: #dc3545;
                        font-size: 2rem;
                        font-weight: bold;
                        margin-bottom: 1rem;
                    }
                    
                    .btn-home {
                        background: linear-gradient(135deg, #dc3545 0%, #b02a37 100%);
                        border: none;
                        color: white;
                        padding: 12px 30px;
                        border-radius: 25px;
                        font-weight: bold;
                        text-decoration: none;
                        display: inline-block;
                        transition: transform 0.3s ease;
                    }
                    
                    .btn-home:hover {
                        transform: translateY(-2px);
                        color: white;
                        text-decoration: none;
                        background: linear-gradient(135deg, #b02a37 0%, #a02834 100%);
                    }
                    
                    .message {
                        color: #666;
                        font-size: 1.1rem;
                        line-height: 1.6;
                        margin-bottom: 2rem;
                    }
                </style>
            </head>
            <body>
                <!-- Navbar -->
                <nav class="navbar navbar-expand-lg navbar-red sticky-top">
                    <div class="container">
                        <!-- Không có logo, chỉ có navbar màu đỏ -->
                    </div>
                </nav>

                <!-- Main Content -->
                <div class="container my-5">
                    <div class="locked-container">
                        <div class="lock-icon">
                            <i class="fas fa-lock"></i>
                        </div>
                        
                        <h1 class="title">Shop đã bị khóa</h1>
                        
                        <div class="message">
                            <p>Rất tiếc, shop của bạn hiện đang bị khóa bởi quản trị viên.</p>
                            <p>Bạn không thể truy cập vào các trang quản lý gian hàng cho đến khi shop được mở khóa.</p>
                        </div>
                        
                        <a href="/" class="btn-home">
                            <i class="fas fa-home me-2"></i>
                            Về trang chủ
                        </a>
                    </div>
                </div>

                <!-- Footer -->
                <footer class="border-top py-3">
                    <div class="container d-flex justify-content-between align-items-center small">
                        <div>© 2025 MMO Market</div>
                        <div class="text-muted">Liên hệ: support@example.com</div>
                    </div>
                </footer>
                
                <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
            </body>
            </html>
            """;
    }
}
