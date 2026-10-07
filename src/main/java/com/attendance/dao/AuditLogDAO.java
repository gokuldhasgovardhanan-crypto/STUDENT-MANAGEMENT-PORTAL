package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.AuditLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for General System Audit Logging.
 */
public class AuditLogDAO {

    public boolean log(String userId, String action, String description, String ipAddress) {
        String sql = "INSERT INTO audit_logs (user_id, action, description, ip_address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId != null ? userId : "SYSTEM");
            ps.setString(2, action);
            ps.setString(3, description);
            ps.setString(4, ipAddress != null ? ipAddress : "127.0.0.1");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("AuditLog Error: " + e.getMessage());
            return false;
        }
    }

    public List<AuditLog> findAll(int limit) throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 200);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog l = new AuditLog(
                            rs.getInt("log_id"),
                            rs.getString("user_id"),
                            rs.getString("action"),
                            rs.getString("description"),
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("ip_address")
                    );
                    list.add(l);
                }
            }
        }
        return list;
    }
}
