package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.Attendance;
import com.attendance.model.Subject;
import com.attendance.model.TimetableEntry;
import com.attendance.service.AttendanceService;
import com.attendance.service.AuthenticationService;
import com.attendance.service.SettingsService;
import com.attendance.service.SubjectService;
import com.attendance.service.TimetableService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;
import com.attendance.util.DateUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Attendance Marking Panel supporting both Daily and Subject-Wise Period Sessions,
 * Timetable Auto-Suggestions, Live QR Code Generation, and Audit-Logged Attendance Corrections.
 */
public class AttendancePanel extends JPanel {

    private final AttendanceService attendanceService;
    private final SettingsService settingsService;
    private final SubjectService subjectService;
    private final TimetableService timetableService;
    private final AuthenticationService authService;

    private final JTextField txtDate;
    private final JComboBox<String> cmbDept;
    private final JComboBox<Integer> cmbYear;
    private final JComboBox<String> cmbSection;

    // Session controls
    private final JComboBox<SubjectItem> cmbSubject;
    private final JComboBox<String> cmbPeriod;
    private final JCheckBox chkSessionMode;

    private final ModernTable table;
    private final DefaultTableModel tableModel;
    private final JLabel lblStatusInfo;

    private List<Attendance> currentList = new ArrayList<>();
    private boolean isExistingAttendance = false;

    public AttendancePanel() {
        this.attendanceService = new AttendanceService();
        this.settingsService = new SettingsService();
        this.subjectService = new SubjectService();
        this.timetableService = new TimetableService();
        this.authService = AuthenticationService.getInstance();

        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        AppSettings settings = settingsService.getSettings();

        // 1. TOP FILTER & CONTROL CARD
        JPanel topCard = new JPanel();
        topCard.setLayout(new BoxLayout(topCard, BoxLayout.Y_AXIS));
        topCard.setBackground(Color.WHITE);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // Row 1: Primary Filter
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        leftFilters.setOpaque(false);

        txtDate = new JTextField(LocalDate.now().format(DateUtil.DISPLAY_DATE_FORMAT), 9);
        txtDate.setToolTipText("Date format: dd-MM-yyyy");

        String[] depts = settings.getDepartments().toArray(new String[0]);
        cmbDept = new JComboBox<>(depts);
        cmbYear = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        cmbSection = new JComboBox<>(new String[]{"A", "B"});

        cmbSubject = new JComboBox<>();
        cmbPeriod = new JComboBox<>(new String[]{"Period 1", "Period 2", "Period 3", "Period 4", "Period 5", "Period 6"});
        chkSessionMode = new JCheckBox("Subject/Period Mode", true);
        chkSessionMode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkSessionMode.setForeground(new Color(30, 41, 59));

        leftFilters.add(new JLabel("Date:"));
        leftFilters.add(txtDate);
        leftFilters.add(new JLabel("Dept:"));
        leftFilters.add(cmbDept);
        leftFilters.add(new JLabel("Year:"));
        leftFilters.add(cmbYear);
        leftFilters.add(new JLabel("Sec:"));
        leftFilters.add(cmbSection);
        leftFilters.add(chkSessionMode);

        ModernButton btnLoad = new ModernButton("Load Students", ModernButton.ButtonType.SECONDARY);
        btnLoad.addActionListener(e -> loadStudents(true));
        leftFilters.add(btnLoad);

        row1.add(leftFilters, BorderLayout.WEST);

        // Action Buttons on Right
        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        quickActions.setOpaque(false);

        ModernButton btnMarkAllPresent = new ModernButton("ALL PRESENT", ModernButton.ButtonType.SUCCESS);
        ModernButton btnMarkAllAbsent = new ModernButton("ALL ABSENT", ModernButton.ButtonType.DANGER);
        ModernButton btnQr = new ModernButton("📱 LIVE QR", ModernButton.ButtonType.PRIMARY);
        ModernButton btnSave = new ModernButton("SAVE ATTENDANCE", ModernButton.ButtonType.PRIMARY);

        btnMarkAllPresent.addActionListener(e -> markAll("PRESENT"));
        btnMarkAllAbsent.addActionListener(e -> markAll("ABSENT"));
        btnQr.addActionListener(e -> onGenerateQr());
        btnSave.addActionListener(e -> saveAttendance());

        quickActions.add(btnMarkAllPresent);
        quickActions.add(btnMarkAllAbsent);
        quickActions.add(btnQr);
        quickActions.add(btnSave);

        row1.add(quickActions, BorderLayout.EAST);
        topCard.add(row1);

        // Row 2: Session details & Timetable Auto-Suggest
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        row2.setOpaque(false);
        row2.add(new JLabel("Subject:"));
        cmbSubject.setPreferredSize(new Dimension(280, 28));
        row2.add(cmbSubject);

        row2.add(new JLabel("Period:"));
        row2.add(cmbPeriod);

        ModernButton btnAutoSuggest = new ModernButton("⚡ Auto-Suggest from Timetable", ModernButton.ButtonType.SECONDARY);
        btnAutoSuggest.addActionListener(e -> autoSuggestTimetable());
        row2.add(btnAutoSuggest);

        ModernButton btnCorrect = new ModernButton("✏️ Correct Attendance", ModernButton.ButtonType.SECONDARY);
        btnCorrect.addActionListener(e -> onCorrectAttendance());
        row2.add(btnCorrect);

        topCard.add(Box.createVerticalStrut(4));
        topCard.add(row2);

        add(topCard, BorderLayout.NORTH);

        // Filter event listeners
        cmbDept.addActionListener(e -> {
            reloadSubjects();
            loadStudents(true);
        });
        cmbYear.addActionListener(e -> {
            reloadSubjects();
            loadStudents(true);
        });
        cmbSection.addActionListener(e -> loadStudents(true));
        cmbSubject.addActionListener(e -> loadStudents(false));
        cmbPeriod.addActionListener(e -> loadStudents(false));

        // 2. CENTER TABLE
        String[] cols = {"S.No", "Register No", "Student Name", "Status (Present / Absent)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3;
            }
        };

