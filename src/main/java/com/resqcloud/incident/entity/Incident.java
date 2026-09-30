package com.resqcloud.incident.entity;

import com.resqcloud.resource.entity.Resource;
import com.resqcloud.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents", indexes = {
    @Index(name = "idx_incidents_status", columnList = "status"),
    @Index(name = "idx_incidents_severity", columnList = "severity"),
    @Index(name = "idx_incidents_type", columnList = "incident_type"),
    @Index(name = "idx_incidents_priority_score", columnList = "priority_score DESC"),
    @Index(name = "idx_incidents_reporter", columnList = "reporter_id"),
    @Index(name = "idx_incidents_responder", columnList = "assigned_responder_id"),
    @Index(name = "idx_incidents_created_at", columnList = "created_at DESC")
})
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_number", nullable = false, unique = true, length = 50)
    private String incidentNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 50)
    private IncidentType incidentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 50)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private IncidentStatus status = IncidentStatus.REPORTED;

    @Column(name = "priority_score", nullable = false)
    private double priorityScore = 0.0;

    @Column(name = "priority_level", nullable = false, length = 50)
    private String priorityLevel = "LOW";

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "affected_people", nullable = false)
    private int affectedPeople = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_responder_id")
    private User assignedResponder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_resource_id")
    private Resource assignedResource;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Incident() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Incident(String incidentNumber, String title, String description,
                    IncidentType incidentType, IncidentSeverity severity,
                    Double latitude, Double longitude, String address,
                    int affectedPeople, User reporter) {
        this.incidentNumber = incidentNumber;
        this.title = title;
        this.description = description;
        this.incidentType = incidentType;
        this.severity = severity;
        this.status = IncidentStatus.REPORTED;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.affectedPeople = affectedPeople;
        this.reporter = reporter;
        this.priorityScore = 0.0;
        this.priorityLevel = "LOW";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
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

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public User getAssignedResponder() {
        return assignedResponder;
    }

    public void setAssignedResponder(User assignedResponder) {
        this.assignedResponder = assignedResponder;
    }

    public Resource getAssignedResource() {
        return assignedResource;
    }

    public void setAssignedResource(Resource assignedResource) {
        this.assignedResource = assignedResource;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
