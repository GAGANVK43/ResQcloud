package com.resqcloud.resource.dto;

import com.resqcloud.resource.entity.Resource;

public class ResourceMatchRecommendation {
    private Resource resource;
    private double distanceKm;
    private double matchScore; // 0 to 100
    private boolean capabilityMatch;
    private String rationale;

    public ResourceMatchRecommendation() {}

    public ResourceMatchRecommendation(Resource resource, double distanceKm, double matchScore, boolean capabilityMatch, String rationale) {
        this.resource = resource;
        this.distanceKm = distanceKm;
        this.matchScore = matchScore;
        this.capabilityMatch = capabilityMatch;
        this.rationale = rationale;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }

    public boolean isCapabilityMatch() {
        return capabilityMatch;
    }

    public void setCapabilityMatch(boolean capabilityMatch) {
        this.capabilityMatch = capabilityMatch;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
