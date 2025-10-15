package com.badat.study1.util;

import com.badat.study1.config.VNPayConfig;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class VNPayUtil {
    
    private final VNPayConfig vnPayConfig;
    
    public VNPayUtil(VNPayConfig vnPayConfig) {
        this.vnPayConfig = vnPayConfig;
    }
    
    public String createPaymentUrl(long amount, String orderInfo, String orderId) {
        return createPaymentUrl(amount, orderInfo, orderId, null);
    }
    
    public String createPaymentUrl(long amount, String orderInfo, String orderId, HttpServletRequest request) {
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVnpVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getVnpCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getVnpTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount * 100));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getVnpCurrencyCode());
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", vnPayConfig.getVnpOrderType());
        vnpParams.put("vnp_Locale", vnPayConfig.getVnpLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getVnpReturnUrl());
        vnpParams.put("vnp_IpAddr", getClientIpAddress(request));
        
        // Không set vnp_BankCode để người dùng tự chọn phương thức thanh toán
        // vnpParams.put("vnp_BankCode", "VNPAYQR"); // Comment out để cho phép tự chọn
        
        // Debug logging
        System.out.println("=== VNPay Debug Info ===");
        System.out.println("TmnCode: " + vnPayConfig.getVnpTmnCode());
        System.out.println("Return URL: " + vnPayConfig.getVnpReturnUrl());
        System.out.println("Amount: " + amount + " VND");
        System.out.println("Order ID: " + orderId);
        System.out.println("Client IP: " + getClientIpAddress(request));
        System.out.println("Bank Code: Not set (User can choose payment method)");
        
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        
        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);
        
        // Sort parameters and build hashData string
        String hashDataString = sortObject(vnpParams);
        
        // Create secure hash
        String vnpSecureHash = hmacSHA512(vnPayConfig.getVnpSecretKey(), hashDataString);
        
        // Build query string with proper encoding
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                query.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                      .append("=")
                      .append(URLEncoder.encode(value, StandardCharsets.UTF_8))
                      .append("&");
            }
        }
        query.append("vnp_SecureHash=").append(vnpSecureHash);
        
        return vnPayConfig.getVnpUrl() + "?" + query.toString();
    }
    
    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC SHA512", e);
        }
    }
    
    /**
     * Get client IP address - fallback to localhost if unable to determine
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request != null) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        }
        
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            return localhost.getHostAddress();
        } catch (Exception e) {
            // Fallback to localhost IP
            return "127.0.0.1";
        }
    }
    
    /**
     * Sort object parameters and build hash data string
     * This method replicates the sortObject function from VNPay demo
     */
    private String sortObject(Map<String, String> obj) {
        List<String> sortedKeys = new ArrayList<>(obj.keySet());
        Collections.sort(sortedKeys);
        
        StringBuilder hashData = new StringBuilder();
        for (String key : sortedKeys) {
            String value = obj.get(key);
            if (value != null && !value.isEmpty()) {
                // Encode value and replace %20 with + as per VNPay requirements
                String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("%20", "+");
                hashData.append(key).append("=").append(encodedValue).append("&");
            }
        }
        
        // Remove the last '&' character
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }
        
        return hashData.toString();
    }
    
    public boolean verifyPayment(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
            return false;
        }
        
        // Remove vnp_SecureHash and vnp_SecureHashType from parameters
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.putAll(params);
        vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");
        
        // Use sortObject method for consistent parameter sorting
        String hashDataString = sortObject(vnpParams);
        
        String secureHash = hmacSHA512(vnPayConfig.getVnpSecretKey(), hashDataString);
        return secureHash.equals(vnpSecureHash);
    }
}