package com.resqcloud.incident.dto;

import com.resqcloud.incident.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;

public class StatusTransitionRequest {

    @NotNull(message = "Target status is required")
    private IncidentStatus targetStatus;

    private String comments;

    public StatusTransitionRequest() {}

    public StatusTransitionRequest(IncidentStatus targetStatus, String comments) {
        this.targetStatus = targetStatus;
        this.comments = comments;
    }

    public IncidentStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(IncidentStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
