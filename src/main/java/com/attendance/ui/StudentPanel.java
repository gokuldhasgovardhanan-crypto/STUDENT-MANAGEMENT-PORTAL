package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.Student;
import com.attendance.service.ExcelExportService;
import com.attendance.service.SettingsService;
import com.attendance.service.StudentService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Student Management Panel with search, filtering, CRUD operations, profile viewing,
 * and Excel export.
 */
public class StudentPanel extends JPanel {

    private final StudentService studentService;
    private final SettingsService settingsService;
    private final ExcelExportService excelExportService;

    private final JTextField txtSearch;
    private final JComboBox<String> cmbDept;
    private final JComboBox<String> cmbYear;
    private final JComboBox<String> cmbSection;

    private final ModernTable table;
    private final DefaultTableModel tableModel;
    private final JLabel lblCount;

    private List<Student> displayedStudents = new ArrayList<>();

    public StudentPanel() {
        this.studentService = new StudentService();
        this.settingsService = new SettingsService();
        this.excelExportService = new ExcelExportService();

        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        AppSettings settings = settingsService.getSettings();

        // Top Search and Filter Bar
        JPanel topFilterCard = new JPanel(new BorderLayout(10, 10));
        topFilterCard.setBackground(Color.WHITE);
        topFilterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel filtersLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filtersLeft.setOpaque(false);

        txtSearch = new JTextField(15);
        txtSearch.putClientProperty("JTextField.placeholderText", "Search Name or Reg No...");

        List<String> depts = new ArrayList<>();
        depts.add("All");
        depts.addAll(settings.getDepartments());
        cmbDept = new JComboBox<>(depts.toArray(new String[0]));

        cmbYear = new JComboBox<>(new String[]{"All", "1", "2", "3", "4"});
        cmbSection = new JComboBox<>(new String[]{"All", "A", "B"});

        ModernButton btnFilter = new ModernButton("Search", ModernButton.ButtonType.SECONDARY);
        ModernButton btnReset = new ModernButton("Reset", ModernButton.ButtonType.SECONDARY);

        btnFilter.addActionListener(e -> applyFilter());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            cmbDept.setSelectedIndex(0);
            cmbYear.setSelectedIndex(0);
            cmbSection.setSelectedIndex(0);
            applyFilter();
        });

        // Search on Enter key in text field
        txtSearch.addActionListener(e -> applyFilter());

        filtersLeft.add(new JLabel("Search:"));
        filtersLeft.add(txtSearch);
        filtersLeft.add(new JLabel("Dept:"));
        filtersLeft.add(cmbDept);
        filtersLeft.add(new JLabel("Year:"));
        filtersLeft.add(cmbYear);
        filtersLeft.add(new JLabel("Section:"));
        filtersLeft.add(cmbSection);
        filtersLeft.add(btnFilter);
        filtersLeft.add(btnReset);

        topFilterCard.add(filtersLeft, BorderLayout.WEST);

        // Action Buttons on Right
        JPanel actionsRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actionsRight.setOpaque(false);

        ModernButton btnAdd = new ModernButton("+ Add Student", ModernButton.ButtonType.PRIMARY);
        ModernButton btnEdit = new ModernButton("Edit", ModernButton.ButtonType.SECONDARY);
        ModernButton btnDelete = new ModernButton("Delete", ModernButton.ButtonType.DANGER);
        ModernButton btnProfile = new ModernButton("View Profile", ModernButton.ButtonType.SECONDARY);
        ModernButton btnImportExcel = new ModernButton("📥 Import Excel", ModernButton.ButtonType.SECONDARY);
        ModernButton btnExportExcel = new ModernButton("Export Excel", ModernButton.ButtonType.SUCCESS);

        btnAdd.addActionListener(e -> addStudent());
        btnEdit.addActionListener(e -> editSelectedStudent());
        btnDelete.addActionListener(e -> deleteSelectedStudent());
        btnProfile.addActionListener(e -> viewSelectedProfile());
        btnImportExcel.addActionListener(e -> {
            ExcelImportDialog dlg = new ExcelImportDialog(SwingUtilities.getWindowAncestor(this));
            dlg.setVisible(true);
            if (dlg.isImported()) {
                loadData();
            }
        });
        btnExportExcel.addActionListener(e -> exportStudentList());

        actionsRight.add(btnAdd);
        actionsRight.add(btnEdit);
        actionsRight.add(btnDelete);
        actionsRight.add(btnProfile);
        actionsRight.add(btnImportExcel);
        actionsRight.add(btnExportExcel);

        topFilterCard.add(actionsRight, BorderLayout.EAST);

        add(topFilterCard, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"ID", "Register No", "Student Name", "Gender", "Department", "Year", "Section", "Email", "Phone"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new ModernTable(tableModel);

        // Double-click row opens profile
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewSelectedProfile();
                }
            }
        });

        JScrollPane scrollTable = new JScrollPane(table);
        add(scrollTable, BorderLayout.CENTER);

        // Bottom status / count bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        lblCount = new JLabel("Showing 0 students");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(100, 116, 139));
        bottomBar.add(lblCount, BorderLayout.WEST);

        add(bottomBar, BorderLayout.SOUTH);

        // Initial Load
        applyFilter();
    }

    public void loadData() {
        applyFilter();
    }

    public void applyFilter() {
        String kw = txtSearch.getText().trim();
        String dept = (String) cmbDept.getSelectedItem();
        String yrStr = (String) cmbYear.getSelectedItem();
        String sec = (String) cmbSection.getSelectedItem();

        Integer year = (yrStr != null && !yrStr.equalsIgnoreCase("All")) ? Integer.parseInt(yrStr) : null;

        try {
            this.displayedStudents = studentService.searchStudents(kw, dept, year, sec);
            tableModel.setRowCount(0);

            for (Student s : displayedStudents) {
                tableModel.addRow(new Object[]{
                        s.getStudentId(),
                        s.getRegisterNo(),
                        s.getStudentName(),
                        s.getGender(),
                        s.getDepartment(),
                        s.getYearOfStudy(),
                        s.getSection(),
                        s.getEmail(),
                        s.getPhoneNumber()
                });
            }

            lblCount.setText("Showing " + displayedStudents.size() + " students");
        } catch (Exception e) {
            lblCount.setText("Error loading students: " + e.getMessage());
        }
    }

    private Student getSelectedStudent() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        int studentId = (Integer) tableModel.getValueAt(modelRow, 0);
        return studentService.getStudentById(studentId);
    }

    private void addStudent() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        StudentFormDialog dialog = new StudentFormDialog(parent, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            applyFilter();
        }
    }

    private void editSelectedStudent() {
        Student s = getSelectedStudent();
        if (s == null) {
            JOptionPane.showMessageDialog(this, "Please select a student from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        StudentFormDialog dialog = new StudentFormDialog(parent, s);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            applyFilter();
        }
    }

    private void deleteSelectedStudent() {
        Student s = getSelectedStudent();
        if (s == null) {
            JOptionPane.showMessageDialog(this, "Please select a student to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this student?\n\n" +
                "Register No: " + s.getRegisterNo() + "\n" +
                "Name: " + s.getStudentName() + "\n\n" +
                "Note: Associated historical attendance records will also be safely removed.",
                "Confirm Student Deletion",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                studentService.deleteStudent(s.getStudentId());
                JOptionPane.showMessageDialog(this, "Student deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                applyFilter();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to delete student: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewSelectedProfile() {
        Student s = getSelectedStudent();
        if (s == null) {
            JOptionPane.showMessageDialog(this, "Please select a student to view their profile.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        StudentProfileDialog dialog = new StudentProfileDialog(parent, s);
        dialog.setVisible(true);
    }

    private void exportStudentList() {
        if (displayedStudents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No student data to export.", "Empty Table", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Student_List.xlsx"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".xlsx")) {
                target = new File(target.getParentFile(), target.getName() + ".xlsx");
            }
            try {
                excelExportService.exportStudentList(displayedStudents, target);
                JOptionPane.showMessageDialog(this, "Student list exported successfully!\nFile: " + target.getAbsolutePath(),
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
