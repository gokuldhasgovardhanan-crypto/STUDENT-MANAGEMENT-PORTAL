package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for in-app Notifications.
 */
public class NotificationDAO {

    public boolean create(Notification n) {
        String sql = "INSERT INTO notifications (title, message, category, target_role, is_read) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getTitle());
            ps.setString(2, n.getMessage());
            ps.setString(3, n.getCategory());
            ps.setString(4, n.getTargetRole() != null ? n.getTargetRole() : "ALL");
            ps.setBoolean(5, n.isRead());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) n.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Notification error: " + e.getMessage());
            return false;
        }
    }

    public List<Notification> findForRole(String role) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE target_role = 'ALL' OR target_role = ? ORDER BY created_at DESC LIMIT 50";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role != null ? role : "ALL");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Notification(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("message"),
                            rs.getString("category"),
                            rs.getString("target_role"),
                            rs.getBoolean("is_read"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        }
        return list;
    }

    public int getUnreadCount(String role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE (target_role = 'ALL' OR target_role = ?) AND is_read = FALSE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role != null ? role : "ALL");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public boolean markAsRead(int id) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean markAllAsRead(String role) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE target_role = 'ALL' OR target_role = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role != null ? role : "ALL");
            return ps.executeUpdate() > 0;
        }
    }
}
