package com.resqcloud.resource.service;

import com.resqcloud.common.utils.GeoLocationUtils;
import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentType;
import com.resqcloud.resource.dto.ResourceMatchRecommendation;
import com.resqcloud.resource.entity.Resource;
import com.resqcloud.resource.entity.ResourceStatus;
import com.resqcloud.resource.entity.ResourceType;
import com.resqcloud.resource.repository.ResourceAssignmentRepository;
import com.resqcloud.resource.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HeuristicResourceMatchingEngine implements ResourceMatchingStrategy {

    private final ResourceRepository resourceRepository;
    private final ResourceAssignmentRepository assignmentRepository;
    private final double maxRadiusKm;
    private final double distanceWeight;
    private final double capabilityWeight;
    private final double workloadWeight;

    private static final Map<IncidentType, Set<ResourceType>> PRIMARY_CAPABILITY_MAP = new EnumMap<>(IncidentType.class);
    private static final Map<IncidentType, Set<ResourceType>> SECONDARY_CAPABILITY_MAP = new EnumMap<>(IncidentType.class);

    static {
        PRIMARY_CAPABILITY_MAP.put(IncidentType.FIRE, EnumSet.of(ResourceType.FIRE_ENGINE, ResourceType.FIREFIGHTERS));
        SECONDARY_CAPABILITY_MAP.put(IncidentType.FIRE, EnumSet.of(ResourceType.RESCUE_VEHICLE, ResourceType.AMBULANCE));

        PRIMARY_CAPABILITY_MAP.put(IncidentType.MEDICAL, EnumSet.of(ResourceType.AMBULANCE, ResourceType.MEDICAL_TEAM));
        SECONDARY_CAPABILITY_MAP.put(IncidentType.MEDICAL, EnumSet.of(ResourceType.VOLUNTEER_TEAM));

        PRIMARY_CAPABILITY_MAP.put(IncidentType.ACCIDENT, EnumSet.of(ResourceType.AMBULANCE, ResourceType.RESCUE_VEHICLE));
        SECONDARY_CAPABILITY_MAP.put(IncidentType.ACCIDENT, EnumSet.of(ResourceType.POLICE_TEAM, ResourceType.FIREFIGHTERS));

        PRIMARY_CAPABILITY_MAP.put(IncidentType.FLOOD, EnumSet.of(ResourceType.RESCUE_VEHICLE, ResourceType.VOLUNTEER_TEAM));
        SECONDARY_CAPABILITY_MAP.put(IncidentType.FLOOD, EnumSet.of(ResourceType.FIRE_ENGINE, ResourceType.MEDICAL_TEAM));

        PRIMARY_CAPABILITY_MAP.put(IncidentType.BUILDING_EMERGENCY, EnumSet.of(ResourceType.FIRE_ENGINE, ResourceType.RESCUE_VEHICLE));
        SECONDARY_CAPABILITY_MAP.put(IncidentType.BUILDING_EMERGENCY, EnumSet.of(ResourceType.FIREFIGHTERS, ResourceType.AMBULANCE));

        PRIMARY_CAPABILITY_MAP.put(IncidentType.MISSING_PERSON, EnumSet.of(ResourceType.POLICE_TEAM, ResourceType.VOLUNTEER_TEAM));
        SECONDARY_CAPABILITY_MAP.put(IncidentType.MISSING_PERSON, EnumSet.of(ResourceType.RESCUE_VEHICLE));

        PRIMARY_CAPABILITY_MAP.put(IncidentType.OTHER, EnumSet.allOf(ResourceType.class));
        SECONDARY_CAPABILITY_MAP.put(IncidentType.OTHER, EnumSet.noneOf(ResourceType.class));
    }

    public HeuristicResourceMatchingEngine(
            ResourceRepository resourceRepository,
            ResourceAssignmentRepository assignmentRepository,
            @Value("${app.matching.max-search-radius-km:50.0}") double maxRadiusKm,
            @Value("${app.matching.distance-weight:0.35}") double distanceWeight,
            @Value("${app.matching.capability-weight:0.40}") double capabilityWeight,
            @Value("${app.matching.workload-weight:0.25}") double workloadWeight) {
        this.resourceRepository = resourceRepository;
        this.assignmentRepository = assignmentRepository;
        this.maxRadiusKm = maxRadiusKm;
        this.distanceWeight = distanceWeight;
        this.capabilityWeight = capabilityWeight;
        this.workloadWeight = workloadWeight;
    }

    @Override
    public List<ResourceMatchRecommendation> matchResources(Incident incident, int limit) {
        // Query candidate resources (must be AVAILABLE)
        List<Resource> candidates = resourceRepository.findByStatus(ResourceStatus.AVAILABLE);
        List<ResourceMatchRecommendation> recommendations = new ArrayList<>();

        for (Resource resource : candidates) {
            double distanceKm = GeoLocationUtils.calculateDistanceKm(
                    incident.getLatitude(), incident.getLongitude(),
                    resource.getLatitude(), resource.getLongitude()
            );

            // Compute Capability Score
            Set<ResourceType> primary = PRIMARY_CAPABILITY_MAP.getOrDefault(incident.getIncidentType(), Collections.emptySet());
            Set<ResourceType> secondary = SECONDARY_CAPABILITY_MAP.getOrDefault(incident.getIncidentType(), Collections.emptySet());

            double capScore;
            boolean isCapabilityMatch = false;
            String rationaleText;

            if (primary.contains(resource.getResourceType())) {
                capScore = 100.0;
                isCapabilityMatch = true;
                rationaleText = "Primary capability match (" + resource.getResourceType() + " for " + incident.getIncidentType() + ")";
            } else if (secondary.contains(resource.getResourceType())) {
                capScore = 70.0;
                isCapabilityMatch = true;
                rationaleText = "Secondary support capability (" + resource.getResourceType() + " for " + incident.getIncidentType() + ")";
            } else {
                capScore = 30.0;
                rationaleText = "Generic backup resource";
            }

            // Compute Distance Score (100 at 0km, decreasing with distance)
            double distScore = Math.max(0.0, 100.0 - (distanceKm * 2.0));

            // Workload factor
            long activeAssignments = assignmentRepository.countActiveAssignmentsForResource(resource.getId());
            double workloadScore = Math.max(0.0, 100.0 - (activeAssignments * 50.0));

            // Composite Match Score
            double totalScore = (capScore * capabilityWeight) + (distScore * distanceWeight) + (workloadScore * workloadWeight);
            totalScore = Math.round(totalScore * 10.0) / 10.0;

            String fullRationale = String.format("%s, %.1f km away, capacity: %d (Suitability: %.1f/100)",
                    rationaleText, distanceKm, resource.getCapacity(), totalScore);

            recommendations.add(new ResourceMatchRecommendation(resource, distanceKm, totalScore, isCapabilityMatch, fullRationale));
        }

        // Sort descending by match score, then ascending by distance
        recommendations.sort(Comparator
                .comparingDouble(ResourceMatchRecommendation::getMatchScore).reversed()
                .thenComparingDouble(ResourceMatchRecommendation::getDistanceKm));

        return recommendations.stream().limit(limit > 0 ? limit : 5).toList();
    }
}
