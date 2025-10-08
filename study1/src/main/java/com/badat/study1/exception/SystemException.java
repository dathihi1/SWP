package com.badat.study1.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;

public class SystemException extends RuntimeException {
  public SystemException(String message) {
    super(message);
  }
}
