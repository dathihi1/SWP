# Phân tích số lượng Transactions trong luồng Nạp Tiền VNPay

## 📊 Tổng quan

Trong luồng nạp tiền qua VNPay, có **2-3 Database Transactions** tùy theo kịch bản:
- **Luồng tạo payment URL:** 1 transaction
- **Luồng callback thành công:** 2 transactions
- **Luồng callback thất bại:** 1 transaction

**Không có External System Transactions** (VNPay callback là incoming request, không phải outbound call).

---

## 🔍 Phân tích chi tiết

### Luồng 1: Tạo Payment URL (POST /payment/create)

#### Quy trình:
1. User chọn số tiền và click "Thanh toán"
2. Gọi `PaymentService.createPaymentUrl()`
3. Tạo orderId
4. Tạo payment URL (không có DB transaction)
5. Gọi `walletHistoryService.saveHistory()` với status PENDING

#### Số lượng Transactions:

**Database Transactions: 1**

```java
// PaymentService.createPaymentUrl() - KHÔNG có @Transactional
public PaymentResponse createPaymentUrl(...) {
    // ... tạo payment URL ...
    
    // Gọi saveHistory với REQUIRES_NEW
    walletHistoryService.saveHistory(
        wallet.getId(),
        amount,
        orderId,
        null,
        WalletHistory.Type.DEPOSIT,
        WalletHistory.Status.PENDING,  // ← PENDING
        description
    );
}

// WalletHistoryService.saveHistory()
@Transactional(propagation = Propagation.REQUIRES_NEW)  // ← Transaction 1
public void saveHistory(...) {
    // Tạo hoặc tìm WalletHistory
    // Save vào database
}
```

**Transaction 1:**
- **Scope:** Tạo WalletHistory với status PENDING
- **Operations:**
  - `findFirstByReferenceId()` - SELECT query
  - `walletHistoryRepository.save()` - INSERT/UPDATE
- **Propagation:** REQUIRES_NEW (transaction riêng biệt)

**External Transactions: 0**
- Tạo payment URL chỉ là string manipulation, không gọi API

---

### Luồng 2: Callback Thành Công (GET /payment/return)

#### Quy trình:
1. VNPay gọi callback về `/payment/return`
2. Verify signature (không có DB transaction)
3. Check ResponseCode = "00" (thành công)
4. Gọi `PaymentService.processPaymentCallback()`
5. Check anti-spam (2 queries)
6. Update wallet balance
7. Update WalletHistory: PENDING → SUCCESS

#### Số lượng Transactions:

**Database Transactions: 2**

```java
// PaymentService.processPaymentCallback()
@Transactional  // ← Transaction chính
public boolean processPaymentCallback(...) {
    // 1. Check anti-spam (trong transaction chính)
    walletHistoryService.existsByTransactionNoAndTypeAndStatus(...);  // SELECT
    walletHistoryService.existsByReferenceIdAndTypeAndStatus(...);    // SELECT
    
    // 2. Update wallet (trong transaction chính)
    wallet.setBalance(newBalance);
    walletRepository.save(wallet);  // UPDATE
    
    // 3. Update WalletHistory (REQUIRES_NEW = transaction riêng)
    walletHistoryService.saveHistory(
        wallet.getId(),
        amount,
        vnpTxnRef,
        vnpTransactionNo,
        WalletHistory.Type.DEPOSIT,
        WalletHistory.Status.SUCCESS,  // ← SUCCESS
        description
    );
}

// WalletHistoryService.saveHistory()
@Transactional(propagation = Propagation.REQUIRES_NEW)  // ← Transaction riêng
public void saveHistory(...) {
    // Find existing PENDING record
    // Update status to SUCCESS
    // Save
}
```

**Transaction 1 (Main Transaction):**
- **Scope:** Check anti-spam + Update wallet
- **Operations:**
  - `existsByTransactionNoAndTypeAndStatus()` - SELECT query
  - `existsByReferenceIdAndTypeAndStatus()` - SELECT query
  - `walletRepository.findByUserId()` - SELECT query
  - `walletRepository.save()` - UPDATE wallet.balance
