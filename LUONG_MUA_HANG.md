# Tài Liệu: Luồng Mua Hàng Hoàn Chỉnh

## 🎯 Tổng Quan

Hệ thống mua hàng được thiết kế với kiến trúc **Queue-based + Event-driven + Async Processing** để đảm bảo:
- **Hiệu suất cao**: Xử lý không đồng bộ
- **Độ tin cậy**: Transaction safety với Redis locks
- **Khả năng mở rộng**: Batch processing
- **Tính nhất quán**: Double-check validation, rollback tự động

---

## 📋 1. LUỒNG CHÍNH: Từ Request đến Hoàn Tất

### **Bước 1: User Khởi Tạo Thanh Toán**

**Endpoint**: `POST /api/improved-payment/process-cart`

**Controller**: `ImprovedPaymentController.processCartPayment()`

**Flow**:
```
User Request (CartPaymentRequest)
    ↓
PaymentService.processCartPayment()
    ↓
1. Validate cart items (có items không?)
2. Validate total amount (số tiền > 0?)
3. Fast-fail: Check wallet balance
4. Fast-fail: Check stock availability
5. Enqueue payment → PaymentQueueService.enqueuePayment()
```

**Validation trước khi enqueue**:
- ✅ Kiểm tra số dư ví có đủ không
- ✅ Kiểm tra tồn kho có đủ không
- ✅ Nếu không đủ → return error ngay (không vào queue)

---

### **Bước 2: Enqueue Payment vào Queue**

**Service**: `PaymentQueueService.enqueuePayment()`

**Flow**:
```
1. User-level Redis lock: "user:payment:lock:{userId}"
   → Tránh multiple concurrent payments từ cùng user
   
2. Kiểm tra user có payment đang pending không
   → Nếu có → reject (tránh double payment)
   
3. Validate stock availability VỚI Redis lock:
   - Lock per product: "stock:validate:{productId}"
   - Kiểm tra số lượng có sẵn
   
4. Validate user balance:
   - Tạm thời hold 0 VND để check balance
   - Release ngay sau đó
   
5. Tạo PaymentQueue record:
   - userId, cartData (JSON), totalAmount
   - status = PENDING
   
6. 🔥 PUBLISH EVENT: PaymentEvent.paymentCreated()
   → Trigger xử lý ngay lập tức
   
7. Return paymentId cho user
```

**User Lock**: Mỗi user chỉ có thể có 1 payment đang pending tại một thời điểm.

---

### **Bước 3: Event-Driven Trigger (Async)**

**Event**: `PaymentEvent.paymentCreated()`

**Listener**: `PaymentEventListener.handlePaymentCreated()`

**Config**: `@Async("paymentTaskExecutor")` → Thread pool riêng cho payments

**Thread Pool Config** (AsyncConfig):
- **Core**: 50 threads
- **Max**: 200 threads  
- **Queue**: 1000 tasks

**Flow**:
```
PaymentEvent được publish
    ↓
PaymentEventListener (async) nhận event
    ↓
PaymentTriggerService.triggerPaymentProcessing(paymentId)
    ↓
CompletableFuture.runAsync() → Xử lý song song
    ↓
PaymentQueueService.processPaymentItem(payment)
```

**Lợi ích**:
- ⚡ Xử lý ngay lập tức khi có payment mới
- 🔄 Fallback với cron job nếu event bị miss

---

### **Bước 4: Cron Job - Backup Processing**

**Service**: `PaymentQueueService.processPaymentQueue()`

**Cron**: `@Scheduled(fixedRate = 1000)` → Chạy mỗi 1 giây

**Distributed Lock**: `"payment-queue:process"` (Redis)

**Flow**:
```
Cron job chạy mỗi 1 giây
    ↓
Acquire distributed lock (tránh multiple instances)
    ↓
Tìm tất cả PaymentQueue với status = PENDING
    ↓
Xử lý batch: processBatchPayments()
    ↓
Chia thành batch 20 payments/batch
    ↓
processSingleBatch() → processPaymentItem()
```

**Tại sao cần cron job?**
- 🛡️ Backup mechanism nếu event bị miss
- 🔄 Xử lý lại các payment bị stuck
- ⚡ Song song với trigger system

