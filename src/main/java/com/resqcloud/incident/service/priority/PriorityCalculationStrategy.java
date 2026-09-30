package com.resqcloud.incident.service.priority;

import com.resqcloud.incident.entity.Incident;

/**
 * Strategy interface for incident priority scoring.
 * Allows switching between rule-based priority engine and future ML-based priority models.
 */
public interface PriorityCalculationStrategy {

    PriorityScoreResult calculatePriority(Incident incident, IncidentPriorityContext context);

    String getStrategyName();
}
