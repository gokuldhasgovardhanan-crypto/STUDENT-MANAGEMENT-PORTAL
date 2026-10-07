package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.Attendance;
import com.attendance.model.AttendanceAudit;
import com.attendance.model.StudentAttendanceSummary;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Attendance marking, calculations, subject-wise attendance,
 * QR validation, corrections, and reporting.
 */
public class AttendanceDAO {

    private static final Logger LOGGER = Logger.getLogger(AttendanceDAO.class.getName());

    /**
     * Retrieves attendance for students in a class on a given date.
     * Backward-compatible for daily attendance view.
     */
    public List<Attendance> getAttendanceForClassAndDate(String dept, int year, String section, LocalDate date) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT s.student_id, s.register_no, s.student_name, s.department, s.year_of_study, s.section, " +
                     "a.attendance_id, a.subject_id, a.period, a.attendance_date, a.status, sub.subject_name " +
                     "FROM students s " +
                     "LEFT JOIN attendance a ON s.student_id = a.student_id AND a.attendance_date = ? " +
                     "LEFT JOIN subjects sub ON a.subject_id = sub.subject_id " +
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
                    att.setPeriod(rs.getString("period") != null ? rs.getString("period") : "Period 1");
                    int subId = rs.getInt("subject_id");
                    if (!rs.wasNull()) att.setSubjectId(subId);
                    att.setSubjectName(rs.getString("subject_name"));
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

