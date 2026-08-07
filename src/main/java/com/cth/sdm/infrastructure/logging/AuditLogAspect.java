package com.cth.sdm.infrastructure.logging;

import com.cth.sdm.domain.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        try {
            String action = auditLog.action();

            // 1. Get current username
            String username = "system";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                username = authentication.getName();
            }

            // 2. Get client IP address
            String ipAddress = "0.0.0.0";
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
            }

            // 3. Inspect method arguments for details like file name, parameters, etc.
            String details = "";
            String fileName = null;
            Object[] args = joinPoint.getArgs();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();

            StringBuilder argBuilder = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    String paramName = (parameterNames != null && parameterNames.length > i) ? parameterNames[i] : "arg" + i;

                    if (args[i] instanceof MultipartFile) {
                        MultipartFile file = (MultipartFile) args[i];
                        fileName = file.getOriginalFilename();
                        argBuilder.append(paramName).append("=").append(fileName).append("; ");
                    } else {
                        argBuilder.append(paramName).append("=").append(args[i].toString()).append("; ");
                    }
                }
            }

            details = "Params: " + argBuilder.toString();
            if (result != null) {
                details += "Result: " + result.toString();
            }

            // 4. Create and save AuditLog entity
            com.cth.sdm.domain.model.AuditLog auditLogEntity = com.cth.sdm.domain.model.AuditLog.builder()
                    .username(username)
                    .action(action)
                    .details(details)
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLogEntity);
            log.info("[AOP AUDIT] Action: {}, User: {}, IP: {}, Details: {}, File: {}",
                    action, username, ipAddress, details, fileName);

        } catch (Exception e) {
            log.error("Failed to execute AuditLogAspect: {}", e.getMessage());
        }
    }
}
