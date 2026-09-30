package com.resqcloud.incident.service.priority;

import java.util.Map;

public class PriorityScoreResult {
    private double totalScore;
    private String priorityLevel;
    private Map<String, Double> factorBreakdown;
    private String calculationModel;

    public PriorityScoreResult() {}

    public PriorityScoreResult(double totalScore, String priorityLevel, Map<String, Double> factorBreakdown, String calculationModel) {
        this.totalScore = totalScore;
        this.priorityLevel = priorityLevel;
        this.factorBreakdown = factorBreakdown;
        this.calculationModel = calculationModel;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public Map<String, Double> getFactorBreakdown() {
        return factorBreakdown;
    }

    public void setFactorBreakdown(Map<String, Double> factorBreakdown) {
        this.factorBreakdown = factorBreakdown;
    }

    public String getCalculationModel() {
        return calculationModel;
    }

    public void setCalculationModel(String calculationModel) {
        this.calculationModel = calculationModel;
    }
}
