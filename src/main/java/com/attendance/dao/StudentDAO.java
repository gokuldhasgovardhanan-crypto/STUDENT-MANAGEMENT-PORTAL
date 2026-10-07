package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.Student;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Student operations.
 */
public class StudentDAO {

    private static final Logger LOGGER = Logger.getLogger(StudentDAO.class.getName());

    public List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT student_id, register_no, student_name, gender, date_of_birth, " +
                     "department, year_of_study, section, email, phone_number, address, " +
                     "admission_date, created_at, updated_at " +
                     "FROM students ORDER BY department ASC, year_of_study ASC, section ASC, register_no ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapStudent(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all students", e);
        }
        return list;
    }

    public Student getStudentById(int studentId) {
        String sql = "SELECT student_id, register_no, student_name, gender, date_of_birth, " +
                     "department, year_of_study, section, email, phone_number, address, " +
                     "admission_date, created_at, updated_at " +
                     "FROM students WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapStudent(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting student by id: " + studentId, e);
        }
        return null;
    }

    public Student getStudentByRegisterNo(String registerNo) {
        String sql = "SELECT student_id, register_no, student_name, gender, date_of_birth, " +
                     "department, year_of_study, section, email, phone_number, address, " +
                     "admission_date, created_at, updated_at " +
                     "FROM students WHERE register_no = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registerNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapStudent(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting student by register no: " + registerNo, e);
        }
        return null;
    }

    public boolean addStudent(Student student) {
        String sql = "INSERT INTO students (register_no, student_name, gender, date_of_birth, " +
                     "department, year_of_study, section, email, phone_number, address, admission_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, student.getRegisterNo());
            ps.setString(2, student.getStudentName());
            ps.setString(3, student.getGender());
            ps.setDate(4, Date.valueOf(student.getDateOfBirth()));
            ps.setString(5, student.getDepartment());
            ps.setInt(6, student.getYearOfStudy());
            ps.setString(7, student.getSection());
            ps.setString(8, student.getEmail());
            ps.setString(9, student.getPhoneNumber());
            ps.setString(10, student.getAddress());
            ps.setDate(11, Date.valueOf(student.getAdmissionDate()));

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        student.setStudentId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding student: " + student.getRegisterNo(), e);
        }
        return false;
    }

    public boolean insert(Student student) {
        return addStudent(student);
    }

    public boolean isRegisterNoTaken(String registerNo, int excludeStudentId) {
        String sql = "SELECT student_id FROM students WHERE register_no = ? AND student_id != ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registerNo);
            ps.setInt(2, excludeStudentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if register_no is taken: " + registerNo, e);
        }
        return false;
    }

    public boolean updateStudent(Student student) {
        String sql = "UPDATE students SET register_no = ?, student_name = ?, gender = ?, date_of_birth = ?, " +
                     "department = ?, year_of_study = ?, section = ?, email = ?, phone_number = ?, " +
                     "address = ?, admission_date = ? WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getRegisterNo());
            ps.setString(2, student.getStudentName());
            ps.setString(3, student.getGender());
            ps.setDate(4, Date.valueOf(student.getDateOfBirth()));
            ps.setString(5, student.getDepartment());
            ps.setInt(6, student.getYearOfStudy());
            ps.setString(7, student.getSection());
            ps.setString(8, student.getEmail());
            ps.setString(9, student.getPhoneNumber());
            ps.setString(10, student.getAddress());
            ps.setDate(11, Date.valueOf(student.getAdmissionDate()));
            ps.setInt(12, student.getStudentId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating student id: " + student.getStudentId(), e);
        }
        return false;
    }

    public boolean deleteStudent(int studentId) {
        // Attendance records are cascaded via foreign key ON DELETE CASCADE
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting student id: " + studentId, e);
        }
        return false;
    }

    public boolean isRegisterNoExists(String registerNo, Integer excludeStudentId) {
        String sql = "SELECT COUNT(*) FROM students WHERE register_no = ?" +
                     (excludeStudentId != null ? " AND student_id != ?" : "");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registerNo);
            if (excludeStudentId != null) {
                ps.setInt(2, excludeStudentId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking register number existence: " + registerNo, e);
        }
        return false;
    }

    public int getTotalStudentCount() {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting student count", e);
        }
        return 0;
    }

    public List<Student> getStudentsByFilter(String department, int year, String section) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT student_id, register_no, student_name, gender, date_of_birth, " +
                     "department, year_of_study, section, email, phone_number, address, " +
                     "admission_date, created_at, updated_at " +
                     "FROM students WHERE department = ? AND year_of_study = ? AND section = ? " +
                     "ORDER BY register_no ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, department);
            ps.setInt(2, year);
            ps.setString(3, section);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapStudent(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error filtering students by class", e);
        }
        return list;
    }

    public List<Student> searchStudents(String keyword, String department, Integer year, String section) {
        List<Student> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT student_id, register_no, student_name, gender, date_of_birth, " +
                "department, year_of_study, section, email, phone_number, address, " +
                "admission_date, created_at, updated_at FROM students WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (student_name LIKE ? OR register_no LIKE ? OR email LIKE ? OR phone_number LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (department != null && !department.trim().isEmpty() && !department.equalsIgnoreCase("All")) {
            sql.append("AND department = ? ");
            params.add(department.trim());
        }

        if (year != null && year > 0) {
            sql.append("AND year_of_study = ? ");
            params.add(year);
        }

        if (section != null && !section.trim().isEmpty() && !section.equalsIgnoreCase("All")) {
            sql.append("AND section = ? ");
            params.add(section.trim());
        }

        sql.append("ORDER BY department ASC, year_of_study ASC, section ASC, register_no ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapStudent(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching students", e);
        }
        return list;
    }

    private Student mapStudent(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setRegisterNo(rs.getString("register_no"));
        s.setStudentName(rs.getString("student_name"));
        s.setGender(rs.getString("gender"));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) s.setDateOfBirth(dob.toLocalDate());
        s.setDepartment(rs.getString("department"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setSection(rs.getString("section"));
        s.setEmail(rs.getString("email"));
        s.setPhoneNumber(rs.getString("phone_number"));
        s.setAddress(rs.getString("address"));
        Date adm = rs.getDate("admission_date");
        if (adm != null) s.setAdmissionDate(adm.toLocalDate());
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) s.setCreatedAt(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("updated_at");
        if (ut != null) s.setUpdatedAt(ut.toLocalDateTime());
        return s;
    }
}
