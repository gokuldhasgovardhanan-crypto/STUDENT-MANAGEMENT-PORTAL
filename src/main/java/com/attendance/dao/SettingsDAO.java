package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.AppSettings;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for settings table.
 */
public class SettingsDAO {

    private static final Logger LOGGER = Logger.getLogger(SettingsDAO.class.getName());

    public AppSettings getSettings() {
        AppSettings settings = new AppSettings();
        String sql = "SELECT setting_key, setting_value FROM settings";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Map<String, String> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }

            if (map.containsKey("college_name")) settings.setCollegeName(map.get("college_name"));
            if (map.containsKey("academic_year")) settings.setAcademicYear(map.get("academic_year"));
            if (map.containsKey("semester")) settings.setSemester(map.get("semester"));
            if (map.containsKey("required_attendance_pct")) {
                try {
                    settings.setRequiredAttendancePct(Double.parseDouble(map.get("required_attendance_pct")));
                } catch (NumberFormatException ignored) {}
            }
            if (map.containsKey("working_days_target")) {
                try {
                    settings.setWorkingDaysTarget(Integer.parseInt(map.get("working_days_target")));
                } catch (NumberFormatException ignored) {}
            }
            if (map.containsKey("departments_list")) settings.setDepartmentsFromCsv(map.get("departments_list"));
            if (map.containsKey("years_list")) settings.setYearsFromCsv(map.get("years_list"));
            if (map.containsKey("sections_list")) settings.setSectionsFromCsv(map.get("sections_list"));

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error loading settings from DB, using defaults", e);
        }

        return settings;
    }

    public boolean saveSettings(AppSettings settings) {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = CURRENT_TIMESTAMP";

        Map<String, String> map = new HashMap<>();
        map.put("college_name", settings.getCollegeName());
        map.put("academic_year", settings.getAcademicYear());
        map.put("semester", settings.getSemester());
        map.put("required_attendance_pct", String.valueOf(settings.getRequiredAttendancePct()));
        map.put("working_days_target", String.valueOf(settings.getWorkingDaysTarget()));
        map.put("departments_list", settings.getDepartmentsCsv());
        map.put("years_list", settings.getYearsCsv());
        map.put("sections_list", settings.getSectionsCsv());

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, String> entry : map.entrySet()) {
                ps.setString(1, entry.getKey());
                ps.setString(2, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving settings", e);
            return false;
        }
    }
}