---

### **Bước 5: Process Payment Item (Core Logic)**

**Service**: `PaymentQueueService.processPaymentItem()`

**Flow chi tiết**:

```
1. Distributed Lock: "payment:process:{paymentId}"
   → Tránh double processing từ multiple instances
   
2. Mark status = PROCESSING
   → Tránh cron job xử lý lại
   
3. Parse cart data từ JSON
   → Lấy danh sách products + quantities
   
4. Generate orderId: "ORDER_{userId}_{timestamp}"
   
5. 🔒 LOCK WAREHOUSE ITEMS TRƯỚC (QUAN TRỌNG!)
   → WarehouseLockService.reserveWarehouseItemsWithTimeout()
   → Timeout: 5 phút
   → Lock database rows với SELECT FOR UPDATE
   
6. 💰 HOLD MONEY SAU KHI ĐÃ LOCK ĐƯỢC HÀNG
   → WalletHoldService.holdMoney()
   → Trừ tiền khỏi wallet
   → Tạo WalletHold record (expires in 1 phút)
   
7. ✅ Validate lại: số lượng locked items có đủ không?
   → Nếu không đủ → rollback tất cả
   
8. 📦 Create Order:
   → OrderService.createOrderFromCart()
   → Tạo Order + OrderItems
   → Link với actual warehouse IDs
   → Mark warehouse items as delivered
   
9. ✅ Mark PaymentQueue status = COMPLETED
   
10. 🔥 User có thể nhận hàng ngay lập tức!
```

**Transaction Safety**:
- ✅ @Transactional → Rollback tự động nếu có lỗi
- ✅ Lock warehouse TRƯỚC → Hold money SAU
- ✅ Nếu hold money xong mà không đủ hàng → Unlock warehouse + Release hold

---

### **Bước 6: Hold Money - Wallet Hold Service**

**Service**: `WalletHoldService.holdMoney()`

**Flow**:
```
1. User-level Redis lock: "user:wallet:lock:{userId}"
   → Tránh race condition khi cùng user
   
2. Double-check balance trong lock
   → Kiểm tra số dư có đủ không
   
3. Trừ tiền khỏi wallet:
   wallet.balance = wallet.balance - amount
   
4. Tạo WalletHold record:
   - userId, amount, orderId
   - status = PENDING
   - expiresAt = now() + 1 minute (test: 1 phút)
   
5. Tạo WalletHistory:
   - Type = PURCHASE
   - Status = PENDING
   - Amount = negative (chi tiêu)
```

**Hold Time**: 1 phút (có thể config)
- Trong 1 phút: tiền đã bị trừ, nhưng chưa chuyển cho seller
- Sau 1 phút: nếu hold expired → chuyển tiền cho seller/admin

---

### **Bước 7: Warehouse Locking**

**Service**: `WarehouseLockService.reserveWarehouseItemsWithTimeout()`

**Flow**:
```
1. Với mỗi product + quantity:
   a. Redis lock: "warehouse:reserve:{productId}"
   b. SELECT FOR UPDATE trong database
      → Lock rows ngay từ đầu (atomicity)
   c. Tìm đủ số lượng items available
   d. Set locked = true, lockedBy = userId, lockedAt = now()
   e. Set reservedUntil = now() + timeoutMinutes
   
2. Batch save tất cả items
   
3. Return list of locked Warehouse items
```

**Transaction Safety**:
- ✅ SELECT FOR UPDATE → Database-level lock
- ✅ Redis lock → Application-level lock
- ✅ Timeout: 5 phút
- ✅ Cron job sẽ release expired reservations

**Cron Job để cleanup**:
- `@Scheduled(fixedRate = 60000)` → Mỗi 1 phút
- Tìm các reservation đã hết hạn → Unlock

---

### **Bước 8: Process Expired Holds (Cron Job)**

**Service**: `WalletHoldService.processExpiredHolds()`

**Cron**: `@Scheduled(fixedRate = 5000)` → Chạy mỗi 5 giây

