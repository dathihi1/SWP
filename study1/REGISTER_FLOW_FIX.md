# Register Flow Fix

## Vấn đề đã được sửa

### 1. **Luồng Register bị sai**
- **Trước**: Verify OTP thành công → Chuyển sang trang đổi mật khẩu
- **Sau**: Verify OTP thành công → Redirect về trang login

### 2. **Thiếu giới hạn OTP attempts**
- **Trước**: Không có giới hạn số lần nhập OTP
- **Sau**: Giới hạn 5 lần nhập OTP, sau đó khóa email 15 phút

## Luồng Register mới

### 1. **User đăng ký**
```
POST /api/auth/register
→ Tạo pending registration
→ Gửi OTP qua email
→ Redirect đến /verify-otp?email=xxx
```

### 2. **User nhập OTP**
```
POST /api/auth/verify-register-otp
→ Kiểm tra OTP (tối đa 5 lần)
→ Nếu đúng: Tạo user với status ACTIVE
→ Redirect đến /login
→ Nếu sai: Thông báo lỗi + tăng attempt counter
```

### 3. **Giới hạn attempts**
- **5 lần sai**: Email bị khóa 15 phút
- **Thông báo rõ ràng**: Hiển thị số lần thử còn lại
- **Rate limiting**: Chống spam OTP

## Các thay đổi đã thực hiện

### 1. **UserService.java**
- Sử dụng `OtpService` thay vì logic cũ
- Tích hợp rate limiting và attempt tracking
- Xóa method `generateOTP()` không sử dụng

### 2. **verify-otp.html**
- Thêm cảnh báo về giới hạn 5 lần thử
- Cải thiện thông báo lỗi
- Hiển thị rõ ràng khi email bị khóa

### 3. **OtpService.java** (đã có sẵn)
- Giới hạn 5 attempts per email
- Rate limiting 1 giờ
- Tự động xóa OTP sau khi verify thành công

## Cấu hình

### application.yaml
```yaml
security:
  rate-limit:
    otp-expire-minutes: 10  # OTP hết hạn sau 10 phút
    otp-max-attempts: 5     # Tối đa 5 lần thử
```

## Lợi ích

1. **Bảo mật cao**: Giới hạn attempts, rate limiting
2. **UX tốt**: Luồng đơn giản, thông báo rõ ràng
3. **Chống spam**: Không thể gửi OTP liên tục
4. **Transaction hoàn chỉnh**: Verify OTP = hoàn thành đăng ký

## Test Cases

### 1. **Register thành công**
1. Đăng ký với email hợp lệ
2. Nhập OTP đúng
3. → Redirect đến /login

### 2. **OTP sai nhiều lần**
1. Đăng ký với email hợp lệ
2. Nhập OTP sai 5 lần
3. → Thông báo "Email bị khóa 15 phút"

### 3. **Rate limiting**
1. Gửi OTP liên tục
2. → Thông báo "Quá nhiều yêu cầu"

## Lưu ý

- OTP chỉ có hiệu lực 10 phút
- Email bị khóa 15 phút sau 5 lần sai
- User được tạo với status ACTIVE ngay sau khi verify OTP
- Không cần đổi mật khẩu sau khi đăng ký
