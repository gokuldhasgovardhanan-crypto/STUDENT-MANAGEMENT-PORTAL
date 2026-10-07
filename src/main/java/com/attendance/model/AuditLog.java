package com.attendance.model;

import java.time.LocalDateTime;

/**
 * Model representing a System-Wide Audit Log Entry.
 */
public class AuditLog {
    private int logId;
    private String userId;
    private String action;
    private String description;
    private LocalDateTime timestamp;
    private String ipAddress = "127.0.0.1";

    public AuditLog() {}

    public AuditLog(int logId, String userId, String action, String description, LocalDateTime timestamp, String ipAddress) {
        this.logId = logId;
        this.userId = userId;
        this.action = action;
        this.description = description;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
