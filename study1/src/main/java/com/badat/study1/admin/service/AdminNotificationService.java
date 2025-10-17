package com.badat.study1.admin.service;

import com.badat.study1.admin.dto.NotificationRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/**
 * Email: dùng JavaMailSender (đã có starter-mail).
 * SMS: để TODO (tích hợp provider).
 * In-app: TODO push vào bảng notifications hoặc Redis pub/sub.
 */
@Service
public class AdminNotificationService {

    private final JavaMailSender mailSender;

    public AdminNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(NotificationRequest req) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(req.getTo());
        msg.setSubject(req.getSubject());
        msg.setText(req.getContent());
        mailSender.send(msg);
    }

    public void sendSms(NotificationRequest req) {
        // TODO: tích hợp SMS provider (Twilio, Subiz, ViettelSMS, v.v.)
    }

    public void sendInApp(NotificationRequest req) {
        // TODO: lưu vào NotificationRepository (nếu có) hoặc tạo bảng notifications
        // ví dụ: notificationRepository.save(...)
    }
}

