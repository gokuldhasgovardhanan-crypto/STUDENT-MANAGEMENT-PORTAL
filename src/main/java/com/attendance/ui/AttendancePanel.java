package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.Attendance;
import com.attendance.service.AttendanceService;
import com.attendance.service.SettingsService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;
import com.attendance.util.DateUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Attendance Marking and Editing panel with duplicate protection,
 * batch status toggles, and direct MySQL synchronization.
 */
public class AttendancePanel extends JPanel {

    private final AttendanceService attendanceService;
    private final SettingsService settingsService;

    private final JTextField txtDate;
    private final JComboBox<String> cmbDept;
    private final JComboBox<Integer> cmbYear;
    private final JComboBox<String> cmbSection;

    private final ModernTable table;
    private final DefaultTableModel tableModel;
    private final JLabel lblStatusInfo;

    private List<Attendance> currentList = new ArrayList<>();
    private boolean isExistingAttendance = false;

    public AttendancePanel() {
        this.attendanceService = new AttendanceService();
        this.settingsService = new SettingsService();

        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        AppSettings settings = settingsService.getSettings();

        // Top Filter & Control Card
        JPanel topCard = new JPanel(new BorderLayout(10, 10));
        topCard.setBackground(Color.WHITE);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        filters.setOpaque(false);

        txtDate = new JTextField(LocalDate.now().format(DateUtil.DISPLAY_DATE_FORMAT), 9);
        txtDate.setToolTipText("Date format: dd-MM-yyyy");

        String[] depts = settings.getDepartments().toArray(new String[0]);
        cmbDept = new JComboBox<>(depts);
        cmbYear = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        cmbSection = new JComboBox<>(new String[]{"A", "B"});

        ModernButton btnLoad = new ModernButton("Load Students", ModernButton.ButtonType.SECONDARY);
        btnLoad.addActionListener(e -> loadStudents(true));

        // Auto-reload when filter changes
        cmbDept.addActionListener(e -> loadStudents(true));
        cmbYear.addActionListener(e -> loadStudents(true));
        cmbSection.addActionListener(e -> loadStudents(true));

        filters.add(new JLabel("Date (dd-MM-yyyy):"));
        filters.add(txtDate);
        filters.add(new JLabel("Department:"));
        filters.add(cmbDept);
        filters.add(new JLabel("Year:"));
        filters.add(cmbYear);
        filters.add(new JLabel("Section:"));
        filters.add(cmbSection);
        filters.add(btnLoad);

        topCard.add(filters, BorderLayout.WEST);

        // Action Buttons on Right
        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        quickActions.setOpaque(false);

        ModernButton btnMarkAllPresent = new ModernButton("MARK ALL PRESENT", ModernButton.ButtonType.SUCCESS);
        ModernButton btnMarkAllAbsent = new ModernButton("MARK ALL ABSENT", ModernButton.ButtonType.DANGER);
        ModernButton btnReset = new ModernButton("RESET", ModernButton.ButtonType.SECONDARY);
        ModernButton btnSave = new ModernButton("SAVE ATTENDANCE", ModernButton.ButtonType.PRIMARY);

        btnMarkAllPresent.addActionListener(e -> markAll("PRESENT"));
        btnMarkAllAbsent.addActionListener(e -> markAll("ABSENT"));
        btnReset.addActionListener(e -> resetStatuses());
        btnSave.addActionListener(e -> saveAttendance());

        quickActions.add(btnMarkAllPresent);
        quickActions.add(btnMarkAllAbsent);
        quickActions.add(btnReset);
        quickActions.add(btnSave);

        topCard.add(quickActions, BorderLayout.EAST);

        add(topCard, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"S.No", "Register No", "Student Name", "Status (Present / Absent)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3; // Only status column is editable
            }
        };

        table = new ModernTable(tableModel);

        // Combo editor for Status column
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

        // Bottom Info Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        lblStatusInfo = new JLabel("Select criteria and mark student attendance.");
        lblStatusInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatusInfo.setForeground(new Color(71, 85, 105));
        bottomBar.add(lblStatusInfo, BorderLayout.WEST);

        add(bottomBar, BorderLayout.SOUTH);

        // Initial Load
        loadStudents(false);
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

        boolean alreadyRecorded = attendanceService.isAttendanceMarked(dept, year, sec, date);
        this.isExistingAttendance = alreadyRecorded;

        if (alreadyRecorded && checkExistingPrompt) {
            int resp = JOptionPane.showConfirmDialog(this,
                    "Attendance already recorded for this date (" + dateStr + ").\nDo you want to edit it?",
                    "Attendance Already Recorded", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (resp != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            this.currentList = attendanceService.getAttendanceForClassAndDate(dept, year, sec, date);
            tableModel.setRowCount(0);

            int sno = 1;
            for (Attendance a : currentList) {
                // If not yet marked, default to PRESENT for fast, ergonomic marking
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

            if (alreadyRecorded) {
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

    private void resetStatuses() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        loadStudents(false);
    }

    private void saveAttendance() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        String dateStr = txtDate.getText().trim();
        LocalDate date = DateUtil.parseDisplayDate(dateStr);
        if (date == null) {
            JOptionPane.showMessageDialog(this, "⚠ Please select/enter a valid date.", "Missing Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (tableModel.getRowCount() == 0 || currentList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students loaded to mark attendance.", "Empty List", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Collect table statuses into currentList
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String status = (String) tableModel.getValueAt(i, 3);
            currentList.get(i).setStatus(status);
        }

        try {
            attendanceService.saveAttendance(date, currentList);
            JOptionPane.showMessageDialog(this, "✓ Attendance saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            lblStatusInfo.setText("✓ Attendance successfully recorded for " + currentList.size() + " students on " + dateStr);
            lblStatusInfo.setForeground(new Color(22, 101, 52));
            isExistingAttendance = true;
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "⚠ " + ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save attendance: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
