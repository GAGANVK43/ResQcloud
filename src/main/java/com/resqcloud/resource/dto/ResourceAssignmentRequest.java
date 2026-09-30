package com.resqcloud.resource.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ResourceAssignmentRequest {

    @NotNull(message = "Resource ID is required")
    private UUID resourceId;

    private UUID responderId;

    private String notes;

    public ResourceAssignmentRequest() {}

    public ResourceAssignmentRequest(UUID resourceId, UUID responderId, String notes) {
        this.resourceId = resourceId;
        this.responderId = responderId;
        this.notes = notes;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public UUID getResponderId() {
        return responderId;
    }

    public void setResponderId(UUID responderId) {
        this.responderId = responderId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