        table = new ModernTable(tableModel);

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"PRESENT", "ABSENT"});
        statusCombo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        TableColumn statusCol = table.getColumnModel().getColumn(3);
        statusCol.setCellEditor(new DefaultCellEditor(statusCombo));
        statusCol.setCellRenderer(new ModernTable.StatusBadgeRenderer());
        statusCol.setPreferredWidth(160);

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(320);

        add(new JScrollPane(table), BorderLayout.CENTER);

        // 3. BOTTOM INFO BAR
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        lblStatusInfo = new JLabel("Select criteria and mark student attendance.");
        lblStatusInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatusInfo.setForeground(new Color(71, 85, 105));
        bottomBar.add(lblStatusInfo, BorderLayout.WEST);

        add(bottomBar, BorderLayout.SOUTH);

        reloadSubjects();
        loadStudents(false);
    }

    private void reloadSubjects() {
        cmbSubject.removeAllItems();
        String dept = (String) cmbDept.getSelectedItem();
        Integer year = (Integer) cmbYear.getSelectedItem();
        if (dept != null && year != null) {
            try {
                List<Subject> list = subjectService.getSubjectsByDeptAndYear(dept, year);
                for (Subject s : list) {
                    cmbSubject.addItem(new SubjectItem(s.getSubjectId(), s.getSubjectCode() + " - " + s.getSubjectName()));
                }
            } catch (Exception ex) {
                // Log or fail silently on UI populate
            }
        }
    }

    private void autoSuggestTimetable() {
        String dept = (String) cmbDept.getSelectedItem();
        Integer year = (Integer) cmbYear.getSelectedItem();
        String sec = (String) cmbSection.getSelectedItem();
        String period = (String) cmbPeriod.getSelectedItem();

        LocalDate today = LocalDate.now();
        DayOfWeek dow = today.getDayOfWeek();
        String dayName = dow.name().substring(0, 1) + dow.name().substring(1).toLowerCase();

        TimetableEntry suggested = timetableService.suggestSubjectForAttendance(dayName, period, null, dept, year != null ? year : 1, sec);
        if (suggested != null) {
            for (int i = 0; i < cmbSubject.getItemCount(); i++) {
                SubjectItem item = cmbSubject.getItemAt(i);
                if (item.id == suggested.getSubjectId()) {
                    cmbSubject.setSelectedIndex(i);
                    lblStatusInfo.setText("⚡ Auto-suggested " + suggested.getSubjectCode() + " (" + suggested.getSubjectName() + ") from timetable.");
                    lblStatusInfo.setForeground(new Color(30, 64, 175));
                    return;
                }
            }
        }
        JOptionPane.showMessageDialog(this, "No specific slot scheduled for " + dayName + " " + period + " in timetable.", "Timetable Suggestion", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadStudents(boolean checkExistingPrompt) {
        String dateStr = txtDate.getText().trim();
        LocalDate date = DateUtil.parseDisplayDate(dateStr);
        if (date == null) {
            lblStatusInfo.setText("⚠ Please enter a valid date in dd-MM-yyyy format.");
            lblStatusInfo.setForeground(new Color(185, 28, 28));
            return;
        }

        String dept = (String) cmbDept.getSelectedItem();
        int year = (Integer) cmbYear.getSelectedItem();
        String sec = (String) cmbSection.getSelectedItem();
        SubjectItem selectedSub = (SubjectItem) cmbSubject.getSelectedItem();
        String period = (String) cmbPeriod.getSelectedItem();

        try {
            if (chkSessionMode.isSelected() && selectedSub != null) {
                this.currentList = attendanceService.getAttendanceForSession(selectedSub.id, dept, year, sec, date, period);
                boolean recorded = attendanceService.isSessionRecorded(selectedSub.id, dept, year, sec, date, period);
                this.isExistingAttendance = recorded;
            } else {
                this.currentList = attendanceService.getAttendanceForClassAndDate(dept, year, sec, date);
                boolean recorded = attendanceService.isAttendanceMarked(dept, year, sec, date);
                this.isExistingAttendance = recorded;
            }

            tableModel.setRowCount(0);
            int sno = 1;
            for (Attendance a : currentList) {
                String status = a.getStatus();
                if (status == null || status.trim().isEmpty()) {
                    status = "PRESENT";
                    a.setStatus("PRESENT");
                }
                tableModel.addRow(new Object[]{
                        sno++,
                        a.getRegisterNo(),
                        a.getStudentName(),
                        status.toUpperCase()
                });
            }

            if (isExistingAttendance) {
                lblStatusInfo.setText("✓ Loaded recorded attendance for " + currentList.size() + " students (Editing Mode).");
                lblStatusInfo.setForeground(new Color(234, 88, 12));
            } else {
                lblStatusInfo.setText("✓ Loaded " + currentList.size() + " students. Ready to mark attendance.");
                lblStatusInfo.setForeground(new Color(22, 101, 52));
            }

        } catch (Exception ex) {
            lblStatusInfo.setText("Error loading class: " + ex.getMessage());
            lblStatusInfo.setForeground(new Color(185, 28, 28));
        }
    }

    private void markAll(String status) {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(status, i, 3);
        }
    }

    private void saveAttendance() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        String dateStr = txtDate.getText().trim();
        LocalDate date = DateUtil.parseDisplayDate(dateStr);
        if (date == null) {
            JOptionPane.showMessageDialog(this, "⚠ Please select a valid date.", "Missing Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (tableModel.getRowCount() == 0 || currentList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students loaded to mark attendance.", "Empty List", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SubjectItem selectedSub = (SubjectItem) cmbSubject.getSelectedItem();
        String period = (String) cmbPeriod.getSelectedItem();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String status = (String) tableModel.getValueAt(i, 3);
            currentList.get(i).setStatus(status);
            if (chkSessionMode.isSelected() && selectedSub != null) {
                currentList.get(i).setSubjectId(selectedSub.id);
                currentList.get(i).setPeriod(period);
            }
        }

        try {
            attendanceService.saveAttendance(date, currentList);
            JOptionPane.showMessageDialog(this, "✓ Attendance saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            lblStatusInfo.setText("✓ Attendance successfully recorded for " + currentList.size() + " students on " + dateStr);
            lblStatusInfo.setForeground(new Color(22, 101, 52));
            isExistingAttendance = true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save attendance: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onGenerateQr() {
        SubjectItem selectedSub = (SubjectItem) cmbSubject.getSelectedItem();
        if (selectedSub == null) {
            JOptionPane.showMessageDialog(this, "Please select a Subject to generate QR Attendance.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String dept = (String) cmbDept.getSelectedItem();
        int year = (Integer) cmbYear.getSelectedItem();
        String sec = (String) cmbSection.getSelectedItem();
        String period = (String) cmbPeriod.getSelectedItem();
        LocalDate date = DateUtil.parseDisplayDate(txtDate.getText().trim());
        if (date == null) date = LocalDate.now();

        try {
            QrAttendanceDialog dlg = new QrAttendanceDialog(
                    SwingUtilities.getWindowAncestor(this),
                    selectedSub.id,
                    selectedSub.label,
                    1,
                    date,
                    period,
                    dept,
                    year,
                    sec
            );
            dlg.setVisible(true);
            loadStudents(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to initiate QR session: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCorrectAttendance() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student row from the table to perform attendance correction.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Attendance att = currentList.get(row);
        String currentStatus = (String) tableModel.getValueAt(row, 3);
        String targetStatus = "PRESENT".equalsIgnoreCase(currentStatus) ? "ABSENT" : "PRESENT";

        String reason = JOptionPane.showInputDialog(
                this,
                "Change status for " + att.getStudentName() + " (" + att.getRegisterNo() + ") from " + currentStatus + " to " + targetStatus + ":\nEnter mandatory audit reason:",
                "Attendance Override & Correction Audit",
                JOptionPane.QUESTION_MESSAGE
        );

        if (reason == null || reason.trim().isEmpty()) {
            if (reason != null) {
                JOptionPane.showMessageDialog(this, "Correction reason is mandatory for compliance audit.", "Reason Required", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        String changer = authService.getCurrentUser() != null ? authService.getCurrentUser().getFullName() : "Faculty Advisor";
        try {
            attendanceService.correctAttendance(att.getAttendanceId(), att.getStudentId(), targetStatus, changer, reason.trim());
            tableModel.setValueAt(targetStatus, row, 3);
            att.setStatus(targetStatus);
            JOptionPane.showMessageDialog(this, "Attendance corrected and logged to audit trail.", "Correction Logged", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to correct attendance: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class SubjectItem {
        final int id;
        final String label;

        SubjectItem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
