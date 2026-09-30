package com.resqcloud.resource.service;

import com.resqcloud.common.audit.AuditAction;
import com.resqcloud.common.audit.AuditLogService;
import com.resqcloud.common.exception.ResourceNotFoundException;
import com.resqcloud.common.exception.ResourceUnavailableException;
import com.resqcloud.common.response.PagedResponse;
import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentStatus;
import com.resqcloud.incident.repository.IncidentRepository;
import com.resqcloud.notification.service.NotificationService;
import com.resqcloud.resource.dto.ResourceAssignmentRequest;
import com.resqcloud.resource.dto.ResourceAssignmentResponse;
import com.resqcloud.resource.dto.ResourceCreateRequest;
import com.resqcloud.resource.dto.ResourceResponse;
import com.resqcloud.resource.dto.ResourceUpdateRequest;
import com.resqcloud.resource.entity.AssignmentStatus;
import com.resqcloud.resource.entity.Resource;
import com.resqcloud.resource.entity.ResourceAssignment;
import com.resqcloud.resource.entity.ResourceStatus;
import com.resqcloud.resource.entity.ResourceType;
import com.resqcloud.resource.repository.ResourceAssignmentRepository;
import com.resqcloud.resource.repository.ResourceRepository;
import com.resqcloud.security.SecurityUtils;
import com.resqcloud.user.entity.User;
import com.resqcloud.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);

    private final ResourceRepository resourceRepository;
    private final ResourceAssignmentRepository assignmentRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public ResourceService(ResourceRepository resourceRepository,
                           ResourceAssignmentRepository assignmentRepository,
                           IncidentRepository incidentRepository,
                           UserRepository userRepository,
                           NotificationService notificationService,
                           AuditLogService auditLogService) {
        this.resourceRepository = resourceRepository;
        this.assignmentRepository = assignmentRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ResourceResponse createResource(ResourceCreateRequest request) {
        Resource resource = new Resource(
                request.getName(),
                request.getResourceType(),
                ResourceStatus.AVAILABLE,
                request.getLatitude(),
                request.getLongitude(),
                request.getAddress(),
                request.getCapacity(),
                request.getContactNumber()
        );

        Resource saved = resourceRepository.save(resource);

        auditLogService.record(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(),
                AuditAction.RESOURCE_CREATED,
                "Resource",
                saved.getId().toString(),
                null,
                "CREATED",
                null,
                "Name: " + saved.getName() + ", Type: " + saved.getResourceType()
        );

        return ResourceResponse.fromEntity(saved);
    }

    @Transactional
    public ResourceResponse updateResource(UUID id, ResourceUpdateRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        String oldState = resource.getStatus().name();

        if (request.getName() != null) resource.setName(request.getName());
        if (request.getResourceType() != null) resource.setResourceType(request.getResourceType());
        if (request.getStatus() != null) resource.setStatus(request.getStatus());
        if (request.getLatitude() != null) resource.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) resource.setLongitude(request.getLongitude());
        if (request.getAddress() != null) resource.setAddress(request.getAddress());
        if (request.getCapacity() != null) resource.setCapacity(request.getCapacity());
        if (request.getContactNumber() != null) resource.setContactNumber(request.getContactNumber());

        Resource updated = resourceRepository.save(resource);

        auditLogService.record(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(),
                AuditAction.RESOURCE_UPDATED,
                "Resource",
                updated.getId().toString(),
                oldState,
                updated.getStatus().name(),
                null,
                "Resource updated"
        );

        return ResourceResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(UUID id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));
        return ResourceResponse.fromEntity(resource);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResourceResponse> listResources(ResourceType type, ResourceStatus status, String search, Pageable pageable) {
        Page<Resource> page = resourceRepository.filterResources(type, status, search, pageable);
        return PagedResponse.from(page.map(ResourceResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getAvailableResources() {
        return resourceRepository.findByStatus(ResourceStatus.AVAILABLE).stream()
                .map(ResourceResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ResourceAssignmentResponse assignResourceToIncident(UUID incidentId, ResourceAssignmentRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", request.getResourceId()));

        if (resource.getStatus() != ResourceStatus.AVAILABLE && resource.getStatus() != ResourceStatus.ASSIGNED) {
            throw new ResourceUnavailableException("Resource '" + resource.getName() + "' is currently in status " + resource.getStatus());
        }

        User responder = null;
        if (request.getResponderId() != null) {
            responder = userRepository.findById(request.getResponderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Responder", "id", request.getResponderId()));
        }

        ResourceAssignment assignment = new ResourceAssignment(
                incidentId,
                resource,
                responder,
                AssignmentStatus.ACCEPTED,
                request.getNotes()
        );

        ResourceAssignment savedAssignment = assignmentRepository.save(assignment);

        // Update resource status
        resource.setStatus(ResourceStatus.ASSIGNED);
        resourceRepository.save(resource);

        // Update incident
        incident.setAssignedResource(resource);
        if (responder != null) {
            incident.setAssignedResponder(responder);
        }
        if (incident.getStatus() == IncidentStatus.REPORTED || incident.getStatus() == IncidentStatus.UNDER_REVIEW || incident.getStatus() == IncidentStatus.PRIORITIZED) {
            incident.setStatus(IncidentStatus.ASSIGNED);
        }
        incidentRepository.save(incident);

        // Audit log
        auditLogService.record(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(),
                AuditAction.RESOURCE_ASSIGNED,
                "ResourceAssignment",
                savedAssignment.getId().toString(),
                ResourceStatus.AVAILABLE.name(),
                ResourceStatus.ASSIGNED.name(),
                null,
                "Assigned to Incident #" + incident.getIncidentNumber()
        );

        // Notifications
        if (responder != null) {
            notificationService.notifyResponderAssigned(incident, responder);
        } else {
            notificationService.notifyAssignmentStatusUpdated(incident, "Resource " + resource.getName() + " assigned");
        }

        return ResourceAssignmentResponse.fromEntity(savedAssignment);
    }

    @Transactional
    public ResourceAssignmentResponse updateAssignmentStatus(UUID assignmentId, AssignmentStatus newStatus) {
        ResourceAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("ResourceAssignment", "id", assignmentId));

        AssignmentStatus oldStatus = assignment.getStatus();
        assignment.setStatus(newStatus);

        Resource resource = assignment.getResource();

        if (newStatus == AssignmentStatus.RELEASED || newStatus == AssignmentStatus.COMPLETED) {
            assignment.setReleasedAt(Instant.now());
            // Check if resource has other active assignments
            long activeCount = assignmentRepository.countActiveAssignmentsForResource(resource.getId());
            if (activeCount <= 1) { // Current assignment was the only active one
                resource.setStatus(ResourceStatus.AVAILABLE);
                resourceRepository.save(resource);
            }
        } else if (newStatus == AssignmentStatus.EN_ROUTE) {
            resource.setStatus(ResourceStatus.EN_ROUTE);
            resourceRepository.save(resource);
        } else if (newStatus == AssignmentStatus.ON_SCENE) {
            resource.setStatus(ResourceStatus.BUSY);
            resourceRepository.save(resource);
        }

        ResourceAssignment updated = assignmentRepository.save(assignment);

        Incident incident = incidentRepository.findById(assignment.getIncidentId()).orElse(null);
        if (incident != null) {
            notificationService.notifyAssignmentStatusUpdated(incident, "Resource " + resource.getName() + " status: " + newStatus);
        }

        auditLogService.record(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(),
                AuditAction.RESOURCE_STATUS_CHANGED,
                "ResourceAssignment",
                assignmentId.toString(),
                oldStatus.name(),
                newStatus.name(),
                null,
                "Assignment status updated"
        );

        return ResourceAssignmentResponse.fromEntity(updated);
    }

    @Transactional
    public void releaseResource(UUID resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", resourceId));

        String oldStatus = resource.getStatus().name();
        resource.setStatus(ResourceStatus.AVAILABLE);
        resourceRepository.save(resource);

        auditLogService.record(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(),
                AuditAction.RESOURCE_RELEASED,
                "Resource",
                resourceId.toString(),
                oldStatus,
                ResourceStatus.AVAILABLE.name(),
                null,
                "Resource released back to pool"
        );
    }

    @Transactional(readOnly = true)
    public List<ResourceAssignmentResponse> getAssignmentsForIncident(UUID incidentId) {
        return assignmentRepository.findByIncidentId(incidentId).stream()
                .map(ResourceAssignmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceAssignmentResponse> getAssignmentsForResponder(UUID responderId) {
        return assignmentRepository.findActiveAssignmentsForResponder(responderId).stream()
                .map(ResourceAssignmentResponse::fromEntity)
                .toList();
    }
}
