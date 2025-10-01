package com.badat.study1.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Builder
public class JwtInfo implements Serializable {
    private String jwtId;
    private Date issueTime;
    private Date expireTime;
}
