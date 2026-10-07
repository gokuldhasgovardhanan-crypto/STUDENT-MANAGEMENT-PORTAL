package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.TimetableEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Timetable Management.
 */
public class TimetableDAO {

    public boolean create(TimetableEntry t) throws SQLException {
        String sql = "INSERT INTO timetable (day_of_week, period_id, subject_id, teacher_id, department, year_of_study, section, room) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getDayOfWeek());
            ps.setInt(2, t.getPeriodId());
            ps.setInt(3, t.getSubjectId());
            if (t.getTeacherId() != null) ps.setInt(4, t.getTeacherId());
            else ps.setNull(4, Types.INTEGER);
            ps.setString(5, t.getDepartment());
            ps.setInt(6, t.getYearOfStudy());
            ps.setString(7, t.getSection());
            ps.setString(8, t.getRoom() != null ? t.getRoom() : "Room 101");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) t.setTimetableId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    public boolean update(TimetableEntry t) throws SQLException {
        String sql = "UPDATE timetable SET day_of_week = ?, period_id = ?, subject_id = ?, teacher_id = ?, " +
                "department = ?, year_of_study = ?, section = ?, room = ? WHERE timetable_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getDayOfWeek());
            ps.setInt(2, t.getPeriodId());
            ps.setInt(3, t.getSubjectId());
            if (t.getTeacherId() != null) ps.setInt(4, t.getTeacherId());
            else ps.setNull(4, Types.INTEGER);
            ps.setString(5, t.getDepartment());
            ps.setInt(6, t.getYearOfStudy());
            ps.setString(7, t.getSection());
            ps.setString(8, t.getRoom());
            ps.setInt(9, t.getTimetableId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int timetableId) throws SQLException {
        String sql = "DELETE FROM timetable WHERE timetable_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, timetableId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<TimetableEntry> findAll() throws SQLException {
        List<TimetableEntry> list = new ArrayList<>();
        String sql = baseSelect() + " ORDER BY FIELD(tt.day_of_week, 'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'), p.period_number";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<TimetableEntry> findByClass(String dept, int year, String section) throws SQLException {
        List<TimetableEntry> list = new ArrayList<>();
        String sql = baseSelect() + " WHERE (tt.department = ? OR ? = 'ALL') AND (tt.year_of_study = ? OR ? = 0) " +
                "AND (tt.section = ? OR ? = 'ALL') " +
                "ORDER BY FIELD(tt.day_of_week, 'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'), p.period_number";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dept != null ? dept : "ALL");
            ps.setString(2, dept != null ? dept : "ALL");
            ps.setInt(3, year);
            ps.setInt(4, year);
            ps.setString(5, section != null ? section : "ALL");
            ps.setString(6, section != null ? section : "ALL");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<TimetableEntry> findByTeacher(int teacherId) throws SQLException {
        List<TimetableEntry> list = new ArrayList<>();
        String sql = baseSelect() + " WHERE tt.teacher_id = ? " +
                "ORDER BY FIELD(tt.day_of_week, 'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'), p.period_number";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Used by Attendance Module: Suggest subject based on Day + Period + Teacher + Section (or Day + Period + Section).
     */
    public TimetableEntry suggestSubjectForSession(String dayOfWeek, String periodName, Integer teacherId, String dept, int year, String section) throws SQLException {
        String sql = baseSelect() + " WHERE tt.day_of_week = ? AND p.period_name = ? " +
                "AND (tt.teacher_id = ? OR ? IS NULL) AND tt.department = ? AND tt.year_of_study = ? AND tt.section = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dayOfWeek);
            ps.setString(2, periodName);
            if (teacherId != null) {
                ps.setInt(3, teacherId);
                ps.setInt(4, teacherId);
            } else {
                ps.setNull(3, Types.INTEGER);
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, dept);
            ps.setInt(6, year);
            ps.setString(7, section);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private String baseSelect() {
        return "SELECT tt.*, p.period_name, CONCAT(p.start_time, ' - ', p.end_time) as time_slot, " +
                "s.subject_code, s.subject_name, t.teacher_name " +
                "FROM timetable tt " +
                "JOIN periods p ON tt.period_id = p.period_id " +
                "JOIN subjects s ON tt.subject_id = s.subject_id " +
                "LEFT JOIN teachers t ON tt.teacher_id = t.teacher_id";
    }

    private TimetableEntry mapRow(ResultSet rs) throws SQLException {
        TimetableEntry e = new TimetableEntry();
        e.setTimetableId(rs.getInt("timetable_id"));
        e.setDayOfWeek(rs.getString("day_of_week"));
        e.setPeriodId(rs.getInt("period_id"));
        e.setSubjectId(rs.getInt("subject_id"));
        int tid = rs.getInt("teacher_id");
        if (!rs.wasNull()) e.setTeacherId(tid);
        e.setDepartment(rs.getString("department"));
        e.setYearOfStudy(rs.getInt("year_of_study"));
        e.setSection(rs.getString("section"));
        e.setRoom(rs.getString("room"));
        e.setPeriodName(rs.getString("period_name"));
        e.setTimeSlot(rs.getString("time_slot"));
        e.setSubjectCode(rs.getString("subject_code"));
        e.setSubjectName(rs.getString("subject_name"));
        e.setTeacherName(rs.getString("teacher_name"));
        return e;
    }
}
