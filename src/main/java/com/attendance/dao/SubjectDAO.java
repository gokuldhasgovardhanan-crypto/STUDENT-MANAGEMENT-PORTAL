package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.Subject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Subject Management and Student-Subject Assignments.
 */
public class SubjectDAO {

    public boolean create(Subject s) throws SQLException {
        String sql = "INSERT INTO subjects (subject_code, subject_name, department, year_of_study, semester, credits, teacher_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getSubjectCode());
            ps.setString(2, s.getSubjectName());
            ps.setString(3, s.getDepartment());
            ps.setInt(4, s.getYearOfStudy());
            ps.setString(5, s.getSemester());
            ps.setInt(6, s.getCredits());
            if (s.getTeacherId() != null) {
                ps.setInt(7, s.getTeacherId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) s.setSubjectId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    public boolean update(Subject s) throws SQLException {
        String sql = "UPDATE subjects SET subject_code = ?, subject_name = ?, department = ?, " +
                "year_of_study = ?, semester = ?, credits = ?, teacher_id = ? WHERE subject_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getSubjectCode());
            ps.setString(2, s.getSubjectName());
            ps.setString(3, s.getDepartment());
            ps.setInt(4, s.getYearOfStudy());
            ps.setString(5, s.getSemester());
            ps.setInt(6, s.getCredits());
            if (s.getTeacherId() != null) {
                ps.setInt(7, s.getTeacherId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setInt(8, s.getSubjectId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int subjectId) throws SQLException {
        String sql = "DELETE FROM subjects WHERE subject_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            return ps.executeUpdate() > 0;
        }
    }

    public Subject findById(int subjectId) throws SQLException {
        String sql = "SELECT s.*, t.teacher_name FROM subjects s " +
                "LEFT JOIN teachers t ON s.teacher_id = t.teacher_id " +
                "WHERE s.subject_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Subject findByCode(String code) throws SQLException {
        String sql = "SELECT s.*, t.teacher_name FROM subjects s " +
                "LEFT JOIN teachers t ON s.teacher_id = t.teacher_id " +
                "WHERE s.subject_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Subject> findAll() throws SQLException {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT s.*, t.teacher_name FROM subjects s " +
                "LEFT JOIN teachers t ON s.teacher_id = t.teacher_id " +
                "ORDER BY s.department, s.year_of_study, s.subject_code";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Subject> findByDepartmentAndYear(String dept, int year) throws SQLException {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT s.*, t.teacher_name FROM subjects s " +
                "LEFT JOIN teachers t ON s.teacher_id = t.teacher_id " +
                "WHERE (s.department = ? OR ? = 'ALL') AND (s.year_of_study = ? OR ? = 0) " +
                "ORDER BY s.subject_code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dept != null ? dept : "ALL");
            ps.setString(2, dept != null ? dept : "ALL");
            ps.setInt(3, year);
            ps.setInt(4, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Subject> findByTeacherId(int teacherId) throws SQLException {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT s.*, t.teacher_name FROM subjects s " +
                "LEFT JOIN teachers t ON s.teacher_id = t.teacher_id " +
                "WHERE s.teacher_id = ? ORDER BY s.subject_code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Subject> getEnrolledSubjects(int studentId) throws SQLException {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT s.*, t.teacher_name FROM subjects s " +
                "JOIN student_subjects ss ON s.subject_id = ss.subject_id " +
                "LEFT JOIN teachers t ON s.teacher_id = t.teacher_id " +
                "WHERE ss.student_id = ? ORDER BY s.subject_code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int assignSubjectToStudents(int subjectId, List<Integer> studentIds) throws SQLException {
        if (studentIds == null || studentIds.isEmpty()) return 0;
        String sql = "INSERT IGNORE INTO student_subjects (student_id, subject_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int sid : studentIds) {
                ps.setInt(1, sid);
                ps.setInt(2, subjectId);
                ps.addBatch();
            }
            int[] res = ps.executeBatch();
            int count = 0;
            for (int r : res) if (r > 0 || r == Statement.SUCCESS_NO_INFO) count++;
            return count;
        }
    }

    public int assignSubjectByClass(int subjectId, String dept, int year, String section) throws SQLException {
        String sql = "INSERT IGNORE INTO student_subjects (student_id, subject_id) " +
                "SELECT student_id, ? FROM students WHERE department = ? AND year_of_study = ? " +
                "AND (section = ? OR ? = 'ALL')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            ps.setString(2, dept);
            ps.setInt(3, year);
            ps.setString(4, section != null ? section : "ALL");
            ps.setString(5, section != null ? section : "ALL");
            return ps.executeUpdate();
        }
    }

    public boolean removeSubjectFromStudent(int studentId, int subjectId) throws SQLException {
        String sql = "DELETE FROM student_subjects WHERE student_id = ? AND subject_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            return ps.executeUpdate() > 0;
        }
    }

    private Subject mapRow(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setSubjectId(rs.getInt("subject_id"));
        s.setSubjectCode(rs.getString("subject_code"));
        s.setSubjectName(rs.getString("subject_name"));
        s.setDepartment(rs.getString("department"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setSemester(rs.getString("semester"));
        s.setCredits(rs.getInt("credits"));
        int tid = rs.getInt("teacher_id");
        if (!rs.wasNull()) s.setTeacherId(tid);
        try {
            s.setTeacherName(rs.getString("teacher_name"));
        } catch (SQLException ignored) {}
        return s;
    }
}
