package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.service.DatabaseInitService;
import com.attendance.service.SettingsService;
import com.attendance.ui.components.ModernButton;

import javax.swing.*;
import java.awt.*;

/**
 * Settings Panel allowing configuration of institution info, academic parameters,
 * attendance rules, and database maintenance.
 */
public class SettingsPanel extends JPanel {

    private final SettingsService settingsService;

    private final JTextField txtCollegeName;
    private final JTextField txtAcademicYear;
    private final JTextField txtSemester;
    private final JTextField txtRequiredPct;
    private final JTextField txtWorkingDays;
    private final JTextField txtDepartments;
    private final JTextField txtYears;
    private final JTextField txtSections;
    private final JCheckBox chkCountLeave;
    private final JSpinner spnQrExpiry;
    private final JComboBox<String> cmbTheme;

    private final JLabel lblStatus;

    public SettingsPanel() {
        this.settingsService = new SettingsService();

        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        AppSettings settings = settingsService.getSettings();

        // Title
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("System & Academic Settings");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(15, 23, 42));
        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Form Card
        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setOpaque(false);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtCollegeName = new JTextField(settings.getCollegeName(), 25);
        txtAcademicYear = new JTextField(settings.getAcademicYear(), 25);
        txtSemester = new JTextField(settings.getSemester(), 25);
        txtRequiredPct = new JTextField(String.valueOf(settings.getRequiredAttendancePct()), 25);
        txtWorkingDays = new JTextField(String.valueOf(settings.getWorkingDaysTarget()), 25);
        txtDepartments = new JTextField(settings.getDepartmentsCsv(), 25);
        txtYears = new JTextField(settings.getYearsCsv(), 25);
        txtSections = new JTextField(settings.getSectionsCsv(), 25);

        chkCountLeave = new JCheckBox("Count approved student leave as attendance present", settings.isCountApprovedLeaveAsPresent());
        spnQrExpiry = new JSpinner(new SpinnerNumberModel(settings.getQrExpirationMinutes() > 0 ? settings.getQrExpirationMinutes() : 5, 1, 60, 1));
        cmbTheme = new JComboBox<>(new String[]{"LIGHT", "DARK"});
        cmbTheme.setSelectedItem(settings.getAppTheme() != null ? settings.getAppTheme().toUpperCase() : "LIGHT");

        int r = 0;
        addFormRow(card, gbc, r++, "College / Institution Name *:", txtCollegeName);
        addFormRow(card, gbc, r++, "Academic Year *:", txtAcademicYear);
        addFormRow(card, gbc, r++, "Current Semester *:", txtSemester);
        addFormRow(card, gbc, r++, "Required Attendance Percentage (%) *:", txtRequiredPct);
        addFormRow(card, gbc, r++, "Semester Working Days Target *:", txtWorkingDays);
        addFormRow(card, gbc, r++, "Departments (comma separated):", txtDepartments);
        addFormRow(card, gbc, r++, "Academic Years (comma separated):", txtYears);
        addFormRow(card, gbc, r++, "Sections (comma separated):", txtSections);
        addFormRow(card, gbc, r++, "Institutional Leave Policy:", chkCountLeave);
        addFormRow(card, gbc, r++, "QR Code Expiry (Minutes):", spnQrExpiry);
        addFormRow(card, gbc, r++, "UI Appearance Theme:", cmbTheme);

        centerContainer.add(card);
        centerContainer.add(Box.createVerticalStrut(16));

