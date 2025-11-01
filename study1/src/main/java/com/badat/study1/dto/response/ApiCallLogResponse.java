package com.badat.study1.dto.response;

import com.badat.study1.model.ApiCallLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiCallLogResponse {
    private Long id;
    private Long userId;
    private String endpoint;
    private String method;
    private Integer statusCode;
    private String requestParams;
    private String requestBody;
    private String responseStatus;
    private Integer durationMs;
    private String ipAddress;
    private String userAgent;
    private String errorMessage;
    private LocalDateTime createdAt;
    
    public static ApiCallLogResponse fromApiCallLog(ApiCallLog log) {
        return ApiCallLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .endpoint(log.getEndpoint())
                .method(log.getMethod())
                .statusCode(log.getStatusCode())
                .requestParams(log.getRequestParams())
                .requestBody(log.getRequestBody())
                .responseStatus(log.getResponseStatus())
                .durationMs(log.getDurationMs())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}























