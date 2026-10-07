package com.attendance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model representing an Attendance Record.
 */
public class Attendance {
    private int attendanceId;
    private int studentId;
    private LocalDate attendanceDate;
    private String status; // "PRESENT" or "ABSENT"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Associated student info for display
    private String registerNo;
    private String studentName;
    private String department;
    private int yearOfStudy;
    private String section;

    public Attendance() {}

    public Attendance(int attendanceId, int studentId, LocalDate attendanceDate, String status) {
        this.attendanceId = attendanceId;
        this.studentId = studentId;
        this.attendanceDate = attendanceDate;
        this.status = status;
    }

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getRegisterNo() { return registerNo; }
    public void setRegisterNo(String registerNo) { this.registerNo = registerNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public boolean isPresent() {
        return "PRESENT".equalsIgnoreCase(status);
    }
}
