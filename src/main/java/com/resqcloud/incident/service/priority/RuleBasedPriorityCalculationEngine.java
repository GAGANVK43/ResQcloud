package com.resqcloud.incident.service.priority;

import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentSeverity;
import com.resqcloud.incident.entity.IncidentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RuleBasedPriorityCalculationEngine implements PriorityCalculationStrategy {

    private final double severityWeight;
    private final double affectedPeopleWeight;
    private final double incidentTypeWeight;
    private final double timeSensitivityWeight;
    private final double resourceAvailabilityWeight;

    private final double lowMax;
    private final double mediumMax;
    private final double highMax;

    public RuleBasedPriorityCalculationEngine(
            @Value("${app.priority.weights.severity:35.0}") double severityWeight,
            @Value("${app.priority.weights.affected-people:25.0}") double affectedPeopleWeight,
            @Value("${app.priority.weights.incident-type:20.0}") double incidentTypeWeight,
            @Value("${app.priority.weights.time-sensitivity:10.0}") double timeSensitivityWeight,
            @Value("${app.priority.weights.resource-availability:10.0}") double resourceAvailabilityWeight,
            @Value("${app.priority.thresholds.low-max:30.0}") double lowMax,
            @Value("${app.priority.thresholds.medium-max:60.0}") double mediumMax,
            @Value("${app.priority.thresholds.high-max:80.0}") double highMax) {
        this.severityWeight = severityWeight;
        this.affectedPeopleWeight = affectedPeopleWeight;
        this.incidentTypeWeight = incidentTypeWeight;
        this.timeSensitivityWeight = timeSensitivityWeight;
        this.resourceAvailabilityWeight = resourceAvailabilityWeight;
        this.lowMax = lowMax;
        this.mediumMax = mediumMax;
        this.highMax = highMax;
    }

    @Override
    public PriorityScoreResult calculatePriority(Incident incident, IncidentPriorityContext context) {
        Map<String, Double> factorBreakdown = new LinkedHashMap<>();

        // 1. Severity Score (max = severityWeight, e.g. 35)
        double severityFactor = computeSeverityFactor(incident.getSeverity());
        double severityScore = Math.round(severityFactor * severityWeight * 100.0) / 100.0;
        factorBreakdown.put("severityScore", severityScore);

        // 2. Affected People Score (max = affectedPeopleWeight, e.g. 25)
        double affectedPeopleFactor = computeAffectedPeopleFactor(incident.getAffectedPeople());
        double affectedPeopleScore = Math.round(affectedPeopleFactor * affectedPeopleWeight * 100.0) / 100.0;
        factorBreakdown.put("affectedPeopleScore", affectedPeopleScore);

        // 3. Incident Type Score (max = incidentTypeWeight, e.g. 20)
        double typeFactor = computeTypeFactor(incident.getIncidentType());
        double typeScore = Math.round(typeFactor * incidentTypeWeight * 100.0) / 100.0;
        factorBreakdown.put("incidentTypeScore", typeScore);

        // 4. Time Sensitivity Score (max = timeSensitivityWeight, e.g. 10)
        long minutes = context != null ? context.getMinutesSinceReported() : 0;
        double timeFactor = Math.min(1.0, minutes / 60.0); // scales up to 1 hr
        double timeScore = Math.round(timeFactor * timeSensitivityWeight * 100.0) / 100.0;
        factorBreakdown.put("timeSensitivityScore", timeScore);

        // 5. Resource Availability / Location Risk (max = resourceAvailabilityWeight, e.g. 10)
        double resourceRiskFactor = 0.5;
        if (context != null) {
            // Lower availability -> higher urgency
            double availFactor = context.getAvailableMatchingResources() == 0 ? 1.0 :
                    Math.max(0.0, 1.0 - (context.getAvailableMatchingResources() * 0.25));
            resourceRiskFactor = (availFactor * 0.5) + (context.getLocationRiskFactor() * 0.5);
        }
        double resourceScore = Math.round(resourceRiskFactor * resourceAvailabilityWeight * 100.0) / 100.0;
        factorBreakdown.put("resourceAvailabilityScore", resourceScore);

        // Total normalized composite score: 0 to 100
        double totalScore = Math.min(100.0, Math.max(0.0, severityScore + affectedPeopleScore + typeScore + timeScore + resourceScore));
        totalScore = Math.round(totalScore * 10.0) / 10.0;

        String level = determinePriorityLevel(totalScore);

        return new PriorityScoreResult(totalScore, level, factorBreakdown, getStrategyName());
    }

    private double computeSeverityFactor(IncidentSeverity severity) {
        if (severity == null) return 0.2;
        return switch (severity) {
            case LOW -> 0.25;
            case MEDIUM -> 0.55;
            case HIGH -> 0.85;
            case CRITICAL -> 1.0;
        };
    }

    private double computeAffectedPeopleFactor(int people) {
        if (people <= 0) return 0.1;
        if (people == 1) return 0.4;
        if (people <= 4) return 0.7;
        if (people <= 10) return 0.9;
        return 1.0;
    }

    private double computeTypeFactor(IncidentType type) {
        if (type == null) return 0.4;
        return switch (type) {
            case FIRE -> 1.0;
            case BUILDING_EMERGENCY -> 0.95;
            case FLOOD -> 0.90;
            case MEDICAL -> 0.85;
            case ACCIDENT -> 0.75;
            case MISSING_PERSON -> 0.65;
            case OTHER -> 0.40;
        };
    }

    public String determinePriorityLevel(double score) {
        if (score <= lowMax) {
            return "LOW";
        } else if (score <= mediumMax) {
            return "MEDIUM";
        } else if (score <= highMax) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }

    @Override
    public String getStrategyName() {
        return "RuleBasedMultiFactorEngine";
    }
}
