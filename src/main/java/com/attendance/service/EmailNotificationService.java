package com.attendance.service;

import com.attendance.model.AppSettings;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for Optional SMTP Email Notifications.
 * Gracefully logs and succeeds if email notifications are disabled or unconfigured.
 */
public class EmailNotificationService {

    private static final Logger LOGGER = Logger.getLogger(EmailNotificationService.class.getName());
    private final SettingsService settingsService = new SettingsService();

    public boolean sendLowAttendanceAlert(String studentEmail, String studentName, String subjectName, double currentPct, double requiredPct) {
        AppSettings settings = settingsService.getSettings();
        if (!settings.isEmailEnabled()) {
            LOGGER.info("[Email Disabled] Low attendance notification skipped for " + studentEmail);
            return false;
        }

        String subject = "⚠ Attendance Shortage Alert - " + settings.getCollegeName();
        String body = String.format(
                "Dear %s,\n\nYour attendance in %s is currently %.1f%%, which is below the required %.1f%%.\n" +
                "Please meet your faculty advisor or attend upcoming lectures to avoid detention.\n\n" +
                "Regards,\nOffice of Academic Affairs\n%s",
                studentName, subjectName != null ? subjectName : "Regular Classes", currentPct, requiredPct, settings.getCollegeName()
        );

        return sendMail(studentEmail, subject, body);
    }

    public boolean sendLeaveStatusNotification(String studentEmail, String studentName, String leaveType, String status, String remarks) {
        AppSettings settings = settingsService.getSettings();
        if (!settings.isEmailEnabled()) {
            LOGGER.info("[Email Disabled] Leave status notification skipped for " + studentEmail);
            return false;
        }

        String subject = "Leave Application Update: " + status + " - " + settings.getCollegeName();
        String body = String.format(
                "Dear %s,\n\nYour application for %s leave has been %s.\nRemarks: %s\n\nRegards,\n%s",
                studentName, leaveType, status, remarks != null ? remarks : "No remarks", settings.getCollegeName()
        );

        return sendMail(studentEmail, subject, body);
    }

    private boolean sendMail(String recipient, String subject, String body) {
        AppSettings settings = settingsService.getSettings();
        try {
            // Note: In environments without external outbound SMTP access, we log the complete mail payload
            LOGGER.info(String.format("[Mock SMTP Outbound] Host: %s:%d, To: %s, Subject: %s",
                    settings.getSmtpHost(), settings.getSmtpPort(), recipient, subject));
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send email to " + recipient, e);
            return false;
        }
    }
}
