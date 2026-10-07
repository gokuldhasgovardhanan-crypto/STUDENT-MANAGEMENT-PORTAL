package com.attendance.service;

import com.attendance.dao.LeaveRequestDAO;
import com.attendance.model.AppSettings;
import com.attendance.model.LeaveRequest;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for Student Leave Applications and Teacher/Admin Approvals.
 */
public class LeaveService {

    private final LeaveRequestDAO leaveDAO = new LeaveRequestDAO();
    private final SettingsService settingsService = new SettingsService();

    public boolean applyLeave(LeaveRequest lr) throws SQLException {
        if (lr.getFromDate().isAfter(lr.getToDate())) {
            throw new IllegalArgumentException("From Date cannot be after To Date.");
        }
        if (lr.getReason() == null || lr.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for leave is mandatory.");
        }
        lr.setStatus("PENDING");
        return leaveDAO.create(lr);
    }

    public boolean approveLeave(int leaveId, String approvedBy, String remarks) throws SQLException {
        return leaveDAO.updateStatus(leaveId, "APPROVED", approvedBy, remarks);
    }

    public boolean rejectLeave(int leaveId, String rejectedBy, String remarks) throws SQLException {
        return leaveDAO.updateStatus(leaveId, "REJECTED", rejectedBy, remarks);
    }

    public List<LeaveRequest> getStudentLeaves(int studentId) throws SQLException {
        return leaveDAO.findByStudentId(studentId);
    }

    public List<LeaveRequest> getLeaveRequestsByStudent(int studentId) throws SQLException {
        return getStudentLeaves(studentId);
    }

    public List<LeaveRequest> getAllLeaves() throws SQLException {
        return leaveDAO.findAll();
    }

    public List<LeaveRequest> getAllLeaveRequests() throws SQLException {
        return getAllLeaves();
    }

    public List<LeaveRequest> getPendingLeaves() throws SQLException {
        return leaveDAO.findByStatus("PENDING");
    }

    public int getPendingCount() {
        try {
            return leaveDAO.getPendingCount();
        } catch (SQLException e) {
            return 0;
        }
    }

    public boolean isStudentOnApprovedLeave(int studentId, LocalDate date) {
        try {
            return leaveDAO.isStudentOnApprovedLeave(studentId, date);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Checks if the student's approved leave should count as attendance PRESENT per configured policy.
     */
    public boolean shouldCountAsPresent(int studentId, LocalDate date) {
        AppSettings settings = settingsService.getSettings();
        if (settings.isCountApprovedLeaveAsPresent()) {
            return isStudentOnApprovedLeave(studentId, date);
        }
        return false;
    }
}
