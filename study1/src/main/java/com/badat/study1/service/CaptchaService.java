package com.badat.study1.service;

import com.badat.study1.dto.response.CaptchaResponse;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {
    
    private final DefaultKaptcha kaptcha;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${security.captcha.expire-minutes:5}")
    private int captchaExpireMinutes;
    
    private static final String CAPTCHA_PREFIX = "captcha:";
    
    public CaptchaResponse generateCaptcha() {
        try {
            // Generate captcha text
            String captchaText = kaptcha.createText();
            String captchaId = UUID.randomUUID().toString();
            
            // Store captcha in Redis
            String redisKey = CAPTCHA_PREFIX + captchaId;
            redisTemplate.opsForValue().set(redisKey, captchaText, captchaExpireMinutes, TimeUnit.MINUTES);
            
            // Generate captcha image
            BufferedImage captchaImage = kaptcha.createImage(captchaText);
            String captchaImageBase64 = convertImageToBase64(captchaImage);
            
            log.info("Captcha generated with ID: {}", captchaId);
            
            return CaptchaResponse.builder()
                    .captchaId(captchaId)
                    .captchaImage(captchaImageBase64)
                    .expiresIn(captchaExpireMinutes * 60) // Convert to seconds
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to generate captcha: {}", e.getMessage());
            throw new RuntimeException("Failed to generate captcha", e);
        }
    }
    
    public boolean validateCaptcha(String captchaId, String userInput) {
        if (captchaId == null || userInput == null) {
            return false;
        }
        
        try {
            String redisKey = CAPTCHA_PREFIX + captchaId;
            String storedCaptcha = (String) redisTemplate.opsForValue().get(redisKey);
            
            if (storedCaptcha == null) {
                log.warn("Captcha not found or expired for ID: {}", captchaId);
                return false;
            }
            
            boolean isValid = storedCaptcha.equals(userInput.trim());
            
            if (isValid) {
                // Remove captcha after successful validation
                redisTemplate.delete(redisKey);
                log.info("Captcha validated successfully for ID: {}", captchaId);
            } else {
                log.warn("Captcha validation failed for ID: {}", captchaId);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Failed to validate captcha: {}", e.getMessage());
            return false;
        }
    }
    
    
    // Generate simple captcha with answer stored in Redis
    public Map<String, String> generateSimpleCaptcha() {
        try {
            // Generate random captcha text
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder captchaText = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                captchaText.append(chars.charAt((int) (Math.random() * chars.length())));
            }
            
            String captchaAnswer = captchaText.toString();
            String captchaId = UUID.randomUUID().toString();
            
            // Try to store captcha answer in Redis, but don't fail if Redis is down
            try {
                String redisKey = CAPTCHA_PREFIX + "simple:" + captchaId;
                redisTemplate.opsForValue().set(redisKey, captchaAnswer, captchaExpireMinutes, TimeUnit.MINUTES);
                log.info("Simple captcha generated with ID: {} and answer: {} (stored in Redis)", captchaId, captchaAnswer);
            } catch (Exception redisError) {
                log.warn("Failed to store captcha in Redis: {}, but continuing with fallback", redisError.getMessage());
                // Continue without Redis - captcha will still work but won't be validated server-side
            }
            
            Map<String, String> result = new HashMap<>();
            result.put("captchaId", captchaId);
            result.put("captchaText", captchaAnswer);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to generate simple captcha: {}", e.getMessage());
            throw new RuntimeException("Failed to generate simple captcha", e);
        }
    }
    
    // Validate simple captcha against stored answer
    public boolean validateSimpleCaptcha(String captchaId, String userInput) {
        if (captchaId == null || userInput == null) {
            return false;
        }
        
        try {
            String redisKey = CAPTCHA_PREFIX + "simple:" + captchaId;
            String storedAnswer = (String) redisTemplate.opsForValue().get(redisKey);
            
            if (storedAnswer == null) {
                log.warn("Simple captcha not found or expired for ID: {}", captchaId);
                return false;
            }
            
            boolean isValid = storedAnswer.equals(userInput.trim());
            
            if (isValid) {
                // Remove captcha after successful validation
                redisTemplate.delete(redisKey);
                log.info("Simple captcha validated successfully for ID: {}", captchaId);
            } else {
                log.warn("Simple captcha validation failed for ID: {} - expected: {}, got: {}", 
                        captchaId, storedAnswer, userInput);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Failed to validate simple captcha: {}", e.getMessage());
            return false;
        }
    }
    
    private String convertImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }
}

