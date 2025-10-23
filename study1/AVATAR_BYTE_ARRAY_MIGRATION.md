# Avatar Storage Migration: From File System to Database Byte Array

## Tổng quan
Đã thay đổi cách lưu trữ avatar từ file system sang database byte array để đơn giản hóa và tăng tính nhất quán.

## Thay đổi chính

### 1. **User Model (`User.java`)**
```java
// TRƯỚC: Có cả avatarData và avatarUrl
@Lob
@Column(name = "avatar_data", columnDefinition = "LONGBLOB")
byte[] avatarData;

@Column(name = "avatar_url", length = 500)
String avatarUrl; // Cho trường hợp lấy từ Google OAuth

// SAU: Chỉ có avatarData
@Lob
@Column(name = "avatar_data", columnDefinition = "LONGBLOB")
byte[] avatarData;
```

### 2. **UserService (`UserService.java`)**

#### Upload Avatar:
```java
// TRƯỚC: Lưu cả byte[] và xóa URL
user.setAvatarData(avatarBytes);
user.setAvatarUrl(null); // Clear URL when using byte data storage

// SAU: Chỉ lưu byte[]
user.setAvatarData(avatarBytes);
```

#### Get Avatar:
```java
// TRƯỚC: Ưu tiên avatarData > avatarUrl > default
if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
    return user.getAvatarData();
}
if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
    // Đọc từ file system...
}

// SAU: Chỉ sử dụng avatarData hoặc default
if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
    return user.getAvatarData();
}
return getDefaultAvatar();
```

#### Delete Avatar:
```java
// TRƯỚC: Xóa cả byte[] và URL
user.setAvatarData(null);
user.setAvatarUrl(null);

// SAU: Chỉ xóa byte[]
user.setAvatarData(null);
```

### 3. **AvatarController (`AvatarController.java`)**
- Không thay đổi logic chính
- Vẫn sử dụng `detectContentType()` để detect image type từ byte array
- Vẫn trả về default avatar khi không có data

## Migration Script

### File: `remove_avatar_url_column.sql`
```sql
-- Xóa cột avatar_url khỏi bảng user
ALTER TABLE user DROP COLUMN IF EXISTS avatar_url;
```

## Lợi ích

### ✅ **Đơn giản hóa:**
- Không cần quản lý file system
- Không cần UUID cho avatar
- Mỗi user chỉ có 1 avatar duy nhất

### ✅ **Tính nhất quán:**
- Tất cả avatar đều lưu trong database
- Không có dependency vào file system
- Dễ backup và restore

### ✅ **Performance:**
- Không cần đọc file từ disk
- Avatar được cache trong database
- Faster retrieval

### ✅ **Scalability:**
- Dễ scale với multiple instances
- Không cần shared file system
- Database replication includes avatars

## Cách sử dụng

### Upload Avatar:
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('/api/profile/avatar', {
    method: 'POST',
    body: formData
});
```

### Get Avatar:
```html
<!-- Sử dụng trực tiếp URL -->
<img src="/api/profile/avatar/123" alt="User Avatar">
<img src="/api/profile/avatar/me" alt="My Avatar">
```

### Delete Avatar:
```javascript
fetch('/api/profile/avatar', {
    method: 'DELETE'
});
```

## Lưu ý

### ⚠️ **Database Size:**
- Avatar lưu dưới dạng LONGBLOB
- Có thể làm tăng kích thước database
- Nên có policy giới hạn kích thước file (hiện tại: 2MB)

### ⚠️ **Memory Usage:**
- Avatar được load vào memory khi truy cập
- Cần monitor memory usage với nhiều user

### ⚠️ **Migration:**
- Chạy migration script để xóa `avatar_url` column
- Backup database trước khi migrate
- Test kỹ sau khi migrate

## Kết luận

Việc chuyển từ file system sang database byte array giúp:
- **Đơn giản hóa** architecture
- **Tăng tính nhất quán** của data
- **Dễ maintain** và scale
- **Giảm complexity** của code

Avatar giờ đây được lưu trực tiếp trong database như byte array, không cần UUID hay file system management.
