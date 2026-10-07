package com.attendance.model;

import java.time.LocalDate;

/**
 * Model representing an official institutional holiday.
 * Configured holidays are not counted as working attendance days in reports.
 */
public class Holiday {
    private int holidayId;
    private LocalDate holidayDate;
    private String holidayName;
    private String description;

    public Holiday() {}

    public Holiday(LocalDate holidayDate, String holidayName, String description) {
        this.holidayDate = holidayDate;
        this.holidayName = holidayName;
        this.description = description;
    }

    public Holiday(int holidayId, LocalDate holidayDate, String holidayName, String description) {
        this.holidayId = holidayId;
        this.holidayDate = holidayDate;
        this.holidayName = holidayName;
        this.description = description;
    }

    public int getHolidayId() { return holidayId; }
    public void setHolidayId(int holidayId) { this.holidayId = holidayId; }

    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }

    public String getHolidayName() { return holidayName; }
    public void setHolidayName(String holidayName) { this.holidayName = holidayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return holidayDate + " - " + holidayName;
    }
}
