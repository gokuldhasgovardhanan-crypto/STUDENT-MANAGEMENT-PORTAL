package com.attendance.ui;

import com.attendance.model.Subject;
import com.attendance.model.Teacher;
import com.attendance.service.SubjectService;
import com.attendance.service.TeacherService;
import com.attendance.ui.components.ModernButton;
import com.attendance.util.ValidationUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Modal dialog for creating and editing Academic Subjects.
 */
public class SubjectFormDialog extends JDialog {

    private final SubjectService subjectService;
    private final TeacherService teacherService;
    private final Subject existingSubject;
    private boolean saved = false;

    private JTextField txtCode;
    private JTextField txtName;
    private JComboBox<String> cmbDept;
    private JComboBox<Integer> cmbYear;
    private JComboBox<String> cmbSemester;
    private JSpinner spnCredits;
    private JComboBox<TeacherItem> cmbTeacher;

    public SubjectFormDialog(Window owner, Subject subject) {
        super(owner, subject == null ? "Add New Subject" : "Edit Subject", ModalityType.APPLICATION_MODAL);
        this.subjectService = new SubjectService();
        this.teacherService = new TeacherService();
        this.existingSubject = subject;

        setSize(480, 520);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 14));
        header.setBackground(new Color(168, 28, 28)); // KIT Crimson Red
        JLabel lblTitle = new JLabel(subject == null ? "📚  Add New Subject" : "✏️  Edit Subject");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        form.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        txtCode = new JTextField();
        txtName = new JTextField();
        cmbDept = new JComboBox<>(new String[]{"CSE", "IT", "AI&DS", "ECE", "EEE", "MECH"});
        cmbYear = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        cmbSemester = new JComboBox<>(new String[]{"I", "II", "III", "IV", "V", "VI", "VII", "VIII"});
        spnCredits = new JSpinner(new SpinnerNumberModel(3, 1, 6, 1));
        cmbTeacher = new JComboBox<>();

        loadTeachers();

        addFormField(form, gbc, 0, "Subject Code (e.g. CS501):", txtCode);
        addFormField(form, gbc, 1, "Subject Name:", txtName);
        addFormField(form, gbc, 2, "Department:", cmbDept);
        addFormField(form, gbc, 3, "Year of Study:", cmbYear);
        addFormField(form, gbc, 4, "Semester:", cmbSemester);
        addFormField(form, gbc, 5, "Credits:", spnCredits);
        addFormField(form, gbc, 6, "Faculty In-Charge:", cmbTeacher);

        if (existingSubject != null) {
            populateData();
        }

        add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);
        ModernButton btnSave = new ModernButton(existingSubject == null ? "Add Subject" : "Update Subject", ModernButton.ButtonType.PRIMARY);

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> handleSave());

        footer.add(btnCancel);
        footer.add(btnSave);
        add(footer, BorderLayout.SOUTH);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        panel.add(comp, gbc);
    }

    private void loadTeachers() {
        cmbTeacher.addItem(new TeacherItem(null, "-- None / Unassigned --"));
        List<Teacher> teachers = teacherService.getAllTeachers();
        for (Teacher t : teachers) {
            cmbTeacher.addItem(new TeacherItem(t.getTeacherId(), t.getTeacherName() + " (" + t.getDepartment() + ")"));
        }
    }

    private void populateData() {
        txtCode.setText(existingSubject.getSubjectCode());
        txtName.setText(existingSubject.getSubjectName());
        cmbDept.setSelectedItem(existingSubject.getDepartment());
        cmbYear.setSelectedItem(existingSubject.getYearOfStudy());
        cmbSemester.setSelectedItem(existingSubject.getSemester());
        spnCredits.setValue(existingSubject.getCredits());

        if (existingSubject.getTeacherId() != null) {
            for (int i = 0; i < cmbTeacher.getItemCount(); i++) {
                TeacherItem item = cmbTeacher.getItemAt(i);
                if (item.id != null && item.id.equals(existingSubject.getTeacherId())) {
                    cmbTeacher.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void handleSave() {
        String code = txtCode.getText().trim();
        String name = txtName.getText().trim();

        if (ValidationUtil.isEmpty(code)) {
            JOptionPane.showMessageDialog(this, "Subject code is mandatory.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (ValidationUtil.isEmpty(name)) {
            JOptionPane.showMessageDialog(this, "Subject name is mandatory.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Subject s = existingSubject != null ? existingSubject : new Subject();
        s.setSubjectCode(code.toUpperCase());
        s.setSubjectName(name);
        s.setDepartment((String) cmbDept.getSelectedItem());
        s.setYearOfStudy((Integer) cmbYear.getSelectedItem());
        s.setSemester((String) cmbSemester.getSelectedItem());
        s.setCredits((Integer) spnCredits.getValue());

        TeacherItem selectedTeacher = (TeacherItem) cmbTeacher.getSelectedItem();
        s.setTeacherId(selectedTeacher != null ? selectedTeacher.id : null);

        try {
            if (existingSubject == null) {
                subjectService.saveSubject(s);
            } else {
                subjectService.updateSubject(s);
            }
            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save subject: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private static class TeacherItem {
        final Integer id;
        final String name;

        TeacherItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
