package com.attendance.model;

/**
 * Model representing a Timetable Slot.
 */
public class TimetableEntry {
    private int timetableId;
    private String dayOfWeek;
    private int periodId;
    private int subjectId;
    private Integer teacherId;
    private String department;
    private int yearOfStudy;
    private String section;
    private String room = "Room 101";

    // Joined Display Info
    private String periodName;
    private String timeSlot;
    private String subjectCode;
    private String subjectName;
    private String teacherName;

    public TimetableEntry() {}

    public TimetableEntry(int timetableId, String dayOfWeek, int periodId, int subjectId, Integer teacherId, String department, int yearOfStudy, String section, String room) {
        this.timetableId = timetableId;
        this.dayOfWeek = dayOfWeek;
        this.periodId = periodId;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.department = department;
        this.yearOfStudy = yearOfStudy;
        this.section = section;
        this.room = room;
    }

    public int getTimetableId() { return timetableId; }
    public void setTimetableId(int timetableId) { this.timetableId = timetableId; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public int getPeriodId() { return periodId; }
    public void setPeriodId(int periodId) { this.periodId = periodId; }

    public int getPeriodNumber() { return periodId; }
    public void setPeriodNumber(int periodNumber) { this.periodId = periodNumber; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
}
