package com.attendance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model representing a Student Leave Application.
 */
public class LeaveRequest {
    private int leaveId;
    private int studentId;
    private String leaveType; // "Medical", "Personal", "College Event", "Emergency", "Other"
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
    private String supportingDoc;
    private String status = "PENDING"; // "PENDING", "APPROVED", "REJECTED"
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Display student info
    private String registerNo;
    private String studentName;
    private String department;
    private int yearOfStudy;
    private String section;

    public LeaveRequest() {}

    public LeaveRequest(int leaveId, int studentId, String leaveType, LocalDate fromDate, LocalDate toDate, String reason, String status) {
        this.leaveId = leaveId;
        this.studentId = studentId;
        this.leaveType = leaveType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.reason = reason;
        this.status = status;
    }

    public int getLeaveId() { return leaveId; }
    public void setLeaveId(int leaveId) { this.leaveId = leaveId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSupportingDoc() { return supportingDoc; }
    public void setSupportingDoc(String supportingDoc) { this.supportingDoc = supportingDoc; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

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
}