- **Propagation:** Default (REQUIRED)
- **Commit:** Khi method hoàn thành thành công

**Transaction 2 (REQUIRES_NEW):**
- **Scope:** Update WalletHistory
- **Operations:**
  - `findFirstByReferenceId()` - SELECT query
  - `walletHistoryRepository.save()` - UPDATE walletHistory.status
- **Propagation:** REQUIRES_NEW (transaction độc lập)
- **Commit:** Ngay sau khi save, không phụ thuộc vào transaction chính

**Lý do sử dụng REQUIRES_NEW:**
- Tránh rollback coupling: Nếu update WalletHistory fail, không ảnh hưởng đến việc cập nhật số dư ví
- Đảm bảo WalletHistory luôn được ghi lại, kể cả khi có lỗi ở transaction chính

**External Transactions: 0**
- VNPay callback là **incoming request**, không phải outbound call
- Hệ thống chỉ nhận và xử lý request, không gọi API ra ngoài

---

### Luồng 3: Callback Thất Bại (GET /payment/return)

#### Quy trình:
1. VNPay gọi callback về `/payment/return`
2. Verify signature (không có DB transaction)
3. Check ResponseCode ≠ "00" (thất bại)
4. Gọi `PaymentService.handleFailedPayment()`
5. Tạo/Update WalletHistory với status FAILED

#### Số lượng Transactions:

**Database Transactions: 1**

```java
// PaymentService.handleFailedPayment() - KHÔNG có @Transactional
public void handleFailedPayment(...) {
    // Tìm wallet (không có transaction, chỉ là query)
    Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
    
    // Gọi saveHistory với REQUIRES_NEW
    walletHistoryService.saveHistory(
        wallet.getId(),
        amount,
        vnpTxnRef,
        vnpTransactionNo,
        WalletHistory.Type.DEPOSIT,
        WalletHistory.Status.FAILED,  // ← FAILED
        description
    );
}

// WalletHistoryService.saveHistory()
@Transactional(propagation = Propagation.REQUIRES_NEW)  // ← Transaction 1
public void saveHistory(...) {
    // Find existing PENDING record (nếu có)
    // Update status to FAILED hoặc tạo mới
    // Save
}
```

**Transaction 1:**
- **Scope:** Create/Update WalletHistory với status FAILED
- **Operations:**
  - `findFirstByReferenceId()` - SELECT query (tìm record PENDING nếu có)
  - `walletHistoryRepository.save()` - INSERT hoặc UPDATE
- **Propagation:** REQUIRES_NEW (transaction riêng biệt)

**External Transactions: 0**
- Tương tự luồng thành công, đây là incoming request

---

## 📈 Tổng kết số lượng Transactions

### Theo từng luồng:

| Luồng | Database Transactions | External Transactions | Tổng |
|-------|----------------------|----------------------|------|
| **Tạo Payment URL** | 1 | 0 | **1** |
| **Callback Thành Công** | 2 | 0 | **2** |
| **Callback Thất Bại** | 1 | 0 | **1** |

### Tổng số cho một giao dịch hoàn chỉnh:

**Kịch bản thành công:**
- Tạo URL: **1 transaction**
- Callback thành công: **2 transactions**
- **Tổng: 3 transactions**

**Kịch bản thất bại:**
- Tạo URL: **1 transaction**
- Callback thất bại: **1 transaction**
- **Tổng: 2 transactions**

---

## 🔍 Chi tiết từng Transaction

### Transaction 1: Tạo WalletHistory PENDING
**Khi:** User tạo payment URL
**Method:** `WalletHistoryService.saveHistory()`
**Propagation:** REQUIRES_NEW
**Operations:**
- SELECT: `findFirstByReferenceId()` (tìm record cũ nếu có)
- INSERT/UPDATE: `save()` WalletHistory với status PENDING

