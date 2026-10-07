package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.AttendanceSession;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Attendance Sessions & QR Code Authentication.
 */
public class AttendanceSessionDAO {

    public boolean create(AttendanceSession s) throws SQLException {
        String sql = "INSERT INTO attendance_session (token, subject_id, teacher_id, attendance_date, " +
                "period, department, year_of_study, section, expires_at, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getToken());
            ps.setInt(2, s.getSubjectId());
            ps.setInt(3, s.getTeacherId());
            ps.setDate(4, Date.valueOf(s.getAttendanceDate()));
            ps.setString(5, s.getPeriod());
            ps.setString(6, s.getDepartment());
            ps.setInt(7, s.getYearOfStudy());
            ps.setString(8, s.getSection());
            ps.setTimestamp(9, Timestamp.valueOf(s.getExpiresAt()));
            ps.setString(10, s.getStatus() != null ? s.getStatus() : "ACTIVE");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) s.setSessionId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    public AttendanceSession findByToken(String token) throws SQLException {
        String sql = "SELECT s.*, sub.subject_name, t.teacher_name FROM attendance_session s " +
                "JOIN subjects sub ON s.subject_id = sub.subject_id " +
                "JOIN teachers t ON s.teacher_id = t.teacher_id " +
                "WHERE s.token = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public boolean expireSession(int sessionId) throws SQLException {
        String sql = "UPDATE attendance_session SET status = 'EXPIRED' WHERE session_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<AttendanceSession> getActiveSessions() throws SQLException {
        List<AttendanceSession> list = new ArrayList<>();
        String sql = "SELECT s.*, sub.subject_name, t.teacher_name FROM attendance_session s " +
                "JOIN subjects sub ON s.subject_id = sub.subject_id " +
                "JOIN teachers t ON s.teacher_id = t.teacher_id " +
                "WHERE s.status = 'ACTIVE' AND s.expires_at > NOW() ORDER BY s.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private AttendanceSession mapRow(ResultSet rs) throws SQLException {
        AttendanceSession s = new AttendanceSession();
        s.setSessionId(rs.getInt("session_id"));
        s.setToken(rs.getString("token"));
        s.setSubjectId(rs.getInt("subject_id"));
        s.setTeacherId(rs.getInt("teacher_id"));
        s.setAttendanceDate(rs.getDate("attendance_date").toLocalDate());
        s.setPeriod(rs.getString("period"));
        s.setDepartment(rs.getString("department"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setSection(rs.getString("section"));
        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) s.setCreatedAt(cat.toLocalDateTime());
        Timestamp eat = rs.getTimestamp("expires_at");
        if (eat != null) s.setExpiresAt(eat.toLocalDateTime());
        s.setStatus(rs.getString("status"));
        s.setSubjectName(rs.getString("subject_name"));
        s.setTeacherName(rs.getString("teacher_name"));
        return s;
    }
}
