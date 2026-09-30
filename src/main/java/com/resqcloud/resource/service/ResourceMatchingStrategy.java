package com.resqcloud.resource.service;

import com.resqcloud.incident.entity.Incident;
import com.resqcloud.resource.dto.ResourceMatchRecommendation;

import java.util.List;

public interface ResourceMatchingStrategy {
    List<ResourceMatchRecommendation> matchResources(Incident incident, int limit);
}
