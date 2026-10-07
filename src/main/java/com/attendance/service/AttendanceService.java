package com.attendance.service;

import com.attendance.dao.AttendanceDAO;
import com.attendance.dao.SettingsDAO;
import com.attendance.model.AppSettings;
import com.attendance.model.Attendance;
import com.attendance.model.DashboardStats;
import com.attendance.model.StudentAttendanceSummary;
import com.attendance.util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;

/**
 * Service managing attendance workflows, validations, calculations, and dashboard metrics.
 */
public class AttendanceService {

    private final AttendanceDAO attendanceDAO;
    private final SettingsDAO settingsDAO;

    public AttendanceService() {
        this.attendanceDAO = new AttendanceDAO();
        this.settingsDAO = new SettingsDAO();
    }

    public List<Attendance> getAttendanceForClassAndDate(String dept, int year, String section, LocalDate date) {
        return attendanceDAO.getAttendanceForClassAndDate(dept, year, section, date);
    }

    public List<Attendance> getAttendanceForSession(int subjectId, String dept, int year, String section, LocalDate date, String period) {
        return attendanceDAO.getAttendanceForSession(subjectId, dept, year, section, date, period);
    }

    public boolean isAttendanceMarked(String dept, int year, String section, LocalDate date) {
        return attendanceDAO.isAttendanceRecorded(dept, year, section, date);
    }

    public boolean isSessionRecorded(int subjectId, String dept, int year, String section, LocalDate date, String period) {
        return attendanceDAO.isSessionRecorded(subjectId, dept, year, section, date, period);
    }

    public boolean correctAttendance(int attendanceId, int studentId, String newStatus, String changedBy, String reason) {
        return attendanceDAO.correctAttendance(attendanceId, studentId, newStatus, changedBy, reason);
    }

    public void saveAttendance(LocalDate date, List<Attendance> records) throws Exception {
        if (date == null) {
            throw new IllegalArgumentException("Attendance date must be selected.");
        }
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("No student attendance records to save.");
        }

        for (Attendance att : records) {
            att.setAttendanceDate(date);
            if (ValidationUtil.isEmpty(att.getStatus())) {
                throw new IllegalArgumentException("Please mark attendance (Present/Absent) for " + att.getStudentName());
            }
            if (!ValidationUtil.isValidAttendanceStatus(att.getStatus())) {
                throw new IllegalArgumentException("Invalid attendance status for " + att.getStudentName());
            }
        }

        boolean ok = attendanceDAO.saveOrUpdateAttendanceBatch(records);
        if (!ok) {
            throw new Exception("Database error occurred while saving attendance records.");
        }
    }

    public StudentAttendanceSummary getStudentAttendanceSummary(int studentId) {
        return attendanceDAO.getStudentAttendanceSummary(studentId);
    }

    public List<java.util.Map<String, Object>> getSubjectWiseAttendanceForStudent(int studentId) {
        return attendanceDAO.getSubjectWiseAttendanceForStudent(studentId);
    }

    public List<Attendance> getStudentAttendanceHistory(int studentId, LocalDate start, LocalDate end) {
        return attendanceDAO.getAttendanceByStudentAndDateRange(studentId, start, end);
    }

    public List<StudentAttendanceSummary> getAllStudentSummaries(String dept, Integer year, String section,
                                                               LocalDate start, LocalDate end) {
        return attendanceDAO.getAllStudentAttendanceSummaries(dept, year, section, start, end);
    }

    public List<StudentAttendanceSummary> getLowAttendanceStudents(double threshold, String dept, Integer year, String section) {
        return attendanceDAO.getLowAttendanceStudents(threshold, dept, year, section);
    }

    public List<Attendance> getDailyAttendanceReport(LocalDate date, String dept, Integer year, String section) {
        return attendanceDAO.getDailyAttendanceReport(date, dept, year, section);
    }

    /**
     * Builds live DashboardStats dynamically from MySQL.
     */
    public DashboardStats getDashboardStats() {
        AppSettings settings = settingsDAO.getSettings();
        LocalDate today = LocalDate.now();

        int[] metrics = attendanceDAO.getTodayAttendanceMetrics(today);
        double avg = attendanceDAO.getOverallAverageAttendance();
        List<StudentAttendanceSummary> lowList = attendanceDAO.getLowAttendanceStudents(
                settings.getRequiredAttendancePct(), null, null, null
        );

        DashboardStats stats = new DashboardStats();
        stats.setTotalStudents(metrics[3]);
        stats.setPresentToday(metrics[0]);
        stats.setAbsentToday(metrics[1]);
        stats.setAttendanceNotMarked(metrics[2]);
        stats.setAverageAttendancePct(avg);
        stats.setStudentsBelowThresholdCount(lowList.size());
        stats.setRequiredAttendancePct(settings.getRequiredAttendancePct());
        stats.setCollegeName(settings.getCollegeName());
        stats.setAcademicYear(settings.getAcademicYear());
        stats.setSemester(settings.getSemester());
        stats.setTodayDate(today);
        stats.setLowAttendanceStudents(lowList);
        stats.setDepartmentAttendance(attendanceDAO.getDepartmentAttendanceAverages());

        return stats;
    }

    public java.util.Map<String, Integer> getAttendanceDistributionCounts() {
        return attendanceDAO.getAttendanceDistribution();
    }

    public java.util.Map<String, Double> getMonthlyAttendanceTrend() {
        return attendanceDAO.getMonthlyAttendanceTrend();
    }

    public java.util.Map<String, Double> getSubjectAverageAttendance() {
        return attendanceDAO.getSubjectAverageAttendance();
    }
}
