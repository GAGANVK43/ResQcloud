package com.resqcloud.resource.dto;

import com.resqcloud.resource.entity.Resource;
import com.resqcloud.resource.entity.ResourceStatus;
import com.resqcloud.resource.entity.ResourceType;
import java.time.Instant;
import java.util.UUID;

public class ResourceResponse {
    private UUID id;
    private String name;
    private ResourceType resourceType;
    private ResourceStatus status;
    private Double latitude;
    private Double longitude;
    private String address;
    private int capacity;
    private String contactNumber;
    private Instant createdAt;
    private Instant updatedAt;

    public ResourceResponse() {}

    public static ResourceResponse fromEntity(Resource resource) {
        ResourceResponse res = new ResourceResponse();
        res.setId(resource.getId());
        res.setName(resource.getName());
        res.setResourceType(resource.getResourceType());
        res.setStatus(resource.getStatus());
        res.setLatitude(resource.getLatitude());
        res.setLongitude(resource.getLongitude());
        res.setAddress(resource.getAddress());
        res.setCapacity(resource.getCapacity());
        res.setContactNumber(resource.getContactNumber());
        res.setCreatedAt(resource.getCreatedAt());
        res.setUpdatedAt(resource.getUpdatedAt());
        return res;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
