package com.resqcloud.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityNameOrderByCreatedAtDesc(String entityName, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:entityName IS NULL OR a.entityName = :entityName) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> filterAuditLogs(@Param("entityName") String entityName,
                                   @Param("action") AuditAction action,
                                   @Param("entityId") String entityId,
                                   Pageable pageable);
}