**Flow**:
```
Cron job chạy mỗi 5 giây
    ↓
Tìm tất cả WalletHold:
   - status = PENDING
   - expiresAt < now()
    ↓
processBatchExpiredHolds()
    ↓
Chia thành batch 10 holds/batch
    ↓
processSingleBatchExpiredHolds()
    ↓
Với mỗi expired hold:
   1. Tìm Order theo orderId
   2. distributePaymentToSellerAndAdmin()
      → Chuyển tiền cho seller (theo commission)
      → Chuyển commission cho admin
      → Update wallet history
      → Update order status = COMPLETED
   3. Mark hold status = COMPLETED
```

**Event-Driven Backup**:
- `WalletHoldEventListener.handleHoldExpired()` cũng trigger xử lý khi có event
- Kết hợp cron job → Đảm bảo không bỏ sót

---

### **Bước 9: Payment Distribution**

**Service**: `WalletHoldService.distributePaymentToSellerAndAdmin()`

**Flow**:
```
1. Lấy tất cả OrderItems từ Order
   → Mỗi OrderItem có: sellerId, sellerAmount, commissionAmount
   
2. Group theo sellerId:
   → Tính tổng sellerAmount cho mỗi seller
   
3. Chuyển tiền cho từng seller:
   - sellerWallet.balance += sellerAmount
   - Tạo WalletHistory: Type = SALE_SUCCESS
   
4. Chuyển commission cho admin (userId = 1):
   - adminWallet.balance += totalCommissionAmount
   - Tạo WalletHistory: Type = COMMISSION
   
5. Update buyer wallet history:
   - Update PURCHASE record: Status = SUCCESS
   
6. Update Order status = COMPLETED
```

**Commission Calculation**:
- Mỗi OrderItem có `sellerAmount` và `commissionAmount`
- Tổng `commissionAmount` → Admin nhận

---

## 🔄 2. ERROR HANDLING & ROLLBACK

### **Khi Payment Processing Fail:**

**Service**: `PaymentQueueService.handlePaymentError()`

**Flow**:
```
1. Unlock tất cả warehouse items đã lock
   → Trả hàng về kho
   
2. Release hold money về ví user
   → walletHoldService.releaseHold(userId, orderId)
   → Hoàn tiền lại
   
3. Mark PaymentQueue status = FAILED
   → Set errorMessage
```

**Auto Rollback**:
- ✅ @Transactional → Rollback tự động
- ✅ Nếu lock warehouse fail → Không hold money
- ✅ Nếu hold money fail → Unlock warehouse

---

## ⚙️ 3. CONFIGURATION & INFRASTRUCTURE

### **Async Configuration** (`AsyncConfig`):

```java
// Payment Processing Thread Pool
- Core: 50 threads
- Max: 200 threads
- Queue: 1000 tasks
- Name: "payment-async-*"

// Wallet Hold Processing Thread Pool  
- Core: 20 threads
- Max: 100 threads
- Queue: 500 tasks
- Name: "wallet-hold-async-*"
```

### **Scheduled Tasks**:

| Service | Method | Frequency | Purpose |
|---------|--------|-----------|---------|
| `PaymentQueueService` | `processPaymentQueue()` | 1 second | Process pending payments |
| `WalletHoldService` | `processExpiredHolds()` | 5 seconds | Process expired holds |
| `WarehouseLockService` | `releaseExpiredReservations()` | 60 seconds | Release expired reservations |

### **Application Config**:

```java
@SpringBootApplication
@EnableScheduling  // Enable cron jobs
@EnableAsync       // Enable async processing
```

---

## 🗄️ 4. DATABASE MODELS

### **PaymentQueue**
- `id`, `userId`, `cartData` (JSON), `totalAmount`
- `status`: PENDING → PROCESSING → COMPLETED/FAILED
- `createdAt`, `processedAt`

### **WalletHold**
- `id`, `userId`, `amount`, `orderId`
- `status`: PENDING → COMPLETED/CANCELLED
- `expiresAt`: Thời gian hết hạn (1 phút)

### **Warehouse**
- `id`, `productId`, `userId` (seller)
- `locked`: true/false
- `lockedBy`: userId đang lock
- `lockedAt`: Thời gian lock
- `reservedUntil`: Thời gian hết hạn reservation (5 phút)
- `isDelete`: true khi đã deliver

