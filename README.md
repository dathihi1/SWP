# MMO Market - Hệ thống Thương mại Điện tử

## 📋 Mô tả dự án

**MMO Market** là một hệ thống thương mại điện tử được phát triển bằng Spring Boot, cung cấp nền tảng cho phép người dùng mua bán các sản phẩm MMO. Dự án được thiết kế với kiến trúc MVC và hỗ trợ đa vai trò người dùng.

## 🚀 Tính năng chính

### 👥 Quản lý người dùng
- **Đăng ký/Đăng nhập** với xác thực OAuth2 (Google)
- **Xác thực đa lớp** với mã OTP qua email
- **Quản lý hồ sơ** cá nhân với upload avatar
- **Phân quyền** theo vai trò: Admin, Seller, Customer
- **Bảo mật** với JWT và rate limiting

### 🏪 Quản lý cửa hàng
- **Đăng ký cửa hàng** với xác minh CCCD
- **Quản lý sản phẩm** với nhiều biến thể
- **Quản lý kho hàng** và số lượng tồn kho
- **Theo dõi đơn hàng** và đánh giá khách hàng
- **Rút tiền** từ ví điện tử

### 🛒 Mua sắm
- **Duyệt sản phẩm** theo danh mục
- **Giỏ hàng** và thanh toán
- **Tích hợp VNPay** cho thanh toán
- **Lịch sử đơn hàng** và thanh toán
- **Đánh giá sản phẩm** và cửa hàng

### 🔧 Quản trị hệ thống
- **Dashboard** tổng quan với thống kê
- **Quản lý người dùng** và cửa hàng
- **Audit logs** theo dõi hoạt động
- **Quản lý yêu cầu rút tiền**
- **Báo cáo** và phân tích

## 🛠️ Công nghệ sử dụng

### Backend
- **Spring Boot 3.5.6** - Framework chính
- **Spring Security** - Bảo mật và xác thực
- **Spring Data JPA** - ORM và quản lý database
- **Spring Mail** - Gửi email OTP
- **Spring OAuth2** - Đăng nhập với Google
- **JWT** - Token authentication
- **Redis** - Cache và session management

### Database
- **MySQL** - Database chính

### Frontend
- **Thymeleaf** - Template engine
- **Bootstrap 5** - UI framework
- **Font Awesome** - Icons
- **JavaScript** - Client-side logic

### Payment & Security
- **VNPay** - Thanh toán online
- **Kaptcha** - Captcha verification
- **Rate Limiting** - Chống spam và tấn công

## 📦 Cài đặt và chạy dự án

### Yêu cầu hệ thống
- **Java 17+**
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**

## 📁 Cấu trúc dự án

```
study1/
├── src/main/java/com/badat/study1/
│   ├── annotation/          # Custom annotations
│   ├── aspect/             # AOP aspects
│   ├── config/             # Configuration classes
│   ├── configuration/      # Security & app configs
│   ├── controller/         # REST & MVC controllers
│   ├── dto/               # Data Transfer Objects
│   ├── event/             # Event handling
│   ├── interceptor/       # Request interceptors
│   ├── model/             # JPA entities
│   ├── repository/        # Data repositories
│   ├── service/           # Business logic
│   └── util/              # Utility classes
├── src/main/resources/
│   ├── static/            # CSS, JS, images
│   ├── templates/         # Thymeleaf templates
│   └── application.yaml   # Configuration
└── pom.xml               # Maven dependencies
```

## 🔐 Bảo mật

- **JWT Authentication** cho API
- **OAuth2** đăng nhập với Google
- **Rate Limiting** chống spam
- **Captcha** cho các form quan trọng
- **Audit Logging** theo dõi hoạt động
- **Input Validation** và sanitization

## 📝 License

Dự án này được phát triển cho mục đích học tập và nghiên cứu.

## 📞 Liên hệ

- **Email**: dat2801zz@gmail.com
- **Project**: MMO Market - SWP391

---
