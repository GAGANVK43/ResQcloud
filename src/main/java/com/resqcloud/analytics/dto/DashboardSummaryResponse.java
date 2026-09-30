package com.resqcloud.analytics.dto;

import java.util.Map;

public class DashboardSummaryResponse {
    private long totalIncidents;
    private long activeIncidents;
    private long criticalIncidents;
    private long highPriorityIncidents;
    private long resolvedIncidents;
    
    private long totalResources;
    private long availableResources;
    private long assignedResources;
    private long busyResources;
    
    private long activeResponders;
    private double avgResponseTimeMinutes;
    private double avgResolutionTimeMinutes;
    
    private Map<String, Long> incidentsByType;
    private Map<String, Long> incidentsBySeverity;
    private Map<String, Long> resourcesByType;

    public DashboardSummaryResponse() {}

    public long getTotalIncidents() {
        return totalIncidents;
    }

    public void setTotalIncidents(long totalIncidents) {
        this.totalIncidents = totalIncidents;
    }

    public long getActiveIncidents() {
        return activeIncidents;
    }

    public void setActiveIncidents(long activeIncidents) {
        this.activeIncidents = activeIncidents;
    }

    public long getCriticalIncidents() {
        return criticalIncidents;
    }

    public void setCriticalIncidents(long criticalIncidents) {
        this.criticalIncidents = criticalIncidents;
    }

    public long getHighPriorityIncidents() {
        return highPriorityIncidents;
    }

    public void setHighPriorityIncidents(long highPriorityIncidents) {
        this.highPriorityIncidents = highPriorityIncidents;
    }

    public long getResolvedIncidents() {
        return resolvedIncidents;
    }

    public void setResolvedIncidents(long resolvedIncidents) {
        this.resolvedIncidents = resolvedIncidents;
    }

    public long getTotalResources() {
        return totalResources;
    }

    public void setTotalResources(long totalResources) {
        this.totalResources = totalResources;
    }

    public long getAvailableResources() {
        return availableResources;
    }

    public void setAvailableResources(long availableResources) {
        this.availableResources = availableResources;
    }

    public long getAssignedResources() {
        return assignedResources;
    }

    public void setAssignedResources(long assignedResources) {
        this.assignedResources = assignedResources;
    }

    public long getBusyResources() {
        return busyResources;
    }

    public void setBusyResources(long busyResources) {
        this.busyResources = busyResources;
    }

    public long getActiveResponders() {
        return activeResponders;
    }

    public void setActiveResponders(long activeResponders) {
        this.activeResponders = activeResponders;
    }

    public double getAvgResponseTimeMinutes() {
        return avgResponseTimeMinutes;
    }

    public void setAvgResponseTimeMinutes(double avgResponseTimeMinutes) {
        this.avgResponseTimeMinutes = avgResponseTimeMinutes;
    }

    public double getAvgResolutionTimeMinutes() {
        return avgResolutionTimeMinutes;
    }

    public void setAvgResolutionTimeMinutes(double avgResolutionTimeMinutes) {
        this.avgResolutionTimeMinutes = avgResolutionTimeMinutes;
    }

    public Map<String, Long> getIncidentsByType() {
        return incidentsByType;
    }

    public void setIncidentsByType(Map<String, Long> incidentsByType) {
        this.incidentsByType = incidentsByType;
    }

    public Map<String, Long> getIncidentsBySeverity() {
        return incidentsBySeverity;
    }

    public void setIncidentsBySeverity(Map<String, Long> incidentsBySeverity) {
        this.incidentsBySeverity = incidentsBySeverity;
    }

    public Map<String, Long> getResourcesByType() {
        return resourcesByType;
    }

    public void setResourcesByType(Map<String, Long> resourcesByType) {
        this.resourcesByType = resourcesByType;
    }
}
