package com.attendance.service;

import com.attendance.model.Attendance;
import com.attendance.model.AttendanceCalculatorResult;
import com.attendance.model.StudentAttendanceSummary;

import java.util.List;

/**
 * Service for Attendance Shortage Calculation and Rule-Based Risk Prediction.
 */
public class AttendanceCalculatorService {

    /**
     * Calculates the exact consecutive classes needed to reach required attendance percentage.
     * Formula: x = ceil((R * T - A) / (1 - R))
     *
     * @param attendedClasses   Classes attended so far (A)
     * @param totalConducted    Total classes conducted so far (T)
     * @param requiredPct       Required attendance percentage (e.g. 75.0)
     * @return Number of consecutive classes to attend with 0 absences (0 if already meeting requirement)
     */
    public int calculateClassesNeeded(int attendedClasses, int totalConducted, double requiredPct) {
        if (totalConducted <= 0) return 0;
        double currentPct = ((double) attendedClasses / totalConducted) * 100.0;
        if (currentPct >= requiredPct) {
            return 0;
        }

        double r = requiredPct / 100.0;
        if (r >= 1.0) {
            // Cannot reach 100% if already missed 1 class
            return (totalConducted - attendedClasses > 0) ? -1 : 0;
        }

        double numerator = (r * totalConducted) - attendedClasses;
        double denominator = 1.0 - r;
        double needed = numerator / denominator;

        return (int) Math.ceil(needed);
    }

    /**
     * Calculates the maximum number of upcoming classes a student can miss while maintaining required attendance.
     * Formula: m = floor((A - R * T) / R)
     *
     * @param attendedClasses Classes attended so far (A)
     * @param totalConducted  Total classes conducted so far (T)
     * @param requiredPct     Required attendance percentage (e.g. 75.0)
     * @return Number of classes that can be missed (0 if current attendance is below required)
     */
    public int calculateMaxMissableClasses(int attendedClasses, int totalConducted, double requiredPct) {
        if (totalConducted <= 0 || attendedClasses <= 0) return 0;
        double r = requiredPct / 100.0;
        if (r <= 0.0) return 0;
        if (attendedClasses < r * totalConducted) {
            return 0; // Already below required percentage
        }
        double numerator = attendedClasses - (r * totalConducted);
        double missable = numerator / r;
        return Math.max(0, (int) Math.floor(missable));
    }

    /**
     * Calculates shortage result with classes needed, missable classes, risk, and advice.
     */
    public AttendanceCalculatorResult calculateShortage(int attended, int conducted, double requiredPct) {
        AttendanceCalculatorResult res = new AttendanceCalculatorResult();
        res.setTotalAttended(attended);
        res.setTotalConducted(conducted);
        res.setRequiredPercentage(requiredPct);
        double pct = conducted > 0 ? ((double) attended / conducted) * 100.0 : 100.0;
        res.setCurrentPercentage(pct);
        int needed = calculateClassesNeeded(attended, conducted, requiredPct);
        int missable = calculateMaxMissableClasses(attended, conducted, requiredPct);
        res.setClassesNeededToReachRequired(needed);
        res.setMaxClassesCanBeMissed(missable);
        res.setMeetingRequirement(pct >= requiredPct);
        if (pct >= requiredPct) {
            res.setRiskCategory("LOW RISK");
            res.setRecommendation("Currently meeting the " + String.format("%.0f%%", requiredPct) + " criteria. You can miss up to " + missable + " classes safely.");
        } else {
            res.setRiskCategory("HIGH RISK");
            res.setRecommendation("Attendance shortage! Attend next " + Math.max(1, needed) + " consecutive classes without absence to reach " + String.format("%.0f%%", requiredPct) + ".");
        }
        return res;
    }

    /**
     * Evaluates full calculator result including risk classification and trend analysis.
     */
    public AttendanceCalculatorResult evaluateStudent(StudentAttendanceSummary summary,
                                                      double requiredPct,
                                                      List<Attendance> recentHistory) {
        AttendanceCalculatorResult result = new AttendanceCalculatorResult();
        result.setStudentId(summary.getStudentId());
        result.setRegisterNo(summary.getRegisterNo());
        result.setStudentName(summary.getStudentName());
        result.setDepartment(summary.getDepartment());
        result.setTotalConducted(summary.getTotalWorkingDays());
        result.setTotalAttended(summary.getPresentDays());
        result.setCurrentPercentage(summary.getAttendancePercentage());
        result.setRequiredPercentage(requiredPct);

        // 1. Shortage & Margin Calculation
        int needed = calculateClassesNeeded(summary.getPresentDays(), summary.getTotalWorkingDays(), requiredPct);
        int missable = calculateMaxMissableClasses(summary.getPresentDays(), summary.getTotalWorkingDays(), requiredPct);
        result.setClassesNeededToReachRequired(needed);
        result.setMaxClassesCanBeMissed(missable);
        result.setMeetingRequirement(summary.getAttendancePercentage() >= requiredPct);

        // 2. Risk Classification
        double pct = summary.getAttendancePercentage();
        if (pct >= 85.0) {
            result.setRiskCategory("LOW RISK");
            result.setRecommendation("Attendance is healthy. Maintain regular attendance.");
        } else if (pct >= 75.0) {
            result.setRiskCategory("MEDIUM RISK");
            result.setRecommendation("Above the mandatory 75% cutoff, but caution is advised to prevent shortages.");
        } else if (pct >= 65.0) {
            result.setRiskCategory("HIGH RISK");
            result.setRecommendation(String.format("Attendance shortage alert! Attend the next %d classes consecutively to reach 75%%.", Math.max(1, needed)));
        } else {
            result.setRiskCategory("CRITICAL");
            result.setRecommendation(String.format("Severe attendance shortage! Immediate academic counseling required. Need %d consecutive classes to reach 75%%.", Math.max(1, needed)));
        }

        // 3. Decline Trend Detection (analyzing recent vs older attendance)
        if (recentHistory != null && recentHistory.size() >= 10) {
            int recentSize = Math.min(10, recentHistory.size());
            int recentPresent = 0;
            for (int i = 0; i < recentSize; i++) {
                if (recentHistory.get(i).isPresent()) recentPresent++;
            }
            double recentRate = ((double) recentPresent / recentSize) * 100.0;
            if (recentRate < (pct - 10.0)) {
                result.setDecliningTrend(true);
                result.setRecommendation(result.getRecommendation() + " (⚠ Warning: Attendance has dropped significantly over recent sessions)");
            }
        }

        return result;
    }
}
