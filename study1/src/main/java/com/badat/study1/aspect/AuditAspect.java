package com.badat.study1.aspect;

import com.badat.study1.annotation.Auditable;
import com.badat.study1.model.User;
import com.badat.study1.service.AuditLogService;
import com.badat.study1.util.RequestMetadataUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Around("within(@org.springframework.web.bind.annotation.RestController *) && execution(* com.badat.study1.controller..*(..))")
    public Object aroundApi(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.currentTimeMillis();
        Exception error = null;
        Object result = null;

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
        String ip = RequestMetadataUtil.extractClientIp(request);
        String ua = RequestMetadataUtil.extractUserAgent(request);

        try {
            result = pjp.proceed();
            return result;
        } catch (Exception e) {
            error = e;
            throw e;
        } finally {
            String action = resolveAction(pjp).orElse("API_CALL");
            boolean success = (error == null);
            String method = request != null ? request.getMethod() : "N/A";
            String uri = request != null ? request.getRequestURI() : pjp.getSignature().toShortString();
            long duration = System.currentTimeMillis() - startTime;
            String status = success ? statusOf(result) : "EXCEPTION";
            String details = String.format("%s %s status=%s durationMs=%d", method, uri, status, duration);

            // Determine category based on action and context
            com.badat.study1.model.AuditLog.Category category = determineCategory(action, request);
            auditLogService.logAction(currentUser(), action, details, ip, success, error != null ? error.getMessage() : null, ua, category);
        }
    }

    private Optional<String> resolveAction(ProceedingJoinPoint pjp) {
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            Auditable ann = method.getAnnotation(Auditable.class);
            if (ann != null && !ann.action().isBlank()) return Optional.of(ann.action());
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private String statusOf(Object result) {
        if (result == null) return "200"; // best-effort default
        String s = result.toString();
        if (s.contains("status=400") || s.contains("BAD_REQUEST")) return "400";
        if (s.contains("status=401") || s.contains("UNAUTHORIZED")) return "401";
        if (s.contains("status=403") || s.contains("FORBIDDEN")) return "403";
        if (s.contains("status=404") || s.contains("NOT_FOUND")) return "404";
        if (s.contains("status=500") || s.contains("INTERNAL_SERVER_ERROR")) return "500";
        return "200";
    }

    private com.badat.study1.model.AuditLog.Category determineCategory(String action, HttpServletRequest request) {
        // Security-related actions
        if (action.contains("LOGIN") || action.contains("AUTH") || action.contains("SECURITY") || 
            action.contains("CAPTCHA") || action.contains("LOCKED") || action.contains("FAILED")) {
            return com.badat.study1.model.AuditLog.Category.SECURITY_EVENT;
        }
        
        // User-facing actions (chỉ những action user thực hiện trực tiếp)
        if (action.contains("PROFILE_UPDATE") || action.contains("LOGOUT") || action.contains("REGISTER") ||
            action.contains("PASSWORD_CHANGE") || action.contains("OTP_VERIFY")) {
            return com.badat.study1.model.AuditLog.Category.USER_ACTION;
        }
        
        // API calls (tất cả các API endpoint khác)
        if (request != null && request.getRequestURI().startsWith("/api/")) {
            return com.badat.study1.model.AuditLog.Category.API_CALL;
        }
        
        // System events (scheduled tasks, maintenance, etc.)
        if (action.contains("SYSTEM") || action.contains("SCHEDULED") || action.contains("MAINTENANCE")) {
            return com.badat.study1.model.AuditLog.Category.SYSTEM_EVENT;
        }
        
        // Default to API_CALL for controller methods
        return com.badat.study1.model.AuditLog.Category.API_CALL;
    }

    private User currentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
                return (User) auth.getPrincipal();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}






