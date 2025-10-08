package com.badat.study1.model;

import jakarta.persistence.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@RedisHash("registrationOtp")
@Getter
@Setter
@NoArgsConstructor
public class RegistrationOtp {
    @Id
    private String email;        // dùng email làm id để dễ lookup
    private String otp;
    private String passwordHash; // lưu hash, không lưu password plain
    @TimeToLive
    private Long ttlSeconds;     // TTL tính bằng giây
}