### Transaction 2: Update Wallet + Check Anti-Spam (Thành công)
**Khi:** Callback thành công
**Method:** `PaymentService.processPaymentCallback()`
**Propagation:** REQUIRED (default)
**Operations:**
- SELECT: `existsByTransactionNoAndTypeAndStatus()` (check anti-spam)
- SELECT: `existsByReferenceIdAndTypeAndStatus()` (check anti-spam)
- SELECT: `findByUserId()` (lấy wallet)
- UPDATE: `save()` wallet.balance += amount

### Transaction 3: Update WalletHistory SUCCESS (Thành công)
**Khi:** Callback thành công (sau transaction 2)
**Method:** `WalletHistoryService.saveHistory()`
**Propagation:** REQUIRES_NEW
**Operations:**
- SELECT: `findFirstByReferenceId()` (tìm record PENDING)
- UPDATE: `save()` WalletHistory.status = SUCCESS, update transactionNo

### Transaction 4: Create/Update WalletHistory FAILED (Thất bại)
**Khi:** Callback thất bại
**Method:** `WalletHistoryService.saveHistory()`
**Propagation:** REQUIRES_NEW
**Operations:**
- SELECT: `findFirstByReferenceId()` (tìm record PENDING nếu có)
- INSERT/UPDATE: `save()` WalletHistory với status FAILED

---

## 💡 Lý do sử dụng REQUIRES_NEW

### Trong `processPaymentCallback()`:

```java
@Transactional  // Transaction chính
public boolean processPaymentCallback(...) {
    // Update wallet
    walletRepository.save(wallet);
    
    // Update history với REQUIRES_NEW
    walletHistoryService.saveHistory(...);  // Transaction riêng
}
```

**Lý do:**
1. **Tránh rollback coupling:**
   - Nếu update WalletHistory fail, transaction chính vẫn commit
   - Số dư ví đã được cập nhật, history có thể retry sau

2. **Đảm bảo audit trail:**
   - WalletHistory phải được ghi lại để tracking
   - Dù có lỗi gì, history vẫn được lưu

3. **Performance:**
   - Transaction nhỏ hơn, nhanh hơn
   - Commit sớm hơn, giảm lock time

### Trong `handleFailedPayment()`:

```java
// Không có @Transactional
public void handleFailedPayment(...) {
    walletHistoryService.saveHistory(...);  // REQUIRES_NEW
}
```

**Lý do:**
- Method này không có transaction bao bọc
- REQUIRES_NEW tạo transaction mới để đảm bảo data được lưu

---

## 🔄 Transaction Flow Diagram

### Luồng thành công:

```
┌─────────────────────────────────────┐
│ 1. createPaymentUrl()               │
│    (No @Transactional)              │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ Transaction 1: REQUIRES_NEW         │
│ saveHistory(PENDING)                │
│ - SELECT findFirstByReferenceId     │
│ - INSERT/UPDATE WalletHistory       │
│ Commit ✓                            │
└─────────────────────────────────────┘

... User thanh toán trên VNPay ...

┌─────────────────────────────────────┐
│ 2. processPaymentCallback()         │
│    (@Transactional)                 │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ Transaction 2: REQUIRED (Main)      │
│ - SELECT existsByTransactionNo      │
│ - SELECT existsByReferenceId        │
│ - SELECT findByUserId               │
│ - UPDATE wallet.balance             │
│ Commit ✓                            │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ Transaction 3: REQUIRES_NEW         │
│ saveHistory(SUCCESS)                │
│ - SELECT findFirstByReferenceId     │
│ - UPDATE WalletHistory.status       │
│ Commit ✓                            │
└─────────────────────────────────────┘
```

### Luồng thất bại:

