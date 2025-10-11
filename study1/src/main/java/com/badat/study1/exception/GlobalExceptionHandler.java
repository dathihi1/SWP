package com.badat.study1.exception;

import com.badat.study1.dto.request.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    // Bắt và xử lý các lỗi xác thực (sai username/password)
    @ExceptionHandler(value = BadCredentialsException.class)
    ResponseEntity<ApiResponse> handlingBadCredentialsException(BadCredentialsException exception){
        ApiResponse apiResponse = ApiResponse.builder()
                .code(401) // Unauthorized
                .message(exception.getMessage())
                .build();
        return ResponseEntity.status(401).body(apiResponse);
    }

    // Bắt và xử lý tất cả các Exception khác chưa được định nghĩa
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handlingRuntimeException(Exception exception){
        ApiResponse apiResponse = ApiResponse.builder()
                .code(500) // Internal Server Error
                .message("An error occurred: " + exception.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(apiResponse);
    }

    @ExceptionHandler(value = UsernameNotFoundException.class)
    ResponseEntity<ApiResponse> handlingUsernameNotFoundException(UsernameNotFoundException exception){
        ApiResponse apiResponse = ApiResponse.builder()
                .code(404) // Not Found
                .message("User not found") // Hoặc một thông báo lỗi phù hợp hơn
                .build();
        return ResponseEntity.status(404).body(apiResponse);
    }
}
