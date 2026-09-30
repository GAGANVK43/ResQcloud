package com.resqcloud.incident.service;

import com.resqcloud.common.audit.AuditAction;
import com.resqcloud.common.audit.AuditLogService;
import com.resqcloud.common.exception.InvalidIncidentStateException;
import com.resqcloud.common.exception.ResourceNotFoundException;
import com.resqcloud.common.exception.UnauthorizedActionException;
import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentStatus;
import com.resqcloud.incident.entity.IncidentStatusHistory;
import com.resqcloud.incident.repository.IncidentRepository;
import com.resqcloud.incident.repository.IncidentStatusHistoryRepository;
import com.resqcloud.notification.service.NotificationService;
import com.resqcloud.security.SecurityUtils;
import com.resqcloud.user.entity.User;
import com.resqcloud.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class IncidentStateTransitionService {

    private static final Logger log = LoggerFactory.getLogger(IncidentStateTransitionService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    // Allowed transition map: Source Status -> Set of permitted Target Statuses
    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(IncidentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(IncidentStatus.REPORTED, EnumSet.of(
                IncidentStatus.UNDER_REVIEW,
                IncidentStatus.PRIORITIZED,
                IncidentStatus.CANCELLED
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.UNDER_REVIEW, EnumSet.of(
                IncidentStatus.PRIORITIZED,
                IncidentStatus.ASSIGNED,
                IncidentStatus.CANCELLED
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.PRIORITIZED, EnumSet.of(
                IncidentStatus.ASSIGNED,
                IncidentStatus.UNDER_REVIEW,
                IncidentStatus.CANCELLED
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.ASSIGNED, EnumSet.of(
                IncidentStatus.RESPONDER_EN_ROUTE,
                IncidentStatus.IN_PROGRESS,
                IncidentStatus.PRIORITIZED,
                IncidentStatus.CANCELLED
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.RESPONDER_EN_ROUTE, EnumSet.of(
                IncidentStatus.IN_PROGRESS,
                IncidentStatus.ASSIGNED,
                IncidentStatus.CANCELLED
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.IN_PROGRESS, EnumSet.of(
                IncidentStatus.RESOLVED,
                IncidentStatus.ASSIGNED
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.RESOLVED, EnumSet.of(
                IncidentStatus.CLOSED,
                IncidentStatus.IN_PROGRESS // Reopening
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.CLOSED, EnumSet.of(
                IncidentStatus.UNDER_REVIEW // Explicit administrative reopen only
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.CANCELLED, EnumSet.noneOf(IncidentStatus.class));
    }

    public IncidentStateTransitionService(IncidentRepository incidentRepository,
                                         IncidentStatusHistoryRepository historyRepository,
                                         UserRepository userRepository,
                                         NotificationService notificationService,
                                         AuditLogService auditLogService) {
        this.incidentRepository = incidentRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Incident transitionState(UUID incidentId, IncidentStatus targetStatus, String comments) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));

        validateTransition(incident, targetStatus);

        IncidentStatus oldStatus = incident.getStatus();
        incident.setStatus(targetStatus);

        if (targetStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(Instant.now());
            if (comments != null) {
                incident.setResolutionNotes(comments);
            }
        }

        Incident updatedIncident = incidentRepository.save(incident);

        // Record in state transition history
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(incident.getReporter()) : incident.getReporter();

        IncidentStatusHistory history = new IncidentStatusHistory(
                incidentId,
                oldStatus,
                targetStatus,
                currentUser,
                comments
        );
        historyRepository.save(history);

        // Record in audit log
        auditLogService.record(
                currentUser.getId(),
                currentUser.getUsername(),
                mapToAuditAction(targetStatus),
                "Incident",
                incident.getId().toString(),
                oldStatus.name(),
                targetStatus.name(),
                null,
                comments
        );

        // Notify stakeholders
        notificationService.notifyIncidentStatusChanged(updatedIncident, oldStatus.name(), targetStatus.name());

        log.info("Incident {} transitioned from {} to {}", incident.getIncidentNumber(), oldStatus, targetStatus);
        return updatedIncident;
    }

    public void validateTransition(Incident incident, IncidentStatus targetStatus) {
        IncidentStatus currentStatus = incident.getStatus();

        if (currentStatus == targetStatus) {
            throw new InvalidIncidentStateException(currentStatus.name(), targetStatus.name(),
                    "Incident is already in status " + targetStatus);
        }

        Set<IncidentStatus> validNextStates = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        if (!validNextStates.contains(targetStatus)) {
            throw new InvalidIncidentStateException(currentStatus.name(), targetStatus.name(),
                    "Valid next states from " + currentStatus + " are: " + validNextStates);
        }

        // Special permission check for reopening CLOSED incident
        if (currentStatus == IncidentStatus.CLOSED) {
            if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("COORDINATOR")) {
                throw new UnauthorizedActionException("Only Coordinators or Administrators can reopen a closed incident");
            }
        }
    }

    public Set<IncidentStatus> getAvailableNextStates(IncidentStatus currentStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    }

    private AuditAction mapToAuditAction(IncidentStatus status) {
        return switch (status) {
            case UNDER_REVIEW -> AuditAction.INCIDENT_REVIEWED;
            case PRIORITIZED -> AuditAction.INCIDENT_PRIORITIZED;
            case ASSIGNED -> AuditAction.INCIDENT_ASSIGNED;
            case RESPONDER_EN_ROUTE -> AuditAction.INCIDENT_RESPONDER_EN_ROUTE;
            case IN_PROGRESS -> AuditAction.INCIDENT_IN_PROGRESS;
            case RESOLVED -> AuditAction.INCIDENT_RESOLVED;
            case CLOSED -> AuditAction.INCIDENT_CLOSED;
            case CANCELLED -> AuditAction.INCIDENT_CANCELLED;
            default -> AuditAction.INCIDENT_STATUS_CHANGED;
        };
    }
}
