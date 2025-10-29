package com.badat.study1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("redisHash")
@Builder
public class RedisToken {
    @Id
    private String jwtID;

    // Time to live in SECONDS. Redis will automatically delete this key after TTL expires.
    @TimeToLive
    private long expirationTime;
}
