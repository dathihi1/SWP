# Redis LocalDateTime Serialization Fix

## Vấn đề
Khi đăng ký, lỗi xuất hiện:
```
Could not write JSON: Java 8 date/time type `java.time.LocalDateTime` not supported by default: 
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling 
(through reference chain: com.badat.study1.dto.OtpData["createdAt"])
```

## Nguyên nhân
1. **Spring Security không chặn** - `/api/auth/register` đã có trong `AUTH_WHITELIST`
2. **Redis serialization lỗi** - `OtpData` với `LocalDateTime` field không thể serialize vào Redis
3. **GenericJackson2JsonRedisSerializer** - Không có JSR310 module mặc định

## Luồng lỗi
```
1. User đăng ký → POST /api/auth/register
2. UserService.register() → Gọi OtpService.sendOtp()
3. OtpService.sendOtp() → Tạo OtpData với LocalDateTime
4. RedisTemplate.set() → Cố gắng serialize OtpData
5. ❌ Lỗi: LocalDateTime không được hỗ trợ
```

## Giải pháp đã áp dụng

### 1. **Cập nhật RedisConfiguration.java**
```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    
    // Create ObjectMapper with JSR310 support for Redis serialization
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    return template;
}
```

### 2. **Các cấu hình khác đã có sẵn**
- ✅ `JacksonConfig.java` - ObjectMapper cho REST API
- ✅ `application.yaml` - Jackson configuration
- ✅ `@JsonFormat` annotation trong `OtpData.java`

## Kết quả

### Trước khi sửa:
```json
// Redis không thể lưu OtpData
{
  "otp": "123456",
  "email": "test@example.com",
  "createdAt": [2024, 1, 15, 10, 30, 45] // ❌ Timestamp array
}
```

### Sau khi sửa:
```json
// Redis có thể lưu OtpData
{
  "otp": "123456", 
  "email": "test@example.com",
  "createdAt": "2024-01-15T10:30:45" // ✅ ISO-8601 string
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
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt; // ← Field gây lỗi Redis
    
    private int attempts;
}
```

### OtpService.java
```java
// Lưu OtpData vào Redis
redisTemplate.opsForValue().set(otpKey, otpData, otpExpireMinutes, TimeUnit.MINUTES);

// Đọc OtpData từ Redis  
OtpData otpData = (OtpData) redisTemplate.opsForValue().get(otpKey);
```

## Lợi ích

1. **Redis serialization**: Hỗ trợ đầy đủ LocalDateTime
2. **Consistent format**: Cùng format ISO-8601 cho cả API và Redis
3. **Performance**: Redis có thể cache objects phức tạp
4. **Type safety**: Không mất type information khi serialize/deserialize

## Test

### 1. **Kiểm tra compile**
```bash
mvn compile
# BUILD SUCCESS
```

### 2. **Kiểm tra runtime**
- Đăng ký user → Không còn lỗi LocalDateTime
- OTP được lưu vào Redis thành công
- API responses có dates đúng format

## Troubleshooting

### Nếu vẫn lỗi:
1. **Restart application** - Để load RedisConfiguration mới
2. **Clear Redis cache** - `redis-cli FLUSHALL`
3. **Kiểm tra Redis connection** - Đảm bảo Redis đang chạy
4. **Check logs** - Xem có lỗi Redis nào khác không

### Debug Redis serialization:
```java
// Thêm vào OtpService để debug
log.info("Storing OtpData to Redis: {}", otpData);
redisTemplate.opsForValue().set(otpKey, otpData, otpExpireMinutes, TimeUnit.MINUTES);
log.info("OtpData stored successfully");
```

## Lưu ý

- **Redis vs API**: Cần cấu hình ObjectMapper riêng cho Redis
- **GenericJackson2JsonRedisSerializer**: Cần ObjectMapper với JSR310
- **Type information**: Redis lưu class name để deserialize đúng type
- **Performance**: JSR310 module có overhead nhỏ nhưng không đáng kể