```
┌─────────────────────────────────────┐
│ 1. createPaymentUrl()               │
│    Transaction 1: REQUIRES_NEW      │
│    saveHistory(PENDING)             │
│    Commit ✓                         │
└─────────────────────────────────────┘

... User thanh toán thất bại ...

┌─────────────────────────────────────┐
│ 2. handleFailedPayment()            │
│    (No @Transactional)              │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ Transaction 4: REQUIRES_NEW         │
│ saveHistory(FAILED)                 │
│ - SELECT findFirstByReferenceId     │
│ - INSERT/UPDATE WalletHistory       │
│ Commit ✓                            │
└─────────────────────────────────────┘
```

---

## 📊 Database Operations Summary

### Queries per Transaction:

**Transaction 1 (PENDING):**
- 1 SELECT: `findFirstByReferenceId()`
- 1 INSERT/UPDATE: `save() WalletHistory`

**Transaction 2 (Thành công - Main):**
- 2 SELECT: `existsByTransactionNo()`, `existsByReferenceId()`
- 1 SELECT: `findByUserId()`
- 1 UPDATE: `save() Wallet`

**Transaction 3 (Thành công - History):**
- 1 SELECT: `findFirstByReferenceId()`
- 1 UPDATE: `save() WalletHistory`

**Transaction 4 (Thất bại):**
- 1 SELECT: `findFirstByReferenceId()`
- 1 INSERT/UPDATE: `save() WalletHistory`

### Tổng số queries:

**Luồng thành công:**
- 6 SELECT queries
- 2 UPDATE queries
- **Tổng: 8 database operations**

**Luồng thất bại:**
- 3 SELECT queries (2 lần findFirstByReferenceId)
- 2 INSERT/UPDATE queries
- **Tổng: 5 database operations**

---

## ⚠️ Lưu ý quan trọng

### 1. Anti-Spam Checks trong Transaction

```java
@Transactional
public boolean processPaymentCallback(...) {
    // Check 1: TransactionNo
    boolean alreadyProcessed = walletHistoryService.existsByTransactionNoAndTypeAndStatus(...);
    
    // Check 2: OrderId
    boolean orderAlreadyProcessed = walletHistoryService.existsByReferenceIdAndTypeAndStatus(...);
    
    // Nếu cả 2 đều false → mới xử lý
    // Update wallet...
}
```

**Lưu ý:**
- 2 checks này nằm trong cùng transaction với update wallet
- Đảm bảo tính nhất quán: Không có race condition khi check và update

### 2. REQUIRES_NEW cho WalletHistory

**Lợi ích:**
- Tránh rollback coupling
- Đảm bảo audit trail
- Performance tốt hơn (transaction nhỏ)

**Nhược điểm:**
- Có thể có 2 commits riêng biệt
- Nếu transaction 2 thành công nhưng transaction 3 fail → số dư đã cộng nhưng history chưa update (có thể retry)

### 3. Không có External Transactions

**Lý do:**
- VNPay callback là **incoming HTTP request**
- Hệ thống chỉ nhận và xử lý, không gọi API ra ngoài
- Tạo payment URL chỉ là string manipulation, không phải API call

---

## 🎯 Kết luận

### Tổng số Transactions:

**Một giao dịch nạp tiền hoàn chỉnh (thành công):**
- **3 Database Transactions**
  - 1 transaction khi tạo URL (PENDING)
  - 2 transactions khi callback thành công (Update wallet + Update history)

**Một giao dịch nạp tiền hoàn chỉnh (thất bại):**
- **2 Database Transactions**
  - 1 transaction khi tạo URL (PENDING)
  - 1 transaction khi callback thất bại (FAILED)

**External System Transactions: 0**
- Không có outbound API calls
- Chỉ có incoming callback từ VNPay

### Đánh giá:

✅ **Ưu điểm:**
- Số lượng transactions hợp lý (2-3 transactions)
- Sử dụng REQUIRES_NEW hợp lý để tránh rollback coupling
- Anti-spam checks trong transaction đảm bảo consistency

⚠️ **Có thể cải thiện:**
- Có thể tối ưu số lượng queries (ví dụ: combine 2 exists checks)
- Có thể sử dụng batch operations nếu cần

---

**Tài liệu này được tạo:** 2025-01-XX
**Phiên bản:** 1.0


