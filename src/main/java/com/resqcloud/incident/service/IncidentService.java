package com.resqcloud.incident.service;

import com.resqcloud.common.audit.AuditAction;
import com.resqcloud.common.audit.AuditLogService;
import com.resqcloud.common.exception.ResourceNotFoundException;
import com.resqcloud.common.exception.UnauthorizedActionException;
import com.resqcloud.common.response.PagedResponse;
import com.resqcloud.common.utils.GeoLocationUtils;
import com.resqcloud.incident.dto.*;
import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentSeverity;
import com.resqcloud.incident.entity.IncidentStatus;
import com.resqcloud.incident.entity.IncidentType;
import com.resqcloud.incident.repository.IncidentRepository;
import com.resqcloud.incident.repository.IncidentStatusHistoryRepository;
import com.resqcloud.notification.service.NotificationService;
import com.resqcloud.resource.dto.ResourceAssignmentResponse;
import com.resqcloud.resource.repository.ResourceAssignmentRepository;
import com.resqcloud.security.SecurityUtils;
import com.resqcloud.user.entity.User;
import com.resqcloud.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentStatusHistoryRepository historyRepository;
    private final ResourceAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final IncidentPriorityService priorityService;
    private final IncidentStateTransitionService stateTransitionService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentStatusHistoryRepository historyRepository,
                           ResourceAssignmentRepository assignmentRepository,
                           UserRepository userRepository,
                           IncidentPriorityService priorityService,
                           IncidentStateTransitionService stateTransitionService,
                           NotificationService notificationService,
                           AuditLogService auditLogService) {
        this.incidentRepository = incidentRepository;
        this.historyRepository = historyRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.priorityService = priorityService;
        this.stateTransitionService = stateTransitionService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public IncidentResponse createIncident(IncidentCreateRequest request) {
        if (!GeoLocationUtils.isValidCoordinate(request.getLatitude(), request.getLongitude())) {
            throw new IllegalArgumentException("Invalid GPS coordinates provided");
        }

        UUID reporterId = SecurityUtils.getCurrentUserId();
        User reporter = null;
        if (reporterId != null) {
            reporter = userRepository.findById(reporterId).orElse(null);
        }
        if (reporter == null) {
            throw new UnauthorizedActionException("You must be logged in to report an incident");
        }

        String incidentNumber = generateIncidentNumber();

        Incident incident = new Incident(
                incidentNumber,
                request.getTitle(),
                request.getDescription(),
                request.getIncidentType(),
                request.getSeverity(),
                request.getLatitude(),
                request.getLongitude(),
                request.getAddress(),
                request.getAffectedPeople(),
                reporter
        );

        Incident savedIncident = incidentRepository.save(incident);

        // Audit log creation
        auditLogService.record(
                reporter.getId(),
                reporter.getUsername(),
                AuditAction.INCIDENT_REPORTED,
                "Incident",
                savedIncident.getId().toString(),
                null,
                IncidentStatus.REPORTED.name(),
                null,
                "Reported: " + incident.getTitle()
        );

        // Automatically calculate priority using priority engine
        priorityService.calculateAndApplyPriority(savedIncident);

        // Broadcast notifications
        notificationService.notifyIncidentCreated(savedIncident);

        log.info("Incident created successfully: number={}, priority={}",
                savedIncident.getIncidentNumber(), savedIncident.getPriorityLevel());

        return IncidentResponse.fromEntity(savedIncident);
    }

    @Transactional
    public IncidentResponse updateIncident(UUID id, IncidentUpdateRequest request) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        // Authorization check: Citizen can only update their own reported incident if still in REPORTED status
        if (SecurityUtils.hasRole("CITIZEN") && !SecurityUtils.hasRole("COORDINATOR") && !SecurityUtils.hasRole("ADMIN")) {
            if (!incident.getReporter().getId().equals(SecurityUtils.getCurrentUserId())) {
                throw new UnauthorizedActionException("You do not have permission to modify this incident");
            }
            if (incident.getStatus() != IncidentStatus.REPORTED) {
                throw new UnauthorizedActionException("Incident has already been processed and can no longer be edited directly");
            }
        }

        boolean shouldRecalculatePriority = false;

        if (request.getTitle() != null) incident.setTitle(request.getTitle());
        if (request.getDescription() != null) incident.setDescription(request.getDescription());
        if (request.getAddress() != null) incident.setAddress(request.getAddress());
        if (request.getLatitude() != null && request.getLongitude() != null) {
            incident.setLatitude(request.getLatitude());
            incident.setLongitude(request.getLongitude());
        }

        if (request.getSeverity() != null && request.getSeverity() != incident.getSeverity()) {
            incident.setSeverity(request.getSeverity());
            shouldRecalculatePriority = true;
        }

        if (request.getIncidentType() != null && request.getIncidentType() != incident.getIncidentType()) {
            incident.setIncidentType(request.getIncidentType());
            shouldRecalculatePriority = true;
        }

        if (request.getAffectedPeople() != null && request.getAffectedPeople() != incident.getAffectedPeople()) {
            incident.setAffectedPeople(request.getAffectedPeople());
            shouldRecalculatePriority = true;
        }

        Incident saved = incidentRepository.save(incident);

        if (shouldRecalculatePriority) {
            priorityService.calculateAndApplyPriority(saved);
        }

        auditLogService.record(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(),
                AuditAction.INCIDENT_UPDATED,
                "Incident",
                saved.getId().toString(),
                null,
                saved.getStatus().name(),
                null,
                "Incident details updated"
        );

        return IncidentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(UUID id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
        return IncidentResponse.fromEntity(incident);
    }

    @Transactional(readOnly = true)
    public IncidentDetailsResponse getIncidentDetails(UUID id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        var historyDtos = historyRepository.findByIncidentIdOrderByChangedAtAsc(id).stream()
                .map(IncidentDetailsResponse.IncidentStatusHistoryDto::new)
                .toList();

        var assignmentDtos = assignmentRepository.findByIncidentId(id).stream()
                .map(ResourceAssignmentResponse::fromEntity)
                .toList();

        var availableNextStates = stateTransitionService.getAvailableNextStates(incident.getStatus());

        return new IncidentDetailsResponse(
                IncidentResponse.fromEntity(incident),
                historyDtos,
                assignmentDtos,
                availableNextStates
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> searchAndFilterIncidents(
            IncidentType type, IncidentSeverity severity, IncidentStatus status,
            UUID reporterId, UUID responderId, String search, Pageable pageable) {

        Page<Incident> page = incidentRepository.searchAndFilterIncidents(type, severity, status, reporterId, responderId, search, pageable);
        return PagedResponse.from(page.map(IncidentResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getMyReportedIncidents(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<Incident> page = incidentRepository.findByReporterIdOrderByCreatedAtDesc(currentUserId, pageable);
        return PagedResponse.from(page.map(IncidentResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getMyAssignedIncidents(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<Incident> page = incidentRepository.findByAssignedResponderIdOrderByCreatedAtDesc(currentUserId, pageable);
        return PagedResponse.from(page.map(IncidentResponse::fromEntity));
    }

    @Transactional
    public IncidentResponse transitionIncidentStatus(UUID incidentId, StatusTransitionRequest request) {
        Incident updated = stateTransitionService.transitionState(incidentId, request.getTargetStatus(), request.getComments());
        return IncidentResponse.fromEntity(updated);
    }

    @Transactional
    public IncidentResponse recalculatePriority(UUID incidentId) {
        priorityService.calculateAndApplyPriority(incidentId);
        return getIncidentById(incidentId);
    }

    private synchronized String generateIncidentNumber() {
        int year = Year.now().getValue();
        int randomCode = 1000 + new Random().nextInt(9000);
        return String.format("INC-%d-%04d", year, randomCode);
    }
}
