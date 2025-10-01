package com.badat.study1.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UserCreateRequest {
    String email;
    String password;
}
