package com.badat.study1.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class IntrospectResponse {
    private boolean valid;
}