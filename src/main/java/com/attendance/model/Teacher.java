package com.attendance.model;

import java.time.LocalDateTime;

/**
 * Model representing a Teacher/Faculty member.
 */
public class Teacher {
    private int teacherId;
    private String employeeId;
    private String teacherName;
    private String department;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Teacher() {}

    public Teacher(int teacherId, String employeeId, String teacherName, String department, String email, String phone) {
        this.teacherId = teacherId;
        this.employeeId = employeeId;
        this.teacherName = teacherName;
        this.department = department;
        this.email = email;
        this.phone = phone;
    }

    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return teacherName + " (" + employeeId + ")";
    }
}
