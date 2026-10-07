package com.attendance.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for formatting and manipulating dates and times across the system.
 */
public class DateUtil {

    public static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter DB_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter REPORT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
    public static final DateTimeFormatter MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

    public static String formatDisplayDate(LocalDate date) {
        return date != null ? date.format(DISPLAY_DATE_FORMAT) : "";
    }

    public static String formatDbDate(LocalDate date) {
        return date != null ? date.format(DB_DATE_FORMAT) : "";
    }

    public static String formatCurrentTimestamp() {
        return LocalDateTime.now().format(REPORT_TIMESTAMP_FORMAT);
    }

    public static String formatDisplayDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(REPORT_TIMESTAMP_FORMAT) : "";
    }

    public static LocalDate parseDisplayDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(text.trim(), DISPLAY_DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDate parseDbDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(text.trim(), DB_DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if a given date is a weekday (Monday - Friday).
     */
    public static boolean isWorkingDay(LocalDate date) {
        if (date == null) return false;
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    /**
     * Returns a list of all working days between start and end date (inclusive).
     */
    public static List<LocalDate> getWorkingDays(LocalDate start, LocalDate end) {
        List<LocalDate> days = new ArrayList<>();
        if (start == null || end == null || start.isAfter(end)) return days;

        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            if (isWorkingDay(curr)) {
                days.add(curr);
            }
            curr = curr.plusDays(1);
        }
        return days;
    }
}
