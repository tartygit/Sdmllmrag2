package com.cth.sdm.service;

import com.cth.sdm.model.AuditLog;
import com.cth.sdm.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    public void log(String username, String ip, String operation, String oldValue, String newValue, String fileName) {
        AuditLog log = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .username(username)
                .ip(ip)
                .operation(operation)
                .oldValue(oldValue)
                .newValue(newValue)
                .fileName(fileName)
                .build();
        auditLogRepository.save(log);
    }
}
