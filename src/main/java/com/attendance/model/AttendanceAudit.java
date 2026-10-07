package com.attendance.model;

import java.time.LocalDateTime;

/**
 * Model representing an Attendance Modification Audit record.
 */
public class AttendanceAudit {
    private int auditId;
    private Integer attendanceId;
    private int studentId;
    private String oldStatus;
    private String newStatus;
    private String changedBy;
    private LocalDateTime changedAt;
    private String reason;

    // Display info
    private String registerNo;
    private String studentName;

    public AttendanceAudit() {}

    public AttendanceAudit(int auditId, Integer attendanceId, int studentId, String oldStatus, String newStatus, String changedBy, LocalDateTime changedAt, String reason) {
        this.auditId = auditId;
        this.attendanceId = attendanceId;
        this.studentId = studentId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.reason = reason;
    }

    public int getAuditId() { return auditId; }
    public void setAuditId(int auditId) { this.auditId = auditId; }

    public Integer getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Integer attendanceId) { this.attendanceId = attendanceId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRegisterNo() { return registerNo; }
    public void setRegisterNo(String registerNo) { this.registerNo = registerNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
}
