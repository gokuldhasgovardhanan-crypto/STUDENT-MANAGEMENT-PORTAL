package com.attendance.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model encapsulating dynamically calculated metrics for the executive dashboard.
 */
public class DashboardStats {
    private int totalStudents;
    private int presentToday;
    private int absentToday;
    private int attendanceNotMarked;
    private double averageAttendancePct;
    private int studentsBelowThresholdCount;
    private double requiredAttendancePct;
    private String collegeName;
    private String academicYear;
    private String semester;
    private LocalDate todayDate;

    private List<StudentAttendanceSummary> lowAttendanceStudents;
    private Map<String, Double> departmentAttendance = new HashMap<>();

    public DashboardStats() {}

    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }

    public int getPresentToday() { return presentToday; }
    public void setPresentToday(int presentToday) { this.presentToday = presentToday; }

    public int getAbsentToday() { return absentToday; }
    public void setAbsentToday(int absentToday) { this.absentToday = absentToday; }

    public int getAttendanceNotMarked() { return attendanceNotMarked; }
    public void setAttendanceNotMarked(int attendanceNotMarked) { this.attendanceNotMarked = attendanceNotMarked; }

    public double getAverageAttendancePct() { return averageAttendancePct; }
    public void setAverageAttendancePct(double averageAttendancePct) { this.averageAttendancePct = averageAttendancePct; }

    public int getStudentsBelowThresholdCount() { return studentsBelowThresholdCount; }
    public void setStudentsBelowThresholdCount(int studentsBelowThresholdCount) { this.studentsBelowThresholdCount = studentsBelowThresholdCount; }

    public double getRequiredAttendancePct() { return requiredAttendancePct; }
    public void setRequiredAttendancePct(double requiredAttendancePct) { this.requiredAttendancePct = requiredAttendancePct; }

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public LocalDate getTodayDate() { return todayDate; }
    public void setTodayDate(LocalDate todayDate) { this.todayDate = todayDate; }

    public List<StudentAttendanceSummary> getLowAttendanceStudents() { return lowAttendanceStudents; }
    public void setLowAttendanceStudents(List<StudentAttendanceSummary> lowAttendanceStudents) { this.lowAttendanceStudents = lowAttendanceStudents; }

    public Map<String, Double> getDepartmentAttendance() { return departmentAttendance; }
    public void setDepartmentAttendance(Map<String, Double> departmentAttendance) { this.departmentAttendance = departmentAttendance; }
}
