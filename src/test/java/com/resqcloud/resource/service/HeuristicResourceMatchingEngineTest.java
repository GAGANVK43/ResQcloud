package com.resqcloud.resource.service;

import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentSeverity;
import com.resqcloud.incident.entity.IncidentType;
import com.resqcloud.resource.dto.ResourceMatchRecommendation;
import com.resqcloud.resource.entity.Resource;
import com.resqcloud.resource.entity.ResourceStatus;
import com.resqcloud.resource.entity.ResourceType;
import com.resqcloud.resource.repository.ResourceAssignmentRepository;
import com.resqcloud.resource.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeuristicResourceMatchingEngineTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceAssignmentRepository assignmentRepository;

    private HeuristicResourceMatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        matchingEngine = new HeuristicResourceMatchingEngine(
                resourceRepository, assignmentRepository, 50.0, 0.35, 0.40, 0.25
        );
    }

    @Test
    @DisplayName("Should prioritize nearest Ambulance for a Medical emergency")
    void testNearestMatchingResourcePrioritized() {
        Incident medicalIncident = new Incident();
        medicalIncident.setIncidentType(IncidentType.MEDICAL);
        medicalIncident.setSeverity(IncidentSeverity.HIGH);
        medicalIncident.setLatitude(37.7749);
        medicalIncident.setLongitude(-122.4194);

        // Resource 1: Nearby Ambulance (0.5 km away)
        Resource nearAmbulance = new Resource("Near Ambulance", ResourceType.AMBULANCE, ResourceStatus.AVAILABLE,
                37.7750, -122.4200, "Downtown", 2, "+15551234");
        nearAmbulance.setId(UUID.randomUUID());

        // Resource 2: Far Ambulance (15 km away)
        Resource farAmbulance = new Resource("Far Ambulance", ResourceType.AMBULANCE, ResourceStatus.AVAILABLE,
                37.6500, -122.4194, "Suburbs", 2, "+15555678");
        farAmbulance.setId(UUID.randomUUID());

        // Resource 3: Fire Engine (Nearby, but secondary capability)
        Resource nearFireEngine = new Resource("Near Engine", ResourceType.FIRE_ENGINE, ResourceStatus.AVAILABLE,
                37.7751, -122.4190, "Downtown Station", 6, "+15559999");
        nearFireEngine.setId(UUID.randomUUID());

        when(resourceRepository.findByStatus(ResourceStatus.AVAILABLE))
                .thenReturn(List.of(nearAmbulance, farAmbulance, nearFireEngine));
        when(assignmentRepository.countActiveAssignmentsForResource(any())).thenReturn(0L);

        List<ResourceMatchRecommendation> matches = matchingEngine.matchResources(medicalIncident, 5);

        assertNotNull(matches);
        assertFalse(matches.isEmpty());
        // Near ambulance should rank #1 because it has primary capability match + shortest distance
        assertEquals(nearAmbulance.getName(), matches.get(0).getResource().getName());
        assertTrue(matches.get(0).isCapabilityMatch());
        assertTrue(matches.get(0).getMatchScore() > matches.get(1).getMatchScore());
    }

    @Test
    @DisplayName("Should return empty list when no resources are available")
    void testNoResourcesAvailable() {
        Incident incident = new Incident();
        incident.setIncidentType(IncidentType.FIRE);
        incident.setLatitude(37.7749);
        incident.setLongitude(-122.4194);

        when(resourceRepository.findByStatus(ResourceStatus.AVAILABLE)).thenReturn(List.of());

        List<ResourceMatchRecommendation> matches = matchingEngine.matchResources(incident, 5);
        assertNotNull(matches);
        assertTrue(matches.isEmpty());
    }
}
