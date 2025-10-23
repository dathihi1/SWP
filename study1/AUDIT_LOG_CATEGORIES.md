# Audit Log Categories

## Tổng quan
Hệ thống audit log đã được cập nhật để hỗ trợ phân loại các hoạt động thành các danh mục khác nhau. Điều này giúp phân biệt giữa các hoạt động mà user có thể nhìn thấy và các hoạt động nội bộ của hệ thống.

## Các loại Category

### 1. USER_ACTION
- **Mô tả**: Các hoạt động mà user có thể nhìn thấy và tương tác trực tiếp
- **Ví dụ**: 
  - LOGIN (đăng nhập thành công)
  - LOGOUT (đăng xuất)
  - PROFILE_UPDATE (cập nhật thông tin cá nhân)
  - PASSWORD_CHANGE (đổi mật khẩu)
  - REGISTER (đăng ký tài khoản)

### 2. API_CALL
- **Mô tả**: Các API calls nội bộ, background processes
- **Ví dụ**:
  - Các API endpoint được gọi từ frontend
  - Background tasks
  - Internal system calls

### 3. SYSTEM_EVENT
- **Mô tả**: Các sự kiện hệ thống, scheduled tasks, maintenance
- **Ví dụ**:
  - Scheduled cleanup tasks
  - System maintenance
  - Database migrations
  - System health checks

### 4. SECURITY_EVENT
- **Mô tả**: Các sự kiện bảo mật, failed attempts, suspicious activities
- **Ví dụ**:
  - ACCOUNT_LOCKED (tài khoản bị khóa)
  - ACCOUNT_UNLOCKED (mở khóa tài khoản)
  - LOGIN_FAILED (đăng nhập thất bại)
  - Failed login attempts
  - Suspicious activities

## Cách sử dụng

### 1. Migration Database
Chạy file migration để thêm cột category:
```sql
-- Chạy file add_audit_log_category.sql
```

### 2. API Endpoints

#### Lấy audit logs với filter theo category
```
GET /api/audit-logs/me?category=USER_ACTION
GET /api/audit-logs/me?category=API_CALL
GET /api/audit-logs/me?category=SECURITY_EVENT
```

#### Lấy danh sách categories có sẵn
```
GET /api/audit-logs/categories
```

### 3. Trong Code

#### Tạo audit log với category cụ thể
```java
// Trong AuditLogService
auditLogService.logAction(user, "PROFILE_UPDATE", "Updated profile information", 
                         ipAddress, true, null, userAgent, AuditLog.Category.USER_ACTION);

auditLogService.logAction(user, "API_CALL", "Internal API call", 
                         ipAddress, true, null, userAgent, AuditLog.Category.API_CALL);
```

#### Sử dụng trong AuditAspect
AuditAspect sẽ tự động xác định category dựa trên:
- Action name (LOGIN, PROFILE, etc.)
- Request URI (API calls)
- Context của request

## Lợi ích

1. **Phân biệt rõ ràng**: User chỉ thấy các hoạt động liên quan đến họ (USER_ACTION)
2. **Bảo mật**: Các hoạt động nội bộ (API_CALL, SYSTEM_EVENT) không hiển thị cho user
3. **Debugging**: Dễ dàng filter và tìm kiếm các loại hoạt động cụ thể
4. **Monitoring**: Theo dõi hiệu suất và bảo mật hệ thống
5. **Compliance**: Đáp ứng yêu cầu audit và compliance

## Lưu ý

- Mặc định, user chỉ thấy các audit logs có category = USER_ACTION
- Admin có thể xem tất cả categories
- Security events được lưu riêng trong SecurityEvent model
- Category được tự động gán dựa trên context và action
