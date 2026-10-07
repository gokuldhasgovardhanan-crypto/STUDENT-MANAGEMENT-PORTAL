package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.AttendanceAudit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Attendance Modification Audits.
 */
public class AttendanceAuditDAO {

    public boolean create(AttendanceAudit audit) throws SQLException {
        String sql = "INSERT INTO attendance_audit (attendance_id, student_id, old_status, new_status, changed_by, reason) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (audit.getAttendanceId() != null) ps.setInt(1, audit.getAttendanceId());
            else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, audit.getStudentId());
            ps.setString(3, audit.getOldStatus());
            ps.setString(4, audit.getNewStatus());
            ps.setString(5, audit.getChangedBy());
            ps.setString(6, audit.getReason());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) audit.setAuditId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    public List<AttendanceAudit> findAll() throws SQLException {
        List<AttendanceAudit> list = new ArrayList<>();
        String sql = "SELECT a.*, s.register_no, s.student_name FROM attendance_audit a " +
                "JOIN students s ON a.student_id = s.student_id " +
                "ORDER BY a.changed_at DESC LIMIT 200";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                AttendanceAudit audit = new AttendanceAudit(
                        rs.getInt("audit_id"),
                        (Integer) rs.getObject("attendance_id"),
                        rs.getInt("student_id"),
                        rs.getString("old_status"),
                        rs.getString("new_status"),
                        rs.getString("changed_by"),
                        rs.getTimestamp("changed_at").toLocalDateTime(),
                        rs.getString("reason")
                );
                audit.setRegisterNo(rs.getString("register_no"));
                audit.setStudentName(rs.getString("student_name"));
                list.add(audit);
            }
        }
        return list;
    }
}
