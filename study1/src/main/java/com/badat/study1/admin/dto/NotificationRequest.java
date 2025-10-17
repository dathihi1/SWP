package com.badat.study1.admin.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private String channel; // EMAIL | SMS | INAPP
    private String to;      // email/phone/userId
    private String subject; // cho email
    private String content;
}
