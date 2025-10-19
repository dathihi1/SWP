# 🚀 Hệ thống thanh toán cải tiến với Hold Money (1 phút)

## 📋 Tổng quan

Hệ thống thanh toán đã được cải tiến với các tính năng:
- ✅ **Hold Money**: Giữ tiền 1 phút trước khi giải ngân
- ✅ **Redis Lock**: Tránh race condition khi mua hàng
- ✅ **Queue Processing**: Xử lý thanh toán tuần tự
- ✅ **Warehouse Lock**: Khóa sản phẩm khi đang xử lý
- ✅ **Auto Release**: Tự động hoàn tiền sau 1 phút

## 🏗️ Kiến trúc hệ thống

```
User Request → Payment Queue → Hold Money → Lock Warehouse → Send Products → Complete/Release
```

## 📊 Database Changes

### Bảng mới:
1. **wallet_hold**: Lưu tiền bị hold
2. **payment_queue**: Queue xử lý thanh toán

### Cập nhật:
- **warehouse**: Thêm fields `locked`, `locked_by`, `locked_at`

## 🔧 Cài đặt

### 1. Chạy Database Migration
```sql
-- Chạy file database_migration.sql
```

### 2. Cài đặt Redis
```bash
# Docker
docker run -d -p 6379:6379 redis:7-alpine

# Hoặc cài đặt local
# Windows: Download từ https://github.com/microsoftarchive/redis/releases
# Linux: sudo apt-get install redis-server
```

### 3. Cập nhật Dependencies
```xml
<!-- Đã thêm vào pom.xml -->
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-redis</artifactId>
</dependency>
```

## 🚀 API Endpoints

### 1. Thanh toán giỏ hàng (Queue System)
```http
POST /api/improved-payment/process-cart
Content-Type: application/json

{
  "cartItems": [
    {
      "productId": 1,
      "quantity": 1,
      "price": 100000,
      "name": "Test Product"
    }
  ],
  "totalAmount": 100000,
  "paymentMethod": "WALLET",
  "notes": "Test payment"
}
```

### 2. Kiểm tra trạng thái payment
```http
GET /api/improved-payment/status/{paymentId}
```

### 3. Test endpoints
```http
# Test cart payment
POST /api/test/cart-payment

# Xem holds của user
GET /api/test/holds/{userId}

# Xem tổng tiền bị hold
GET /api/test/holds/total/{userId}

# Force release hold
POST /api/test/holds/release/{holdId}

# Force complete hold
POST /api/test/holds/complete/{holdId}
```

## ⏰ Luồng xử lý (1 phút hold)

### Bước 1: User gửi thanh toán
```
POST /api/improved-payment/process-cart
→ Payment được thêm vào queue
→ Status: PENDING
```

### Bước 2: Queue Worker xử lý (mỗi 10 giây)
```
1. Hold money trong ví user (1 phút)
2. Lock warehouse items
3. Gửi sản phẩm cho user
4. Status: COMPLETED
```

### Bước 3: Auto Release (mỗi 30 giây)
```
Sau 1 phút → Tự động hoàn tiền về ví user
Status: CANCELLED
```

## 🔍 Monitoring & Logs

### Logs quan trọng:
```
INFO  - Holding money for user 1: 100000 VND, order: ORDER_1_1234567890
INFO  - Successfully locked warehouse item: 1 for product: 1
INFO  - Sending 1 products to user: 1
INFO  - Processing expired holds...
INFO  - Found 1 expired holds
INFO  - Hold 1 released successfully for user 1: 100000 VND
```

### Database queries để monitor:
```sql
-- Xem holds đang active
SELECT * FROM wallet_hold WHERE status = 'PENDING' AND expires_at > NOW();

-- Xem payments trong queue
SELECT * FROM payment_queue WHERE status = 'PENDING';

-- Xem warehouse items bị lock
SELECT * FROM warehouse WHERE locked = TRUE;
```

## 🧪 Test Scenarios

### 1. Test Hold Money (1 phút)
```bash
# 1. Tạo payment
curl -X POST http://localhost:8080/api/test/cart-payment

# 2. Kiểm tra holds
curl http://localhost:8080/api/test/holds/1

# 3. Đợi 1 phút, kiểm tra lại
curl http://localhost:8080/api/test/holds/1
```

### 2. Test Race Condition
```bash
# Gửi nhiều request cùng lúc
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/test/cart-payment &
done
```

### 3. Test Manual Release
```bash
# Force release hold
curl -X POST http://localhost:8080/api/test/holds/release/1
```

## ⚠️ Lưu ý quan trọng

1. **Redis phải chạy**: Hệ thống cần Redis để lock
2. **Database migration**: Chạy migration trước khi start app
3. **Thời gian hold**: Hiện tại là 1 phút (để test)
4. **Queue processing**: Chạy mỗi 10 giây
5. **Auto release**: Chạy mỗi 30 giây

## 🔧 Configuration

### application.yaml
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  task:
    scheduling:
      enabled: true
    execution:
      pool:
        core-size: 2
        max-size: 10
```

### Main Application
```java
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Study1Application {
    // ...
}
```

## 📈 Performance

- **Queue processing**: Mỗi 10 giây
- **Hold release**: Mỗi 30 giây  
- **Redis lock timeout**: 5 giây
- **Hold duration**: 1 phút (test)

## 🐛 Troubleshooting

### 1. Redis connection error
```
Check Redis is running: redis-cli ping
```

### 2. Database migration error
```
Check database connection and run migration.sql
```

### 3. Queue not processing
```
Check logs for @Scheduled methods
Check Redis connection
```

### 4. Holds not releasing
```
Check cron job logs
Check database for expired holds
```

## 🎯 Kết quả mong đợi

1. **Hold money**: Tiền được giữ 1 phút trước khi giải ngân
2. **Race condition**: Không có trùng hàng
3. **Queue processing**: Xử lý tuần tự, không bị conflict
4. **Auto release**: Tự động hoàn tiền sau 1 phút
5. **Monitoring**: Logs chi tiết cho debugging
