package com.badat.study1.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private int code = 200;
    private String message;
    private T result;
}
