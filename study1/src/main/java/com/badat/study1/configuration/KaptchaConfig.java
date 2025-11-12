package com.badat.study1.configuration;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {
    
    @Value("${security.captcha.length:6}")
    private int captchaLength;
    
    @Bean
    public DefaultKaptcha kaptcha() {
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        
        // Image properties - increased width to prevent text overflow (110x52px)
        properties.setProperty("kaptcha.image.width", "110");
        properties.setProperty("kaptcha.image.height", "52");
        
        // Text properties - easier to read
        properties.setProperty("kaptcha.textproducer.char.length", String.valueOf(captchaLength));
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789");
        properties.setProperty("kaptcha.textproducer.font.size", "28"); // Reduced for better fit and readability
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0"); // Pure black for better visibility
        properties.setProperty("kaptcha.textproducer.font.names", "Arial");
        properties.setProperty("kaptcha.textproducer.char.space", "3"); // Add spacing between characters
        
        // Noise properties - reduced noise for better readability
        properties.setProperty("kaptcha.noise.color", "240,240,240"); // Very light gray noise (almost invisible)
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.DefaultNoise");
        
        // Background properties - brighter for better contrast
        properties.setProperty("kaptcha.background.color.from", "255,255,255"); // White
        properties.setProperty("kaptcha.background.color.to", "250,250,250"); // Very light gray
        
        // Border properties - subtle border
        properties.setProperty("kaptcha.border", "yes");
        properties.setProperty("kaptcha.border.color", "221,221,221"); // Light gray border
        properties.setProperty("kaptcha.border.thickness", "1");
        
        // Word properties - adjust renderer to prevent overflow
        properties.setProperty("kaptcha.word.impl", "com.google.code.kaptcha.text.impl.DefaultWordRenderer");
        properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.ShadowGimpy"); // Less distortion
        
        // Session properties
        properties.setProperty("kaptcha.session.key", "kaptchaCode");
        properties.setProperty("kaptcha.session.date", "kaptchaCodeDate");
        
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        
        return kaptcha;
    }
}

