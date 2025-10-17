package com.badat.study1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body) {
        try {
            log.info("Attempting to send email to: {}", to);
            log.info("Email subject: {}", subject);
            log.info("Email body: {}", body);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("dat2801zz@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            log.info("Sending email...");
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception ex) {
            log.error("Failed to send email to: {}, error: {}", to, ex.getMessage(), ex);
            // Surface cause to caller for logging/handling and easier debugging
            throw new RuntimeException("Failed to send email: " + ex.getMessage(), ex);
        }
    }
}
