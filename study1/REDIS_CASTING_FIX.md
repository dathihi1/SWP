# Redis Casting Errors Fix

## Vấn đề
Khi deserialize objects từ Redis, gặp 2 lỗi casting:

1. **LinkedHashMap casting error:**
```
class java.util.LinkedHashMap cannot be cast to class com.badat.study1.dto.OtpData
```

2. **Integer/Long casting error:**
```
class java.lang.Integer cannot be cast to class java.lang.Long
```

## Nguyên nhân

### 1. **LinkedHashMap Issue:**
- Redis trả về `LinkedHashMap` thay vì object gốc
- `GenericJackson2JsonRedisSerializer` deserialize thành Map
- Direct casting `(OtpData) rawData` thất bại

### 2. **Integer/Long Issue:**
- JSON deserialize numbers thành `Integer` thay vì `Long`
- Java type system không cho phép cast `Integer` → `Long`
- Thường xảy ra với ID fields

## Giải pháp đã áp dụng

### 1. **OtpService.java - Safe Casting**

**Trước:**
```java
OtpData otpData = (OtpData) redisTemplate.opsForValue().get(otpKey);
```

**Sau:**
```java
Object rawData = redisTemplate.opsForValue().get(otpKey);
OtpData otpData = convertToOtpData(rawData);

private OtpData convertToOtpData(Object rawData) {
    try {
        if (rawData instanceof OtpData) {
            return (OtpData) rawData;
        }
        
        if (rawData instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) rawData;
            return objectMapper.convertValue(map, OtpData.class);
        }
        
        return objectMapper.convertValue(rawData, OtpData.class);
    } catch (Exception e) {
        log.error("Failed to convert raw data to OtpData: {}", e.getMessage());
        return null;
    }
}
```

### 2. **UserService.java - Safe Casting**

**Trước:**
```java
UserCreateRequest request = (UserCreateRequest) redisTemplate.opsForValue().get(registrationKey);
```

**Sau:**
```java
Object rawData = redisTemplate.opsForValue().get(registrationKey);
UserCreateRequest request = convertToUserCreateRequest(rawData);

private UserCreateRequest convertToUserCreateRequest(Object rawData) {
    try {
        if (rawData instanceof UserCreateRequest) {
            return (UserCreateRequest) rawData;
        }
        
        if (rawData instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) rawData;
            return objectMapper.convertValue(map, UserCreateRequest.class);
        }
        
        return objectMapper.convertValue(rawData, UserCreateRequest.class);
    } catch (Exception e) {
        log.error("Failed to convert raw data to UserCreateRequest: {}", e.getMessage());
        return null;
    }
}
```

### 3. **Dependency Injection**

**OtpService.java:**
```java
private final ObjectMapper objectMapper;

@RequiredArgsConstructor // Lombok tự động inject
public class OtpService {
    // Constructor được tạo tự động
}
```

**UserService.java:**
```java
private final ObjectMapper objectMapper;

public UserService(..., ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
}
```

## Lợi ích

### 1. **Type Safety:**
- Không còn ClassCastException
- Graceful handling của các type khác nhau
- Fallback mechanisms

### 2. **Flexibility:**
- Hỗ trợ cả direct objects và Map
- Tương thích với các Redis serializer khác
- Dễ debug và maintain

### 3. **Error Handling:**
- Log errors thay vì crash
- Return null để caller có thể handle
- Clear error messages

## Test Cases

### 1. **OtpData Conversion:**
```java
// Test với OtpData object
OtpData original = OtpData.builder().otp("123456").build();
redisTemplate.opsForValue().set("test", original);
Object retrieved = redisTemplate.opsForValue().get("test");
OtpData converted = convertToOtpData(retrieved);
// ✅ Should work regardless of actual type
```

### 2. **UserCreateRequest Conversion:**
```java
// Test với UserCreateRequest object
UserCreateRequest original = new UserCreateRequest();
original.setEmail("test@example.com");
redisTemplate.opsForValue().set("test", original);
Object retrieved = redisTemplate.opsForValue().get("test");
UserCreateRequest converted = convertToUserCreateRequest(retrieved);
// ✅ Should work regardless of actual type
```

## Troubleshooting

### Nếu vẫn lỗi:

1. **Check ObjectMapper configuration:**
```java
@Bean
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
}
```

2. **Check Redis serializer:**
```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
    return template;
}
```

3. **Add debug logging:**
```java
log.info("Raw data type: {}", rawData.getClass().getName());
log.info("Raw data content: {}", rawData);
```

## Lưu ý

- **Performance**: ObjectMapper conversion có overhead nhỏ
- **Memory**: Không tạo thêm objects không cần thiết
- **Compatibility**: Hoạt động với mọi Redis serializer
- **Maintenance**: Dễ thêm support cho types mới
