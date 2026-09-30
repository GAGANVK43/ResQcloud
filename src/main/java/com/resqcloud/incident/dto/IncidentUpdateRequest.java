package com.resqcloud.incident.dto;

import com.resqcloud.incident.entity.IncidentSeverity;
import com.resqcloud.incident.entity.IncidentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class IncidentUpdateRequest {

    @Size(max = 200)
    private String title;

    private String description;

    private IncidentType incidentType;

    private IncidentSeverity severity;

    @Min(value = -90)
    @Max(value = 90)
    private Double latitude;

    @Min(value = -180)
    @Max(value = 180)
    private Double longitude;

    @Size(max = 255)
    private String address;

    @Min(value = 0)
    private Integer affectedPeople;

    public IncidentUpdateRequest() {}

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

    public Integer getAffectedPeople() {
        return affectedPeople;
    }

    public void setAffectedPeople(Integer affectedPeople) {
        this.affectedPeople = affectedPeople;
    }
}
