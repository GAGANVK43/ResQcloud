package com.resqcloud.incident.entity;

import com.resqcloud.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_status_history", indexes = {
    @Index(name = "idx_history_incident", columnList = "incident_id"),
    @Index(name = "idx_history_changed_at", columnList = "changed_at DESC")
})
public class IncidentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 50)
    private IncidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 50)
    private IncidentStatus toStatus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    public IncidentStatusHistory() {
        this.changedAt = Instant.now();
    }

    public IncidentStatusHistory(UUID incidentId, IncidentStatus fromStatus, IncidentStatus toStatus, User changedBy, String comments) {
        this.incidentId = incidentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.comments = comments;
        this.changedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public IncidentStatus getFromStatus() {
        return fromStatus;
    }

    public IncidentStatus getToStatus() {
        return toStatus;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public String getComments() {
        return comments;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
