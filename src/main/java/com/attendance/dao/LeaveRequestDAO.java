package com.attendance.dao;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.LeaveRequest;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Student Leave Requests.
 */
public class LeaveRequestDAO {

    public boolean create(LeaveRequest lr) throws SQLException {
        String sql = "INSERT INTO leave_requests (student_id, leave_type, from_date, to_date, reason, supporting_doc, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, lr.getStudentId());
            ps.setString(2, lr.getLeaveType());
            ps.setDate(3, Date.valueOf(lr.getFromDate()));
            ps.setDate(4, Date.valueOf(lr.getToDate()));
            ps.setString(5, lr.getReason());
            ps.setString(6, lr.getSupportingDoc());
            ps.setString(7, lr.getStatus() != null ? lr.getStatus() : "PENDING");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) lr.setLeaveId(rs.getInt(1));
                }
                return true;
            }
            return false;
        }
    }

    public boolean updateStatus(int leaveId, String status, String approvedBy, String remarks) throws SQLException {
        String sql = "UPDATE leave_requests SET status = ?, approved_by = ?, approved_at = NOW(), remarks = ? WHERE leave_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, approvedBy);
            ps.setString(3, remarks);
            ps.setInt(4, leaveId);
            return ps.executeUpdate() > 0;
        }
    }

    public LeaveRequest findById(int leaveId) throws SQLException {
        String sql = baseSelect() + " WHERE lr.leave_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaveId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<LeaveRequest> findByStudentId(int studentId) throws SQLException {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = baseSelect() + " WHERE lr.student_id = ? ORDER BY lr.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<LeaveRequest> findAll() throws SQLException {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = baseSelect() + " ORDER BY FIELD(lr.status, 'PENDING', 'APPROVED', 'REJECTED'), lr.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<LeaveRequest> findByStatus(String status) throws SQLException {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = baseSelect() + " WHERE lr.status = ? ORDER BY lr.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean isStudentOnApprovedLeave(int studentId, LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM leave_requests WHERE student_id = ? AND status = 'APPROVED' " +
                "AND ? BETWEEN from_date AND to_date";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public int getPendingCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM leave_requests WHERE status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private String baseSelect() {
        return "SELECT lr.*, s.register_no, s.student_name, s.department, s.year_of_study, s.section " +
                "FROM leave_requests lr " +
                "JOIN students s ON lr.student_id = s.student_id";
    }

    private LeaveRequest mapRow(ResultSet rs) throws SQLException {
        LeaveRequest lr = new LeaveRequest();
        lr.setLeaveId(rs.getInt("leave_id"));
        lr.setStudentId(rs.getInt("student_id"));
        lr.setLeaveType(rs.getString("leave_type"));
        lr.setFromDate(rs.getDate("from_date").toLocalDate());
        lr.setToDate(rs.getDate("to_date").toLocalDate());
        lr.setReason(rs.getString("reason"));
        lr.setSupportingDoc(rs.getString("supporting_doc"));
        lr.setStatus(rs.getString("status"));
        lr.setApprovedBy(rs.getString("approved_by"));
        Timestamp aat = rs.getTimestamp("approved_at");
        if (aat != null) lr.setApprovedAt(aat.toLocalDateTime());
        lr.setRemarks(rs.getString("remarks"));
        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) lr.setCreatedAt(cat.toLocalDateTime());
        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) lr.setUpdatedAt(uat.toLocalDateTime());

        lr.setRegisterNo(rs.getString("register_no"));
        lr.setStudentName(rs.getString("student_name"));
        lr.setDepartment(rs.getString("department"));
        lr.setYearOfStudy(rs.getInt("year_of_study"));
        lr.setSection(rs.getString("section"));
        return lr;
    }
}