    public boolean isAttendanceRecorded(String dept, int year, String section, LocalDate date) {
        String sql = "SELECT COUNT(a.attendance_id) " +
                     "FROM attendance a " +
                     "INNER JOIN students s ON a.student_id = s.student_id " +
                     "WHERE a.attendance_date = ? AND s.department = ? AND s.year_of_study = ? AND s.section = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, dept);
            ps.setInt(3, year);
            ps.setString(4, section);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if attendance is recorded", e);
        }
        return false;
    }

    public List<Attendance> getAttendanceByStudentAndDateRange(int studentId, LocalDate start, LocalDate end) {
        List<Attendance> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT a.attendance_id, a.student_id, a.subject_id, a.attendance_date, a.period, a.status, " +
                "sub.subject_code, sub.subject_name " +
                "FROM attendance a " +
                "LEFT JOIN subjects sub ON a.subject_id = sub.subject_id " +
                "WHERE a.student_id = ? "
        );
        if (start != null) sql.append("AND a.attendance_date >= ? ");
        if (end != null) sql.append("AND a.attendance_date <= ? ");
        sql.append("ORDER BY a.attendance_date DESC, a.period ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int p = 1;
            ps.setInt(p++, studentId);
            if (start != null) ps.setDate(p++, Date.valueOf(start));
            if (end != null) ps.setDate(p++, Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attendance att = new Attendance();
                    att.setAttendanceId(rs.getInt("attendance_id"));
                    att.setStudentId(rs.getInt("student_id"));
                    int subId = rs.getInt("subject_id");
                    if (!rs.wasNull()) att.setSubjectId(subId);
                    java.sql.Date d = rs.getDate("attendance_date");
                    if (d != null) att.setAttendanceDate(d.toLocalDate());
                    att.setPeriod(rs.getString("period"));
                    att.setStatus(rs.getString("status"));
                    att.setSubjectCode(rs.getString("subject_code"));
                    att.setSubjectName(rs.getString("subject_name"));
                    list.add(att);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving attendance history for student: " + studentId, e);
        }
        return list;
    }

    /**
     * Retrieves attendance for students for a specific Subject, Date, and Period session.
     */
    public List<Attendance> getAttendanceForSession(int subjectId, String dept, int year, String section, LocalDate date, String period) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT s.student_id, s.register_no, s.student_name, s.department, s.year_of_study, s.section, " +
                     "a.attendance_id, a.attendance_date, a.period, a.status " +
                     "FROM students s " +
                     "LEFT JOIN attendance a ON s.student_id = a.student_id AND a.subject_id = ? " +
                     "AND a.attendance_date = ? AND a.period = ? " +
                     "WHERE s.department = ? AND s.year_of_study = ? AND s.section = ? " +
                     "ORDER BY s.register_no ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, period);
            ps.setString(4, dept);
            ps.setInt(5, year);
            ps.setString(6, section);

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
                    att.setSubjectId(subjectId);
                    att.setAttendanceDate(date);
                    att.setPeriod(period);
                    String st = rs.getString("status");
                    att.setStatus(st != null ? st : "");
                    list.add(att);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading session attendance", e);
        }
        return list;
    }

    /**
     * Checks if attendance has already been recorded for this class, subject, date, and period.
     */
    public boolean isSessionRecorded(int subjectId, String dept, int year, String section, LocalDate date, String period) {
        String sql = "SELECT COUNT(a.attendance_id) " +
                     "FROM attendance a " +
                     "INNER JOIN students s ON a.student_id = s.student_id " +
                     "WHERE a.subject_id = ? AND a.attendance_date = ? AND a.period = ? " +
                     "AND s.department = ? AND s.year_of_study = ? AND s.section = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, period);
            ps.setString(4, dept);
            ps.setInt(5, year);
            ps.setString(6, section);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking session attendance recorded", e);
        }
        return false;
    }

    /**
     * Saves or updates a batch of attendance records inside a single transaction.
     * Supports both general attendance and subject-wise period attendance.
     */
    public boolean saveOrUpdateAttendanceBatch(List<Attendance> records) {
        if (records == null || records.isEmpty()) return true;

        String sqlWithSubject = "INSERT INTO attendance (student_id, subject_id, attendance_date, period, status) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = CURRENT_TIMESTAMP";

        String sqlWithoutSubject = "INSERT INTO attendance (student_id, attendance_date, status) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = CURRENT_TIMESTAMP";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            for (Attendance att : records) {
                if (att.getStatus() == null || att.getStatus().trim().isEmpty()) continue;

                if (att.getSubjectId() != null && att.getSubjectId() > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlWithSubject)) {
                        ps.setInt(1, att.getStudentId());
                        ps.setInt(2, att.getSubjectId());
                        ps.setDate(3, Date.valueOf(att.getAttendanceDate()));
                        ps.setString(4, att.getPeriod() != null ? att.getPeriod() : "Period 1");
                        ps.setString(5, att.getStatus().toUpperCase());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(sqlWithoutSubject)) {
                        ps.setInt(1, att.getStudentId());
                        ps.setDate(2, Date.valueOf(att.getAttendanceDate()));
                        ps.setString(3, att.getStatus().toUpperCase());
                        ps.executeUpdate();
                    }
                }
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
     * Validates and records QR attendance for a student scanning a session token.
     */
    public String recordQrAttendance(String token, int studentId) {
        String querySession = "SELECT * FROM attendance_session WHERE token = ?";
        String checkDuplicate = "SELECT attendance_id, status FROM attendance " +
                "WHERE student_id = ? AND subject_id = ? AND attendance_date = ? AND period = ?";
        String insertAttendance = "INSERT INTO attendance (student_id, subject_id, attendance_date, period, status) " +
                "VALUES (?, ?, ?, ?, 'PRESENT') " +
                "ON DUPLICATE KEY UPDATE status = 'PRESENT', updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Validate Session Token
            int sessionId, subjectId;
            LocalDate attDate;
            String period;
            Timestamp expiresAt;
            String status;

            try (PreparedStatement ps = conn.prepareStatement(querySession)) {
                ps.setString(1, token);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return "Invalid session token. Please scan a valid QR code.";
                    }
                    sessionId = rs.getInt("session_id");
                    subjectId = rs.getInt("subject_id");
                    attDate = rs.getDate("attendance_date").toLocalDate();
                    period = rs.getString("period");
                    expiresAt = rs.getTimestamp("expires_at");
                    status = rs.getString("status");
                }
            }

            // 2. Validate Expiration
            if (!"ACTIVE".equalsIgnoreCase(status) || (expiresAt != null && expiresAt.before(new java.util.Date()))) {
                return "This attendance QR code has expired! Please ask the teacher for an active session.";
            }

            // 3. Check Duplicate
            try (PreparedStatement ps = conn.prepareStatement(checkDuplicate)) {
                ps.setInt(1, studentId);
                ps.setInt(2, subjectId);
                ps.setDate(3, Date.valueOf(attDate));
                ps.setString(4, period);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && "PRESENT".equalsIgnoreCase(rs.getString("status"))) {
                        return "You have ALREADY been marked PRESENT for this session!";
                    }
                }
            }

            // 4. Mark Present
            try (PreparedStatement ps = conn.prepareStatement(insertAttendance)) {
                ps.setInt(1, studentId);
                ps.setInt(2, subjectId);
                ps.setDate(3, Date.valueOf(attDate));
                ps.setString(4, period);
                ps.executeUpdate();
            }

            return "SUCCESS";
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error recording QR attendance", e);
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Performs an Attendance Correction and logs to attendance_audit.
     */
    public boolean correctAttendance(int attendanceId, int studentId, String newStatus, String changedBy, String reason) {
        String selectOld = "SELECT status FROM attendance WHERE attendance_id = ?";
        String updateAtt = "UPDATE attendance SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE attendance_id = ?";
        String insertAudit = "INSERT INTO attendance_audit (attendance_id, student_id, old_status, new_status, changed_by, reason) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String oldStatus = "UNKNOWN";
            try (PreparedStatement ps = conn.prepareStatement(selectOld)) {
                ps.setInt(1, attendanceId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) oldStatus = rs.getString("status");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(updateAtt)) {
                ps.setString(1, newStatus);
                ps.setInt(2, attendanceId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(insertAudit)) {
                ps.setInt(1, attendanceId);
                ps.setInt(2, studentId);
                ps.setString(3, oldStatus);
                ps.setString(4, newStatus);
                ps.setString(5, changedBy);
                ps.setString(6, reason);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error correcting attendance", e);
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
     * Subject-Wise Attendance Breakdown for Student Portal.
     * Returns Map of subjectCode -> [subjectName, conducted, attended, percentage].
     */
    public List<Map<String, Object>> getSubjectWiseAttendanceForStudent(int studentId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT s.subject_id, s.subject_code, s.subject_name, s.credits, " +
                "COUNT(a.attendance_id) AS conducted, " +
                "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS attended " +
                "FROM subjects s " +
                "JOIN student_subjects ss ON s.subject_id = ss.subject_id AND ss.student_id = ? " +
                "LEFT JOIN attendance a ON s.subject_id = a.subject_id AND a.student_id = ? " +
                "GROUP BY s.subject_id, s.subject_code, s.subject_name, s.credits " +
                "ORDER BY s.subject_code";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("subjectId", rs.getInt("subject_id"));
                    map.put("subjectCode", rs.getString("subject_code"));
                    map.put("subjectName", rs.getString("subject_name"));
                    map.put("credits", rs.getInt("credits"));
                    int conducted = rs.getInt("conducted");
                    int attended = rs.getInt("attended");
                    map.put("conducted", conducted);
                    map.put("attended", attended);
                    double pct = conducted > 0 ? (double) attended / conducted * 100.0 : 0.0;
                    map.put("percentage", Math.round(pct * 10.0) / 10.0);
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting subject-wise attendance for student", e);
        }
        return list;
    }

    /**
     * Monthly attendance trend percentage across the last 6 months.
     */
    public Map<String, Double> getMonthlyAttendanceTrend() {
        Map<String, Double> trend = new LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(attendance_date, '%b %Y') AS month_label, " +
                "COUNT(*) AS total_classes, " +
                "SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count " +
                "FROM attendance " +
                "GROUP BY DATE_FORMAT(attendance_date, '%Y-%m'), month_label " +
                "ORDER BY MIN(attendance_date) ASC LIMIT 6";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String label = rs.getString("month_label");
                int total = rs.getInt("total_classes");
                int present = rs.getInt("present_count");
                double pct = total > 0 ? (double) present / total * 100.0 : 0.0;
                trend.put(label, Math.round(pct * 10.0) / 10.0);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading monthly attendance trend", e);
        }
        return trend;
    }

    /**
     * Attendance distribution buckets across all students:
     * 90-100%, 80-89%, 75-79%, 65-74%, Below 65%
     */
    public Map<String, Integer> getAttendanceDistribution() {
        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("90-100%", 0);
        dist.put("80-89%", 0);
        dist.put("75-79%", 0);
        dist.put("65-74%", 0);
        dist.put("Below 65%", 0);

        List<StudentAttendanceSummary> summaries = getAllStudentAttendanceSummaries(null, null, null, null, null);
        for (StudentAttendanceSummary s : summaries) {
            double p = s.getAttendancePercentage();
            if (p >= 90.0) dist.put("90-100%", dist.get("90-100%") + 1);
            else if (p >= 80.0) dist.put("80-89%", dist.get("80-89%") + 1);
            else if (p >= 75.0) dist.put("75-79%", dist.get("75-79%") + 1);
            else if (p >= 65.0) dist.put("65-74%", dist.get("65-74%") + 1);
            else dist.put("Below 65%", dist.get("Below 65%") + 1);
        }
        return dist;
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
                if (rs.next()) return mapSummary(rs);
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
                while (rs.next()) list.add(mapSummary(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading student attendance summaries", e);
        }
        return list;
    }

    /**
     * Gets low attendance students below the specified threshold.
     */
    public List<StudentAttendanceSummary> getLowAttendanceStudents(double thresholdPct, String dept, Integer year, String section) {
        List<StudentAttendanceSummary> summaries = getAllStudentAttendanceSummaries(dept, year, section, null, null);
        List<StudentAttendanceSummary> lowList = new ArrayList<>();
        for (StudentAttendanceSummary s : summaries) {
            if (s.getTotalWorkingDays() > 0 && s.getAttendancePercentage() < thresholdPct) {
                lowList.add(s);
            }
        }
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
                            present += count;
                        } else if ("ABSENT".equalsIgnoreCase(status)) {
                            absent += count;
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
                "SELECT a.attendance_id, a.student_id, a.attendance_date, a.status, a.period, " +
                "s.register_no, s.student_name, s.department, s.year_of_study, s.section, sub.subject_name " +
                "FROM attendance a " +
                "INNER JOIN students s ON a.student_id = s.student_id " +
                "LEFT JOIN subjects sub ON a.subject_id = sub.subject_id " +
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
                    att.setPeriod(rs.getString("period") != null ? rs.getString("period") : "Period 1");
                    att.setStatus(rs.getString("status"));
                    att.setRegisterNo(rs.getString("register_no"));
                    att.setStudentName(rs.getString("student_name"));
                    att.setDepartment(rs.getString("department"));
                    att.setYearOfStudy(rs.getInt("year_of_study"));
                    att.setSection(rs.getString("section"));
                    att.setSubjectName(rs.getString("subject_name"));
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
