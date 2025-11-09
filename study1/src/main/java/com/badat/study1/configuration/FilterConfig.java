package com.badat.study1.configuration;

import com.badat.study1.filter.CaptchaValidationFilter;
import com.badat.study1.service.CaptchaRateLimitService;
import com.badat.study1.service.CaptchaService;
import com.badat.study1.service.IpLockoutService;
import com.badat.study1.service.SecurityEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class FilterConfig {

    @Bean
    public CaptchaValidationFilter captchaValidationFilter(
            CaptchaService captchaService,
            CaptchaRateLimitService captchaRateLimitService,
            SecurityEventService securityEventService,
            IpLockoutService ipLockoutService,
            ObjectMapper objectMapper,
            @Value("${security.captcha-filter.enabled:true}") boolean enabled,
            @Value("${security.captcha-filter.paths:/api/auth/login,/api/auth/register,/api/auth/forgot-password}") String paths,
            @Value("${security.captcha-filter.methods:POST}") String methods
    ) {
        List<String> pathList = Arrays.stream(paths.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        List<String> methodList = Arrays.stream(methods.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return CaptchaValidationFilter.create(captchaService, captchaRateLimitService, securityEventService, 
                ipLockoutService, objectMapper, enabled, pathList, methodList);
    }
}




