package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.Teacher;
import com.attendance.service.SettingsService;
import com.attendance.service.TeacherService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Teacher/Faculty management panel with search, add, edit, and delete functionality.
 */
public class TeacherPanel extends JPanel {

    private final TeacherService teacherService;
    private final SettingsService settingsService;

    private final JTextField txtSearch;
    private final JComboBox<String> cmbDept;
    private final ModernTable table;
    private final DefaultTableModel tableModel;
    private final JLabel lblCount;

    private List<Teacher> currentList = new ArrayList<>();

    public TeacherPanel() {
        this.teacherService = new TeacherService();
        this.settingsService = new SettingsService();

        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        AppSettings settings = settingsService.getSettings();

        // Top Filter & Actions Card
        JPanel topCard = new JPanel(new BorderLayout(10, 10));
        topCard.setBackground(Color.WHITE);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filters.setOpaque(false);

        txtSearch = new JTextField(16);
        txtSearch.putClientProperty("JTextField.placeholderText", "Search Name or Emp ID...");

        List<String> depts = new ArrayList<>();
        depts.add("All");
        depts.addAll(settings.getDepartments());
        cmbDept = new JComboBox<>(depts.toArray(new String[0]));

        ModernButton btnSearch = new ModernButton("Search", ModernButton.ButtonType.SECONDARY);
        ModernButton btnReset = new ModernButton("Reset", ModernButton.ButtonType.SECONDARY);

        btnSearch.addActionListener(e -> applyFilter());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            cmbDept.setSelectedIndex(0);
            applyFilter();
        });
        txtSearch.addActionListener(e -> applyFilter());

        filters.add(new JLabel("Search:"));
        filters.add(txtSearch);
        filters.add(new JLabel("Department:"));
        filters.add(cmbDept);
        filters.add(btnSearch);
        filters.add(btnReset);

        topCard.add(filters, BorderLayout.WEST);

        // Action Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);

        ModernButton btnAdd = new ModernButton("+ Add Faculty", ModernButton.ButtonType.PRIMARY);
        ModernButton btnEdit = new ModernButton("Edit", ModernButton.ButtonType.SECONDARY);
        ModernButton btnDelete = new ModernButton("Delete", ModernButton.ButtonType.DANGER);

        btnAdd.addActionListener(e -> addTeacher());
        btnEdit.addActionListener(e -> editTeacher());
        btnDelete.addActionListener(e -> deleteTeacher());

        actions.add(btnAdd);
        actions.add(btnEdit);
        actions.add(btnDelete);

        topCard.add(actions, BorderLayout.EAST);

        add(topCard, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"ID", "Employee ID", "Teacher Name", "Department", "Email Address", "Phone Number"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new ModernTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        lblCount = new JLabel("Showing 0 faculty members");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(100, 116, 139));
        bottomBar.add(lblCount, BorderLayout.WEST);

        add(bottomBar, BorderLayout.SOUTH);

        applyFilter();
    }

    public void applyFilter() {
        String kw = txtSearch.getText().trim();
        String dept = (String) cmbDept.getSelectedItem();

        try {
            this.currentList = teacherService.searchTeachers(kw, dept);
            tableModel.setRowCount(0);

            for (Teacher t : currentList) {
                tableModel.addRow(new Object[]{
                        t.getTeacherId(),
                        t.getEmployeeId(),
                        t.getTeacherName(),
                        t.getDepartment(),
                        t.getEmail(),
                        t.getPhone()
                });
            }
            lblCount.setText("Showing " + currentList.size() + " faculty members");
        } catch (Exception ex) {
            lblCount.setText("Error loading faculty: " + ex.getMessage());
        }
    }

    private Teacher getSelectedTeacher() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        int id = (Integer) tableModel.getValueAt(modelRow, 0);
        return teacherService.getTeacherById(id);
    }

    private void addTeacher() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        TeacherFormDialog dialog = new TeacherFormDialog(owner, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            applyFilter();
        }
    }

    private void editTeacher() {
        Teacher t = getSelectedTeacher();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Please select a teacher to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        TeacherFormDialog dialog = new TeacherFormDialog(owner, t);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            applyFilter();
        }
    }

    private void deleteTeacher() {
        Teacher t = getSelectedTeacher();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Please select a teacher to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete teacher: " + t.getTeacherName() + " (" + t.getEmployeeId() + ")?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                teacherService.deleteTeacher(t.getTeacherId());
                JOptionPane.showMessageDialog(this, "Teacher deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                applyFilter();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to delete: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
