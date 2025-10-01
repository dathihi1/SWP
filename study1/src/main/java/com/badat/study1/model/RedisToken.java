package com.badat.study1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("redisHash")
@Builder
public class RedisToken {
    @Id
    private String jwtID;
    private long expirationTime;
}
