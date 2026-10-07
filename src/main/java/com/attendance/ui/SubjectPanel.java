package com.attendance.ui;

import com.attendance.model.Subject;
import com.attendance.service.SubjectService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel for managing academic subjects, faculty assignments, and batch enrollments.
 */
public class SubjectPanel extends JPanel {

    private final SubjectService subjectService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel lblStatus;

    private JComboBox<String> cmbDeptFilter;
    private JComboBox<String> cmbYearFilter;
    private JTextField txtSearch;

    public SubjectPanel() {
        this.subjectService = new SubjectService();
        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // 1. TOP HEADER & TOOLBAR
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JLabel lblTitle = new JLabel("Academic Subject Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 41, 59));

        JLabel lblSubtitle = new JLabel("Define curriculum courses, allocate credits, and assign faculty mentors");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitle.setForeground(new Color(100, 116, 139));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);
        titleBlock.add(lblTitle);
        titleBlock.add(lblSubtitle);
        headerBar.add(titleBlock, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtns.setOpaque(false);

        ModernButton btnAdd = new ModernButton("+ Add Subject", ModernButton.ButtonType.PRIMARY);
        ModernButton btnEdit = new ModernButton("Edit", ModernButton.ButtonType.SECONDARY);
        ModernButton btnDelete = new ModernButton("Delete", ModernButton.ButtonType.DANGER);
        ModernButton btnEnroll = new ModernButton("👥 Enroll Class", ModernButton.ButtonType.SUCCESS);

        btnAdd.addActionListener(e -> onAddSubject());
        btnEdit.addActionListener(e -> onEditSubject());
        btnDelete.addActionListener(e -> onDeleteSubject());
        btnEnroll.addActionListener(e -> onEnrollClass());

        actionBtns.add(btnAdd);
        actionBtns.add(btnEdit);
        actionBtns.add(btnDelete);
        actionBtns.add(btnEnroll);
        headerBar.add(actionBtns, BorderLayout.EAST);

        topContainer.add(headerBar);
        topContainer.add(Box.createVerticalStrut(14));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));

        filterBar.add(new JLabel("Department:"));
        cmbDeptFilter = new JComboBox<>(new String[]{"All", "CSE", "IT", "AI&DS", "ECE", "EEE", "MECH"});
        cmbDeptFilter.addActionListener(e -> loadData());
        filterBar.add(cmbDeptFilter);

        filterBar.add(Box.createHorizontalStrut(8));
        filterBar.add(new JLabel("Year:"));
        cmbYearFilter = new JComboBox<>(new String[]{"All", "1", "2", "3", "4"});
        cmbYearFilter.addActionListener(e -> loadData());
        filterBar.add(cmbYearFilter);

        filterBar.add(Box.createHorizontalStrut(8));
        filterBar.add(new JLabel("Search:"));
        txtSearch = new JTextField(16);
        txtSearch.putClientProperty("JTextField.placeholderText", "Search by code or name...");
        txtSearch.addActionListener(e -> loadData());
        filterBar.add(txtSearch);

        ModernButton btnSearch = new ModernButton("Filter", ModernButton.ButtonType.SECONDARY);
        btnSearch.addActionListener(e -> loadData());
        filterBar.add(btnSearch);

        topContainer.add(filterBar);
        add(topContainer, BorderLayout.NORTH);

        // 2. TABLE AREA
        String[] cols = {"ID", "Subject Code", "Subject Name", "Department", "Year", "Semester", "Credits", "Faculty In-Charge"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new ModernTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(260);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        table.getColumnModel().getColumn(5).setPreferredWidth(70);
        table.getColumnModel().getColumn(6).setPreferredWidth(60);
        table.getColumnModel().getColumn(7).setPreferredWidth(180);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. BOTTOM FOOTER
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        lblStatus = new JLabel("Loading subjects...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(100, 116, 139));
        footer.add(lblStatus, BorderLayout.WEST);

        add(footer, BorderLayout.SOUTH);

        loadData();
    }

    public void loadData() {
        tableModel.setRowCount(0);
        String dept = (String) cmbDeptFilter.getSelectedItem();
        String yrStr = (String) cmbYearFilter.getSelectedItem();
        String search = txtSearch.getText().trim().toLowerCase();

        Integer year = "All".equalsIgnoreCase(yrStr) ? null : Integer.parseInt(yrStr);
        String deptVal = "All".equalsIgnoreCase(dept) ? null : dept;

        List<Subject> list = subjectService.getAllSubjects();
        int count = 0;

        for (Subject s : list) {
            if (deptVal != null && !deptVal.equalsIgnoreCase(s.getDepartment())) continue;
            if (year != null && s.getYearOfStudy() != year) continue;
            if (!search.isEmpty()) {
                boolean matchCode = s.getSubjectCode().toLowerCase().contains(search);
                boolean matchName = s.getSubjectName().toLowerCase().contains(search);
                if (!matchCode && !matchName) continue;
            }

            tableModel.addRow(new Object[]{
                    s.getSubjectId(),
                    s.getSubjectCode(),
                    s.getSubjectName(),
                    s.getDepartment(),
                    s.getYearOfStudy(),
                    s.getSemester(),
                    s.getCredits(),
                    s.getTeacherName() != null ? s.getTeacherName() : "Unassigned"
            });
            count++;
        }

        lblStatus.setText(String.format("Showing %d subjects", count));
    }

    private void onAddSubject() {
        SubjectFormDialog dlg = new SubjectFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            loadData();
        }
    }

    private void onEditSubject() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a subject to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int subjectId = (Integer) tableModel.getValueAt(row, 0);
        Subject s = subjectService.getSubjectById(subjectId);
        if (s != null) {
            SubjectFormDialog dlg = new SubjectFormDialog(SwingUtilities.getWindowAncestor(this), s);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                loadData();
            }
        }
    }

    private void onDeleteSubject() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a subject to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int subjectId = (Integer) tableModel.getValueAt(row, 0);
        String code = (String) tableModel.getValueAt(row, 1);
        String name = (String) tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete subject " + code + " - " + name + "?\nThis will remove all associated student enrollments.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                subjectService.deleteSubject(subjectId);
                loadData();
                JOptionPane.showMessageDialog(this, "Subject deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to delete subject: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onEnrollClass() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a subject to batch enroll students.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int subjectId = (Integer) tableModel.getValueAt(row, 0);
        String code = (String) tableModel.getValueAt(row, 1);
        String dept = (String) tableModel.getValueAt(row, 3);
        int year = (Integer) tableModel.getValueAt(row, 4);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Enroll all registered students of " + dept + " Year " + year + " into subject " + code + "?",
                "Confirm Batch Enrollment",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int count = subjectService.enrollClass(dept, year, subjectId);
                JOptionPane.showMessageDialog(this,
                        "Successfully enrolled " + count + " students into " + code + ".",
                        "Batch Enrollment Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to enroll students: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
