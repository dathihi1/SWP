package com.badat.study1.dto.response;

import com.badat.study1.model.UserActivityLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for UserActivityLog with parsed browser and device information
 * 
 * Note: Mozilla/5.0 is a historical token used by all modern browsers for compatibility.
 * It does NOT indicate Mozilla browser. The actual browser is determined from other parts
 * of the User-Agent string. Example: "Mozilla/5.0 ... Chrome/120.0" → Chrome browser
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityLogResponse {
    
    // Original fields from UserActivityLog
    private Long id;
    private Long userId;
    private String action;
    private UserActivityLog.Category category;
    private String entityType;
    private Long entityId;
    private String details;
    private Boolean success;
    private String failureReason;
    private String ipAddress;
    private String userAgent;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Parsed fields from User-Agent string
    private String browserName;
    private String browserVersion;
    private String operatingSystem;
    private String deviceType;
    
    /**
     * Convert UserActivityLog entity to DTO with parsed User-Agent information
     */
    public static UserActivityLogResponse fromEntity(UserActivityLog entity) {
        UserActivityLogResponseBuilder builder = UserActivityLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .action(entity.getAction())
                .category(entity.getCategory())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .details(entity.getDetails())
                .success(entity.getSuccess())
                .failureReason(entity.getFailureReason())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());
        
        // Parse User-Agent string if available
        if (entity.getUserAgent() != null && !entity.getUserAgent().trim().isEmpty()) {
            try {
                eu.bitwalker.useragentutils.UserAgent userAgent = 
                    eu.bitwalker.useragentutils.UserAgent.parseUserAgentString(entity.getUserAgent());
                
                builder.browserName(userAgent.getBrowser().getName())
                       .browserVersion(userAgent.getBrowserVersion().getVersion())
                       .operatingSystem(userAgent.getOperatingSystem().getName())
                       .deviceType(userAgent.getOperatingSystem().getDeviceType().getName());
            } catch (Exception e) {
                // If parsing fails, set default values
                builder.browserName("Unknown")
                       .browserVersion("Unknown")
                       .operatingSystem("Unknown")
                       .deviceType("Unknown");
            }
        } else {
            // No User-Agent available
            builder.browserName("N/A")
                   .browserVersion("N/A")
                   .operatingSystem("N/A")
                   .deviceType("N/A");
        }
        
        return builder.build();
    }
}