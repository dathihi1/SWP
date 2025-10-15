package com.badat.study1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VNPayConfig {
    
    @Value("${vnpay.url}")
    private String vnpUrl;
    
    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;
    
    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;
    
    @Value("${vnpay.secret-key}")
    private String vnpSecretKey;
    
    @Value("${vnpay.version}")
    private String vnpVersion;
    
    @Value("${vnpay.command}")
    private String vnpCommand;
    
    @Value("${vnpay.order-type}")
    private String vnpOrderType;
    
    @Value("${vnpay.locale}")
    private String vnpLocale;
    
    @Value("${vnpay.currency-code}")
    private String vnpCurrencyCode;

    // Getters
    public String getVnpUrl() {
        return vnpUrl;
    }

    public String getVnpReturnUrl() {
        return vnpReturnUrl;
    }

    public String getVnpTmnCode() {
        return vnpTmnCode;
    }

    public String getVnpSecretKey() {
        return vnpSecretKey;
    }

    public String getVnpVersion() {
        return vnpVersion;
    }

    public String getVnpCommand() {
        return vnpCommand;
    }

    public String getVnpOrderType() {
        return vnpOrderType;
    }

    public String getVnpLocale() {
        return vnpLocale;
    }

    public String getVnpCurrencyCode() {
        return vnpCurrencyCode;
    }
}