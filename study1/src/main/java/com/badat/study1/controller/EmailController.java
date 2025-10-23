package com.badat.study1.controller;

import com.badat.study1.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    /**
     * Test endpoint để gửi email với file HTML đính kèm
     */
    @PostMapping("/send-html-attachment")
    public ResponseEntity<?> sendEmailWithHtmlAttachment(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String htmlBody,
            @RequestParam String htmlFilePath,
            @RequestParam(defaultValue = "attachment") String attachmentName) {
        
        try {
            emailService.sendEmailWithHtmlAttachment(to, subject, htmlBody, htmlFilePath, attachmentName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email with HTML attachment sent successfully");
            response.put("to", to);
            response.put("subject", subject);
            response.put("attachment", attachmentName + ".html");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to send email with HTML attachment: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send email: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Test endpoint để gửi email với nội dung HTML từ string
     */
    @PostMapping("/send-html-content")
    public ResponseEntity<?> sendEmailWithHtmlContent(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String htmlBody,
            @RequestParam String htmlContent,
            @RequestParam(defaultValue = "attachment") String attachmentName) {
        
        try {
            emailService.sendEmailWithHtmlContent(to, subject, htmlBody, htmlContent, attachmentName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email with HTML content sent successfully");
            response.put("to", to);
            response.put("subject", subject);
            response.put("attachment", attachmentName + ".html");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to send email with HTML content: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send email: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Test endpoint để gửi email OTP với file HTML đính kèm
     */
    @PostMapping("/send-otp-with-html")
    public ResponseEntity<?> sendOtpWithHtmlAttachment(
            @RequestParam String to,
            @RequestParam String otp) {
        
        try {
            String subject = "Mã OTP xác thực - MMO Market";
            String htmlBody = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #0d6efd;">Xác thực OTP</h2>
                    <p>Xin chào,</p>
                    <p>Mã OTP của bạn là: <strong style="color: #dc3545; font-size: 24px;">%s</strong></p>
                    <p>Mã này có hiệu lực trong 10 phút.</p>
                    <p>Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                    <hr>
                    <p style="color: #6c757d; font-size: 12px;">Đây là email tự động, vui lòng không trả lời.</p>
                </body>
                </html>
                """, otp);
            
            // Tạo nội dung HTML cho file đính kèm
            String htmlAttachmentContent = String.format("""
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Mã OTP - MMO Market</title>
                    <style>
                        body { font-family: Arial, sans-serif; background: #f8f9fa; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .header { text-align: center; color: #0d6efd; margin-bottom: 30px; }
                        .otp-code { font-size: 48px; font-weight: bold; color: #dc3545; text-align: center; margin: 30px 0; padding: 20px; background: #f8f9fa; border-radius: 8px; letter-spacing: 5px; }
                        .warning { background: #fff3cd; border: 1px solid #ffeaa7; color: #856404; padding: 15px; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; color: #6c757d; font-size: 12px; margin-top: 30px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔐 Mã OTP Xác Thực</h1>
                            <p>MMO Market - Hệ thống bảo mật</p>
                        </div>
                        
                        <p>Xin chào,</p>
                        <p>Bạn đã yêu cầu mã OTP để xác thực tài khoản. Mã OTP của bạn là:</p>
                        
                        <div class="otp-code">%s</div>
                        
                        <div class="warning">
                            <strong>⚠️ Lưu ý quan trọng:</strong>
                            <ul>
                                <li>Mã OTP có hiệu lực trong <strong>10 phút</strong></li>
                                <li>Không chia sẻ mã này với bất kỳ ai</li>
                                <li>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email</li>
                            </ul>
                        </div>
                        
                        <p>Nếu bạn gặp vấn đề, vui lòng liên hệ hỗ trợ.</p>
                        
                        <div class="footer">
                            <p>© 2025 MMO Market. Đây là email tự động, vui lòng không trả lời.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, otp);
            
            emailService.sendEmailWithHtmlContent(to, subject, htmlBody, htmlAttachmentContent, "otp-verification");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP email with HTML attachment sent successfully");
            response.put("to", to);
            response.put("otp", otp);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to send OTP email with HTML attachment: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send OTP email: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}





