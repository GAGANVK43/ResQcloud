package com.resqcloud.resource.dto;

import com.resqcloud.resource.entity.AssignmentStatus;
import com.resqcloud.resource.entity.ResourceAssignment;
import java.time.Instant;
import java.util.UUID;

public class ResourceAssignmentResponse {
    private UUID id;
    private UUID incidentId;
    private UUID resourceId;
    private String resourceName;
    private String resourceType;
    private UUID responderId;
    private String responderName;
    private AssignmentStatus status;
    private Instant assignedAt;
    private Instant releasedAt;
    private String notes;

    public ResourceAssignmentResponse() {}

    public static ResourceAssignmentResponse fromEntity(ResourceAssignment assignment) {
        ResourceAssignmentResponse dto = new ResourceAssignmentResponse();
        dto.setId(assignment.getId());
        dto.setIncidentId(assignment.getIncidentId());
        if (assignment.getResource() != null) {
            dto.setResourceId(assignment.getResource().getId());
            dto.setResourceName(assignment.getResource().getName());
            dto.setResourceType(assignment.getResource().getResourceType().name());
        }
        if (assignment.getResponder() != null) {
            dto.setResponderId(assignment.getResponder().getId());
            dto.setResponderName(assignment.getResponder().getFullName());
        }
        dto.setStatus(assignment.getStatus());
        dto.setAssignedAt(assignment.getAssignedAt());
        dto.setReleasedAt(assignment.getReleasedAt());
        dto.setNotes(assignment.getNotes());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(UUID incidentId) {
        this.incidentId = incidentId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public UUID getResponderId() {
        return responderId;
    }

    public void setResponderId(UUID responderId) {
        this.responderId = responderId;
    }

    public String getResponderName() {
        return responderName;
    }

    public void setResponderName(String responderName) {
        this.responderName = responderName;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
