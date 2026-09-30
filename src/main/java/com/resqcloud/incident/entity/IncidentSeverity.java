package com.resqcloud.incident.entity;

public enum IncidentSeverity {
    LOW(10),
    MEDIUM(20),
    HIGH(30),
    CRITICAL(40);

    private final int baseWeight;

    IncidentSeverity(int baseWeight) {
        this.baseWeight = baseWeight;
    }

    public int getBaseWeight() {
        return baseWeight;
    }
}
