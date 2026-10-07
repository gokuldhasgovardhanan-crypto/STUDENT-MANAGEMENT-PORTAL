package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for User authentication and management.
 */
public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    public User findByUsername(String username) {
        String sql = "SELECT user_id, username, password_hash, full_name, role, email, created_at, updated_at " +
                     "FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by username: " + username, e);
        }
        return null;
    }

    public User findById(int userId) {
        String sql = "SELECT user_id, username, password_hash, full_name, role, email, created_at, updated_at " +
                     "FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by id: " + userId, e);
        }
        return null;
    }

    public boolean createUser(User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, email) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getEmail());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setUserId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating user: " + user.getUsername(), e);
        }
        return false;
    }

    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating password for userId: " + userId, e);
        }
        return false;
    }

    public int countUsers() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting users", e);
        }
        return 0;
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, username, password_hash, full_name, role, email, created_at, updated_at FROM users ORDER BY username";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all users", e);
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        u.setEmail(rs.getString("email"));
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) u.setCreatedAt(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("updated_at");
        if (ut != null) u.setUpdatedAt(ut.toLocalDateTime());
        return u;
    }
}
