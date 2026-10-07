package com.attendance.service;

import com.attendance.dao.HolidayDAO;
import com.attendance.model.Holiday;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing institutional holidays and calendar exclusions.
 */
public class HolidayService {

    private final HolidayDAO holidayDAO;

    public HolidayService() {
        this.holidayDAO = new HolidayDAO();
    }

    public boolean addHoliday(Holiday holiday) throws SQLException {
        if (holiday == null || holiday.getHolidayDate() == null || holiday.getHolidayName() == null || holiday.getHolidayName().trim().isEmpty()) {
            throw new IllegalArgumentException("Holiday date and name are required.");
        }
        return holidayDAO.addHoliday(holiday);
    }

    public boolean deleteHoliday(int holidayId) throws SQLException {
        return holidayDAO.deleteHoliday(holidayId);
    }

    public List<Holiday> getAllHolidays() {
        return holidayDAO.getAllHolidays();
    }

    public boolean isHoliday(LocalDate date) {
        return holidayDAO.isHoliday(date);
    }

    public int getHolidayCountBetween(LocalDate start, LocalDate end) {
        return holidayDAO.getHolidayCountBetween(start, end);
    }
}
