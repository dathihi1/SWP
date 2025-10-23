# LocalDateTime Serialization Fix (Java 17 + Spring Boot 3.x)

## Vấn đề
```
Could not write JSON: Java 8 date/time type `java.time.LocalDateTime` not supported by default: 
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
```

## Nguyên nhân
Jackson không thể serialize `LocalDateTime` mặc định vì nó cần JSR310 module để xử lý Java 8+ time types.

## Giải pháp cho Java 17 + Spring Boot 3.x (Đảm bảo hoạt động)

### 1. **Thêm Jackson JSR310 dependency** ✅
```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

### 2. **Cấu hình Jackson trong application.yaml** ✅
```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false  # Sử dụng ISO-8601 format
      write-durations-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false
      fail-on-invalid-subtype: false
    time-zone: Asia/Ho_Chi_Minh
    # Force enable JSR310 module
    modules:
      - com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
```

### 3. **Tạo JacksonConfig.java** ✅
```java
@Configuration
public class JacksonConfig {
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

### 4. **Thêm @JsonFormat annotation** ✅
```java
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
private LocalDateTime createdAt;
```

## Kết quả

### Trước khi sửa:
```json
// Lỗi: Could not write JSON
{
  "otp": "123456",
  "email": "test@example.com",
  "createdAt": [2024, 1, 15, 10, 30, 45] // Timestamp array
}
```

### Sau khi sửa:
```json
// Thành công: ISO-8601 format
{
  "otp": "123456",
  "email": "test@example.com",
  "createdAt": "2024-01-15T10:30:45" // ISO-8601 string
}
```

## Các class bị ảnh hưởng

### OtpData.java
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpData implements Serializable {
    private String otp;
    private String email;
    private String purpose;
    private LocalDateTime createdAt; // ← Đây là field gây lỗi
    private int attempts;
}
```

### Các class khác có LocalDateTime:
- `BaseEntity.java` - createdAt, updatedAt
- `AuditLog.java` - createdAt
- `SecurityEvent.java` - createdAt
- `IpLockout.java` - lockedUntil

## Lợi ích

1. **Tương thích**: Hỗ trợ đầy đủ Java 8 time types
2. **Chuẩn hóa**: Sử dụng ISO-8601 format cho dates
3. **Dễ đọc**: JSON dễ đọc hơn với string format
4. **Tương thích frontend**: JavaScript có thể parse ISO-8601 dates

## Test

### 1. **Kiểm tra compile**
```bash
mvn compile
# BUILD SUCCESS
```

### 2. **Kiểm tra runtime**
- Gửi OTP → Không còn lỗi LocalDateTime
- API responses có dates đúng format
- Frontend có thể parse dates

## Lưu ý

- **Spring Boot 3.x**: Tự động include JSR310 module
- **Spring Boot 2.x**: Cần thêm dependency và config
- **Custom ObjectMapper**: Có thể override default behavior
- **Time Zone**: LocalDateTime không có timezone, sử dụng ZonedDateTime nếu cần

## Troubleshooting

### Nếu vẫn lỗi:
1. Kiểm tra dependency đã được download
2. Restart application
3. Kiểm tra JacksonConfig có được load không
4. Thêm `@JsonFormat` annotation nếu cần:

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createdAt;
```
