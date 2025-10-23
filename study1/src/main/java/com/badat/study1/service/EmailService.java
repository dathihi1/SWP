package com.badat.study1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    /**
     * Gửi email với file HTML đính kèm
     * @param to Email người nhận
     * @param subject Tiêu đề email
     * @param htmlBody Nội dung HTML của email
     * @param htmlFilePath Đường dẫn đến file HTML cần đính kèm
     * @param attachmentName Tên file đính kèm (không bao gồm extension)
     */
    public void sendEmailWithHtmlAttachment(String to, String subject, String htmlBody, String htmlFilePath, String attachmentName) {
        try {
            log.info("Attempting to send email with HTML attachment to: {}", to);
            log.info("Email subject: {}", subject);
            log.info("HTML file path: {}", htmlFilePath);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("dat2801zz@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML content
            
            // Đính kèm file HTML
            File htmlFile = new File(htmlFilePath);
            if (htmlFile.exists()) {
                FileSystemResource fileResource = new FileSystemResource(htmlFile);
                helper.addAttachment(attachmentName + ".html", fileResource);
                log.info("HTML file attached: {}", htmlFilePath);
            } else {
                log.warn("HTML file not found: {}", htmlFilePath);
            }
            
            log.info("Sending email with HTML attachment...");
            mailSender.send(message);
            log.info("Email with HTML attachment sent successfully to: {}", to);
            
        } catch (MessagingException ex) {
            log.error("Failed to send email with HTML attachment to: {}, error: {}", to, ex.getMessage(), ex);
            throw new RuntimeException("Failed to send email with HTML attachment: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gửi email HTML đơn giản (không đính kèm file)
     * @param to Email người nhận
     * @param subject Tiêu đề email
     * @param htmlBody Nội dung HTML của email
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            log.info("Attempting to send HTML email to: {}", to);
            log.info("Email subject: {}", subject);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("dat2801zz@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML content
            
            log.info("Sending HTML email...");
            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
            
        } catch (MessagingException ex) {
            log.error("Failed to send HTML email to: {}, error: {}", to, ex.getMessage(), ex);
            throw new RuntimeException("Failed to send HTML email: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gửi email với file HTML đính kèm (sử dụng nội dung HTML từ string)
     * @param to Email người nhận
     * @param subject Tiêu đề email
     * @param htmlBody Nội dung HTML của email
     * @param htmlContent Nội dung HTML để tạo file đính kèm
     * @param attachmentName Tên file đính kèm (không bao gồm extension)
     */
    public void sendEmailWithHtmlContent(String to, String subject, String htmlBody, String htmlContent, String attachmentName) {
        try {
            log.info("Attempting to send email with HTML content to: {}", to);
            log.info("Email subject: {}", subject);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("dat2801zz@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML content
            
            // Tạo file HTML tạm thời và đính kèm
            Path tempFile = Files.createTempFile(attachmentName, ".html");
            Files.write(tempFile, htmlContent.getBytes("UTF-8"));
            
            FileSystemResource fileResource = new FileSystemResource(tempFile.toFile());
            helper.addAttachment(attachmentName + ".html", fileResource);
            
            log.info("Sending email with HTML content...");
            mailSender.send(message);
            log.info("Email with HTML content sent successfully to: {}", to);
            
            // Xóa file tạm thời
            Files.deleteIfExists(tempFile);
            
        } catch (MessagingException | IOException ex) {
            log.error("Failed to send email with HTML content to: {}, error: {}", to, ex.getMessage(), ex);
            throw new RuntimeException("Failed to send email with HTML content: " + ex.getMessage(), ex);
        }
    }
}
