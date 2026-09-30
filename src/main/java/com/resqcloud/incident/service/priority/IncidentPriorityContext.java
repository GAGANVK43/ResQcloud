package com.resqcloud.incident.service.priority;

public class IncidentPriorityContext {
    private long availableMatchingResources;
    private double locationRiskFactor; // 0.0 to 1.0 (e.g. dense urban / hospital zone / hazardous facility)
    private long minutesSinceReported;

    public IncidentPriorityContext() {
        this.availableMatchingResources = 1;
        this.locationRiskFactor = 0.5;
        this.minutesSinceReported = 0;
    }

    public IncidentPriorityContext(long availableMatchingResources, double locationRiskFactor, long minutesSinceReported) {
        this.availableMatchingResources = availableMatchingResources;
        this.locationRiskFactor = locationRiskFactor;
        this.minutesSinceReported = minutesSinceReported;
    }

    public long getAvailableMatchingResources() {
        return availableMatchingResources;
    }

    public void setAvailableMatchingResources(long availableMatchingResources) {
        this.availableMatchingResources = availableMatchingResources;
    }

    public double getLocationRiskFactor() {
        return locationRiskFactor;
    }

    public void setLocationRiskFactor(double locationRiskFactor) {
        this.locationRiskFactor = locationRiskFactor;
    }

    public long getMinutesSinceReported() {
        return minutesSinceReported;
    }

    public void setMinutesSinceReported(long minutesSinceReported) {
        this.minutesSinceReported = minutesSinceReported;
    }
}
