package com.resqcloud.incident.service.priority;

import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentSeverity;
import com.resqcloud.incident.entity.IncidentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedPriorityCalculationEngineTest {

    private RuleBasedPriorityCalculationEngine priorityEngine;

    @BeforeEach
    void setUp() {
        // Initialize engine with standard default weights
        priorityEngine = new RuleBasedPriorityCalculationEngine(
                35.0, 25.0, 20.0, 10.0, 10.0,
                30.0, 60.0, 80.0
        );
    }

    @Test
    @DisplayName("Should evaluate Low Severity incident with 0 affected people as LOW or LOW-MEDIUM priority")
    void testLowSeverityIncident() {
        Incident incident = new Incident();
        incident.setSeverity(IncidentSeverity.LOW);
        incident.setIncidentType(IncidentType.OTHER);
        incident.setAffectedPeople(0);

        IncidentPriorityContext context = new IncidentPriorityContext(10, 0.2, 0);

        PriorityScoreResult result = priorityEngine.calculatePriority(incident, context);

        assertNotNull(result);
        assertTrue(result.getTotalScore() <= 40.0, "Score should be low for minor other incident");
        assertTrue(result.getFactorBreakdown().containsKey("severityScore"));
        assertTrue(result.getFactorBreakdown().containsKey("affectedPeopleScore"));
        assertEquals("RuleBasedMultiFactorEngine", result.getCalculationModel());
    }

    @Test
    @DisplayName("Should evaluate Critical Severity Fire with many affected people as CRITICAL priority")
    void testCriticalSeverityIncident() {
        Incident incident = new Incident();
        incident.setSeverity(IncidentSeverity.CRITICAL);
        incident.setIncidentType(IncidentType.FIRE);
        incident.setAffectedPeople(15);

        IncidentPriorityContext context = new IncidentPriorityContext(0, 0.9, 30);

        PriorityScoreResult result = priorityEngine.calculatePriority(incident, context);

        assertNotNull(result);
        assertTrue(result.getTotalScore() >= 80.0, "Score should be >= 80 for critical multi-victim fire");
        assertEquals("CRITICAL", result.getPriorityLevel());
    }

    @Test
    @DisplayName("Should verify boundary threshold mappings")
    void testBoundaryThresholds() {
        assertEquals("LOW", priorityEngine.determinePriorityLevel(30.0));
        assertEquals("MEDIUM", priorityEngine.determinePriorityLevel(30.1));
        assertEquals("MEDIUM", priorityEngine.determinePriorityLevel(60.0));
        assertEquals("HIGH", priorityEngine.determinePriorityLevel(60.1));
        assertEquals("HIGH", priorityEngine.determinePriorityLevel(80.0));
        assertEquals("CRITICAL", priorityEngine.determinePriorityLevel(80.1));
        assertEquals("CRITICAL", priorityEngine.determinePriorityLevel(100.0));
    }
}
