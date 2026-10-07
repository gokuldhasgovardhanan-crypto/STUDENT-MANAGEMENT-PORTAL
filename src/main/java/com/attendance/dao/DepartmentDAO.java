package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for departments table.
 */
public class DepartmentDAO {

    private static final Logger LOGGER = Logger.getLogger(DepartmentDAO.class.getName());

    public List<Department> getAllDepartments() {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT dept_id, dept_code, dept_name FROM departments ORDER BY dept_code";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Department(
                        rs.getInt("dept_id"),
                        rs.getString("dept_code"),
                        rs.getString("dept_name")
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading departments", e);
        }
        return list;
    }

    public boolean insertDepartment(Department dept) {
        String sql = "INSERT INTO departments (dept_code, dept_name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dept.getDeptCode());
            ps.setString(2, dept.getDeptName());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting department: " + dept.getDeptCode(), e);
        }
        return false;
    }

    public boolean deleteDepartment(String deptCode) {
        String sql = "DELETE FROM departments WHERE dept_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deptCode);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting department: " + deptCode, e);
        }
        return false;
    }
}