        // Maintenance Card
        JPanel maintCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 12));
        maintCard.setBackground(Color.WHITE);
        maintCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        JLabel lblMaint = new JLabel("Database Maintenance:");
        lblMaint.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMaint.setForeground(new Color(71, 85, 105));

        ModernButton btnDbConfig = new ModernButton("Database Connection", ModernButton.ButtonType.SECONDARY);
        ModernButton btnBackupRestore = new ModernButton("💾 Backup & Restore SQL", ModernButton.ButtonType.PRIMARY);
        ModernButton btnHolidays = new ModernButton("📅 Declared Holidays", ModernButton.ButtonType.SECONDARY);
        ModernButton btnResetSeed = new ModernButton("Re-seed 100 Demo Students & Data", ModernButton.ButtonType.DANGER);

        btnDbConfig.addActionListener(e -> openDbConfig());
        btnBackupRestore.addActionListener(e -> {
            BackupRestoreDialog dlg = new BackupRestoreDialog(SwingUtilities.getWindowAncestor(this));
            dlg.setVisible(true);
        });
        btnHolidays.addActionListener(e -> {
            HolidayDialog dlg = new HolidayDialog(SwingUtilities.getWindowAncestor(this));
            dlg.setVisible(true);
        });
        btnResetSeed.addActionListener(e -> reseedDemoData());

        maintCard.add(lblMaint);
        maintCard.add(btnDbConfig);
        maintCard.add(btnBackupRestore);
        maintCard.add(btnHolidays);
        maintCard.add(btnResetSeed);

        centerContainer.add(maintCard);
        centerContainer.add(Box.createVerticalStrut(10));

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        centerContainer.add(lblStatus);

        add(new JScrollPane(centerContainer), BorderLayout.CENTER);

        // Buttons Bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnBar.setBackground(new Color(248, 250, 252));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnReset = new ModernButton("RESTORE DEFAULTS", ModernButton.ButtonType.SECONDARY);
        ModernButton btnSave = new ModernButton("SAVE SETTINGS", ModernButton.ButtonType.PRIMARY);

        btnReset.addActionListener(e -> restoreDefaults());
        btnSave.addActionListener(e -> saveSettings());

        btnBar.add(btnReset);
        btnBar.add(btnSave);
        add(btnBar, BorderLayout.SOUTH);
    }

    private void addFormRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(51, 65, 85));
        p.add(lbl, gbc);

        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.65;
        p.add(comp, gbc);
    }

    private void saveSettings() {
        try {
            AppSettings s = new AppSettings();
            s.setCollegeName(txtCollegeName.getText().trim());
            s.setAcademicYear(txtAcademicYear.getText().trim());
            s.setSemester(txtSemester.getText().trim());
            s.setRequiredAttendancePct(Double.parseDouble(txtRequiredPct.getText().trim()));
            s.setWorkingDaysTarget(Integer.parseInt(txtWorkingDays.getText().trim()));
            s.setDepartmentsFromCsv(txtDepartments.getText().trim());
            s.setYearsFromCsv(txtYears.getText().trim());
            s.setSectionsFromCsv(txtSections.getText().trim());
            s.setCountApprovedLeaveAsPresent(chkCountLeave.isSelected());
            s.setQrExpirationMinutes((Integer) spnQrExpiry.getValue());
            s.setAppTheme((String) cmbTheme.getSelectedItem());

            settingsService.saveSettings(s);
            lblStatus.setText("✓ Settings saved successfully!");
            lblStatus.setForeground(new Color(22, 101, 52));
            JOptionPane.showMessageDialog(this, "Settings updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values for percentage and working days.", "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException iae) {
            JOptionPane.showMessageDialog(this, "⚠ " + iae.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save settings: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restoreDefaults() {
        txtCollegeName.setText("KIT ENGINEERING COLLEGE");
        txtAcademicYear.setText("2026-27");
        txtSemester.setText("V");
        txtRequiredPct.setText("75.0");
        txtWorkingDays.setText("60");
        txtDepartments.setText("CSE,IT,AI&DS,ECE,EEE,MECH");
        txtYears.setText("1,2,3,4");
        txtSections.setText("A,B");
    }

    private void openDbConfig() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        DatabaseConfigDialog dlg = new DatabaseConfigDialog(owner);
        dlg.setVisible(true);
    }

    private void reseedDemoData() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Warning: This will recreate tables and re-seed 100 demo students, faculty, and historical attendance.\n" +
                "Are you sure you want to proceed?",
                "Confirm Re-seed", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseInitService.initializeDatabaseAndSeed(true);
                JOptionPane.showMessageDialog(this, "Database re-seeded successfully with 100 students and attendance!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error re-seeding database: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
