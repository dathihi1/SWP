package com.badat.study1.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemException extends RuntimeException{
    private ErrorCode errorCode;

    public SystemException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
