package com.attendance.service;

import com.attendance.dao.SettingsDAO;
import com.attendance.model.AppSettings;
import com.attendance.util.ValidationUtil;

/**
 * Service managing settings configuration and updates.
 */
public class SettingsService {

    private final SettingsDAO settingsDAO;

    public SettingsService() {
        this.settingsDAO = new SettingsDAO();
    }

    public AppSettings getSettings() {
        return settingsDAO.getSettings();
    }

    public void saveSettings(AppSettings settings) throws Exception {
        if (settings == null) {
            throw new IllegalArgumentException("Settings cannot be null.");
        }
        if (ValidationUtil.isEmpty(settings.getCollegeName())) {
            throw new IllegalArgumentException("College Name cannot be empty.");
        }
        if (ValidationUtil.isEmpty(settings.getAcademicYear())) {
            throw new IllegalArgumentException("Academic Year cannot be empty.");
        }
        if (ValidationUtil.isEmpty(settings.getSemester())) {
            throw new IllegalArgumentException("Semester cannot be empty.");
        }
        if (settings.getRequiredAttendancePct() <= 0 || settings.getRequiredAttendancePct() > 100) {
            throw new IllegalArgumentException("Required attendance percentage must be between 1 and 100.");
        }
        if (settings.getWorkingDaysTarget() <= 0) {
            throw new IllegalArgumentException("Working days target must be greater than 0.");
        }

        boolean ok = settingsDAO.saveSettings(settings);
        if (!ok) {
            throw new Exception("Failed to save settings into database.");
        }
    }
}
