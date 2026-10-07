package com.attendance.service;

import com.attendance.dao.AttendanceAuditDAO;
import com.attendance.dao.AuditLogDAO;
import com.attendance.model.AttendanceAudit;
import com.attendance.model.AuditLog;

import java.sql.SQLException;
import java.util.List;

/**
 * Service for General System Audit Logging and Attendance Correction Audits.
 */
public class AuditService {

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AttendanceAuditDAO attendanceAuditDAO = new AttendanceAuditDAO();

    public void log(String userId, String action, String description) {
        auditLogDAO.log(userId, action, description, "127.0.0.1");
    }

    public List<AuditLog> getRecentLogs(int limit) throws SQLException {
        return auditLogDAO.findAll(limit);
    }

    public List<AttendanceAudit> getAttendanceAudits() throws SQLException {
        return attendanceAuditDAO.findAll();
    }
}
