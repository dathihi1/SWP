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
        
        // Image properties
        properties.setProperty("kaptcha.image.width", "200");
        properties.setProperty("kaptcha.image.height", "50");
        
        // Text properties
        properties.setProperty("kaptcha.textproducer.char.length", String.valueOf(captchaLength));
        properties.setProperty("kaptcha.textproducer.char.string", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        properties.setProperty("kaptcha.textproducer.font.size", "40");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.textproducer.font.names", "Arial,Courier");
        
        // Noise properties
        properties.setProperty("kaptcha.noise.color", "white");
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.DefaultNoise");
        
        // Background properties
        properties.setProperty("kaptcha.background.color.from", "lightGray");
        properties.setProperty("kaptcha.background.color.to", "white");
        
        // Border properties
        properties.setProperty("kaptcha.border", "yes");
        properties.setProperty("kaptcha.border.color", "105,179,90");
        properties.setProperty("kaptcha.border.thickness", "1");
        
        // Word properties
        properties.setProperty("kaptcha.word.impl", "com.google.code.kaptcha.text.impl.DefaultWordRenderer");
        
        // Session properties
        properties.setProperty("kaptcha.session.key", "kaptchaCode");
        properties.setProperty("kaptcha.session.date", "kaptchaCodeDate");
        
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        
        return kaptcha;
    }
}

