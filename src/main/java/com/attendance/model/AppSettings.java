package com.attendance.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Model representing persistent application and academic configuration settings.
 */
public class AppSettings {
    private String collegeName = "KIT ENGINEERING COLLEGE";
    private String academicYear = "2026-27";
    private String semester = "V";
    private double requiredAttendancePct = 75.0;
    private int workingDaysTarget = 60;
    private List<String> departments = new ArrayList<>(Arrays.asList("CSE", "IT", "AI&DS", "ECE", "EEE", "MECH"));
    private List<String> years = new ArrayList<>(Arrays.asList("1", "2", "3", "4"));
    private List<String> sections = new ArrayList<>(Arrays.asList("A", "B"));

    public AppSettings() {}

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public double getRequiredAttendancePct() { return requiredAttendancePct; }
    public void setRequiredAttendancePct(double requiredAttendancePct) { this.requiredAttendancePct = requiredAttendancePct; }

    public int getWorkingDaysTarget() { return workingDaysTarget; }
    public void setWorkingDaysTarget(int workingDaysTarget) { this.workingDaysTarget = workingDaysTarget; }

    public List<String> getDepartments() { return departments; }
    public void setDepartments(List<String> departments) { this.departments = departments; }

    public List<String> getYears() { return years; }
    public void setYears(List<String> years) { this.years = years; }

    public List<String> getSections() { return sections; }
    public void setSections(List<String> sections) { this.sections = sections; }

    public String getDepartmentsCsv() {
        return String.join(",", departments);
    }

    public void setDepartmentsFromCsv(String csv) {
        if (csv != null && !csv.trim().isEmpty()) {
            this.departments = Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    public String getYearsCsv() {
        return String.join(",", years);
    }

    public void setYearsFromCsv(String csv) {
        if (csv != null && !csv.trim().isEmpty()) {
            this.years = Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    public String getSectionsCsv() {
        return String.join(",", sections);
    }

    public void setSectionsFromCsv(String csv) {
        if (csv != null && !csv.trim().isEmpty()) {
            this.sections = Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }
}
