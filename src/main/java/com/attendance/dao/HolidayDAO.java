package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.Holiday;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Holiday records.
 */
public class HolidayDAO {

    public boolean addHoliday(Holiday holiday) throws SQLException {
        String sql = "INSERT INTO holidays (holiday_date, holiday_name, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(holiday.getHolidayDate()));
            ps.setString(2, holiday.getHolidayName());
            ps.setString(3, holiday.getDescription());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        holiday.setHolidayId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    public boolean deleteHoliday(int holidayId) throws SQLException {
        String sql = "DELETE FROM holidays WHERE holiday_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, holidayId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Holiday> getAllHolidays() {
        List<Holiday> list = new ArrayList<>();
        String sql = "SELECT holiday_id, holiday_date, holiday_name, description FROM holidays ORDER BY holiday_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date d = rs.getDate("holiday_date");
                list.add(new Holiday(
                        rs.getInt("holiday_id"),
                        d != null ? d.toLocalDate() : null,
                        rs.getString("holiday_name"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            // Log or return empty list
        }
        return list;
    }

    public boolean isHoliday(LocalDate date) {
        if (date == null) return false;
        String sql = "SELECT COUNT(*) FROM holidays WHERE holiday_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException ignored) {}
        return false;
    }

    public int getHolidayCountBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        String sql = "SELECT COUNT(*) FROM holidays WHERE holiday_date BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {}
        return 0;
    }
}