### **Order**
- `id`, `orderCode`, `userId` (buyer)
- `status`: PENDING → COMPLETED

### **OrderItem**
- `id`, `orderId`, `productId`, `warehouseId`
- `sellerId`, `sellerAmount`, `commissionAmount`

---

## 🔒 5. REDIS LOCKS (Distributed Locking)

### **Lock Keys**:

| Lock Key | Purpose | Location |
|----------|---------|-----------|
| `user:payment:lock:{userId}` | Tránh multiple payments từ cùng user | PaymentQueueService |
| `stock:validate:{productId}` | Validate stock không đồng thời | PaymentQueueService |
| `payment:process:{paymentId}` | Tránh double processing | PaymentQueueService |
| `payment-queue:process` | Cron job lock (multiple instances) | PaymentQueueService |
| `user:wallet:lock:{userId}` | Tránh race condition wallet | WalletHoldService |
| `warehouse:lock:{productId}` | Lock warehouse items | WarehouseLockService |
| `warehouse:reserve:{productId}` | Reserve warehouse với timeout | WarehouseLockService |

---

## 🎯 6. CÁC ĐIỂM QUAN TRỌNG

### **1. Thứ Tự Xử Lý (Quan Trọng!)**:
```
✅ LOCK WAREHOUSE TRƯỚC
✅ HOLD MONEY SAU
```
**Lý do**: Tránh hold tiền mà không có hàng → Phải unlock warehouse ngay lập tức

### **2. Fast-Fail Validation**:
- Validate số dư và tồn kho TRƯỚC khi enqueue
- Không tốn thời gian xử lý nếu không đủ điều kiện

### **3. Double Processing Prevention**:
- Distributed locks cho payment processing
- Status check trước khi process
- User-level lock để tránh concurrent payments

### **4. Event + Cron Dual Processing**:
- Event: Xử lý ngay lập tức (low latency)
- Cron: Backup mechanism (high reliability)
- Kết hợp → Best of both worlds

### **5. Batch Processing**:
- Xử lý nhiều payments/holds cùng lúc
- Tăng throughput
- Giảm database round trips

---

## 📊 7. FLOW DIAGRAM

```
[User Request]
    ↓
[PaymentService.processCartPayment]
    ├─ Validate balance ✓
    ├─ Validate stock ✓
    └─ Enqueue payment
    ↓
[PaymentQueueService.enqueuePayment]
    ├─ User lock
    ├─ Validate stock (with lock)
    └─ Publish PaymentEvent 🔥
    ↓
    ├─ [Event Listener] → Trigger immediately ⚡
    └─ [Cron Job] → Process every 1s 🔄
    ↓
[PaymentQueueService.processPaymentItem]
    ├─ Payment lock
    ├─ LOCK WAREHOUSE ITEMS 🔒
    ├─ HOLD MONEY 💰
    ├─ CREATE ORDER 📦
    └─ Mark COMPLETED ✅
    ↓
[WalletHoldService]
    ├─ Hold expires in 1 minute
    ├─ [Cron Job] → Check every 5s
    └─ [Event Listener] → Trigger on expiry
    ↓
[distributePaymentToSellerAndAdmin]
    ├─ Transfer to seller 💸
    ├─ Transfer commission to admin 💸
    └─ Update order status ✅
```

---

## 🚀 8. PERFORMANCE OPTIMIZATIONS

1. **Async Processing**: Không block main thread
2. **Batch Operations**: Xử lý nhiều items cùng lúc
3. **Parallel Processing**: CompletableFuture cho concurrent tasks
4. **Redis Caching**: Distributed locks, giảm database load
5. **SELECT FOR UPDATE**: Database-level locking
6. **Optimized Queries**: Batch save, fetch join

---

## ⚠️ 9. EDGE CASES HANDLED

1. **Insufficient Balance**: Fast-fail trước khi enqueue
2. **Out of Stock**: Fast-fail trước khi enqueue
3. **Concurrent Payments**: User-level lock
4. **Multiple Instances**: Distributed locks
5. **Processing Failure**: Auto rollback + unlock + refund
6. **Expired Reservations**: Cron job cleanup
7. **Hold Expiry**: Auto distribute payment
8. **Event Miss**: Cron job backup

