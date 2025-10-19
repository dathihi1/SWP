# Google OAuth2 Login Setup

## Tổng quan
Ứng dụng đã được cấu hình để hỗ trợ đăng nhập bằng Google OAuth2. Người dùng có thể đăng nhập bằng tài khoản Google của họ.

## Các tính năng đã implement

### 1. Cấu hình OAuth2
- Đã thêm Spring Boot OAuth2 Client dependency
- Cấu hình Google OAuth2 trong `application.yaml`
- Client ID và Client Secret từ Google Console

### 2. Cập nhật User Model
- Thêm trường `provider` (LOCAL, GOOGLE)
- Thêm trường `provider_id` để lưu Google ID
- Cập nhật database schema

### 3. Custom OAuth2 User Service
- Xử lý thông tin từ Google OAuth2
- Kiểm tra email đã tồn tại hay chưa
- Tạo user mới hoặc cập nhật user hiện tại
- Tự động tạo password random cho user Google

### 4. Security Configuration
- Cấu hình OAuth2 login endpoint
- Xử lý success/failure redirects
- Tích hợp với JWT authentication

### 5. Frontend
- Cập nhật login.html với nút "Đăng nhập với Google"
- Styling cho nút Google login

## Luồng hoạt động

### Đăng nhập lần đầu với Google
1. User click "Đăng nhập với Google"
2. Redirect đến Google OAuth2 consent screen
3. User đồng ý cấp quyền
4. Google redirect về `/oauth2/success`
5. Hệ thống kiểm tra email:
   - Nếu email chưa tồn tại: Tạo user mới với provider=GOOGLE
   - Nếu email đã tồn tại với provider=LOCAL: Cập nhật thành provider=GOOGLE
   - Nếu email đã tồn tại với provider khác: Báo lỗi
6. Tạo JWT token và redirect về trang chủ

### Đăng nhập lần sau với Google
1. User click "Đăng nhập với Google"
2. Google redirect về `/oauth2/success`
3. Hệ thống tìm user theo provider_id
4. Tạo JWT token và redirect về trang chủ

## Cấu hình cần thiết

### 1. Google Console Setup
- Tạo project trong Google Cloud Console
- Enable Google+ API
- Tạo OAuth2 credentials
- Thêm redirect URI: `http://localhost:8080/login/oauth2/code/google`

### 2. Application Properties
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: "your-client-id"
            client-secret: "your-client-secret"
            scope:
              - email
              - profile
        provider:
          google:
            authorization-uri: "https://accounts.google.com/o/oauth2/auth"
            token-uri: "https://oauth2.googleapis.com/token"
            user-info-uri: "https://www.googleapis.com/oauth2/v2/userinfo"
            user-name-attribute: "id"
```

### 3. Database Migration
Cần thêm các trường mới vào bảng `user`:
```sql
ALTER TABLE user ADD COLUMN provider VARCHAR(20) DEFAULT 'LOCAL';
ALTER TABLE user ADD COLUMN provider_id VARCHAR(100);
```

## Endpoints

- `GET /oauth2/authorization/google` - Bắt đầu OAuth2 flow
- `GET /oauth2/success` - Xử lý sau khi đăng nhập thành công
- `GET /oauth2/failure` - Xử lý khi đăng nhập thất bại

## Bảo mật

- Tất cả OAuth2 requests đều được validate
- JWT tokens được tạo sau khi xác thực thành công
- Email được sử dụng làm unique identifier
- Password random được tạo cho user Google (không thể đăng nhập bằng password thông thường)

## Troubleshooting

### Lỗi "Email already associated with another account"
- Xảy ra khi email đã được sử dụng bởi provider khác
- Cần kiểm tra và xử lý trong CustomOAuth2UserService

### Lỗi "OAuth2 authentication failed"
- Kiểm tra client-id và client-secret
- Kiểm tra redirect URI trong Google Console
- Kiểm tra network connectivity

### Lỗi "Unsupported OAuth2 provider"
- Chỉ hỗ trợ Google OAuth2
- Cần cập nhật code để hỗ trợ provider khác

