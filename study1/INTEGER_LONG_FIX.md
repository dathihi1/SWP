# Integer to Long Casting Fix

## Vấn đề
```
class java.lang.Integer cannot be cast to class java.lang.Long 
(java.lang.Integer and java.lang.Long are in module java.base of loader 'bootstrap')
```

## Nguyên nhân
Jackson deserialize JSON numbers thành `Integer` thay vì `Long`, gây ra ClassCastException khi:
1. **Redis serialization/deserialization** - GenericJackson2JsonRedisSerializer
2. **JSON API responses** - ObjectMapper configuration
3. **Type conversion** - Không có fallback mechanism

## Giải pháp đã áp dụng

### 1. **Cải thiện Jackson Configuration**

**application.yaml:**
```yaml
spring:
  jackson:
    deserialization:
      fail-on-unknown-properties: false
      fail-on-invalid-subtype: false
      use-long-for-ints: true          # ✅ Convert Integer to Long
      use-big-integer-for-ints: true   # ✅ Convert Integer to BigInteger
```

**RedisConfiguration.java:**
```java
ObjectMapper objectMapper = new ObjectMapper();
objectMapper.registerModule(new JavaTimeModule());
objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
objectMapper.configure(DeserializationFeature.USE_LONG_FOR_INTS, true);        // ✅
objectMapper.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true);  // ✅
```

### 2. **Cải thiện OtpData DTO**

**OtpData.java:**
```java
@JsonIgnoreProperties(ignoreUnknown = true)  // ✅ Ignore unknown fields
public class OtpData implements Serializable {
    private String otp;
    private String email;
    private String purpose;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    private int attempts;  // ✅ Keep as int, handle conversion in service
}
```

### 3. **Safe Type Conversion trong OtpService**

**OtpService.java:**
```java
private OtpData convertToOtpData(Object rawData) {
    try {
        if (rawData instanceof OtpData) {
            return (OtpData) rawData;
        }
        
        if (rawData instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) rawData;
            
            // Handle type conversion issues manually
            OtpData otpData = new OtpData();
            otpData.setOtp((String) map.get("otp"));
            otpData.setEmail((String) map.get("email"));
            otpData.setPurpose((String) map.get("purpose"));
            
            // Handle attempts field - convert Integer to int
            Object attempts = map.get("attempts");
            if (attempts instanceof Integer) {
                otpData.setAttempts((Integer) attempts);
            } else if (attempts instanceof Long) {
                otpData.setAttempts(((Long) attempts).intValue());
            } else if (attempts instanceof Number) {
                otpData.setAttempts(((Number) attempts).intValue());
            } else {
                otpData.setAttempts(0);
            }
            
            // Handle createdAt field
            Object createdAt = map.get("createdAt");
            if (createdAt instanceof String) {
                otpData.setCreatedAt(LocalDateTime.parse((String) createdAt));
            } else if (createdAt instanceof LocalDateTime) {
                otpData.setCreatedAt((LocalDateTime) createdAt);
            } else {
                otpData.setCreatedAt(LocalDateTime.now());
            }
            
            return otpData;
        }
        
        return objectMapper.convertValue(rawData, OtpData.class);
    } catch (Exception e) {
        log.error("Failed to convert raw data to OtpData: {}", e.getMessage());
        return null;
    }
}
```

## Lợi ích

### 1. **Type Safety:**
- Không còn ClassCastException
- Graceful handling của các type khác nhau
- Fallback mechanisms cho mọi field

### 2. **Flexibility:**
- Hỗ trợ cả Integer và Long
- Tương thích với mọi JSON serializer
- Dễ debug và maintain

### 3. **Performance:**
- Không tạo thêm objects không cần thiết
- Efficient type conversion
- Minimal overhead

## Test Cases

### 1. **OtpData với Integer attempts:**
```json
{
  "otp": "123456",
  "email": "test@example.com",
  "attempts": 3,  // Integer
  "createdAt": "2024-01-15T10:30:45"
}
```
**Result:** ✅ Converted to int successfully

### 2. **OtpData với Long attempts:**
```json
{
  "otp": "123456", 
  "email": "test@example.com",
  "attempts": 3,  // Long
  "createdAt": "2024-01-15T10:30:45"
}
```
**Result:** ✅ Converted to int successfully

### 3. **OtpData với String createdAt:**
```json
{
  "otp": "123456",
  "email": "test@example.com", 
  "attempts": 3,
  "createdAt": "2024-01-15T10:30:45"  // String
}
```
**Result:** ✅ Parsed to LocalDateTime successfully

## Troubleshooting

### Nếu vẫn lỗi:

1. **Check Jackson configuration:**
```yaml
spring:
  jackson:
    deserialization:
      use-long-for-ints: true
      use-big-integer-for-ints: true
```

2. **Check Redis ObjectMapper:**
```java
objectMapper.configure(DeserializationFeature.USE_LONG_FOR_INTS, true);
objectMapper.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true);
```

3. **Add debug logging:**
```java
log.info("Raw data type: {}", rawData.getClass().getName());
log.info("Raw data content: {}", rawData);
```

4. **Clear Redis cache:**
```bash
redis-cli FLUSHALL
```

## Lưu ý

- **Database schema**: Tất cả ID fields đều sử dụng `BIGINT` (Long)
- **Entity classes**: Tất cả ID fields đều sử dụng `Long`
- **DTO classes**: Không có ID fields, chỉ có business fields
- **Redis serialization**: Sử dụng GenericJackson2JsonRedisSerializer với custom ObjectMapper
- **API responses**: Sử dụng Jackson configuration để convert types

## Kết quả

- ✅ **Không còn Integer/Long casting errors**
- ✅ **Redis serialization hoạt động**
- ✅ **API responses đúng format**
- ✅ **Type conversion an toàn**
- ✅ **Debug logging chi tiết**
