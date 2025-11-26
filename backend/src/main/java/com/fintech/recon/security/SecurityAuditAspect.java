package com.fintech.recon.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * Security audit logging aspect - logs all security-relevant events
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditAspect {

    /**
     * Log authentication attempts
     */
    @Before("execution(* com.fintech.recon.api.AuthController.login(..))")
    public void logAuthenticationAttempt(JoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        
        log.info("SECURITY_AUDIT | event=AUTH_ATTEMPT | ip={} | timestamp={} | user-agent={}", 
                ipAddress, 
                LocalDateTime.now(),
                request != null ? request.getHeader("User-Agent") : "unknown");
    }

    /**
     * Log successful authentication
     */
    @AfterReturning(pointcut = "execution(* com.fintech.recon.api.AuthController.login(..))", returning = "result")
    public void logAuthenticationSuccess(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        
        log.info("SECURITY_AUDIT | event=AUTH_SUCCESS | ip={} | timestamp={}", 
                ipAddress, 
                LocalDateTime.now());
    }

    /**
     * Log failed authentication
     */
    @AfterThrowing(pointcut = "execution(* com.fintech.recon.api.AuthController.login(..))", throwing = "ex")
    public void logAuthenticationFailure(JoinPoint joinPoint, Throwable ex) {
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        
        log.warn("SECURITY_AUDIT | event=AUTH_FAILURE | ip={} | reason={} | timestamp={}", 
                ipAddress, 
                ex.getMessage(),
                LocalDateTime.now());
    }

    /**
     * Log dispute approval/rejection actions
     */
    @Before("execution(* com.fintech.recon.api.DisputeController.approve*(..)) || " +
            "execution(* com.fintech.recon.api.DisputeController.reject*(..))")
    public void logDisputeAction(JoinPoint joinPoint) {
        String user = getCurrentUser();
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        Object[] args = joinPoint.getArgs();
        
        log.info("SECURITY_AUDIT | event=DISPUTE_ACTION | user={} | action={} | args={} | ip={} | timestamp={}", 
                user,
                joinPoint.getSignature().getName(),
                Arrays.toString(args),
                ipAddress,
                LocalDateTime.now());
    }

    /**
     * Log file upload actions
     */
    @Before("execution(* com.fintech.recon.api.IngestionController.upload*(..))")
    public void logFileUpload(JoinPoint joinPoint) {
        String user = getCurrentUser();
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        
        log.info("SECURITY_AUDIT | event=FILE_UPLOAD | user={} | ip={} | timestamp={}", 
                user,
                ipAddress,
                LocalDateTime.now());
    }

    /**
     * Log report export actions
     */
    @Before("execution(* com.fintech.recon.api.ReportController.export*(..))")
    public void logReportExport(JoinPoint joinPoint) {
        String user = getCurrentUser();
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        
        log.info("SECURITY_AUDIT | event=REPORT_EXPORT | user={} | method={} | ip={} | timestamp={}", 
                user,
                joinPoint.getSignature().getName(),
                ipAddress,
                LocalDateTime.now());
    }

    /**
     * Log admin actions
     */
    @Before("execution(* com.fintech.recon.api..*Controller.*(..)) && @annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public void logAdminAction(JoinPoint joinPoint) {
        String user = getCurrentUser();
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        
        log.info("SECURITY_AUDIT | event=ADMIN_ACTION | user={} | method={} | class={} | ip={} | timestamp={}", 
                user,
                joinPoint.getSignature().getName(),
                joinPoint.getSignature().getDeclaringTypeName(),
                ipAddress,
                LocalDateTime.now());
    }

    /**
     * Log security exceptions
     */
    @AfterThrowing(pointcut = "execution(* com.fintech.recon.api..*(..))", throwing = "ex")
    public void logSecurityException(JoinPoint joinPoint, Throwable ex) {
        if (ex instanceof SecurityException || 
            ex.getClass().getName().contains("Security") ||
            ex.getClass().getName().contains("Access")) {
            
            String user = getCurrentUser();
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = getClientIpAddress(request);
            
            log.error("SECURITY_AUDIT | event=SECURITY_EXCEPTION | user={} | exception={} | message={} | ip={} | timestamp={}", 
                    user,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ipAddress,
                    LocalDateTime.now());
        }
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        
        return request.getRemoteAddr();
    }
}
