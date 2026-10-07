package com.attendance.model;

/**
 * Model representing a Class Period / Lecture Slot.
 */
public class Period {
    private int periodId;
    private int periodNumber;
    private String periodName;
    private String startTime;
    private String endTime;
    private boolean active = true;

    public Period() {}

    public Period(int periodId, int periodNumber, String periodName, String startTime, String endTime) {
        this.periodId = periodId;
        this.periodNumber = periodNumber;
        this.periodName = periodName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getPeriodId() { return periodId; }
    public void setPeriodId(int periodId) { this.periodId = periodId; }

    public int getPeriodNumber() { return periodNumber; }
    public void setPeriodNumber(int periodNumber) { this.periodNumber = periodNumber; }

    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return periodName + " (" + startTime + " - " + endTime + ")";
    }
}
