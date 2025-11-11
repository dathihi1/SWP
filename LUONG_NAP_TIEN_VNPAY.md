# Tài liệu chi tiết: Luồng Nạp Tiền Qua VNPay (VNPay Deposit Flow)

## 📋 Mục lục
1. [Tổng quan](#tổng-quan)
2. [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
3. [Các thành phần chính](#các-thành-phần-chính)
4. [Luồng nghiệp vụ chi tiết](#luồng-nghiệp-vụ-chi-tiết)
5. [API Endpoints](#api-endpoints)
6. [VNPay Integration](#vnpay-integration)
7. [Database Schema](#database-schema)
8. [Bảo mật và Validation](#bảo-mật-và-validation)
9. [Error Handling](#error-handling)
10. [Frontend Flow](#frontend-flow)
11. [Các file liên quan](#các-file-liên-quan)

---

## 🎯 Tổng quan

Hệ thống nạp tiền cho phép người dùng nạp tiền vào ví của họ thông qua **VNPay** - một cổng thanh toán phổ biến tại Việt Nam. Quy trình bao gồm:

1. **User** chọn số tiền cần nạp trên trang payment
2. Hệ thống tạo **payment URL** với VNPay và redirect user đến trang thanh toán VNPay
3. **User** thanh toán trên VNPay (QR Code, Internet Banking, thẻ ATM...)
4. **VNPay** gọi callback về hệ thống với kết quả thanh toán
5. Hệ thống **verify signature** và **cập nhật số dư ví** nếu thành công
6. **User** được redirect về trang kết quả

**Đặc điểm:**
- Hỗ trợ nhiều phương thức thanh toán (QR Code, Internet Banking, thẻ ATM)
- Xác thực chữ ký số (HMAC SHA512) để đảm bảo an toàn
- Chống spam/duplicate với cơ chế kiểm tra transaction đã xử lý
- Tự động cập nhật số dư ví sau khi thanh toán thành công

---

## 🏗️ Kiến trúc hệ thống

```
┌─────────────┐
│   Frontend  │ (payment.html, payment-result.html)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Controller │ (PaymentController.java)
└──────┬──────┘
       │
       ├──► VNPayUtil (Tạo URL, Verify signature)
       │
       ▼
┌─────────────┐
│   Service   │ (PaymentService.java)
└──────┬──────┘
       │
       ├──► WalletRepository (Cập nhật số dư)
       ├──► WalletHistoryService (Lưu lịch sử)
       │
       ▼
┌─────────────┐
│  Database   │ (wallet, wallethistory)
└─────────────┘
       │
       ▲
       │
┌─────────────┐
│    VNPay    │ (Payment Gateway)
│   Sandbox   │
└─────────────┘
```

---

## 📦 Các thành phần chính

### 1. Configuration

#### `VNPayConfig.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/config/VNPayConfig.java`

**Mô tả:** Cấu hình thông tin VNPay từ `application.yaml`

**Các thông số:**
- `vnpUrl`: URL của VNPay (sandbox: `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`)
- `vnpReturnUrl`: URL callback khi thanh toán xong (`http://localhost:8080/payment/return`)
- `vnpTmnCode`: Mã merchant của VNPay (`WFNMT41C`)
- `vnpSecretKey`: Secret key để tạo/verify signature (`GEGAKB9OUVUZFVVMOB5YNTSVBY1IWKTC`)
- `vnpVersion`: Phiên bản API (`2.1.0`)
- `vnpCommand`: Command (`pay`)
- `vnpOrderType`: Loại đơn hàng (`other`)
- `vnpLocale`: Ngôn ngữ (`vn`)
- `vnpCurrencyCode`: Mã tiền tệ (`VND`)

**Cấu hình trong `application.yaml`:**
```yaml
vnpay:
  url: "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
  return-url: "http://localhost:8080/payment/return"
  tmn-code: "WFNMT41C"
  secret-key: "GEGAKB9OUVUZFVVMOB5YNTSVBY1IWKTC"
  version: "2.1.0"
  command: "pay"
  order-type: "other"
  locale: "vn"
  currency-code: "VND"
```

---

### 2. Utility Class

#### `VNPayUtil.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/util/VNPayUtil.java`

**Mô tả:** Utility class để tạo payment URL và verify signature từ VNPay

#### Các phương thức chính:

##### `createPaymentUrl(long amount, String orderInfo, String orderId, HttpServletRequest request)`
**Mục đích:** Tạo payment URL để redirect user đến VNPay

**Quy trình:**
1. Tạo `TreeMap` để sắp xếp parameters theo thứ tự alphabet
2. Thêm các parameters bắt buộc:
   - `vnp_Version`: Phiên bản API
   - `vnp_Command`: Command (pay)
   - `vnp_TmnCode`: Merchant code
   - `vnp_Amount`: Số tiền (nhân 100 vì VNPay dùng đơn vị xu)
   - `vnp_CurrCode`: Mã tiền tệ (VND)
   - `vnp_TxnRef`: Mã đơn hàng (orderId)
   - `vnp_OrderInfo`: Thông tin đơn hàng
   - `vnp_OrderType`: Loại đơn hàng
   - `vnp_Locale`: Ngôn ngữ
   - `vnp_ReturnUrl`: URL callback
   - `vnp_IpAddr`: IP của client
   - `vnp_CreateDate`: Thời gian tạo (yyyyMMddHHmmss)
   - `vnp_ExpireDate`: Thời gian hết hạn (tạo + 15 phút)
3. Sắp xếp parameters và tạo hash data string
4. Tạo **HMAC SHA512 signature** với secret key
5. Build query string với URL encoding
6. Trả về full URL: `{vnpUrl}?{queryString}&vnp_SecureHash={signature}`

**Ví dụ output:**
```
https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?
vnp_Amount=10000000&
vnp_Command=pay&
vnp_CreateDate=20250101120000&
vnp_CurrCode=VND&
vnp_ExpireDate=20250101121500&
vnp_IpAddr=127.0.0.1&
vnp_Locale=vn&
vnp_OrderInfo=Nạp+tiền+vào+ví+MMO+Market&
vnp_OrderType=other&
vnp_ReturnUrl=http://localhost:8080/payment/return&
vnp_TmnCode=WFNMT41C&
vnp_TxnRef=WALLET_1_1704067200000&
vnp_Version=2.1.0&
vnp_SecureHash=abc123def456...
```

##### `verifyPayment(Map<String, String> params)`
**Mục đích:** Verify signature từ VNPay callback

**Quy trình:**
1. Lấy `vnp_SecureHash` từ parameters
2. Loại bỏ `vnp_SecureHash` và `vnp_SecureHashType` khỏi params
3. Sắp xếp parameters theo thứ tự alphabet
4. Tạo hash data string
5. Tạo HMAC SHA512 với secret key
6. So sánh với `vnp_SecureHash` nhận được
7. Trả về `true` nếu khớp, `false` nếu không

**Lưu ý quan trọng:**
- Phải loại bỏ `vnp_SecureHash` và `vnp_SecureHashType` trước khi verify
- Phải sắp xếp parameters theo đúng thứ tự như khi tạo URL
- Phải encode value và replace `%20` với `+` như yêu cầu của VNPay

##### `hmacSHA512(String key, String data)`
**Mục đích:** Tạo HMAC SHA512 signature

**Implementation:**
```java
Mac mac = Mac.getInstance("HmacSHA512");
SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
mac.init(secretKeySpec);
byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
// Convert to hex string
```

##### `getClientIpAddress(HttpServletRequest request)`
**Mục đích:** Lấy IP address của client (hỗ trợ proxy/load balancer)

**Quy trình:**
1. Kiểm tra header `X-Forwarded-For` (lấy IP đầu tiên nếu có nhiều)
2. Kiểm tra header `X-Real-IP`
3. Fallback: `request.getRemoteAddr()`
4. Nếu không có request: lấy localhost IP

---

### 3. Service

#### `PaymentService.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/service/PaymentService.java`

**Mô tả:** Service xử lý logic nghiệp vụ nạp tiền

#### Các phương thức chính:

##### `createPaymentUrl(PaymentRequest request, HttpServletRequest httpRequest)`
**Mục đích:** Tạo payment URL cho user

**Quy trình:**
1. Lấy user hiện tại từ SecurityContext
2. Tạo unique orderId: `WALLET_{userId}_{timestamp}`
   - Ví dụ: `WALLET_1_1704067200000`
3. Gọi `VNPayUtil.createPaymentUrl()` để tạo payment URL
4. **Tạo WalletHistory với status PENDING** ngay lập tức
   - Type: DEPOSIT
   - Status: PENDING
   - ReferenceId: orderId
   - Description: "Deposit via VNPay - Pending"
5. Trả về `PaymentResponse` chứa payment URL

**Lưu ý:**
- Tạo WalletHistory PENDING ngay để track transaction
- OrderId format: `WALLET_{userId}_{timestamp}` để dễ extract user ID sau này

##### `processPaymentCallback(String orderId, Long amount, String vnpTxnRef, String vnpTransactionNo)`
**Mục đích:** Xử lý callback từ VNPay khi thanh toán thành công

**Quy trình:**
1. **Extract user ID từ orderId:**
   - Parse orderId: `WALLET_{userId}_{timestamp}`
   - Lấy userId từ phần thứ 2

2. **ANTI-SPAM: Kiểm tra transaction đã xử lý:**
   - Kiểm tra `vnpTransactionNo` đã tồn tại với status SUCCESS?
   - Kiểm tra `orderId` đã tồn tại với status SUCCESS?
   - Nếu đã xử lý → return `true` (không báo lỗi cho user)

3. **Tìm wallet của user:**
   - Tìm wallet theo userId

4. **Cập nhật số dư ví:**
   - `wallet.balance += amount`
   - Save wallet

5. **Cập nhật WalletHistory:**
   - Tìm record PENDING với referenceId = orderId
   - Update status: PENDING → SUCCESS
   - Update transactionNo
   - Update description: "Deposit via VNPay - TransactionNo: {vnpTransactionNo}"

6. **Trả về `true` nếu thành công**

**Lưu ý quan trọng:**
- Sử dụng `@Transactional` để đảm bảo atomicity
- Có cơ chế chống duplicate/spam
- WalletHistory được update thay vì tạo mới (reuse record PENDING)

##### `handleFailedPayment(String orderId, Long amount, String vnpTxnRef, String vnpTransactionNo, String responseCode)`
**Mục đích:** Xử lý khi thanh toán thất bại

**Quy trình:**
1. Extract user ID từ orderId
2. Tìm wallet của user
3. Tạo/update WalletHistory với:
   - Type: DEPOSIT
   - Status: FAILED
   - Description: "Deposit failed via VNPay - Code: {responseCode} - TransactionNo: {vnpTransactionNo}"

**Lưu ý:**
- Không cập nhật số dư ví
- Chỉ ghi log để tracking

---

### 4. Controller

#### `PaymentController.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/controller/PaymentController.java`

**Mô tả:** Controller xử lý các request liên quan đến payment

#### Các endpoint:

##### GET `/payment`
- **Mục đích:** Hiển thị trang nạp tiền
- **Quyền:** Public (có thể cần authentication)
- **Response:** `payment.html`

##### POST `/payment/create`
- **Mục đích:** Tạo payment URL
- **Request Body:**
```json
{
  "amount": 100000,
  "orderInfo": "Nạp tiền vào ví MMO Market"
}
```
- **Response:**
```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "orderId": "WALLET_1_1704067200000",
  "message": "Payment URL created successfully",
  "success": true
}
```

##### GET `/payment/return`
- **Mục đích:** Callback từ VNPay sau khi thanh toán
- **Query Parameters:** Tất cả parameters từ VNPay (vnp_TxnRef, vnp_Amount, vnp_ResponseCode, vnp_SecureHash, ...)
- **Response:** `payment-result.html` với thông tin kết quả

**Quy trình xử lý callback:**
1. **Verify signature** từ VNPay
   - Gọi `vnPayUtil.verifyPayment(params)`
   - Nếu không hợp lệ → hiển thị lỗi

2. **Kiểm tra ResponseCode:**
   - `"00"` = Thành công
   - Khác `"00"` = Thất bại

3. **Nếu thành công:**
   - Parse amount (chia 100 vì VNPay dùng đơn vị xu)
   - Gọi `paymentService.processPaymentCallback()`
   - Hiển thị trang kết quả thành công

4. **Nếu thất bại:**
   - Gọi `paymentService.handleFailedPayment()`
   - Hiển thị trang kết quả thất bại

**Các parameters từ VNPay:**
- `vnp_TxnRef`: Mã đơn hàng (orderId)
- `vnp_TransactionNo`: Mã giao dịch từ VNPay
- `vnp_Amount`: Số tiền (đơn vị xu, ví dụ: 10000000 = 100,000 VNĐ)
- `vnp_ResponseCode`: Mã phản hồi (`"00"` = thành công)
- `vnp_TransactionStatus`: Trạng thái giao dịch (`"00"` = thành công)
- `vnp_SecureHash`: Chữ ký số để verify
- `vnp_BankCode`: Mã ngân hàng (nếu có)
- `vnp_PayDate`: Ngày thanh toán (yyyyMMddHHmmss)

---

### 5. Model

#### `WalletHistory.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/model/WalletHistory.java`

**Mô tả:** Entity lưu lịch sử giao dịch ví

**Các trường liên quan đến deposit:**
- `type`: `DEPOSIT`
- `status`: `PENDING`, `SUCCESS`, `FAILED`
- `amount`: Số tiền nạp
- `referenceId`: OrderId (ví dụ: `WALLET_1_1704067200000`)
- `transactionNo`: Mã giao dịch từ VNPay
- `description`: Mô tả giao dịch

#### `PaymentLog.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/model/PaymentLog.java`

**Mô tả:** Entity lưu log thanh toán (có thể không được sử dụng trong luồng hiện tại)

---

### 6. Service Helper

#### `WalletHistoryService.java`
**Vị trí:** `study1/src/main/java/com/badat/study1/service/WalletHistoryService.java`

**Mô tả:** Service quản lý WalletHistory

#### Các phương thức:

##### `saveHistory(...)`
**Mục đích:** Lưu/cập nhật WalletHistory

**Quy trình cho DEPOSIT:**
1. Tìm record PENDING với `referenceId = orderId`
2. Nếu tìm thấy:
   - Update status, description, transactionNo
   - Update `updatedAt`
3. Nếu không tìm thấy:
   - Tạo record mới với status PENDING hoặc SUCCESS

**Lưu ý:**
- Sử dụng `REQUIRES_NEW` transaction để tránh rollback coupling
- DEPOSIT reuse record PENDING, các type khác luôn tạo mới

##### `existsByTransactionNoAndTypeAndStatus(...)`
**Mục đích:** Kiểm tra transaction đã xử lý (chống duplicate)

##### `existsByReferenceIdAndTypeAndStatus(...)`
**Mục đích:** Kiểm tra orderId đã xử lý (chống duplicate)

---

## 🔄 Luồng nghiệp vụ chi tiết

### Luồng 1: User nạp tiền thành công

```
┌─────────┐
│  User   │
└────┬────┘
     │
     │ 1. Truy cập /payment
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị form chọn số tiền
└────┬────────┘
     │
     │ 2. Chọn số tiền (ví dụ: 100,000 VNĐ)
     │    Click "Thanh toán qua VNPay"
     ▼
┌─────────────┐
│   Frontend  │ POST /payment/create
│             │ { amount: 100000, orderInfo: "..." }
└────┬────────┘
     │
     │ 3. createPaymentUrl()
     ▼
┌─────────────┐
│   Service   │ 
│  - Tạo orderId: WALLET_1_1704067200000
│  - Tạo WalletHistory PENDING
│  - Gọi VNPayUtil.createPaymentUrl()
└────┬────────┘
     │
     │ 4. createPaymentUrl()
     ▼
┌─────────────┐
│  VNPayUtil  │ 
│  - Tạo parameters
│  - Tạo HMAC SHA512 signature
│  - Build payment URL
└────┬────────┘
     │
     │ 5. Trả về payment URL
     ▼
┌─────────────┐
│   Frontend  │ Redirect user đến VNPay
└────┬────────┘
     │
     │ 6. User thanh toán trên VNPay
     ▼
┌─────────────┐
│    VNPay    │ 
│  - User chọn phương thức (QR, Banking...)
│  - User thanh toán
│  - VNPay xử lý thanh toán
└────┬────────┘
     │
     │ 7. VNPay gọi callback
     │    GET /payment/return?{params}&vnp_SecureHash=...
     ▼
┌─────────────┐
│ Controller  │ paymentReturn()
└────┬────────┘
     │
     │ 8. Verify signature
     ▼
┌─────────────┐
│  VNPayUtil  │ verifyPayment()
│             │ - Verify HMAC SHA512
└────┬────────┘
     │
     │ 9. Signature hợp lệ
     │    ResponseCode = "00" (thành công)
     ▼
┌─────────────┐
│   Service   │ processPaymentCallback()
└────┬────────┘
     │
     │ 10. Extract userId từ orderId
     │     Check anti-spam (transaction đã xử lý?)
     │     Find wallet
     │     wallet.balance += amount
     │     Update WalletHistory: PENDING → SUCCESS
     ▼
┌─────────────┐
│  Database   │ 
│  - Update Wallet.balance
│  - Update WalletHistory.status = SUCCESS
└────┬────────┘
     │
     │ 11. Trả về kết quả
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị payment-result.html
│             │ "Thanh toán thành công!"
└─────────────┘
```

**Chi tiết các bước:**

1. **Bước 1-2: User chọn số tiền**
   - User truy cập `/payment`
   - Chọn số tiền từ danh sách (50k, 100k, 200k...) hoặc nhập custom
   - Click "Thanh toán qua VNPay"

2. **Bước 3-4: Tạo payment URL**
   - Frontend gọi `POST /payment/create` với amount và orderInfo
   - Backend tạo orderId: `WALLET_{userId}_{timestamp}`
   - Tạo WalletHistory với status PENDING
   - Tạo payment URL với VNPay parameters và signature

3. **Bước 5-6: Redirect đến VNPay**
   - Frontend redirect user đến payment URL
   - User thấy trang thanh toán VNPay

4. **Bước 7-8: User thanh toán và VNPay callback**
   - User chọn phương thức thanh toán (QR Code, Internet Banking, thẻ ATM...)
   - User hoàn tất thanh toán
   - VNPay gọi callback về `/payment/return` với tất cả parameters

5. **Bước 9-10: Verify và xử lý**
   - Backend verify signature từ VNPay
   - Kiểm tra ResponseCode = "00" (thành công)
   - Extract userId từ orderId
   - Kiểm tra anti-spam (tránh xử lý duplicate)
   - Cập nhật số dư ví
   - Update WalletHistory từ PENDING → SUCCESS

6. **Bước 11: Hiển thị kết quả**
   - User được redirect về trang kết quả
   - Hiển thị thông báo thành công và số tiền đã nạp

---

### Luồng 2: User hủy thanh toán hoặc thanh toán thất bại

```
┌─────────┐
│  User   │
└────┬────┘
     │
     │ 1-6. Tương tự luồng 1
     ▼
┌─────────────┐
│    VNPay    │ 
│  - User hủy hoặc thanh toán thất bại
│  - ResponseCode ≠ "00"
└────┬────────┘
     │
     │ 7. VNPay gọi callback với ResponseCode ≠ "00"
     ▼
┌─────────────┐
│ Controller  │ paymentReturn()
└────┬────────┘
     │
     │ 8. Verify signature ✓
     │    ResponseCode ≠ "00" (thất bại)
     ▼
┌─────────────┐
│   Service   │ handleFailedPayment()
│             │ - Tạo WalletHistory với status FAILED
│             │ - Không cập nhật số dư
└────┬────────┘
     │
     │ 9. Ghi log
     ▼
┌─────────────┐
│  Database   │ 
│  - Insert/Update WalletHistory (FAILED)
└────┬────────┘
     │
     │ 10. Trả về kết quả
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị payment-result.html
│             │ "Thanh toán thất bại!"
└─────────────┘
```

**Lưu ý:**
- Nếu user hủy trên VNPay, vẫn có callback với ResponseCode khác "00"
- WalletHistory được tạo với status FAILED để tracking
- Số dư ví không thay đổi

---

### Luồng 3: Signature không hợp lệ

```
┌─────────┐
│  User   │
└────┬────┘
     │
     │ 1-6. Tương tự luồng 1
     ▼
┌─────────────┐
│    VNPay    │ 
│  - Gọi callback
└────┬────────┘
     │
     │ 7. VNPay gọi callback
     ▼
┌─────────────┐
│ Controller  │ paymentReturn()
└────┬────────┘
     │
     │ 8. Verify signature
     ▼
┌─────────────┐
│  VNPayUtil  │ verifyPayment()
│             │ → Signature không hợp lệ ✗
└────┬────────┘
     │
     │ 9. Trả về lỗi
     ▼
┌─────────────┐
│   Frontend  │ Hiển thị payment-result.html
│             │ "Chữ ký thanh toán không hợp lệ!"
└─────────────┘
```

**Lưu ý:**
- Signature không hợp lệ có thể do:
  - Secret key không khớp
  - Parameters bị thay đổi
  - Lỗi khi verify hash
- Không cập nhật số dư ví
- Không tạo WalletHistory

---

## 📡 API Endpoints

| Method | Endpoint | Mô tả | Input | Output |
|--------|----------|-------|-------|--------|
| GET | `/payment` | Trang nạp tiền | - | `payment.html` |
| POST | `/payment/create` | Tạo payment URL | `PaymentRequest` | `PaymentResponse` |
| GET | `/payment/return` | Callback từ VNPay | Query params từ VNPay | `payment-result.html` |

### Request/Response DTOs

#### `PaymentRequest`
```json
{
  "amount": 100000,
  "orderInfo": "Nạp tiền vào ví MMO Market"
}
```

#### `PaymentResponse`
```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "orderId": "WALLET_1_1704067200000",
  "message": "Payment URL created successfully",
  "success": true,
  "paymentId": null
}
```

---

## 🔐 VNPay Integration

### Cách VNPay hoạt động

1. **Tạo payment URL:**
   - Merchant (hệ thống) tạo payment URL với đầy đủ thông tin
   - Tạo signature (HMAC SHA512) để đảm bảo tính toàn vẹn
   - Redirect user đến URL này

2. **User thanh toán:**
   - User chọn phương thức thanh toán trên VNPay
   - User hoàn tất thanh toán

3. **VNPay callback:**
   - VNPay gọi GET request về `returnUrl` với tất cả parameters
   - Bao gồm signature để merchant verify

4. **Merchant verify và xử lý:**
   - Verify signature để đảm bảo request từ VNPay
   - Kiểm tra ResponseCode để biết kết quả
   - Cập nhật trạng thái đơn hàng

### VNPay Parameters

#### Parameters khi tạo URL:
- `vnp_Version`: Phiên bản API (2.1.0)
- `vnp_Command`: Command (pay)
- `vnp_TmnCode`: Merchant code
- `vnp_Amount`: Số tiền (đơn vị xu, ví dụ: 10000000 = 100,000 VNĐ)
- `vnp_CurrCode`: Mã tiền tệ (VND)
- `vnp_TxnRef`: Mã đơn hàng (unique)
- `vnp_OrderInfo`: Thông tin đơn hàng
- `vnp_OrderType`: Loại đơn hàng
- `vnp_Locale`: Ngôn ngữ (vn, en)
- `vnp_ReturnUrl`: URL callback
- `vnp_IpAddr`: IP của client
- `vnp_CreateDate`: Thời gian tạo (yyyyMMddHHmmss)
- `vnp_ExpireDate`: Thời gian hết hạn (yyyyMMddHHmmss)
- `vnp_BankCode`: Mã ngân hàng (tùy chọn, để user tự chọn nếu không set)

#### Parameters từ callback:
- `vnp_TxnRef`: Mã đơn hàng
- `vnp_TransactionNo`: Mã giao dịch từ VNPay
- `vnp_Amount`: Số tiền (đơn vị xu)
- `vnp_ResponseCode`: Mã phản hồi (`"00"` = thành công)
- `vnp_TransactionStatus`: Trạng thái (`"00"` = thành công)
- `vnp_SecureHash`: Chữ ký số
- `vnp_BankCode`: Mã ngân hàng đã sử dụng
- `vnp_PayDate`: Ngày thanh toán (yyyyMMddHHmmss)
- `vnp_CardType`: Loại thẻ (nếu có)

### Response Codes

| Code | Ý nghĩa |
|------|---------|
| `00` | Giao dịch thành công |
| `07` | Trừ tiền thành công, giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường) |
| `09` | Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking |
| `10` | Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần |
| `11` | Đã hết hạn chờ thanh toán. Xin vui lòng thực hiện lại giao dịch |
| `12` | Thẻ/Tài khoản bị khóa |
| `13` | Nhập sai mật khẩu xác thực giao dịch (OTP) |
| `51` | Tài khoản không đủ số dư để thực hiện giao dịch |
| `65` | Tài khoản đã vượt quá hạn mức giao dịch trong ngày |
| `75` | Ngân hàng thanh toán đang bảo trì |
| `79` | Nhập sai mật khẩu thanh toán quá số lần quy định |

---

## 💾 Database Schema

### Bảng `wallethistory`

```sql
CREATE TABLE wallethistory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,  -- DEPOSIT, WITHDRAW, PURCHASE, REFUND, SALE, SALE_SUCCESS, COMMISSION
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20),  -- PENDING, SUCCESS, FAILED, CANCELED
    reference_id VARCHAR(255),  -- OrderId (ví dụ: WALLET_1_1704067200000)
    transaction_no VARCHAR(255),  -- Mã giao dịch từ VNPay
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    ...
);
```

**Ví dụ record cho deposit thành công:**
```
id: 1
wallet_id: 1
type: DEPOSIT
amount: 100000.00
status: SUCCESS
reference_id: WALLET_1_1704067200000
transaction_no: 13123456
description: Deposit via VNPay - TransactionNo: 13123456
created_at: 2025-01-01 12:00:00
updated_at: 2025-01-01 12:05:00
```

**Ví dụ record cho deposit thất bại:**
```
id: 2
wallet_id: 1
type: DEPOSIT
amount: 100000.00
status: FAILED
reference_id: WALLET_1_1704067200001
transaction_no: null
description: Deposit failed via VNPay - Code: 51 - TransactionNo: 
created_at: 2025-01-01 12:10:00
updated_at: 2025-01-01 12:10:00
```

### Bảng `wallet`

```sql
CREATE TABLE wallet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    ...
);
```

**Cập nhật:**
- Khi deposit thành công: `balance = balance + amount`

---

## 🔒 Bảo mật và Validation

### 1. Signature Verification

**Mục đích:** Đảm bảo request từ VNPay và không bị thay đổi

**Cách hoạt động:**
1. VNPay tạo signature với secret key và tất cả parameters
2. Merchant verify signature với cùng secret key
3. Nếu signature không khớp → request không hợp lệ

**Lưu ý:**
- Secret key phải được bảo mật tuyệt đối
- Không được hardcode trong code, nên dùng environment variables
- Signature được tạo bằng HMAC SHA512

### 2. Anti-Spam/Duplicate Protection

**Mục đích:** Tránh xử lý duplicate transaction

**Cơ chế:**
1. Kiểm tra `vnpTransactionNo` đã tồn tại với status SUCCESS?
2. Kiểm tra `orderId` đã tồn tại với status SUCCESS?
3. Nếu đã xử lý → return `true` (không báo lỗi, nhưng không xử lý lại)

**Lợi ích:**
- Tránh user được cộng tiền nhiều lần
- VNPay có thể gọi callback nhiều lần (do network, retry...)

### 3. Transaction Management

**Sử dụng `@Transactional`:**
- Đảm bảo tất cả operations (update wallet, update history) hoặc thành công hoặc rollback
- Tránh trường hợp số dư được cộng nhưng history không được tạo

**Sử dụng `REQUIRES_NEW` cho WalletHistory:**
- Tách transaction riêng để tránh rollback coupling
- Nếu có lỗi khi save history, không ảnh hưởng đến việc cập nhật số dư

### 4. Validation

#### Amount Validation
- **Minimum:** 10,000 VNĐ (frontend)
- **Maximum:** 10,000,000 VNĐ (frontend)
- **Format:** Số nguyên, không âm

#### OrderId Format
- Phải bắt đầu với `WALLET_`
- Format: `WALLET_{userId}_{timestamp}`
- Ví dụ: `WALLET_1_1704067200000`

### 5. Security Best Practices

1. **Secret Key:**
   - Không commit vào git
   - Sử dụng environment variables hoặc secure vault
   - Rotate định kỳ

2. **IP Whitelist (nếu có):**
   - Có thể whitelist IP của VNPay để chỉ nhận callback từ IP đó

3. **HTTPS:**
   - Luôn sử dụng HTTPS cho returnUrl
   - Đảm bảo data được mã hóa trong transit

4. **Logging:**
   - Log tất cả payment transactions để audit
   - Không log sensitive data (secret key, card numbers...)

---

## ⚠️ Error Handling

### Các lỗi thường gặp và cách xử lý

#### 1. Signature không hợp lệ

**Nguyên nhân:**
- Secret key không khớp
- Parameters bị thay đổi
- Lỗi khi verify hash

**Xử lý:**
- Hiển thị lỗi: "Chữ ký thanh toán không hợp lệ!"
- Không cập nhật số dư ví
- Log lỗi để kiểm tra

#### 2. ResponseCode ≠ "00"

**Nguyên nhân:**
- User hủy thanh toán
- Tài khoản không đủ số dư
- Thẻ bị khóa
- Nhập sai OTP

**Xử lý:**
- Gọi `handleFailedPayment()` để ghi log
- Hiển thị lỗi với ResponseCode
- Không cập nhật số dư ví

#### 3. Transaction đã xử lý (Duplicate)

**Nguyên nhân:**
- VNPay gọi callback nhiều lần
- User refresh trang callback

**Xử lý:**
- Kiểm tra anti-spam
- Return `true` (không báo lỗi)
- Không xử lý lại

#### 4. Wallet không tồn tại

**Nguyên nhân:**
- User ID không hợp lệ
- Wallet chưa được tạo

**Xử lý:**
- Log lỗi
- Return `false`
- Hiển thị lỗi: "Không tìm thấy ví của bạn"

#### 5. OrderId không đúng format

**Nguyên nhân:**
- OrderId không bắt đầu với `WALLET_`
- Không thể parse userId

**Xử lý:**
- Log lỗi
- Return `false`
- Không xử lý

#### 6. Exception khi xử lý

**Xử lý:**
- Catch exception và log
- Return `false`
- Hiển thị lỗi: "Đã xảy ra lỗi khi xử lý giao dịch"

---

## 🎨 Frontend Flow

### Trang `payment.html`

#### 1. Hiển thị form
- **Amount buttons:** 6 nút với các mức tiền phổ biến (50k, 100k, 200k, 500k, 1M, 2M)
- **Custom amount input:** Cho phép nhập số tiền tùy chỉnh (10k - 10M)
- **Payment button:** Nút "Thanh toán qua VNPay" (disabled cho đến khi chọn số tiền)

#### 2. Chọn số tiền
- Click vào amount button → Chọn số tiền đó
- Nhập custom amount → Chọn số tiền tùy chỉnh
- Button được enable khi có số tiền hợp lệ (10k - 10M)

#### 3. Tạo payment
- Click "Thanh toán qua VNPay"
- Hiển thị loading modal
- Gọi API `POST /payment/create`:
  ```javascript
  {
    amount: selectedAmount,
    orderInfo: 'Nạp tiền vào ví MMO Market'
  }
  ```
- Nếu thành công:
  - Redirect user đến `data.paymentUrl`
  - User được chuyển đến trang VNPay

#### 4. Sau khi thanh toán
- VNPay redirect về `/payment/return`
- Backend xử lý và redirect đến `payment-result.html`

---

### Trang `payment-result.html`

#### 1. Hiển thị kết quả

**Nếu thành công:**
- Icon: ✓ (màu xanh)
- Tiêu đề: "Thanh toán thành công!"
- Message: "Nạp tiền thành công!"
- Hiển thị số tiền đã nạp
- Hiển thị mã đơn hàng và mã truy vết
- Thông báo: "Số dư ví đã được cập nhật"

**Nếu thất bại:**
- Icon: ✗ (màu đỏ)
- Tiêu đề: "Thanh toán thất bại!"
- Message: Lỗi cụ thể
- Gợi ý: "Vui lòng kiểm tra lại thông tin thanh toán"
- Hiển thị mã đơn hàng và mã truy vết (nếu có)

#### 2. Action buttons
- "Về trang chủ": Link đến `/`
- "Nạp tiền tiếp": Link đến `/payment`

---

## 📁 Các file liên quan

### Backend Files

#### Configuration
- `study1/src/main/java/com/badat/study1/config/VNPayConfig.java`
- `study1/src/main/resources/application.yaml` (VNPay config)

#### Utility
- `study1/src/main/java/com/badat/study1/util/VNPayUtil.java`

#### Controller
- `study1/src/main/java/com/badat/study1/controller/PaymentController.java`

#### Service
- `study1/src/main/java/com/badat/study1/service/PaymentService.java`
- `study1/src/main/java/com/badat/study1/service/WalletHistoryService.java`

#### Model
- `study1/src/main/java/com/badat/study1/model/WalletHistory.java`
- `study1/src/main/java/com/badat/study1/model/Wallet.java`
- `study1/src/main/java/com/badat/study1/model/PaymentLog.java`

#### DTO
- `study1/src/main/java/com/badat/study1/dto/request/PaymentRequest.java`
- `study1/src/main/java/com/badat/study1/dto/response/PaymentResponse.java`

#### Repository
- `study1/src/main/java/com/badat/study1/repository/WalletRepository.java`
- `study1/src/main/java/com/badat/study1/repository/WalletHistoryRepository.java`

### Frontend Files

#### Templates
- `study1/src/main/resources/templates/payment.html`
- `study1/src/main/resources/templates/payment-result.html`

---

## 🔄 State Machine - Trạng thái giao dịch

```
                    ┌─────────┐
                    │ PENDING │ (Tạo payment URL, tạo WalletHistory)
                    └────┬────┘
                         │
                         │ VNPay callback
                         │ ResponseCode = "00"
                         ▼
                    ┌────────┐
                    │SUCCESS │ (Cập nhật số dư ví)
                    └────────┘
                         │
                         │ VNPay callback
                         │ ResponseCode ≠ "00"
                         ▼
                    ┌────────┐
                    │ FAILED │ (Không cập nhật số dư)
                    └────────┘
```

**Chuyển đổi trạng thái:**

| Từ | Đến | Điều kiện | Thao tác |
|----|-----|-----------|----------|
| - | PENDING | Tạo payment URL | Tạo WalletHistory PENDING |
| PENDING | SUCCESS | VNPay callback, ResponseCode = "00" | Cập nhật số dư, Update WalletHistory |
| PENDING | FAILED | VNPay callback, ResponseCode ≠ "00" | Update WalletHistory, không cập nhật số dư |

---

## 🧪 Test Cases

### Test Case 1: Nạp tiền thành công
**Input:**
- Amount: 100,000 VNĐ
- User có wallet hợp lệ

**Expected:**
- Tạo WalletHistory PENDING
- Redirect đến VNPay
- Sau khi thanh toán thành công:
  - WalletHistory update thành SUCCESS
  - Số dư ví tăng 100,000 VNĐ
  - Hiển thị trang kết quả thành công

### Test Case 2: Nạp tiền thất bại (User hủy)
**Input:**
- Amount: 100,000 VNĐ
- User hủy thanh toán trên VNPay

**Expected:**
- VNPay callback với ResponseCode ≠ "00"
- WalletHistory update thành FAILED
- Số dư ví không thay đổi
- Hiển thị trang kết quả thất bại

### Test Case 3: Signature không hợp lệ
**Input:**
- Callback từ VNPay với signature không hợp lệ

**Expected:**
- Verify signature fail
- Không cập nhật số dư
- Không update WalletHistory
- Hiển thị lỗi: "Chữ ký thanh toán không hợp lệ"

### Test Case 4: Duplicate transaction
**Input:**
- VNPay gọi callback lần 2 với cùng transactionNo

**Expected:**
- Kiểm tra anti-spam: Transaction đã xử lý
- Return `true` (không báo lỗi)
- Không xử lý lại
- Số dư không thay đổi

### Test Case 5: Amount validation
**Input:**
- Amount < 10,000 VNĐ
- Amount > 10,000,000 VNĐ

**Expected:**
- Frontend validation: Button disabled
- Không cho phép submit

### Test Case 6: OrderId không đúng format
**Input:**
- OrderId không bắt đầu với `WALLET_`

**Expected:**
- Log lỗi
- Return `false`
- Không xử lý

---

## 📝 Notes và Best Practices

### 1. OrderId Format
- **Format:** `WALLET_{userId}_{timestamp}`
- **Lý do:** Dễ extract userId khi xử lý callback
- **Unique:** Sử dụng timestamp để đảm bảo unique

### 2. Amount Handling
- **VNPay:** Sử dụng đơn vị xu (ví dụ: 10000000 = 100,000 VNĐ)
- **Hệ thống:** Lưu VNĐ (ví dụ: 100000.00)
- **Conversion:** Nhân 100 khi gửi, chia 100 khi nhận

### 3. WalletHistory Management
- **DEPOSIT:** Reuse record PENDING, update thành SUCCESS
- **Lý do:** Tránh duplicate records, dễ track transaction

### 4. Anti-Spam Protection
- **Kiểm tra transactionNo:** Tránh xử lý duplicate
- **Kiểm tra orderId:** Tránh xử lý duplicate
- **Return true:** Nếu đã xử lý, không báo lỗi cho user

### 5. Error Handling
- **Log tất cả errors:** Để debug và audit
- **User-friendly messages:** Không hiển thị technical errors
- **Graceful degradation:** Không crash khi có lỗi

### 6. Testing
- **Sandbox environment:** Sử dụng VNPay sandbox để test
- **Test cases:** Test tất cả scenarios (success, fail, duplicate, invalid signature...)
- **Integration test:** Test với VNPay sandbox

---

## 🚀 Future Improvements

### 1. Tính năng có thể thêm
- **Payment history:** Trang xem lịch sử nạp tiền
- **Auto-retry:** Tự động retry khi callback fail
- **Webhook:** Nhận webhook từ VNPay thay vì chỉ dựa vào callback
- **Multiple payment methods:** Hỗ trợ thêm các cổng thanh toán khác
- **Refund:** Hỗ trợ hoàn tiền

### 2. Performance
- **Caching:** Cache wallet balance (tùy chọn)
- **Async processing:** Xử lý callback async
- **Queue system:** Sử dụng queue để xử lý payment (nếu có nhiều giao dịch)

### 3. Security
- **IP Whitelist:** Whitelist IP của VNPay
- **Rate limiting:** Giới hạn số lần tạo payment trong một khoảng thời gian
- **2FA:** Thêm xác thực 2 yếu tố cho nạp tiền lớn

### 4. Monitoring
- **Alerts:** Cảnh báo khi có lỗi thanh toán
- **Dashboard:** Dashboard theo dõi số lượng và giá trị giao dịch
- **Analytics:** Phân tích xu hướng nạp tiền

---

## 📞 Support

Nếu có thắc mắc hoặc cần hỗ trợ, vui lòng liên hệ:
- **Email:** support@example.com
- **VNPay Documentation:** https://sandbox.vnpayment.vn/apis/
- **VNPay Support:** https://sandbox.vnpayment.vn/

---

**Tài liệu này được cập nhật lần cuối:** 2025-01-XX
**Phiên bản:** 1.0

