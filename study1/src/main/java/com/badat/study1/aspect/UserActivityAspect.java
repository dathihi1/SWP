package com.badat.study1.aspect;

import com.badat.study1.annotation.UserActivity;
import com.badat.study1.model.User;
import com.badat.study1.service.UserActivityLogService;
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

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class UserActivityAspect {

    private final UserActivityLogService userActivityLogService;

    @Around("@annotation(com.badat.study1.annotation.UserActivity)")
    public Object aroundUserActivity(ProceedingJoinPoint pjp) throws Throwable {
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
            try {
                UserActivity annotation = getAnnotation(pjp);
                if (annotation != null) {
                    User user = currentUser();
                    if (user != null) {
                        String endpoint = request != null ? request.getRequestURI() : "N/A";
                        String method = request != null ? request.getMethod() : "N/A";
                        boolean success = (error == null);
                        
                        // Extract entity information from method parameters if possible
                        String entityType = extractEntityType(pjp);
                        Long entityId = extractEntityId(pjp);
                        
                        // Create details based on action and result
                        String details = createDetails(annotation.action(), success, error, pjp);
                        
                        // Log the user activity
                        userActivityLogService.logUserActivity(
                            user, 
                            annotation.action(), 
                            annotation.category(), 
                            entityType, 
                            entityId, 
                            details, 
                            endpoint, 
                            method, 
                            ip, 
                            ua,
                            success,
                            error != null ? error.getMessage() : null
                        );
                    }
                }
            } catch (Exception e) {
                log.error("Error in UserActivityAspect: {}", e.getMessage());
            }
        }
    }

    private UserActivity getAnnotation(ProceedingJoinPoint pjp) {
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            return method.getAnnotation(UserActivity.class);
        } catch (Exception e) {
            log.warn("Error getting UserActivity annotation: {}", e.getMessage());
            return null;
        }
    }

    private String extractEntityType(ProceedingJoinPoint pjp) {
        // Try to determine entity type from method name or parameters
        String methodName = pjp.getSignature().getName().toLowerCase();
        if (methodName.contains("product")) return "Product";
        if (methodName.contains("order")) return "Order";
        if (methodName.contains("payment")) return "Payment";
        if (methodName.contains("review")) return "Review";
        if (methodName.contains("cart")) return "Cart";
        return "Account";
    }

    private Long extractEntityId(ProceedingJoinPoint pjp) {
        // Try to extract entity ID from method parameters
        Object[] args = pjp.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }

    private String createDetails(String action, boolean success, Exception error, ProceedingJoinPoint pjp) {
        StringBuilder details = new StringBuilder();
        details.append("Action: ").append(action);
        details.append(", Success: ").append(success);
        
        if (error != null) {
            details.append(", Error: ").append(error.getMessage());
        }
        
        // Add method signature for context
        details.append(", Method: ").append(pjp.getSignature().toShortString());
        
        return details.toString();
    }

    private User currentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
                return (User) auth.getPrincipal();
            }
        } catch (Exception e) {
            log.warn("Error getting current user: {}", e.getMessage());
        }
        return null;
    }
}
