package com.resqcloud.resource.service;

import com.resqcloud.common.exception.ResourceNotFoundException;
import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.repository.IncidentRepository;
import com.resqcloud.resource.dto.ResourceMatchRecommendation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ResourceMatchingService {

    private final IncidentRepository incidentRepository;
    private final ResourceMatchingStrategy matchingStrategy;

    public ResourceMatchingService(IncidentRepository incidentRepository, ResourceMatchingStrategy matchingStrategy) {
        this.incidentRepository = incidentRepository;
        this.matchingStrategy = matchingStrategy;
    }

    @Transactional(readOnly = true)
    public List<ResourceMatchRecommendation> getRecommendationsForIncident(UUID incidentId, int limit) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));

        return matchingStrategy.matchResources(incident, limit);
    }

    public List<ResourceMatchRecommendation> getRecommendationsForIncident(Incident incident, int limit) {
        return matchingStrategy.matchResources(incident, limit);
    }
}
