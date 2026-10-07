package com.attendance.service;

import com.attendance.dao.PeriodDAO;
import com.attendance.dao.TimetableDAO;
import com.attendance.model.Period;
import com.attendance.model.TimetableEntry;

import java.sql.SQLException;
import java.util.List;

/**
 * Service for managing Periods, Timetable schedules, and session auto-suggestions.
 */
public class TimetableService {

    private final TimetableDAO timetableDAO = new TimetableDAO();
    private final PeriodDAO periodDAO = new PeriodDAO();

    // Periods
    public List<Period> getAllPeriods() throws SQLException {
        return periodDAO.findAll();
    }

    public Period getPeriodById(int id) throws SQLException {
        return periodDAO.findById(id);
    }

    public boolean savePeriod(Period p) throws SQLException {
        if (p.getPeriodId() > 0) return periodDAO.update(p);
        return periodDAO.create(p);
    }

    public boolean deletePeriod(int id) throws SQLException {
        return periodDAO.delete(id);
    }

    // Timetable
    public List<TimetableEntry> getAllTimetableEntries() throws SQLException {
        return timetableDAO.findAll();
    }

    public List<TimetableEntry> getTimetableByClass(String dept, int year, String section) throws SQLException {
        return timetableDAO.findByClass(dept, year, section);
    }

    public List<TimetableEntry> getTimetableByTeacher(int teacherId) throws SQLException {
        return timetableDAO.findByTeacher(teacherId);
    }

    public boolean createEntry(TimetableEntry t) throws SQLException {
        return timetableDAO.create(t);
    }

    public boolean updateEntry(TimetableEntry t) throws SQLException {
        return timetableDAO.update(t);
    }

    public boolean deleteEntry(int id) throws SQLException {
        return timetableDAO.delete(id);
    }

    public TimetableEntry suggestSubjectForAttendance(String dayOfWeek, String periodName, Integer teacherId, String dept, int year, String section) {
        try {
            return timetableDAO.suggestSubjectForSession(dayOfWeek, periodName, teacherId, dept, year, section);
        } catch (SQLException e) {
            return null;
        }
    }
}
