package com.badat.study1.dto.response;

import com.badat.study1.model.UserActivityLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserActivityLogResponse {
    private Long id;
    private Long userId;
    private String action;
    private String category;
    private String entityType;
    private Long entityId;
    private String details;
    private String ipAddress;
    private String userAgent;
    private String metadata;
    private LocalDateTime createdAt;
    
    public static UserActivityLogResponse fromUserActivityLog(UserActivityLog log) {
        return UserActivityLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .action(log.getAction())
                .category(log.getCategory() != null ? log.getCategory().name() : null)
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .metadata(log.getMetadata())
                .createdAt(log.getCreatedAt())
                .build();
    }
}





