package com.resqcloud.incident.service;

import com.resqcloud.common.audit.AuditAction;
import com.resqcloud.common.audit.AuditLogService;
import com.resqcloud.common.exception.ResourceNotFoundException;
import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.repository.IncidentRepository;
import com.resqcloud.incident.service.priority.IncidentPriorityContext;
import com.resqcloud.incident.service.priority.PriorityCalculationStrategy;
import com.resqcloud.incident.service.priority.PriorityScoreResult;
import com.resqcloud.notification.service.NotificationService;
import com.resqcloud.resource.entity.ResourceStatus;
import com.resqcloud.resource.repository.ResourceRepository;
import com.resqcloud.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class IncidentPriorityService {

    private static final Logger log = LoggerFactory.getLogger(IncidentPriorityService.class);

    private final IncidentRepository incidentRepository;
    private final ResourceRepository resourceRepository;
    private final PriorityCalculationStrategy priorityStrategy;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public IncidentPriorityService(IncidentRepository incidentRepository,
                                  ResourceRepository resourceRepository,
                                  PriorityCalculationStrategy priorityStrategy,
                                  NotificationService notificationService,
                                  AuditLogService auditLogService) {
        this.incidentRepository = incidentRepository;
        this.resourceRepository = resourceRepository;
        this.priorityStrategy = priorityStrategy;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PriorityScoreResult calculateAndApplyPriority(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));

        return calculateAndApplyPriority(incident);
    }

    @Transactional
    public PriorityScoreResult calculateAndApplyPriority(Incident incident) {
        long availableResources = resourceRepository.countByStatus(ResourceStatus.AVAILABLE);
        long minutesElapsed = Duration.between(incident.getCreatedAt(), Instant.now()).toMinutes();

        IncidentPriorityContext context = new IncidentPriorityContext(
                availableResources,
                0.5,
                minutesElapsed
        );

        PriorityScoreResult result = priorityStrategy.calculatePriority(incident, context);

        double oldScore = incident.getPriorityScore();
        String oldLevel = incident.getPriorityLevel();

        incident.setPriorityScore(result.getTotalScore());
        incident.setPriorityLevel(result.getPriorityLevel());
        incidentRepository.save(incident);

        log.info("Priority calculated for incident {}: score={}, level={}, model={}",
                incident.getIncidentNumber(), result.getTotalScore(), result.getPriorityLevel(), result.getCalculationModel());

        // Emit audit log
        auditLogService.record(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(),
                AuditAction.INCIDENT_PRIORITIZED,
                "Incident",
                incident.getId().toString(),
                oldLevel + " (" + oldScore + ")",
                result.getPriorityLevel() + " (" + result.getTotalScore() + ")",
                null,
                "Model: " + result.getCalculationModel()
        );

        // Notify
        notificationService.notifyIncidentPrioritized(incident);

        return result;
    }

    public PriorityScoreResult evaluatePreview(Incident incident) {
        long availableResources = resourceRepository.countByStatus(ResourceStatus.AVAILABLE);
        IncidentPriorityContext context = new IncidentPriorityContext(availableResources, 0.5, 0);
        return priorityStrategy.calculatePriority(incident, context);
    }
}
