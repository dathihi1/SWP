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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {
    
    private final DefaultKaptcha kaptcha;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${security.captcha.expire-minutes:5}")
    private int captchaExpireMinutes;
    
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String CAPTCHA_COOKIE_NAME = "captcha_id";
    
    /**
     * Generate captcha image and return cookie header value
     * @return Pair of CaptchaResponse and cookie header string
     */
    public Map.Entry<CaptchaResponse, String> generateCaptchaWithCookie() {
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
            
            // Create HttpOnly cookie header
            ResponseCookie cookie = ResponseCookie.from(CAPTCHA_COOKIE_NAME, captchaId)
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(captchaExpireMinutes * 60)
                    .build();
            
            log.info("Captcha generated with ID: {} (will be stored in cookie)", captchaId);
            log.debug("Captcha text: {} for ID: {}", captchaText, captchaId);
            
            // Return response and cookie header
            CaptchaResponse captchaResponse = CaptchaResponse.builder()
                    .captchaImage(captchaImageBase64)
                    .expiresIn(captchaExpireMinutes * 60) // Convert to seconds
                    .build();
            
            return Map.entry(captchaResponse, cookie.toString());
                    
        } catch (Exception e) {
            log.error("Failed to generate captcha: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate captcha", e);
        }
    }
    
    /**
     * @deprecated Use generateCaptchaWithCookie() instead
     */
    @Deprecated
    public CaptchaResponse generateCaptcha(HttpServletResponse response) {
        Map.Entry<CaptchaResponse, String> result = generateCaptchaWithCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, result.getValue());
        return result.getKey();
    }
    
    /**
     * Validate captcha using ID from HttpOnly cookie
     */
    public boolean validateCaptcha(HttpServletRequest request, String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            log.warn("Captcha validation failed: userInput is null or empty");
            return false;
        }
        
        // Read captchaId from cookie
        String captchaId = getCaptchaIdFromCookie(request);
        if (captchaId == null) {
            log.warn("No captcha_id cookie found - listing all cookies:");
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    log.warn("  Cookie: {} = {}", cookie.getName(), cookie.getValue());
                }
            } else {
                log.warn("  No cookies at all in request");
            }
            return false;
        }
        
        log.info("Found captcha_id cookie: {}", captchaId);
        log.debug("User input (trimmed): '{}'", userInput.trim());
        
        try {
            String redisKey = CAPTCHA_PREFIX + captchaId;
            String storedCaptcha = (String) redisTemplate.opsForValue().get(redisKey);
            
            if (storedCaptcha == null) {
                log.warn("Captcha not found or expired for ID: {} (Redis key: {})", captchaId, redisKey);
                return false;
            }
            
            log.debug("Stored captcha text: '{}'", storedCaptcha);
            
            String trimmedInput = userInput.trim();
            boolean isValid = storedCaptcha.equals(trimmedInput);
            
            log.info("Captcha comparison - stored: '{}' vs input: '{}' -> {}", 
                    storedCaptcha, trimmedInput, isValid);
            
            // Always delete captcha after validation (one-time use)
            redisTemplate.delete(redisKey);
            
            if (isValid) {
                log.info("Captcha validated successfully for ID: {}", captchaId);
            } else {
                log.warn("Captcha validation failed for ID: {} - stored: '{}' vs input: '{}'", 
                        captchaId, storedCaptcha, trimmedInput);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Failed to validate captcha: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Clear captcha cookie after use
     */
    public void clearCaptchaCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(CAPTCHA_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    
    /**
     * Extract captchaId from HttpOnly cookie
     */
    private String getCaptchaIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (CAPTCHA_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Legacy method - kept for backward compatibility (deprecated)
     */
    @Deprecated
    public CaptchaResponse generateCaptcha() {
        throw new UnsupportedOperationException("Use generateCaptcha(HttpServletResponse) instead");
    }
    
    /**
     * Legacy method - kept for backward compatibility (deprecated)
     */
    @Deprecated
    public boolean validateCaptcha(String captchaId, String userInput) {
        throw new UnsupportedOperationException("Use validateCaptcha(HttpServletRequest, String) instead");
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
                String simpleKey = CAPTCHA_PREFIX + "simple:" + captchaId;
                String normalKey = CAPTCHA_PREFIX + captchaId;
                redisTemplate.opsForValue().set(simpleKey, captchaAnswer, captchaExpireMinutes, TimeUnit.MINUTES);
                // Also store under normal key to be compatible with cookie-based validation
                redisTemplate.opsForValue().set(normalKey, captchaAnswer, captchaExpireMinutes, TimeUnit.MINUTES);
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
        log.info("Validating simple captcha - ID: {}, User input: '{}'", captchaId, userInput);
        
        if (captchaId == null || userInput == null) {
            log.warn("Captcha validation failed - null input: captchaId={}, userInput={}", captchaId, userInput);
            return false;
        }
        
        try {
            String redisKey = CAPTCHA_PREFIX + "simple:" + captchaId;
            String storedAnswer = (String) redisTemplate.opsForValue().get(redisKey);
            
            log.info("Captcha validation - Redis key: {}, Stored answer: '{}'", redisKey, storedAnswer);
            
            if (storedAnswer == null) {
                log.warn("Simple captcha not found or expired for ID: {}", captchaId);
                return false;
            }
            
            boolean isValid = storedAnswer.equals(userInput.trim());
            log.info("Captcha validation result: {} (stored: '{}' vs input: '{}')", isValid, storedAnswer, userInput.trim());
            
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

