package com.attendance.service;

import com.attendance.dao.AttendanceDAO;
import com.attendance.model.Attendance;
import com.attendance.model.StudentAttendanceSummary;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Service managing all report generation and data aggregations.
 */
public class ReportService {

    private final AttendanceDAO attendanceDAO;

    public ReportService() {
        this.attendanceDAO = new AttendanceDAO();
    }

    public List<Attendance> getDailyAttendanceReport(LocalDate date, String dept, Integer year, String section) {
        return attendanceDAO.getDailyAttendanceReport(date, dept, year, section);
    }

    public List<Attendance> getStudentReport(int studentId, LocalDate start, LocalDate end) {
        return attendanceDAO.getAttendanceByStudentAndDateRange(studentId, start, end);
    }

    public List<StudentAttendanceSummary> getMonthlyReport(int month, int year, String dept, Integer yearOfStudy, String section) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return attendanceDAO.getAllStudentAttendanceSummaries(dept, yearOfStudy, section, start, end);
    }

    public List<StudentAttendanceSummary> getDateRangeReport(LocalDate start, LocalDate end, String dept, Integer year, String section) {
        return attendanceDAO.getAllStudentAttendanceSummaries(dept, year, section, start, end);
    }

    public Map<String, Double> getDepartmentWiseStats() {
        return attendanceDAO.getDepartmentAttendanceAverages();
    }

    public List<StudentAttendanceSummary> getLowAttendanceReport(double threshold, String dept, Integer year, String section) {
        return attendanceDAO.getLowAttendanceStudents(threshold, dept, year, section);
    }
}
