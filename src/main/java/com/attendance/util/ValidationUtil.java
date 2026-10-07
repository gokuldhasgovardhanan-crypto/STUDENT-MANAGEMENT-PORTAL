package com.attendance.util;

import java.util.regex.Pattern;

/**
 * Utility for comprehensive field and business logic validations.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9+()\\-\\s]{10,15}$"
    );

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[a-zA-Z\\s.'\\-]{2,100}$"
    );

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) return false;
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidName(String name) {
        if (isEmpty(name)) return false;
        return NAME_PATTERN.matcher(name.trim()).matches();
    }

    public static boolean isValidYear(int year) {
        return year >= 1 && year <= 4;
    }

    public static boolean isValidAttendanceStatus(String status) {
        return "PRESENT".equalsIgnoreCase(status) || "ABSENT".equalsIgnoreCase(status);
    }
}
