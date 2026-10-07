package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.Attendance;
import com.attendance.model.StudentAttendanceSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Attendance marking, calculations, and reporting.
 */
public class AttendanceDAO {

    private static final Logger LOGGER = Logger.getLogger(AttendanceDAO.class.getName());

    /**
     * Retrieves attendance for students in a class on a given date.
     * If no attendance record exists yet for a student on that date, status will be null or empty.
     */
    public List<Attendance> getAttendanceForClassAndDate(String dept, int year, String section, LocalDate date) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT s.student_id, s.register_no, s.student_name, s.department, s.year_of_study, s.section, " +
                     "a.attendance_id, a.attendance_date, a.status " +
                     "FROM students s " +
                     "LEFT JOIN attendance a ON s.student_id = a.student_id AND a.attendance_date = ? " +
                     "WHERE s.department = ? AND s.year_of_study = ? AND s.section = ? " +
                     "ORDER BY s.register_no ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, dept);
            ps.setInt(3, year);
            ps.setString(4, section);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attendance att = new Attendance();
                    att.setStudentId(rs.getInt("student_id"));
                    att.setRegisterNo(rs.getString("register_no"));
                    att.setStudentName(rs.getString("student_name"));
                    att.setDepartment(rs.getString("department"));
                    att.setYearOfStudy(rs.getInt("year_of_study"));
                    att.setSection(rs.getString("section"));
                    att.setAttendanceId(rs.getInt("attendance_id"));
                    att.setAttendanceDate(date);
                    String st = rs.getString("status");
                    att.setStatus(st != null ? st : "");
                    list.add(att);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading class attendance for date: " + date, e);
        }
        return list;
    }

    /**
     * Checks if attendance has already been recorded for this class on this date.
     */
    public boolean isAttendanceRecorded(String dept, int year, String section, LocalDate date) {
        String sql = "SELECT COUNT(a.attendance_id) " +
                     "FROM attendance a " +
                     "INNER JOIN students s ON a.student_id = s.student_id " +
                     "WHERE s.department = ? AND s.year_of_study = ? AND s.section = ? AND a.attendance_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dept);
            ps.setInt(2, year);
            ps.setString(3, section);
            ps.setDate(4, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if attendance recorded", e);
        }
        return false;
    }

    /**
     * Saves or updates a batch of attendance records inside a single transaction.
     * Enforces (student_id, attendance_date) UNIQUE constraint.
     */
    public boolean saveOrUpdateAttendanceBatch(List<Attendance> records) {
        if (records == null || records.isEmpty()) return true;

        String sql = "INSERT INTO attendance (student_id, attendance_date, status) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = CURRENT_TIMESTAMP";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Attendance att : records) {
                    if (att.getStatus() == null || att.getStatus().trim().isEmpty()) {
                        continue;
                    }
                    ps.setInt(1, att.getStudentId());
                    ps.setDate(2, Date.valueOf(att.getAttendanceDate()));
                    ps.setString(3, att.getStatus().toUpperCase());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving attendance batch", e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Gets attendance history for an individual student within a date range.
     */
    public List<Attendance> getAttendanceByStudentAndDateRange(int studentId, LocalDate start, LocalDate end) {
        List<Attendance> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT a.attendance_id, a.student_id, a.attendance_date, a.status, " +
                "s.register_no, s.student_name, s.department, s.year_of_study, s.section " +
                "FROM attendance a " +
                "INNER JOIN students s ON a.student_id = s.student_id " +
                "WHERE a.student_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(studentId);

        if (start != null) {
            sql.append("AND a.attendance_date >= ? ");
            params.add(Date.valueOf(start));
        }
        if (end != null) {
            sql.append("AND a.attendance_date <= ? ");
            params.add(Date.valueOf(end));
        }

        sql.append("ORDER BY a.attendance_date DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attendance att = new Attendance();
                    att.setAttendanceId(rs.getInt("attendance_id"));
                    att.setStudentId(rs.getInt("student_id"));
                    Date d = rs.getDate("attendance_date");
                    if (d != null) att.setAttendanceDate(d.toLocalDate());
                    att.setStatus(rs.getString("status"));
                    att.setRegisterNo(rs.getString("register_no"));
                    att.setStudentName(rs.getString("student_name"));
                    att.setDepartment(rs.getString("department"));
                    att.setYearOfStudy(rs.getInt("year_of_study"));
                    att.setSection(rs.getString("section"));
                    list.add(att);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading student attendance history", e);
        }
        return list;
    }

    /**
     * Calculates the attendance summary for a specific student dynamically.
     */
    public StudentAttendanceSummary getStudentAttendanceSummary(int studentId) {
        String sql = "SELECT s.student_id, s.register_no, s.student_name, s.department, s.year_of_study, s.section, " +
                     "s.email, s.phone_number, " +
                     "COUNT(a.attendance_id) AS total_days, " +
                     "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_days, " +
                     "SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_days " +
                     "FROM students s " +
                     "LEFT JOIN attendance a ON s.student_id = a.student_id " +
                     "WHERE s.student_id = ? " +
                     "GROUP BY s.student_id, s.register_no, s.student_name, s.department, s.year_of_study, s.section, s.email, s.phone_number";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSummary(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating student attendance summary", e);
        }
        return null;
    }

    /**
     * Calculates attendance summary for all students matching filters and date range.
     */
    public List<StudentAttendanceSummary> getAllStudentAttendanceSummaries(String dept, Integer year, String section,
                                                                          LocalDate start, LocalDate end) {
        List<StudentAttendanceSummary> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT s.student_id, s.register_no, s.student_name, s.department, s.year_of_study, s.section, " +
                "s.email, s.phone_number, " +
                "COUNT(a.attendance_id) AS total_days, " +
                "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_days, " +
                "SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_days " +
                "FROM students s " +
                "LEFT JOIN attendance a ON s.student_id = a.student_id "
        );
        List<Object> params = new ArrayList<>();

        if (start != null && end != null) {
            sql.append("AND a.attendance_date BETWEEN ? AND ? ");
            params.add(Date.valueOf(start));
            params.add(Date.valueOf(end));
        } else if (start != null) {
            sql.append("AND a.attendance_date >= ? ");
            params.add(Date.valueOf(start));
        } else if (end != null) {
            sql.append("AND a.attendance_date <= ? ");
            params.add(Date.valueOf(end));
        }

        sql.append("WHERE 1=1 ");

        if (dept != null && !dept.trim().isEmpty() && !dept.equalsIgnoreCase("All")) {
            sql.append("AND s.department = ? ");
            params.add(dept.trim());
        }
        if (year != null && year > 0) {
            sql.append("AND s.year_of_study = ? ");
            params.add(year);
        }
        if (section != null && !section.trim().isEmpty() && !section.equalsIgnoreCase("All")) {
            sql.append("AND s.section = ? ");
            params.add(section.trim());
        }

        sql.append("GROUP BY s.student_id, s.register_no, s.student_name, s.department, s.year_of_study, s.section, s.email, s.phone_number ");
        sql.append("ORDER BY s.department ASC, s.year_of_study ASC, s.section ASC, s.register_no ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSummary(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading student attendance summaries", e);
        }
        return list;
    }

    /**
     * Retrieves all students whose overall attendance percentage is below the given threshold.
     */
    public List<StudentAttendanceSummary> getLowAttendanceStudents(double thresholdPct, String dept, Integer year, String section) {
        List<StudentAttendanceSummary> summaries = getAllStudentAttendanceSummaries(dept, year, section, null, null);
        List<StudentAttendanceSummary> lowList = new ArrayList<>();
        for (StudentAttendanceSummary s : summaries) {
            if (s.getTotalWorkingDays() > 0 && s.getAttendancePercentage() < thresholdPct) {
                lowList.add(s);
            }
        }
        // Sort ascending by percentage (lowest attendance first)
        lowList.sort(Comparator.comparingDouble(StudentAttendanceSummary::getAttendancePercentage));
        return lowList;
    }

    /**
     * Computes today's attendance metrics: [presentCount, absentCount, notMarkedCount, totalStudents]
     */
    public int[] getTodayAttendanceMetrics(LocalDate today) {
        int present = 0;
        int absent = 0;
        int total = 0;

        String totalSql = "SELECT COUNT(*) FROM students";
        String todaySql = "SELECT status, COUNT(*) AS cnt FROM attendance WHERE attendance_date = ? GROUP BY status";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(totalSql)) {
                if (rs.next()) total = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(todaySql)) {
                ps.setDate(1, Date.valueOf(today));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String status = rs.getString("status");
                        int count = rs.getInt("cnt");
                        if ("PRESENT".equalsIgnoreCase(status)) {
                            present = count;
                        } else if ("ABSENT".equalsIgnoreCase(status)) {
                            absent = count;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting today's attendance metrics", e);
        }

        int notMarked = Math.max(0, total - (present + absent));
        return new int[]{present, absent, notMarked, total};
    }

    /**
     * Computes the college-wide average attendance percentage.
     */
    public double getOverallAverageAttendance() {
        String sql = "SELECT COUNT(*) AS total_records, " +
                     "SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS present_records " +
                     "FROM attendance";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt("total_records");
                int present = rs.getInt("present_records");
                if (total > 0) {
                    return Math.round(((double) present / total * 100.0) * 100.0) / 100.0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error computing overall average attendance", e);
        }
        return 0.0;
    }

    /**
     * Computes average attendance % by department.
     */
    public Map<String, Double> getDepartmentAttendanceAverages() {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT s.department, " +
                     "COUNT(a.attendance_id) AS total_records, " +
                     "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_records " +
                     "FROM students s " +
                     "INNER JOIN attendance a ON s.student_id = a.student_id " +
                     "GROUP BY s.department " +
                     "ORDER BY s.department ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String dept = rs.getString("department");
                int total = rs.getInt("total_records");
                int present = rs.getInt("present_records");
                double pct = total > 0 ? (double) present / total * 100.0 : 0.0;
                map.put(dept, Math.round(pct * 10.0) / 10.0);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading department attendance averages", e);
        }
        return map;
    }

    /**
     * Gets daily attendance report for a specific class on a date.
     */
    public List<Attendance> getDailyAttendanceReport(LocalDate date, String dept, Integer year, String section) {
        List<Attendance> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT a.attendance_id, a.student_id, a.attendance_date, a.status, " +
                "s.register_no, s.student_name, s.department, s.year_of_study, s.section " +
                "FROM attendance a " +
                "INNER JOIN students s ON a.student_id = s.student_id " +
                "WHERE a.attendance_date = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(date));

        if (dept != null && !dept.trim().isEmpty() && !dept.equalsIgnoreCase("All")) {
            sql.append("AND s.department = ? ");
            params.add(dept.trim());
        }
        if (year != null && year > 0) {
            sql.append("AND s.year_of_study = ? ");
            params.add(year);
        }
        if (section != null && !section.trim().isEmpty() && !section.equalsIgnoreCase("All")) {
            sql.append("AND s.section = ? ");
            params.add(section.trim());
        }

        sql.append("ORDER BY s.department ASC, s.year_of_study ASC, s.section ASC, s.register_no ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attendance att = new Attendance();
                    att.setAttendanceId(rs.getInt("attendance_id"));
                    att.setStudentId(rs.getInt("student_id"));
                    att.setAttendanceDate(date);
                    att.setStatus(rs.getString("status"));
                    att.setRegisterNo(rs.getString("register_no"));
                    att.setStudentName(rs.getString("student_name"));
                    att.setDepartment(rs.getString("department"));
                    att.setYearOfStudy(rs.getInt("year_of_study"));
                    att.setSection(rs.getString("section"));
                    list.add(att);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading daily attendance report", e);
        }
        return list;
    }

    private StudentAttendanceSummary mapSummary(ResultSet rs) throws SQLException {
        StudentAttendanceSummary s = new StudentAttendanceSummary();
        s.setStudentId(rs.getInt("student_id"));
        s.setRegisterNo(rs.getString("register_no"));
        s.setStudentName(rs.getString("student_name"));
        s.setDepartment(rs.getString("department"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setSection(rs.getString("section"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone_number"));

        int total = rs.getInt("total_days");
        int present = rs.getInt("present_days");
        int absent = rs.getInt("absent_days");

        s.setTotalWorkingDays(total);
        s.setPresentDays(present);
        s.setAbsentDays(absent);

        double pct = total > 0 ? ((double) present / total) * 100.0 : 0.0;
        pct = Math.round(pct * 100.0) / 100.0;
        s.setAttendancePercentage(pct);
        s.setStatus(StudentAttendanceSummary.calculateStatus(pct));

        return s;
    }
}
