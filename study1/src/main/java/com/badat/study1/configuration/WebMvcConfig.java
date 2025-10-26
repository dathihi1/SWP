package com.badat.study1.configuration;

import org.springframework.context.annotation.Configuration;

@Configuration
public class WebMvcConfig {
    // ApiLoggingInterceptor has been removed and replaced with ApiCallLogFilter
    // which is configured in the Spring Security filter chain
}
