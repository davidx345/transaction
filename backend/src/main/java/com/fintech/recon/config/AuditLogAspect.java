package com.fintech.recon.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    // Intercept any method in DisputeService that starts with 'approve' or 'reject'
    @AfterReturning(pointcut = "execution(* com.fintech.recon.service.DisputeService.approve*(..)) || execution(* com.fintech.recon.service.DisputeService.reject*(..))", returning = "result")
    public void logDisputeAction(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // In a real app, we'd extract the User from SecurityContextHolder
        String actor = "system_user"; 
        
        log.info("AUDIT LOG: Action={} Actor={} Args={}", methodName.toUpperCase(), actor, args);
        
        // TODO: Persist to audit_log table asynchronously
    }
    
    @AfterReturning(pointcut = "execution(* com.fintech.recon.service.RefundService.initiateRefund(..))", returning = "result")
    public void logRefundAction(JoinPoint joinPoint, Object result) {
        Object[] args = joinPoint.getArgs();
        log.info("AUDIT LOG: Action=INITIATE_REFUND Actor=system_user Args={}", args);
    }
}
