package com.resqcloud.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ResponderStatusUpdateRequest {
    @NotBlank(message = "Availability status is required")
    @Pattern(regexp = "AVAILABLE|BUSY|OFFLINE", message = "Status must be AVAILABLE, BUSY, or OFFLINE")
    private String availabilityStatus;

    private Double currentLatitude;
    private Double currentLongitude;

    public ResponderStatusUpdateRequest() {}

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public Double getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(Double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public Double getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(Double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }
}
