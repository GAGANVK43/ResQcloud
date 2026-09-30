package com.resqcloud.incident.dto;

import com.resqcloud.incident.entity.IncidentStatus;
import com.resqcloud.incident.entity.IncidentStatusHistory;
import com.resqcloud.resource.dto.ResourceAssignmentResponse;

import java.util.List;
import java.util.Set;

public class IncidentDetailsResponse {
    private IncidentResponse incident;
    private List<IncidentStatusHistoryDto> history;
    private List<ResourceAssignmentResponse> assignments;
    private Set<IncidentStatus> availableNextStates;

    public IncidentDetailsResponse() {}

    public IncidentDetailsResponse(IncidentResponse incident, List<IncidentStatusHistoryDto> history,
                                   List<ResourceAssignmentResponse> assignments, Set<IncidentStatus> availableNextStates) {
        this.incident = incident;
        this.history = history;
        this.assignments = assignments;
        this.availableNextStates = availableNextStates;
    }

    public IncidentResponse getIncident() {
        return incident;
    }

    public void setIncident(IncidentResponse incident) {
        this.incident = incident;
    }

    public List<IncidentStatusHistoryDto> getHistory() {
        return history;
    }

    public void setHistory(List<IncidentStatusHistoryDto> history) {
        this.history = history;
    }

    public List<ResourceAssignmentResponse> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<ResourceAssignmentResponse> assignments) {
        this.assignments = assignments;
    }

    public Set<IncidentStatus> getAvailableNextStates() {
        return availableNextStates;
    }

    public void setAvailableNextStates(Set<IncidentStatus> availableNextStates) {
        this.availableNextStates = availableNextStates;
    }

    public static class IncidentStatusHistoryDto {
        private String fromStatus;
        private String toStatus;
        private String changedBy;
        private String comments;
        private String changedAt;

        public IncidentStatusHistoryDto(IncidentStatusHistory h) {
            this.fromStatus = h.getFromStatus() != null ? h.getFromStatus().name() : null;
            this.toStatus = h.getToStatus().name();
            this.changedBy = h.getChangedBy() != null ? h.getChangedBy().getFullName() : "SYSTEM";
            this.comments = h.getComments();
            this.changedAt = h.getChangedAt().toString();
        }

        public String getFromStatus() { return fromStatus; }
        public String getToStatus() { return toStatus; }
        public String getChangedBy() { return changedBy; }
        public String getComments() { return comments; }
        public String getChangedAt() { return changedAt; }
    }
}
