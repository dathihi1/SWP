# Tài liệu chi tiết: Luồng Rút Tiền (Withdrawal Flow)

## 📋 Mục lục
1. [Tổng quan](#tổng-quan)
2. [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
3. [Các thành phần chính](#các-thành-phần-chính)
4. [Luồng nghiệp vụ chi tiết](#luồng-nghiệp-vụ-chi-tiết)
5. [API Endpoints](#api-endpoints)
6. [Database Schema](#database-schema)
7. [Bảo mật và Validation](#bảo-mật-và-validation)
8. [Error Handling](#error-handling)
9. [Frontend Flow](#frontend-flow)
10. [Các file liên quan](#các-file-liên-quan)

---

## 🎯 Tổng quan

Hệ thống rút tiền cho phép **Seller** (người bán) rút tiền từ ví của họ về tài khoản ngân hàng. Quy trình bao gồm:

1. **Seller** tạo yêu cầu rút tiền (có xác thực OTP)
2. Hệ thống **hold** (tạm giữ) số tiền từ ví
3. **Admin** duyệt hoặc từ chối yêu cầu
4. Nếu được duyệt: Tiền được chuyển (đã trừ từ lúc tạo yêu cầu)
5. Nếu bị từ chối: Tiền được hoàn về ví
6. **Seller** có thể hủy yêu cầu đang chờ duyệt

---

## 🏗️ Kiến trúc hệ thống

```
┌─────────────┐
│   Frontend  │ (withdraw.html, admin/withdraw-requests.html)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Controller │ (WithdrawController.java)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Service   │ (WithdrawService.java)
└──────┬──────┘
       │
       ├──► Redis (Locking)
       ├──► OTP Service (OTP Verification)
       │
       ▼
┌─────────────┐
│ Repository  │ (WithdrawRequestRepository, WalletRepository, ...)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Database   │ (withdrawrequest, wallet, wallethistory)
└─────────────┘
```

---

## 📦 Các thành phần chính

### 1. Model (Entity Classes)

#### `WithdrawRequest.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/model/WithdrawRequest.java`

**Mô tả:** Entity đại diện cho yêu cầu rút tiền

**Các trường:**
- `id` (Long): ID của yêu cầu
- `shopId` (Long): ID của shop (gian hàng)
- `shop` (Shop): Quan hệ Many-to-One với Shop
- `amount` (BigDecimal): Số tiền cần rút (precision: 15, scale: 2)
- `status` (Status): Trạng thái yêu cầu
- `bankAccountNumber` (String, max 50): Số tài khoản ngân hàng
- `bankAccountName` (String, max 100): Tên chủ tài khoản
- `bankName` (String, max 100): Tên ngân hàng
- `note` (String, max 255): Ghi chú (tùy chọn)

**Enum Status:**
```java
public enum Status {
    PENDING,    // Đang chờ admin duyệt
    APPROVED,   // Đã được admin duyệt
    REJECTED,   // Bị admin từ chối
    CANCELLED   // Người dùng tự hủy
}
```

#### `WalletHistory.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/model/WalletHistory.java`

**Mô tả:** Lịch sử giao dịch của ví

**Các trường liên quan đến withdraw:**
- `type` (Type): WITHDRAW
- `status` (Status): PENDING, SUCCESS, FAILED, CANCELED
- `amount` (BigDecimal): Số tiền
- `referenceId` (String): ID của WithdrawRequest (để liên kết)
- `description` (String): Mô tả giao dịch

---

### 2. Repository

#### `WithdrawRequestRepository.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/repository/WithdrawRequestRepository.java`

**Các phương thức chính:**
```java
// Tìm theo shop ID
List<WithdrawRequest> findByShopId(Long shopId);

// Tìm theo shop ID và chưa bị xóa
List<WithdrawRequest> findByShopIdAndIsDeleteFalse(Long shopId);

// Tìm theo trạng thái
List<WithdrawRequest> findByStatus(WithdrawRequest.Status status);

// Tìm theo shop ID, sắp xếp theo thời gian tạo (mới nhất trước)
List<WithdrawRequest> findByShopIdOrderByCreatedAtDesc(Long shopId);

// Tìm theo shop ID và trạng thái
List<WithdrawRequest> findByShopIdAndStatus(Long shopId, WithdrawRequest.Status status);

// Tìm với bộ lọc phức tạp (ngày, số tiền, thông tin ngân hàng)
List<WithdrawRequest> findByShopIdWithFilters(...);
```

---

### 3. Service

#### `WithdrawService.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/service/WithdrawService.java`

**Các phương thức chính:**

##### `createWithdrawRequest(WithdrawRequestDto requestDto)`
**Mục đích:** Tạo yêu cầu rút tiền mới

**Quy trình:**
1. Lấy user hiện tại từ SecurityContext
2. Tìm shop của user
3. **Acquire Redis Lock** (`user:withdraw:lock:{userId}`) để tránh race condition
4. Validate:
   - Số tiền > 0 và ≥ 100,000 VNĐ
   - Số tiền ≤ số dư ví
   - Thông tin ngân hàng không rỗng
   - Không có yêu cầu PENDING khác
5. Tạo `WithdrawRequest` với status PENDING
6. **Trừ tiền từ ví** (hold amount)
7. Tạo `WalletHistory` với type WITHDRAW, status PENDING
8. **Release lock**
9. Trả về `WithdrawRequestResponse`

**Điểm quan trọng:**
- Sử dụng Redis lock để đảm bảo atomicity
- Tiền được trừ ngay khi tạo yêu cầu (hold)
- Chỉ cho phép 1 yêu cầu PENDING tại một thời điểm

##### `approveWithdrawRequest(Long requestId)`
**Mục đích:** Admin duyệt yêu cầu rút tiền

**Quy trình:**
1. Kiểm tra quyền ADMIN
2. Tìm `WithdrawRequest` theo ID
3. Kiểm tra status = PENDING
4. Cập nhật status = APPROVED
5. Cập nhật `WalletHistory` từ PENDING → SUCCESS
6. **Lưu ý:** Tiền đã được trừ từ lúc tạo yêu cầu, không cần trừ lại

##### `rejectWithdrawRequest(Long requestId)`
**Mục đích:** Admin từ chối yêu cầu rút tiền

**Quy trình:**
1. Kiểm tra quyền ADMIN
2. Tìm `WithdrawRequest` theo ID
3. Kiểm tra status = PENDING
4. Cập nhật status = REJECTED
5. **Hoàn tiền vào ví** (balance += amount)
6. Cập nhật `WalletHistory` từ PENDING → FAILED

##### `cancelWithdrawRequest(Long requestId)`
**Mục đích:** Seller hủy yêu cầu của mình

**Quy trình:**
1. Lấy user hiện tại
2. Tìm `WithdrawRequest` theo ID
3. Kiểm tra quyền sở hữu (shopId phải khớp)
4. Kiểm tra status = PENDING
5. **Hoàn tiền vào ví** (balance += amount)
6. Cập nhật `WalletHistory` từ PENDING → CANCELED
7. Tạo `WalletHistory` mới type REFUND, status SUCCESS
8. Cập nhật status = CANCELLED

##### Các phương thức khác:
- `getWithdrawRequestsByUser()`: Lấy danh sách yêu cầu của user hiện tại
- `getWithdrawRequestsByUserWithFilters(...)`: Lấy danh sách với bộ lọc
- `getAllPendingWithdrawRequests()`: Lấy tất cả yêu cầu PENDING (admin)
- `filterWithdrawRequests(...)`: Lọc danh sách yêu cầu theo các tiêu chí

---

### 4. Controller

#### `WithdrawController.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/controller/WithdrawController.java`

**Các endpoint:**

##### GET `/withdraw`
- **Mục đích:** Hiển thị trang rút tiền cho seller
- **Quyền:** SELLER, ADMIN
- **Response:** `withdraw.html`

##### POST `/api/withdraw/send-otp`
- **Mục đích:** Gửi mã OTP qua email để xác thực
- **Quyền:** SELLER, ADMIN
- **Request Body:** `{}`
- **Response:** `{ "message": "Mã OTP đã được gửi đến email của bạn" }`

##### POST `/api/withdraw/request`
- **Mục đích:** Tạo yêu cầu rút tiền mới
- **Quyền:** SELLER, ADMIN
- **Request Body:**
```json
{
  "amount": 100000,
  "bankAccountNumber": "1234567890",
  "bankAccountName": "Nguyen Van A",
  "bankName": "Vietcombank",
  "note": "Ghi chú (tùy chọn)",
  "otp": "123456"
}
```
- **Response:** `WithdrawRequestResponse`

##### POST `/api/withdraw/cancel/{requestId}`
- **Mục đích:** Hủy yêu cầu rút tiền
- **Quyền:** SELLER, ADMIN (chỉ hủy được yêu cầu của mình)
- **Response:** `{ "message": "Hủy yêu cầu rút tiền thành công..." }`

##### GET `/api/withdraw/requests`
- **Mục đích:** Lấy danh sách yêu cầu rút tiền của user hiện tại
- **Quyền:** SELLER, ADMIN
- **Query Parameters:**
  - `startDate` (String, optional): Ngày bắt đầu
  - `endDate` (String, optional): Ngày kết thúc
  - `status` (String, optional): PENDING, APPROVED, REJECTED, CANCELLED
  - `minAmount` (String, optional): Số tiền tối thiểu
  - `maxAmount` (String, optional): Số tiền tối đa
  - `bankAccountNumber` (String, optional): Số tài khoản
  - `bankName` (String, optional): Tên ngân hàng
  - `bankAccountName` (String, optional): Tên chủ tài khoản
- **Response:** `List<WithdrawRequestResponse>`

##### GET `/api/admin/withdraw/requests`
- **Mục đích:** Lấy tất cả yêu cầu rút tiền (cho admin)
- **Quyền:** ADMIN
- **Query Parameters:** Tương tự như trên, thêm:
  - `searchName` (String, optional): Tìm theo tên shop
  - `searchAccount` (String, optional): Tìm theo số tài khoản
  - `searchBank` (String, optional): Tìm theo tên ngân hàng
- **Response:** `List<WithdrawRequestResponse>`

##### POST `/api/admin/withdraw/approve/{requestId}`
- **Mục đích:** Admin duyệt yêu cầu
- **Quyền:** ADMIN
- **Response:** `{ "message": "Duyệt yêu cầu rút tiền thành công" }`

##### POST `/api/admin/withdraw/reject/{requestId}`
- **Mục đích:** Admin từ chối yêu cầu
- **Quyền:** ADMIN
- **Response:** `{ "message": "Từ chối yêu cầu rút tiền thành công" }`

##### GET `/admin/withdraw-requests`
- **Mục đích:** Hiển thị trang quản lý yêu cầu rút tiền (admin)
- **Quyền:** ADMIN
- **Response:** `admin/withdraw-requests.html`

---

## 🔄 Luồng nghiệp vụ chi tiết

### Luồng 1: Seller tạo yêu cầu rút tiền

```
┌─────────┐
│ Seller  │
└────┬────┘
     │
     │ 1. Truy cập /withdraw
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị form rút tiền
└────┬────────┘
     │
     │ 2. Điền form & Click "Gửi OTP"
     ▼
┌─────────────┐
│ Controller  │ POST /api/withdraw/send-otp
└────┬────────┘
     │
     │ 3. Gọi OtpService
     ▼
┌─────────────┐
│ OtpService  │ Gửi OTP qua email
└────┬────────┘
     │
     │ 4. OTP đã gửi
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị form nhập OTP
└────┬────────┘
     │
     │ 5. Nhập OTP & Submit
     ▼
┌─────────────┐
│ Controller  │ POST /api/withdraw/request
└────┬────────┘
     │
     │ 6. Verify OTP
     ▼
┌─────────────┐
│  OtpService │ Verify OTP
└────┬────────┘
     │
     │ 7. OTP hợp lệ
     ▼
┌─────────────┐
│   Service   │ createWithdrawRequest()
└────┬────────┘
     │
     │ 8. Acquire Redis Lock
     ▼
┌─────────────┐
│    Redis    │ Lock user:withdraw:lock:{userId}
└────┬────────┘
     │
     │ 9. Validate & Create
     ▼
┌─────────────┐
│  Database   │ 
│  - Insert WithdrawRequest (PENDING)
│  - Update Wallet (balance -= amount)
│  - Insert WalletHistory (PENDING)
└────┬────────┘
     │
     │ 10. Release Lock
     ▼
┌─────────────┐
│    Redis    │ Unlock
└────┬────────┘
     │
     │ 11. Response
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị thông báo thành công
│             │ Reload trang để cập nhật số dư
└─────────────┘
```

**Chi tiết các bước:**

1. **Bước 1-2: Người dùng điền form**
   - Nhập số tiền (≥ 100,000 VNĐ)
   - Nhập thông tin ngân hàng (số tài khoản, tên chủ TK, tên ngân hàng)
   - Nhập ghi chú (tùy chọn)
   - Click "Gửi OTP"

2. **Bước 3-4: Gửi và nhận OTP**
   - Backend gửi OTP đến email của user
   - Frontend hiển thị form nhập OTP
   - Có timer 60 giây và nút "Gửi lại OTP"

3. **Bước 5-7: Xác thực OTP**
   - User nhập OTP và submit
   - Backend verify OTP với email, purpose, và IP address
   - Nếu OTP không hợp lệ → trả về lỗi

4. **Bước 8-10: Tạo yêu cầu (có lock)**
   - Acquire Redis lock với key `user:withdraw:lock:{userId}`
   - Trong lock:
     - Validate tất cả thông tin
     - Kiểm tra không có yêu cầu PENDING khác
     - Tạo `WithdrawRequest` với status PENDING
     - Trừ tiền từ ví ngay lập tức
     - Tạo `WalletHistory` để ghi nhận
   - Release lock

5. **Bước 11: Kết quả**
   - Yêu cầu được tạo thành công
   - Số dư ví giảm (tiền đã hold)
   - Frontend reload để hiển thị số dư mới

---

### Luồng 2: Admin duyệt yêu cầu

```
┌─────────┐
│  Admin  │
└────┬────┘
     │
     │ 1. Truy cập /admin/withdraw-requests
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị danh sách yêu cầu PENDING
└────┬────────┘
     │
     │ 2. Click "Duyệt" trên một yêu cầu
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị modal xác nhận
└────┬────────┘
     │
     │ 3. Xác nhận
     ▼
┌─────────────┐
│ Controller  │ POST /api/admin/withdraw/approve/{id}
└────┬────────┘
     │
     │ 4. approveWithdrawRequest()
     ▼
┌─────────────┐
│   Service   │ 
│  - Check ADMIN permission
│  - Find WithdrawRequest
│  - Check status = PENDING
│  - Update status = APPROVED
│  - Update WalletHistory: PENDING → SUCCESS
└────┬────────┘
     │
     │ 5. Save to DB
     ▼
┌─────────────┐
│  Database   │
│  - Update WithdrawRequest.status = APPROVED
│  - Update WalletHistory.status = SUCCESS
│  (Tiền đã được trừ từ lúc tạo, không cần trừ lại)
└────┬────────┘
     │
     │ 6. Response
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị thông báo thành công
│             │ Reload danh sách
└─────────────┘
```

**Lưu ý quan trọng:**
- Tiền đã được trừ từ ví ngay khi tạo yêu cầu (bước hold)
- Khi duyệt, chỉ cần cập nhật status, không cần trừ tiền lại
- WalletHistory được cập nhật từ PENDING → SUCCESS

---

### Luồng 3: Admin từ chối yêu cầu

```
┌─────────┐
│  Admin  │
└────┬────┘
     │
     │ 1. Click "Từ chối"
     ▼
┌─────────────┐
│ Controller  │ POST /api/admin/withdraw/reject/{id}
└────┬────────┘
     │
     │ 2. rejectWithdrawRequest()
     ▼
┌─────────────┐
│   Service   │ 
│  - Check ADMIN permission
│  - Find WithdrawRequest
│  - Check status = PENDING
│  - Update status = REJECTED
│  - Hoàn tiền: wallet.balance += amount
│  - Update WalletHistory: PENDING → FAILED
└────┬────────┘
     │
     │ 3. Save to DB
     ▼
┌─────────────┐
│  Database   │
│  - Update WithdrawRequest.status = REJECTED
│  - Update Wallet.balance += amount (hoàn tiền)
│  - Update WalletHistory.status = FAILED
└────┬────────┘
     │
     │ 4. Response
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị thông báo thành công
└─────────────┘
```

**Lưu ý:**
- Tiền được hoàn về ví ngay khi từ chối
- WalletHistory được cập nhật từ PENDING → FAILED

---

### Luồng 4: Seller hủy yêu cầu

```
┌─────────┐
│ Seller  │
└────┬────┘
     │
     │ 1. Click "Hủy yêu cầu" trên yêu cầu PENDING
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị confirm dialog
└────┬────────┘
     │
     │ 2. Xác nhận
     ▼
┌─────────────┐
│ Controller  │ POST /api/withdraw/cancel/{id}
└────┬────────┘
     │
     │ 3. cancelWithdrawRequest()
     ▼
┌─────────────┐
│   Service   │ 
│  - Check ownership (user's shop)
│  - Check status = PENDING
│  - Hoàn tiền: wallet.balance += amount
│  - Update WalletHistory: PENDING → CANCELED
│  - Create WalletHistory: REFUND, SUCCESS
│  - Update status = CANCELLED
└────┬────────┘
     │
     │ 4. Save to DB
     ▼
┌─────────────┐
│  Database   │
│  - Update WithdrawRequest.status = CANCELLED
│  - Update Wallet.balance += amount
│  - Update WalletHistory (old): CANCELED
│  - Insert WalletHistory (new): REFUND, SUCCESS
└────┬────────┘
     │
     │ 5. Response
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị thông báo thành công
│             │ Reload trang để cập nhật số dư
└─────────────┘
```

**Lưu ý:**
- Chỉ seller mới có thể hủy yêu cầu của mình
- Chỉ có thể hủy yêu cầu ở trạng thái PENDING
- Tiền được hoàn về ví
- Tạo thêm 1 bản ghi WalletHistory type REFUND để ghi nhận việc hoàn tiền

---

## 📡 API Endpoints

### Seller Endpoints

| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|-------|
| GET | `/withdraw` | Trang rút tiền | SELLER, ADMIN |
| POST | `/api/withdraw/send-otp` | Gửi mã OTP | SELLER, ADMIN |
| POST | `/api/withdraw/request` | Tạo yêu cầu rút tiền | SELLER, ADMIN |
| POST | `/api/withdraw/cancel/{id}` | Hủy yêu cầu | SELLER, ADMIN |
| GET | `/api/withdraw/requests` | Lấy danh sách yêu cầu | SELLER, ADMIN |

### Admin Endpoints

| Method | Endpoint | Mô tả | Quyền |
|--------|----------|-------|-------|
| GET | `/admin/withdraw-requests` | Trang quản lý yêu cầu | ADMIN |
| GET | `/api/admin/withdraw/requests` | Lấy tất cả yêu cầu | ADMIN |
| POST | `/api/admin/withdraw/approve/{id}` | Duyệt yêu cầu | ADMIN |
| POST | `/api/admin/withdraw/reject/{id}` | Từ chối yêu cầu | ADMIN |

---

## 💾 Database Schema

### Bảng `withdrawrequest`

```sql
CREATE TABLE withdrawrequest (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shop_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    bank_account_number VARCHAR(50) NOT NULL,
    bank_account_name VARCHAR(100) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_delete BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (shop_id) REFERENCES shop(id)
);
```

**Indexes:**
- `shop_id`: Tìm nhanh theo shop
- `status`: Lọc theo trạng thái
- `created_at`: Sắp xếp theo thời gian

### Bảng `wallethistory`

```sql
CREATE TABLE wallethistory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,  -- WITHDRAW, REFUND, ...
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20),  -- PENDING, SUCCESS, FAILED, CANCELED
    reference_id VARCHAR(255),  -- ID của WithdrawRequest
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    ...
);
```

**Liên kết:**
- `reference_id` = `withdrawrequest.id` (khi type = WITHDRAW)

---

## 🔒 Bảo mật và Validation

### 1. Phân quyền (Security)

**Cấu hình trong `SecurityConfiguration.java`:**
```java
.requestMatchers("/withdraw").hasAnyRole("SELLER", "ADMIN")
.requestMatchers("/api/withdraw/**").hasAnyRole("SELLER", "ADMIN")
.requestMatchers("/api/admin/withdraw/**").hasRole("ADMIN")
```

### 2. Redis Lock

**Mục đích:** Tránh race condition khi user tạo nhiều yêu cầu cùng lúc

**Implementation:**
```java
String lockKey = "user:withdraw:lock:" + user.getId();
Lock lock = redisLockRegistry.obtain(lockKey);

if (lock.tryLock(10, TimeUnit.SECONDS)) {
    try {
        // Xử lý tạo yêu cầu
    } finally {
        lock.unlock();
    }
}
```

**Lợi ích:**
- Đảm bảo chỉ 1 yêu cầu được tạo tại một thời điểm cho mỗi user
- Tránh trường hợp số dư bị âm do concurrent requests

### 3. OTP Verification

**Quy trình:**
1. User click "Gửi OTP"
2. System gửi OTP đến email của user
3. User nhập OTP khi submit form
4. Backend verify OTP với:
   - Email
   - Purpose ("Yêu cầu rút tiền")
   - IP Address (từ header X-Forwarded-For hoặc RemoteAddr)

**Security:**
- OTP có thời hạn (thường 5-10 phút)
- OTP chỉ được sử dụng 1 lần
- Ghi log IP address để tracking

### 4. Validation Rules

#### Số tiền (Amount)
- **Bắt buộc:** Phải có giá trị
- **Minimum:** ≥ 100,000 VNĐ
- **Maximum:** ≤ Số dư ví hiện tại
- **Format:** Số dương, không âm

#### Thông tin ngân hàng
- **Bank Account Number:**
  - Bắt buộc, không rỗng
  - Chỉ chứa số (0-9)
  - Tối thiểu 8 ký tự
  - Tối đa 50 ký tự

- **Bank Account Name:**
  - Bắt buộc, không rỗng
  - Tối thiểu 2 ký tự
  - Tối đa 100 ký tự

- **Bank Name:**
  - Bắt buộc, không rỗng
  - Tối thiểu 2 ký tự
  - Tối đa 100 ký tự

#### Business Rules
- **Một yêu cầu PENDING:** Chỉ cho phép 1 yêu cầu PENDING tại một thời điểm
- **Quyền sở hữu:** Chỉ có thể hủy yêu cầu của chính mình
- **Trạng thái:** Chỉ có thể hủy/duyệt/từ chối yêu cầu ở trạng thái PENDING

### 5. Transaction Management

**Sử dụng `@Transactional`** để đảm bảo:
- Tất cả các thao tác database trong một method hoặc thành công hoặc rollback
- Tính nhất quán dữ liệu

**Ví dụ:**
```java
@Transactional
public void approveWithdrawRequest(Long requestId) {
    // Nếu bất kỳ bước nào fail, tất cả sẽ rollback
    WithdrawRequest request = ...;
    request.setStatus(APPROVED);
    withdrawRequestRepository.save(request);
    
    WalletHistory history = ...;
    history.setStatus(SUCCESS);
    walletHistoryRepository.save(history);
}
```

---

## ⚠️ Error Handling

### Các lỗi thường gặp và cách xử lý

#### 1. Validation Errors

**Số tiền không hợp lệ:**
```json
{
  "error": "Số tiền phải lớn hơn 0"
}
```
```json
{
  "error": "Số tiền tối thiểu là 100,000 VNĐ"
}
```
```json
{
  "error": "Số tiền rút không được vượt quá số dư hiện có: 500000 VNĐ"
}
```

**Thông tin ngân hàng không hợp lệ:**
```json
{
  "error": "Số tài khoản ngân hàng không được để trống"
}
```

**Đã có yêu cầu PENDING:**
```json
{
  "error": "Bạn đã có yêu cầu rút tiền đang chờ duyệt. Vui lòng chờ admin xử lý yêu cầu trước đó."
}
```

#### 2. Authentication/Authorization Errors

**Không có quyền:**
```json
{
  "error": "Chỉ admin mới có thể duyệt yêu cầu rút tiền"
}
```

**Không sở hữu yêu cầu:**
```json
{
  "error": "Bạn không có quyền hủy yêu cầu này"
}
```

#### 3. OTP Errors

**OTP không hợp lệ:**
```json
{
  "error": "Mã OTP không hợp lệ hoặc đã hết hạn"
}
```

**Không thể gửi OTP:**
```json
{
  "error": "Không thể gửi OTP. Vui lòng thử lại sau"
}
```

#### 4. Business Logic Errors

**Yêu cầu đã được xử lý:**
```json
{
  "error": "Yêu cầu này đã được xử lý"
}
```

**Không tìm thấy:**
```json
{
  "error": "Không tìm thấy yêu cầu rút tiền"
}
```

**Chưa có gian hàng:**
```json
{
  "error": "Bạn chưa có gian hàng"
}
```

**Không tìm thấy ví:**
```json
{
  "error": "Không tìm thấy ví của bạn"
}
```

#### 5. System Errors

**Lock timeout:**
```json
{
  "error": "Bạn đang có yêu cầu rút tiền đang được xử lý. Vui lòng đợi hoàn tất trước khi tạo yêu cầu mới."
}
```

**Interrupted:**
```json
{
  "error": "Bị gián đoạn khi tạo yêu cầu rút tiền"
}
```

---

## 🎨 Frontend Flow

### Trang Seller: `withdraw.html`

#### 1. Hiển thị ban đầu
- Hiển thị số dư ví hiện tại
- Hiển thị form tạo yêu cầu (ẩn)
- Hiển thị danh sách lịch sử rút tiền (loading)

#### 2. Tạo yêu cầu mới

**Step 1: Điền form**
- Form validation real-time:
  - Kiểm tra số tiền ≥ 100,000 và ≤ số dư
  - Kiểm tra thông tin ngân hàng
  - Nút "Gửi OTP" chỉ enable khi form hợp lệ

**Step 2: Gửi OTP**
- Click "Gửi OTP"
- Disable button, hiển thị spinner
- Gọi API `/api/withdraw/send-otp`
- Nếu thành công:
  - Hiển thị form nhập OTP
  - Start timer 60 giây
  - Hiện nút "Gửi lại OTP" sau khi hết hạn

**Step 3: Xác thực và submit**
- Nhập OTP (chỉ cho phép số)
- Click "Xác thực và gửi yêu cầu"
- Gọi API `/api/withdraw/request` với OTP
- Nếu thành công:
  - Hiển thị thông báo thành công
  - Đóng modal
  - Reload trang sau 2 giây để cập nhật số dư

#### 3. Lọc và tìm kiếm
- Bộ lọc theo:
  - Khoảng thời gian (từ ngày - đến ngày)
  - Trạng thái (PENDING, APPROVED, REJECTED, CANCELLED)
  - Khoảng số tiền (min - max)
  - Số tài khoản, tên ngân hàng, tên chủ TK
- Click "Lọc" → Gọi API với query parameters
- Click "Xóa bộ lọc" → Reset form và reload

#### 4. Hủy yêu cầu
- Hiển thị nút "Hủy yêu cầu" trên yêu cầu PENDING
- Click → Hiển thị confirm dialog
- Xác nhận → Gọi API `/api/withdraw/cancel/{id}`
- Thành công → Reload trang

---

### Trang Admin: `admin/withdraw-requests.html`

#### 1. Hiển thị thống kê
- 4 card thống kê:
  - Đang chờ duyệt (PENDING)
  - Đã duyệt (APPROVED)
  - Đã từ chối (REJECTED)
  - Đã hủy (CANCELLED)
- Click vào card → Lọc theo trạng thái tương ứng

#### 2. Bộ lọc
- Tương tự trang seller, thêm:
  - Tìm theo tên shop (searchName)
  - Tìm theo số tài khoản (searchAccount)
  - Tìm theo tên ngân hàng (searchBank)

#### 3. Danh sách yêu cầu
- Hiển thị thông tin:
  - Số tiền
  - Tên shop
  - Thông tin ngân hàng
  - Trạng thái
  - Thời gian tạo
- Với yêu cầu PENDING: Hiển thị 2 nút "Duyệt" và "Từ chối"

#### 4. Duyệt/Từ chối
- Click "Duyệt" hoặc "Từ chối"
- Hiển thị modal xác nhận với thông tin chi tiết
- Xác nhận → Gọi API tương ứng
- Thành công → Reload danh sách và cập nhật thống kê

---

## 📁 Các file liên quan

### Backend Files

#### Models
- `study1/src/main/java/com/badat/study1/model/WithdrawRequest.java`
- `study1/src/main/java/com/badat/study1/model/WalletHistory.java`
- `study1/src/main/java/com/badat/study1/model/Wallet.java`
- `study1/src/main/java/com/badat/study1/model/Shop.java`

#### Repositories
- `study1/src/main/java/com/badat/study1/repository/WithdrawRequestRepository.java`
- `study1/src/main/java/com/badat/study1/repository/WalletRepository.java`
- `study1/src/main/java/com/badat/study1/repository/WalletHistoryRepository.java`
- `study1/src/main/java/com/badat/study1/repository/ShopRepository.java`

#### Services
- `study1/src/main/java/com/badat/study1/service/WithdrawService.java`
- `study1/src/main/java/com/badat/study1/service/OtpService.java` (được sử dụng để gửi/verify OTP)

#### Controllers
- `study1/src/main/java/com/badat/study1/controller/WithdrawController.java`
- `study1/src/main/java/com/badat/study1/controller/AdminViewController.java` (trang admin dashboard)

#### DTOs
- `study1/src/main/java/com/badat/study1/dto/request/WithdrawRequestDto.java`
- `study1/src/main/java/com/badat/study1/dto/response/WithdrawRequestResponse.java`

#### Configuration
- `study1/src/main/java/com/badat/study1/configuration/SecurityConfiguration.java` (phân quyền)

### Frontend Files

#### Templates
- `study1/src/main/resources/templates/withdraw.html` (trang rút tiền cho seller)
- `study1/src/main/resources/templates/admin/withdraw-requests.html` (trang quản lý cho admin)

#### Static Resources
- `study1/src/main/resources/static/js/auth.js` (được sử dụng cho authentication)

---

## 📊 State Machine - Trạng thái yêu cầu rút tiền

```
                    ┌─────────┐
                    │ PENDING │ (Tạo yêu cầu, tiền đã hold)
                    └────┬────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
    ┌────────┐    ┌──────────┐    ┌──────────┐
    │APPROVED│    │ REJECTED │    │CANCELLED │
    └────────┘    └──────────┘    └──────────┘
    (Admin duyệt)  (Admin từ chối) (User hủy)
    Tiền giữ nguyên  Tiền hoàn lại   Tiền hoàn lại
```

**Chuyển đổi trạng thái:**

| Từ | Đến | Điều kiện | Thao tác |
|----|-----|-----------|----------|
| PENDING | APPROVED | Admin duyệt | Cập nhật status, WalletHistory: PENDING → SUCCESS |
| PENDING | REJECTED | Admin từ chối | Hoàn tiền, WalletHistory: PENDING → FAILED |
| PENDING | CANCELLED | User hủy | Hoàn tiền, WalletHistory: PENDING → CANCELED + REFUND |

**Lưu ý:**
- Chỉ có thể chuyển từ PENDING sang các trạng thái khác
- Một khi đã APPROVED, REJECTED, hoặc CANCELLED, không thể thay đổi

---

## 🔄 Tác động đến Wallet và WalletHistory

### Khi tạo yêu cầu (PENDING)
```
Wallet:
  balance = balance - amount  (hold tiền)

WalletHistory:
  - type: WITHDRAW
  - status: PENDING
  - amount: amount
  - referenceId: withdrawRequest.id
```

### Khi duyệt (APPROVED)
```
Wallet:
  balance = balance  (không thay đổi, đã trừ từ lúc tạo)

WalletHistory:
  - status: PENDING → SUCCESS
  - description: "Rút tiền thành công từ yêu cầu #X - BankName - AccountNumber"
```

### Khi từ chối (REJECTED)
```
Wallet:
  balance = balance + amount  (hoàn tiền)

WalletHistory:
  - status: PENDING → FAILED
  - description: "Yêu cầu rút tiền #X bị từ chối - Tiền đã được hoàn trả"
```

### Khi hủy (CANCELLED)
```
Wallet:
  balance = balance + amount  (hoàn tiền)

WalletHistory (cũ):
  - status: PENDING → CANCELED
  - description: "Yêu cầu rút tiền #X đã bị hủy - Tiền đã được hoàn trả"

WalletHistory (mới):
  - type: REFUND
  - status: SUCCESS
  - amount: amount
  - description: "Hoàn tiền từ hủy yêu cầu rút tiền #X"
```

---

## 🧪 Test Cases

### Test Case 1: Tạo yêu cầu thành công
**Input:**
- Số tiền: 200,000 VNĐ
- Số dư ví: 500,000 VNĐ
- Thông tin ngân hàng hợp lệ
- OTP hợp lệ

**Expected:**
- Tạo WithdrawRequest với status PENDING
- Số dư ví giảm thành 300,000 VNĐ
- Tạo WalletHistory với status PENDING

### Test Case 2: Tạo yêu cầu với số tiền vượt quá số dư
**Input:**
- Số tiền: 600,000 VNĐ
- Số dư ví: 500,000 VNĐ

**Expected:**
- Lỗi: "Số tiền rút không được vượt quá số dư hiện có"
- Không tạo WithdrawRequest
- Số dư ví không thay đổi

### Test Case 3: Tạo yêu cầu khi đã có yêu cầu PENDING
**Input:**
- Đã có 1 yêu cầu PENDING

**Expected:**
- Lỗi: "Bạn đã có yêu cầu rút tiền đang chờ duyệt"
- Không tạo yêu cầu mới

### Test Case 4: Admin duyệt yêu cầu
**Input:**
- Yêu cầu với status PENDING

**Expected:**
- Status chuyển thành APPROVED
- WalletHistory status chuyển thành SUCCESS
- Số dư ví không thay đổi (đã trừ từ lúc tạo)

### Test Case 5: Admin từ chối yêu cầu
**Input:**
- Yêu cầu với status PENDING, amount = 200,000 VNĐ
- Số dư ví hiện tại: 300,000 VNĐ (sau khi đã hold)

**Expected:**
- Status chuyển thành REJECTED
- Số dư ví tăng thành 500,000 VNĐ (hoàn tiền)
- WalletHistory status chuyển thành FAILED

### Test Case 6: User hủy yêu cầu
**Input:**
- Yêu cầu PENDING của chính user đó

**Expected:**
- Status chuyển thành CANCELLED
- Số dư ví tăng (hoàn tiền)
- WalletHistory cũ: CANCELED
- WalletHistory mới: REFUND, SUCCESS

### Test Case 7: Concurrent requests (Race condition)
**Input:**
- User tạo 2 yêu cầu đồng thời với cùng số tiền

**Expected:**
- Redis lock đảm bảo chỉ 1 yêu cầu được xử lý
- Yêu cầu thứ 2 bị từ chối hoặc chờ đợi

---

## 📝 Notes và Best Practices

### 1. Redis Lock
- **Tại sao cần:** Tránh race condition khi user tạo nhiều yêu cầu cùng lúc
- **Key pattern:** `user:withdraw:lock:{userId}`
- **Timeout:** 10 giây
- **Best practice:** Luôn unlock trong finally block

### 2. OTP Verification
- **Security:** OTP chỉ được sử dụng 1 lần
- **Expiry:** OTP có thời hạn (thường 5-10 phút)
- **Tracking:** Ghi log IP address để tracking

### 3. Transaction Management
- **Sử dụng @Transactional:** Đảm bảo tính nhất quán dữ liệu
- **Rollback:** Nếu bất kỳ bước nào fail, tất cả sẽ rollback

### 4. Error Messages
- **User-friendly:** Thông báo lỗi rõ ràng, dễ hiểu
- **Security:** Không leak thông tin nhạy cảm trong error message

### 5. Logging
- **Log quan trọng:** Tất cả các thao tác duyệt/từ chối đều được log
- **Format:** `log.info("Admin {} approved withdraw request: {} for amount: {} VND", ...)`

### 6. Validation
- **Frontend:** Validation real-time để UX tốt
- **Backend:** Validation bắt buộc để đảm bảo security
- **Double-check:** Kiểm tra lại tất cả validation ở backend

---

## 🚀 Future Improvements

### 1. Tính năng có thể thêm
- **Bulk approval:** Admin có thể duyệt nhiều yêu cầu cùng lúc
- **Auto-approval:** Tự động duyệt yêu cầu dưới một số tiền nhất định
- **Withdrawal limits:** Giới hạn số lần rút trong ngày/tháng
- **Notification:** Gửi email thông báo khi yêu cầu được duyệt/từ chối
- **Export:** Export danh sách yêu cầu ra file Excel/PDF

### 2. Performance
- **Caching:** Cache số dư ví để giảm database queries
- **Pagination:** Phân trang cho danh sách yêu cầu
- **Indexing:** Tối ưu database indexes

### 3. Security
- **Rate limiting:** Giới hạn số lần gửi OTP trong một khoảng thời gian
- **2FA:** Thêm xác thực 2 yếu tố cho admin
- **Audit trail:** Ghi chi tiết hơn về ai duyệt/từ chối yêu cầu

---

## 📞 Support

Nếu có thắc mắc hoặc cần hỗ trợ, vui lòng liên hệ:
- **Email:** support@example.com
- **Documentation:** Xem thêm các file README.md trong project

---

**Tài liệu này được cập nhật lần cuối:** 2025-01-XX
**Phiên bản:** 1.0

