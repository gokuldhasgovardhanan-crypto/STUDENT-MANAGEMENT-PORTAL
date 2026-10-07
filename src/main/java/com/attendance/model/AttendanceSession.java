package com.attendance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model representing an Attendance Session (used for QR code attendance and Live Session).
 */
public class AttendanceSession {
    private int sessionId;
    private String token;
    private int subjectId;
    private int teacherId;
    private LocalDate attendanceDate;
    private String period;
    private String department;
    private int yearOfStudy;
    private String section;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String status = "ACTIVE"; // "ACTIVE", "EXPIRED", "CLOSED"

    // Display info
    private String subjectName;
    private String teacherName;

    public AttendanceSession() {}

    public AttendanceSession(int sessionId, String token, int subjectId, int teacherId, LocalDate attendanceDate, String period, String department, int yearOfStudy, String section, LocalDateTime createdAt, LocalDateTime expiresAt, String status) {
        this.sessionId = sessionId;
        this.token = token;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.attendanceDate = attendanceDate;
        this.period = period;
        this.department = department;
        this.yearOfStudy = yearOfStudy;
        this.section = section;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public boolean isExpired() {
        return "EXPIRED".equalsIgnoreCase(status) || (expiresAt != null && LocalDateTime.now().isAfter(expiresAt));
    }
}
