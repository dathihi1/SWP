package com.badat.study1.config;

import com.badat.study1.interceptor.ShopLockInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final ShopLockInterceptor shopLockInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(shopLockInterceptor)
                .addPathPatterns("/seller/**")
                .excludePathPatterns("/seller/register"); // Cho phép truy cập trang đăng ký seller
    }
}