---

## 🔍 10. MONITORING & LOGGING

Tất cả các bước đều có logging chi tiết:
- Log info: Quá trình xử lý bình thường
- Log warn: Cảnh báo (không đủ hàng, lock fail...)
- Log error: Lỗi nghiêm trọng (rollback, exception...)

**Key Metrics**:
- Số lượng payments trong queue
- Số lượng expired holds
- Thời gian xử lý payment
- Lock acquisition rate

---

## ✅ KẾT LUẬN

Hệ thống mua hàng được thiết kế với:
- ✅ **High Performance**: Async + Batch + Parallel
- ✅ **High Reliability**: Event + Cron + Locks
- ✅ **Transaction Safety**: Rollback + Validation
- ✅ **Scalability**: Distributed locks + Queue system
- ✅ **Fault Tolerance**: Error handling + Retry mechanisms

**Luồng hoạt động**:
1. User request → Validation → Enqueue
2. Event trigger → Immediate processing
3. Cron job → Backup processing
4. Lock warehouse → Hold money → Create order
5. Hold expires → Distribute payment
6. Order completed → User receives items

---

## 📎 Phụ lục: Code tham chiếu trực quan (đã triển khai trong codebase)

### Endpoint khởi tạo thanh toán giỏ hàng
```23:46:study1/src/main/java/com/badat/study1/controller/ImprovedPaymentController.java
@PostMapping("/process-cart")
public ResponseEntity<PaymentResponse> processCartPayment(@RequestBody CartPaymentRequest request) {
    ...
    PaymentResponse response = paymentService.processCartPayment(request);
    ...
}
```

### Xử lý request, validate nhanh và enqueue vào hàng đợi
```200:307:study1/src/main/java/com/badat/study1/service/PaymentService.java
/**
 * Xử lý thanh toán giỏ hàng với queue system
 */
public PaymentResponse processCartPayment(CartPaymentRequest request) {
    ...
    // Fast-fail: Check wallet balance
    ...
    // Fast-fail: Check stock availability
    ...
    // Enqueue payment
    Long paymentId = paymentQueueService.enqueuePayment(
        user.getId(), 
        cartInfo.getCartItems(), 
        cartInfo.getTotalAmount()
    );
    ...
}
```

### Enqueue + khoá theo user + validate tồn kho + publish event
```41:89:study1/src/main/java/com/badat/study1/service/PaymentQueueService.java
/**
 * Thêm payment request vào queue với validation stock trước và user-level lock
 */
@Transactional
public Long enqueuePayment(Long userId, List<Map<String, Object>> cartItems, BigDecimal totalAmount) {
    ...
    // User-level lock
    ...
    // Kiểm tra pending payments
    ...
    // VALIDATE STOCK với Redis lock
    validateStockAvailability(cartItems);
    // Kiểm tra số dư ví
    validateUserBalance(userId, totalAmount);
    ...
    paymentQueueRepository.save(paymentQueue);
    // Publish event để trigger xử lý ngay lập tức
    eventPublisher.publishEvent(PaymentEvent.paymentCreated(this, paymentQueue.getId(), userId));
    ...
}
```

### Event và Listener xử lý ngay (async)
```11:31:study1/src/main/java/com/badat/study1/event/PaymentEvent.java
public class PaymentEvent extends ApplicationEvent {
    private final Long paymentId;
    ...
    public static PaymentEvent paymentCreated(Object source, Long paymentId, Long userId) {
        return new PaymentEvent(source, paymentId, userId, "PAYMENT_CREATED");
    }
}
```

```23:36:study1/src/main/java/com/badat/study1/event/PaymentEventListener.java
@EventListener
@Async("paymentTaskExecutor")
public void handlePaymentCreated(PaymentEvent event) {
    ...
    paymentTriggerService.triggerPaymentProcessing(event.getPaymentId());
}
```

