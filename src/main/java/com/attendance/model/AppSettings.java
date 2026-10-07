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

    // Upgrade v2.0 Settings
    private String countApprovedLeaveAsPresent = "NO"; // "YES" / "NO"
    private int qrExpirationMinutes = 5;
    private String smtpHost = "smtp.gmail.com";
    private int smtpPort = 587;
    private String smtpUsername = "";
    private String smtpPassword = "";
    private String emailNotificationsEnabled = "NO"; // "YES" / "NO"
    private String appTheme = "LIGHT"; // "LIGHT" / "DARK"

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

    public String getCountApprovedLeaveAsPresent() { return countApprovedLeaveAsPresent; }
    public void setCountApprovedLeaveAsPresent(String countApprovedLeaveAsPresent) { this.countApprovedLeaveAsPresent = countApprovedLeaveAsPresent; }
    public void setCountApprovedLeaveAsPresent(boolean countApprovedLeaveAsPresent) {
        this.countApprovedLeaveAsPresent = countApprovedLeaveAsPresent ? "YES" : "NO";
    }

    public boolean isCountApprovedLeaveAsPresent() {
        return "YES".equalsIgnoreCase(countApprovedLeaveAsPresent);
    }

    public int getQrExpirationMinutes() { return qrExpirationMinutes; }
    public void setQrExpirationMinutes(int qrExpirationMinutes) { this.qrExpirationMinutes = qrExpirationMinutes; }

    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }

    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }

    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }

    public String getEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(String emailNotificationsEnabled) { this.emailNotificationsEnabled = emailNotificationsEnabled; }

    public boolean isEmailEnabled() {
        return "YES".equalsIgnoreCase(emailNotificationsEnabled);
    }

    public String getAppTheme() { return appTheme; }
    public void setAppTheme(String appTheme) { this.appTheme = appTheme; }

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
