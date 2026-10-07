package com.attendance.model;

/**
 * Result model for Attendance Shortage Calculation and Rule-based Risk Prediction.
 */
public class AttendanceCalculatorResult {
    private int studentId;
    private String registerNo;
    private String studentName;
    private String department;
    private int totalConducted;
    private int totalAttended;
    private double currentPercentage;
    private double requiredPercentage = 75.0;

    // Shortage Calculation
    private int classesNeededToReachRequired; // Consecutive classes with 0 absences to hit 75%
    private boolean meetingRequirement;

    // Rule-Based Prediction Risk
    // LOW RISK (>=85%), MEDIUM RISK (75%-84.99%), HIGH RISK (65%-74.99%), CRITICAL (<65%)
    private String riskCategory; // "LOW RISK", "MEDIUM RISK", "HIGH RISK", "CRITICAL"
    private boolean decliningTrend; // true if recent weeks have shown significant attendance drop
    private String recommendation;

    // Maximum classes that can be missed while maintaining required percentage: floor((A - R*T) / R)
    private int maxClassesCanBeMissed;

    public AttendanceCalculatorResult() {}

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getRegisterNo() { return registerNo; }
    public void setRegisterNo(String registerNo) { this.registerNo = registerNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getTotalConducted() { return totalConducted; }
    public void setTotalConducted(int totalConducted) { this.totalConducted = totalConducted; }

    public int getTotalAttended() { return totalAttended; }
    public void setTotalAttended(int totalAttended) { this.totalAttended = totalAttended; }

    public double getCurrentPercentage() { return currentPercentage; }
    public void setCurrentPercentage(double currentPercentage) { this.currentPercentage = currentPercentage; }

    public double getRequiredPercentage() { return requiredPercentage; }
    public void setRequiredPercentage(double requiredPercentage) { this.requiredPercentage = requiredPercentage; }

    public int getClassesNeededToReachRequired() { return classesNeededToReachRequired; }
    public void setClassesNeededToReachRequired(int classesNeededToReachRequired) { this.classesNeededToReachRequired = classesNeededToReachRequired; }

    public int getMaxClassesCanBeMissed() { return maxClassesCanBeMissed; }
    public void setMaxClassesCanBeMissed(int maxClassesCanBeMissed) { this.maxClassesCanBeMissed = maxClassesCanBeMissed; }

    public boolean isMeetingRequirement() { return meetingRequirement; }
    public void setMeetingRequirement(boolean meetingRequirement) { this.meetingRequirement = meetingRequirement; }

    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }

    public boolean isDecliningTrend() { return decliningTrend; }
    public void setDecliningTrend(boolean decliningTrend) { this.decliningTrend = decliningTrend; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public boolean isShortage() {
        return !meetingRequirement || classesNeededToReachRequired > 0;
    }

    public String getAdvice() {
        if (recommendation != null && !recommendation.trim().isEmpty()) {
            return recommendation;
        }
        if (isShortage()) {
            return "Shortage: Attend next " + classesNeededToReachRequired + " consecutive classes to reach " + String.format("%.0f%%", requiredPercentage) + ".";
        } else {
            return "In Good Standing. You can miss up to " + maxClassesCanBeMissed + " classes while staying at or above " + String.format("%.0f%%", requiredPercentage) + ".";
        }
    }
}
