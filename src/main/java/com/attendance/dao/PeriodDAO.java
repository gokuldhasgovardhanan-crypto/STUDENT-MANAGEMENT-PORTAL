package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.Period;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Class Periods.
 */
public class PeriodDAO {

    public List<Period> findAll() throws SQLException {
        List<Period> list = new ArrayList<>();
        String sql = "SELECT * FROM periods WHERE is_active = TRUE ORDER BY period_number";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Period p = new Period(
                        rs.getInt("period_id"),
                        rs.getInt("period_number"),
                        rs.getString("period_name"),
                        rs.getString("start_time"),
                        rs.getString("end_time")
                );
                p.setActive(rs.getBoolean("is_active"));
                list.add(p);
            }
        }
        return list;
    }

    public Period findById(int periodId) throws SQLException {
        String sql = "SELECT * FROM periods WHERE period_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, periodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Period p = new Period(
                            rs.getInt("period_id"),
                            rs.getInt("period_number"),
                            rs.getString("period_name"),
                            rs.getString("start_time"),
                            rs.getString("end_time")
                    );
                    p.setActive(rs.getBoolean("is_active"));
                    return p;
                }
            }
        }
        return null;
    }

    public boolean create(Period p) throws SQLException {
        String sql = "INSERT INTO periods (period_number, period_name, start_time, end_time, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getPeriodNumber());
            ps.setString(2, p.getPeriodName());
            ps.setString(3, p.getStartTime());
            ps.setString(4, p.getEndTime());
            ps.setBoolean(5, p.isActive());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) p.setPeriodId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    public boolean update(Period p) throws SQLException {
        String sql = "UPDATE periods SET period_number = ?, period_name = ?, start_time = ?, end_time = ?, is_active = ? WHERE period_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getPeriodNumber());
            ps.setString(2, p.getPeriodName());
            ps.setString(3, p.getStartTime());
            ps.setString(4, p.getEndTime());
            ps.setBoolean(5, p.isActive());
            ps.setInt(6, p.getPeriodId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int periodId) throws SQLException {
        String sql = "DELETE FROM periods WHERE period_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, periodId);
            return ps.executeUpdate() > 0;
        }
    }
}
