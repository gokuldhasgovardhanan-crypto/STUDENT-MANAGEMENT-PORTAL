package com.attendance.model;

import java.time.LocalDateTime;

/**
 * Model representing an in-app Alert / Notification.
 */
public class Notification {
    private int id;
    private String title;
    private String message;
    private String category; // "LOW_ATTENDANCE", "LEAVE_REQUEST", "ATTENDANCE_SESSION", "CORRECTION", "SYSTEM"
    private String targetRole = "ALL"; // "ALL", "ADMIN", "TEACHER", "STUDENT"
    private boolean read = false;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(int id, String title, String message, String category, String targetRole, boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.category = category;
        this.targetRole = targetRole;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
