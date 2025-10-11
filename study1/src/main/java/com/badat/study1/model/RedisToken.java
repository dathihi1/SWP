package com.badat.study1.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash("invalidated_token")
public class RedisToken {
    @Id
    private String id;
    private String logoutTime;

    @TimeToLive
    private Long expiration;
}