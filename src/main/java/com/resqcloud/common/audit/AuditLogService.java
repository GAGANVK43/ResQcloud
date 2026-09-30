package com.resqcloud.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, String username, AuditAction action, String entityName, 
                       String entityId, String previousState, String newState, String ipAddress, String metadata) {
        try {
            AuditLog auditLog = new AuditLog(userId, username, action, entityName, entityId, previousState, newState, ipAddress, metadata);
            auditLogRepository.save(auditLog);
            log.info("AUDIT LOG: action={}, entity={}/{}, user={}", action, entityName, entityId, username);
        } catch (Exception e) {
            log.error("Failed to record audit log for action {}: {}", action, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(String entityName, AuditAction action, String entityId, Pageable pageable) {
        return auditLogRepository.filterAuditLogs(entityName, action, entityId, pageable);
    }
}
