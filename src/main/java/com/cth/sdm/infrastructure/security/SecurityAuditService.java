package com.cth.sdm.infrastructure.security;

import com.cth.sdm.domain.model.AuditLog;
import com.cth.sdm.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logLoginSuccess(String username, String ipAddress) {
        saveLog(username, "LOGIN_SUCCESS", "User", null, "User successfully authenticated.", ipAddress);
    }

    @Transactional
    public void logLoginFailure(String username, String details, String ipAddress) {
        saveLog(username != null ? username : "anonymous", "LOGIN_FAILURE", "User", null, details, ipAddress);
    }

    @Transactional
    public void logAccountLockout(String username, String details, String ipAddress) {
        saveLog(username, "ACCOUNT_LOCKOUT", "User", null, details, ipAddress);
    }

    @Transactional
    public void logAdminUnlock(String username, String adminUsername, String ipAddress) {
        saveLog(adminUsername, "ADMIN_UNLOCK", "User", null, "Administrator unlocked account for: " + username, ipAddress);
    }

    @Transactional
    public void logPasswordReset(String username, String ipAddress) {
        saveLog(username, "PASSWORD_RESET", "User", null, "User successfully reset their password.", ipAddress);
    }

    @Transactional
    public void logUnauthorizedAccess(String username, String resource, String action, String ipAddress) {
        saveLog(username != null ? username : "anonymous", "UNAUTHORIZED_ACCESS", resource, null,
                "Unauthorized attempt to perform action: " + action + " on resource: " + resource, ipAddress);
    }

    private void saveLog(String username, String action, String entityName, Long entityId, String details, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .username(username)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress != null ? ipAddress : "0.0.0.0")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            auditLogRepository.save(auditLog);
            log.info("[AUDIT] Action: {}, User: {}, Details: {}", action, username, details);
        } catch (Exception e) {
            log.error("Failed to write audit log to database: {}", e.getMessage());
        }
    }
}