### Trigger service chạy ngay lập tức (song song)
```38:60:study1/src/main/java/com/badat/study1/service/PaymentTriggerService.java
@Transactional
public void triggerPaymentProcessing(Long paymentId) {
    ...
    CompletableFuture.runAsync(() -> {
        try {
            paymentQueueService.processPaymentItem(payment);
            ...
        } catch (Exception e) {
            ...
        }
    }, executorService);
}
```

### Cron job xử lý queue mỗi 1 giây + distributed lock
```185:214:study1/src/main/java/com/badat/study1/service/PaymentQueueService.java
@Scheduled(fixedRate = 1000) // Mỗi 1 giây
public void processPaymentQueue() {
    String lockKey = "payment-queue:process";
    Lock lock = redisLockRegistry.obtain(lockKey);
    if (lock.tryLock(5, ...)) {
        ...
        List<PaymentQueue> pendingPayments = paymentQueueRepository
            .findByStatusOrderByCreatedAtAsc(PaymentQueue.Status.PENDING);
        processBatchPayments(pendingPayments);
    } else {
        ...
    }
}
```

### Core: xử lý 1 payment item (lock kho → hold tiền → tạo order)
```219:305:study1/src/main/java/com/badat/study1/service/PaymentQueueService.java
@Transactional
public void processPaymentItem(PaymentQueue payment) {
    ...
    // Mark PROCESSING
    ...
    // Parse cart
    ...
    // Generate orderId
    ...
    // 1) LOCK WAREHOUSE ITEMS TRƯỚC (reserve có timeout)
    List<Warehouse> lockedItems = warehouseLockService.reserveWarehouseItemsWithTimeout(productQuantities, payment.getUserId(), 5);
    // 2) HOLD MONEY SAU KHI ĐÃ LOCK
    walletHoldService.holdMoney(payment.getUserId(), payment.getTotalAmount(), orderId);
    // 3) Tạo order + gán warehouseId thực tế + mark delivered
    createOrderWithItems(...);
    // 4) Mark COMPLETED
    ...
}
```

### Hold tiền trong ví (user-level lock) + lịch sử ví
```46:114:study1/src/main/java/com/badat/study1/service/WalletHoldService.java
@Transactional
public void holdMoney(Long userId, BigDecimal amount, String orderId) {
    // User-level lock
    ...
    // Kiểm tra số dư + trừ tiền
    ...
    // Tạo WalletHold (expiresAt = now + 1 phút)
    ...
    // Tạo WalletHistory (PURCHASE, PENDING)
    ...
}
```

### Cron job xử lý hold hết hạn mỗi 5 giây (chuyển tiền seller/admin)
```333:344:study1/src/main/java/com/badat/study1/service/WalletHoldService.java
@Scheduled(fixedRate = 5000) // Mỗi 5 giây
public void processExpiredHolds() {
    List<WalletHold> expiredHolds = walletHoldRepository
        .findByStatusAndExpiresAtBefore(WalletHold.Status.PENDING, Instant.now());
    processBatchExpiredHolds(expiredHolds);
}
```

```350:469:study1/src/main/java/com/badat/study1/service/WalletHoldService.java
@Transactional
private void distributePaymentToSellerAndAdmin(WalletHold hold, List<Order> orders) {
    // Group tiền theo seller, chuyển tiền seller
    // Chuyển commission cho admin (userId=1)
    // Update lịch sử ví buyer
    // Update Order status = COMPLETED
}
```

### Đặt chỗ kho với timeout + SELECT FOR UPDATE + Cron release reservation
```291:359:study1/src/main/java/com/badat/study1/service/WarehouseLockService.java
@Transactional
public List<Warehouse> reserveWarehouseItemsWithTimeout(Map<Long, Integer> productQuantities, Long userId, int timeoutMinutes) {
    for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
        String lockKey = "warehouse:reserve:" + productId;
        Lock lock = redisLockRegistry.obtain(lockKey);
        if (lock.tryLock(5, ...)) {
            // SELECT FOR UPDATE
            List<Warehouse> items = warehouseRepository.findAvailableItemsForReservation(productId, requiredQuantity);
            // Set locked=true, lockedBy, lockedAt, reservedUntil=now()+timeout
            warehouseRepository.saveAll(items);
        }
        ...
    }
}
```

