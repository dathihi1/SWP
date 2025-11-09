package com.badat.study1.filter;

import com.badat.study1.dto.response.CaptchaResponse;
import com.badat.study1.model.SecurityEvent;
import com.badat.study1.service.CaptchaRateLimitService;
import com.badat.study1.service.CaptchaService;
import com.badat.study1.service.IpLockoutService;
import com.badat.study1.service.SecurityEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class CaptchaValidationFilter extends OncePerRequestFilter {

    private final CaptchaService captchaService;
    private final CaptchaRateLimitService captchaRateLimitService;
    private final SecurityEventService securityEventService;
    private final IpLockoutService ipLockoutService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Set<String> protectedPaths;
    private final Set<String> protectedMethods;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        if (!enabled) return true;
        String method = request.getMethod();
        if (method == null) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        if (!protectedMethods.contains(method)) return true;
        boolean match = false;
        for (String path : protectedPaths) {
            if (path == null) {
                continue;
            }
            if (pathMatcher.match(path, uri) || uri.startsWith(path)) {
                match = true;
                break;
            }
        }
        return !match;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        byte[] cachedBody = new byte[0];
        boolean isJsonRequest = isJsonRequest(request);

        if (isJsonRequest) {
            try {
                cachedBody = request.getInputStream().readAllBytes();
                log.debug("Request body read for caching - path: {}, size: {} bytes", request.getRequestURI(), cachedBody.length);
            } catch (IOException e) {
                log.error("Failed to read request body: {}", e.getMessage());
                cachedBody = new byte[0];
            }
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, cachedBody);

        try {
            String ipAddress = getClientIpAddress(cachedRequest);

            if (captchaRateLimitService.isCaptchaRateLimited(ipAddress)) {
                log.warn("Captcha rate limited - IP: {}", ipAddress);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                String json = objectMapper.createObjectNode()
                        .put("error", "Quá nhiều lần nhập sai captcha. Vui lòng thử lại sau 15 phút")
                        .put("captchaRateLimited", true)
                        .toString();
                response.getWriter().write(json);
                return;
            }

            String captchaCode = extractCaptchaCode(cachedRequest, cachedBody);
            String username = extractUsername(cachedRequest, cachedBody);

            log.debug("Extracted captcha code: {}, username: {} for path: {}",
                    captchaCode != null ? "present" : "missing",
                    username != null ? username : "missing",
                    request.getRequestURI());

            if (captchaCode == null || captchaCode.trim().isEmpty()) {
                log.warn("Captcha missing for {} {} from IP: {}", request.getMethod(), request.getRequestURI(), ipAddress);
                ipLockoutService.recordFailedAttempt(ipAddress, username != null ? username : "unknown");
                securityEventService.logCaptchaRequired(ipAddress, username != null ? username : "unknown");
                writeCaptchaError(response, true, ipAddress);
                return;
            }

            boolean valid = false;
            try {
                valid = captchaService.validateCaptcha(cachedRequest, captchaCode);
            } catch (Exception e) {
                log.error("Captcha validation exception: {}", e.getMessage(), e);
            }

            if (!valid) {
                log.warn("Captcha invalid for {} {} from IP: {}", request.getMethod(), request.getRequestURI(), ipAddress);
                ipLockoutService.recordFailedAttempt(ipAddress, username != null ? username : "unknown");
                captchaRateLimitService.recordFailedCaptchaAttempt(ipAddress);
                securityEventService.logSecurityEvent(SecurityEvent.EventType.CAPTCHA_FAILED, ipAddress,
                        "Captcha validation failed for " + request.getRequestURI());
                writeCaptchaError(response, false, ipAddress);
                return;
            }

            ResponseCookie clearCookie = ResponseCookie.from("captcha_id", "")
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

            log.debug("Before passing to controller - cached body size: {} bytes for path: {}",
                    cachedBody.length, request.getRequestURI());

            filterChain.doFilter(cachedRequest, response);
        } catch (Exception e) {
            log.error("Error in CaptchaValidationFilter: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Internal server error\"}");
        }
    }

    private String extractCaptchaCode(HttpServletRequest request, byte[] bodyBytes) throws IOException {
        if (!isJsonRequest(request)) {
            return request.getParameter("captchaCode") != null ? request.getParameter("captchaCode") : request.getParameter("captcha");
        }
        if (bodyBytes == null || bodyBytes.length == 0) return null;
        try {
            JsonNode node = objectMapper.readTree(bodyBytes);
            if (node.hasNonNull("captchaCode")) return node.get("captchaCode").asText();
            if (node.hasNonNull("captcha")) return node.get("captcha").asText();
        } catch (Exception ignore) {}
        return null;
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    private void writeCaptchaError(HttpServletResponse response, boolean missing, String ipAddress) throws IOException {
        // Generate a new captcha and set cookie header
        Map.Entry<CaptchaResponse, String> newCaptchaResult = captchaService.generateCaptchaWithCookie();

        ResponseCookie clearCookie = ResponseCookie.from("captcha_id", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, newCaptchaResult.getValue());

        String json = objectMapper.createObjectNode()
                .put("error", missing ? "Vui lòng nhập mã xác thực" : "Mã xác thực không đúng, vui lòng nhập lại")
                .put("captchaRequired", true)
                .set("captcha", objectMapper.valueToTree(newCaptchaResult.getKey()))
                .toString();
        response.getWriter().write(json);
    }
    
    /**
     * Extract username from request body (for login endpoint)
     * Uses cached content from ContentCachingRequestWrapper
     */
    private String extractUsername(HttpServletRequest request, byte[] bodyBytes) {
        try {
            if (!isJsonRequest(request)) {
                return request.getParameter("username");
            }
            if (bodyBytes == null || bodyBytes.length == 0) return null;
            String body = new String(bodyBytes, StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("username")) return node.get("username").asText();
            if (node.hasNonNull("email")) return node.get("email").asText();
        } catch (Exception ignore) {}
        return null;
    }
    
    /**
     * Get client IP address from request (supports proxy/load balancer)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    public static CaptchaValidationFilter create(CaptchaService captchaService,
                                                 CaptchaRateLimitService captchaRateLimitService,
                                                 SecurityEventService securityEventService,
                                                 IpLockoutService ipLockoutService,
                                                 ObjectMapper objectMapper,
                                                 boolean enabled,
                                                 List<String> paths,
                                                 List<String> methods) {
        return new CaptchaValidationFilter(
                captchaService,
                captchaRateLimitService,
                securityEventService,
                ipLockoutService,
                objectMapper,
                enabled,
                paths == null ? Set.of() : paths.stream().collect(Collectors.toSet()),
                methods == null ? Set.of("POST") : methods.stream().collect(Collectors.toSet())
        );
    }

    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody != null ? cachedBody : new byte[0];
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Not supporting asynchronous reading
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

    }
}




