package com.resqcloud.incident.dto;

import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentSeverity;
import com.resqcloud.incident.entity.IncidentStatus;
import com.resqcloud.incident.entity.IncidentType;
import java.time.Instant;
import java.util.UUID;

public class IncidentResponse {
    private UUID id;
    private String incidentNumber;
    private String title;
    private String description;
    private IncidentType incidentType;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private double priorityScore;
    private String priorityLevel;
    private Double latitude;
    private Double longitude;
    private String address;
    private int affectedPeople;
    private UUID reporterId;
    private String reporterName;
    private UUID assignedResponderId;
    private String assignedResponderName;
    private UUID assignedResourceId;
    private String assignedResourceName;
    private String resolutionNotes;
    private Instant resolvedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public IncidentResponse() {}

    public static IncidentResponse fromEntity(Incident incident) {
        IncidentResponse dto = new IncidentResponse();
        dto.setId(incident.getId());
        dto.setIncidentNumber(incident.getIncidentNumber());
        dto.setTitle(incident.getTitle());
        dto.setDescription(incident.getDescription());
        dto.setIncidentType(incident.getIncidentType());
        dto.setSeverity(incident.getSeverity());
        dto.setStatus(incident.getStatus());
        dto.setPriorityScore(incident.getPriorityScore());
        dto.setPriorityLevel(incident.getPriorityLevel());
        dto.setLatitude(incident.getLatitude());
        dto.setLongitude(incident.getLongitude());
        dto.setAddress(incident.getAddress());
        dto.setAffectedPeople(incident.getAffectedPeople());
        if (incident.getReporter() != null) {
            dto.setReporterId(incident.getReporter().getId());
            dto.setReporterName(incident.getReporter().getFullName());
        }
        if (incident.getAssignedResponder() != null) {
            dto.setAssignedResponderId(incident.getAssignedResponder().getId());
            dto.setAssignedResponderName(incident.getAssignedResponder().getFullName());
        }
        if (incident.getAssignedResource() != null) {
            dto.setAssignedResourceId(incident.getAssignedResource().getId());
            dto.setAssignedResourceName(incident.getAssignedResource().getName());
        }
        dto.setResolutionNotes(incident.getResolutionNotes());
        dto.setResolvedAt(incident.getResolvedAt());
        dto.setCreatedAt(incident.getCreatedAt());
        dto.setUpdatedAt(incident.getUpdatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(IncidentType incidentType) {
        this.incidentType = incidentType;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(double priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getAffectedPeople() {
        return affectedPeople;
    }

    public void setAffectedPeople(int affectedPeople) {
        this.affectedPeople = affectedPeople;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public void setReporterId(UUID reporterId) {
        this.reporterId = reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public UUID getAssignedResponderId() {
        return assignedResponderId;
    }

    public void setAssignedResponderId(UUID assignedResponderId) {
        this.assignedResponderId = assignedResponderId;
    }

    public String getAssignedResponderName() {
        return assignedResponderName;
    }

    public void setAssignedResponderName(String assignedResponderName) {
        this.assignedResponderName = assignedResponderName;
    }

    public UUID getAssignedResourceId() {
        return assignedResourceId;
    }

    public void setAssignedResourceId(UUID assignedResourceId) {
        this.assignedResourceId = assignedResourceId;
    }

    public String getAssignedResourceName() {
        return assignedResourceName;
    }

    public void setAssignedResourceName(String assignedResourceName) {
        this.assignedResourceName = assignedResourceName;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