```364:389:study1/src/main/java/com/badat/study1/service/WarehouseLockService.java
@Scheduled(fixedRate = 60000) // Mỗi 1 phút
@Transactional
public void releaseExpiredReservations() {
    List<Warehouse> expiredItems = warehouseRepository.findByLockedTrueAndReservedUntilBefore(now);
    for (Warehouse item : expiredItems) {
        item.setLocked(false);
        item.setLockedBy(null);
        item.setLockedAt(null);
        item.setReservedUntil(null);
    }
    warehouseRepository.saveAll(expiredItems);
}
```

### Cấu hình Async thread pools
```22:57:study1/src/main/java/com/badat/study1/config/AsyncConfig.java
@Bean(name = "paymentTaskExecutor")
public Executor paymentTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(50);
    executor.setMaxPoolSize(200);
    executor.setQueueCapacity(1000);
    ...
    return executor;
}

@Bean(name = "walletHoldTaskExecutor")
public Executor walletHoldTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(20);
    executor.setMaxPoolSize(100);
    executor.setQueueCapacity(500);
    ...
    return executor;
}
```

### Bật Scheduling + Async
```8:15:study1/src/main/java/com/badat/study1/Study1Application.java
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Study1Application {
    public static void main(String[] args) {
        SpringApplication.run(Study1Application.class, args);
    }
}
```

### Cấu hình Redis + RedisLockRegistry
```20:51:study1/src/main/java/com/badat/study1/configuration/RedisConfiguration.java
public LettuceConnectionFactory redisConnectionFactory() {
    return new LettuceConnectionFactory();
}

@Bean
public RedisLockRegistry redisLockRegistry(RedisConnectionFactory connectionFactory) {
    return new RedisLockRegistry(connectionFactory, "payment-locks", 30000);
}
```


Dưới đây là danh sách đầy đủ các file Java tham gia xử lý luồng thanh toán (nhóm theo vai trò):
Controller
study1/src/main/java/com/badat/study1/controller/ImprovedPaymentController.java

Services (core flow)
study1/src/main/java/com/badat/study1/service/PaymentService.java
study1/src/main/java/com/badat/study1/service/PaymentQueueService.java
study1/src/main/java/com/badat/study1/service/PaymentTriggerService.java
study1/src/main/java/com/badat/study1/service/WalletHoldService.java
study1/src/main/java/com/badat/study1/service/WarehouseLockService.java

Services (phụ trợ được gọi trong flow)
study1/src/main/java/com/badat/study1/service/OrderService.java
study1/src/main/java/com/badat/study1/service/WalletHistoryService.java
study1/src/main/java/com/badat/study1/service/CartService.java

Events (event-driven triggers)
study1/src/main/java/com/badat/study1/event/PaymentEvent.java
study1/src/main/java/com/badat/study1/event/PaymentEventListener.java
study1/src/main/java/com/badat/study1/event/WalletHoldEvent.java
study1/src/main/java/com/badat/study1/event/WalletHoldEventListener.java

Configurations
study1/src/main/java/com/badat/study1/config/AsyncConfig.java
study1/src/main/java/com/badat/study1/configuration/RedisConfiguration.java
study1/src/main/java/com/badat/study1/Study1Application.java (có @EnableScheduling, @EnableAsync)

Repositories
study1/src/main/java/com/badat/study1/repository/PaymentQueueRepository.java
study1/src/main/java/com/badat/study1/repository/WalletHoldRepository.java
study1/src/main/java/com/badat/study1/repository/WalletRepository.java
study1/src/main/java/com/badat/study1/repository/WarehouseRepository.java
study1/src/main/java/com/badat/study1/repository/OrderRepository.java
study1/src/main/java/com/badat/study1/repository/OrderItemRepository.java

Models/Entities
study1/src/main/java/com/badat/study1/model/PaymentQueue.java
study1/src/main/java/com/badat/study1/model/WalletHold.java
study1/src/main/java/com/badat/study1/model/Wallet.java
study1/src/main/java/com/badat/study1/model/Warehouse.java
study1/src/main/java/com/badat/study1/model/Order.java
study1/src/main/java/com/badat/study1/model/OrderItem.java