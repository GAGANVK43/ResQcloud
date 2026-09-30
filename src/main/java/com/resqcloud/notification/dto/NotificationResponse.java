package com.resqcloud.notification.dto;

import com.resqcloud.notification.entity.Notification;
import com.resqcloud.notification.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

public class NotificationResponse {
    private UUID id;
    private String title;
    private String message;
    private NotificationType type;
    private UUID relatedIncidentId;
    private String relatedIncidentNumber;
    private boolean read;
    private Instant createdAt;

    public NotificationResponse() {}

    public static NotificationResponse fromEntity(Notification notification) {
        NotificationResponse dto = new NotificationResponse();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        if (notification.getRelatedIncident() != null) {
            dto.setRelatedIncidentId(notification.getRelatedIncident().getId());
            dto.setRelatedIncidentNumber(notification.getRelatedIncident().getIncidentNumber());
        }
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public UUID getRelatedIncidentId() {
        return relatedIncidentId;
    }

    public void setRelatedIncidentId(UUID relatedIncidentId) {
        this.relatedIncidentId = relatedIncidentId;
    }

    public String getRelatedIncidentNumber() {
        return relatedIncidentNumber;
    }

    public void setRelatedIncidentNumber(String relatedIncidentNumber) {
        this.relatedIncidentNumber = relatedIncidentNumber;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
