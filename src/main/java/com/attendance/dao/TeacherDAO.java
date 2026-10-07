package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.Teacher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Teacher operations.
 */
public class TeacherDAO {

    private static final Logger LOGGER = Logger.getLogger(TeacherDAO.class.getName());

    public List<Teacher> getAllTeachers() {
        List<Teacher> list = new ArrayList<>();
        String sql = "SELECT teacher_id, employee_id, teacher_name, department, email, phone, created_at, updated_at " +
                     "FROM teachers ORDER BY teacher_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapTeacher(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading teachers", e);
        }
        return list;
    }

    public Teacher getTeacherById(int teacherId) {
        String sql = "SELECT teacher_id, employee_id, teacher_name, department, email, phone, created_at, updated_at " +
                     "FROM teachers WHERE teacher_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTeacher(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting teacher by id: " + teacherId, e);
        }
        return null;
    }

    public Teacher getTeacherByEmployeeId(String empId) {
        String sql = "SELECT teacher_id, employee_id, teacher_name, department, email, phone, created_at, updated_at " +
                     "FROM teachers WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTeacher(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting teacher by employee id: " + empId, e);
        }
        return null;
    }

    public boolean addTeacher(Teacher teacher) {
        String sql = "INSERT INTO teachers (employee_id, teacher_name, department, email, phone) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, teacher.getEmployeeId());
            ps.setString(2, teacher.getTeacherName());
            ps.setString(3, teacher.getDepartment());
            ps.setString(4, teacher.getEmail());
            ps.setString(5, teacher.getPhone());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        teacher.setTeacherId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding teacher: " + teacher.getTeacherName(), e);
        }
        return false;
    }

    public boolean updateTeacher(Teacher teacher) {
        String sql = "UPDATE teachers SET employee_id = ?, teacher_name = ?, department = ?, email = ?, phone = ? " +
                     "WHERE teacher_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teacher.getEmployeeId());
            ps.setString(2, teacher.getTeacherName());
            ps.setString(3, teacher.getDepartment());
            ps.setString(4, teacher.getEmail());
            ps.setString(5, teacher.getPhone());
            ps.setInt(6, teacher.getTeacherId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating teacher id: " + teacher.getTeacherId(), e);
        }
        return false;
    }

    public boolean deleteTeacher(int teacherId) {
        String sql = "DELETE FROM teachers WHERE teacher_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting teacher id: " + teacherId, e);
        }
        return false;
    }

    public boolean isEmployeeIdExists(String empId, Integer excludeTeacherId) {
        String sql = "SELECT COUNT(*) FROM teachers WHERE employee_id = ?" +
                     (excludeTeacherId != null ? " AND teacher_id != ?" : "");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            if (excludeTeacherId != null) {
                ps.setInt(2, excludeTeacherId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking employee ID exists: " + empId, e);
        }
        return false;
    }

    public List<Teacher> searchTeachers(String keyword, String department) {
        List<Teacher> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT teacher_id, employee_id, teacher_name, department, email, phone, created_at, updated_at " +
                "FROM teachers WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (teacher_name LIKE ? OR employee_id LIKE ? OR email LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (department != null && !department.trim().isEmpty() && !department.equalsIgnoreCase("All")) {
            sql.append("AND department = ? ");
            params.add(department.trim());
        }

        sql.append("ORDER BY teacher_name ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTeacher(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching teachers", e);
        }
        return list;
    }

    private Teacher mapTeacher(ResultSet rs) throws SQLException {
        Teacher t = new Teacher();
        t.setTeacherId(rs.getInt("teacher_id"));
        t.setEmployeeId(rs.getString("employee_id"));
        t.setTeacherName(rs.getString("teacher_name"));
        t.setDepartment(rs.getString("department"));
        t.setEmail(rs.getString("email"));
        t.setPhone(rs.getString("phone"));
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) t.setCreatedAt(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("updated_at");
        if (ut != null) t.setUpdatedAt(ut.toLocalDateTime());
        return t;
    }
}
