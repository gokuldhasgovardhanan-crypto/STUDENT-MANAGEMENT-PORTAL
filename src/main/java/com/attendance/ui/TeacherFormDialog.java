package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.Teacher;
import com.attendance.service.SettingsService;
import com.attendance.service.TeacherService;
import com.attendance.ui.components.ModernButton;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for adding or updating faculty records with employee ID uniqueness validation.
 */
public class TeacherFormDialog extends JDialog {

    private final TeacherService teacherService;
    private final Teacher existingTeacher;
    private boolean saved = false;

    private final JTextField txtEmpId;
    private final JTextField txtName;
    private final JComboBox<String> cmbDept;
    private final JTextField txtEmail;
    private final JTextField txtPhone;

    public TeacherFormDialog(Frame owner, Teacher teacherToEdit) {
        super(owner, teacherToEdit == null ? "Add New Faculty" : "Edit Faculty - " + teacherToEdit.getEmployeeId(), true);
        this.teacherService = new TeacherService();
        this.existingTeacher = teacherToEdit;

        setSize(450, 360);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        setResizable(false);

        AppSettings settings = new SettingsService().getSettings();

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 14));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        txtEmpId = new JTextField();
        txtName = new JTextField();
        String[] depts = settings.getDepartments().toArray(new String[0]);
        cmbDept = new JComboBox<>(depts);
        txtEmail = new JTextField();
        txtPhone = new JTextField();

        formPanel.add(new JLabel("Employee ID *:"));
        formPanel.add(txtEmpId);
        formPanel.add(new JLabel("Teacher Name *:"));
        formPanel.add(txtName);
        formPanel.add(new JLabel("Department *:"));
        formPanel.add(cmbDept);
        formPanel.add(new JLabel("Email Address *:"));
        formPanel.add(txtEmail);
        formPanel.add(new JLabel("Phone Number *:"));
        formPanel.add(txtPhone);

        if (existingTeacher != null) {
            txtEmpId.setText(existingTeacher.getEmployeeId());
            txtName.setText(existingTeacher.getTeacherName());
            cmbDept.setSelectedItem(existingTeacher.getDepartment());
            txtEmail.setText(existingTeacher.getEmail());
            txtPhone.setText(existingTeacher.getPhone());
        }

        add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnBar.setBackground(new Color(248, 250, 252));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnCancel = new ModernButton("CANCEL", ModernButton.ButtonType.SECONDARY);
        ModernButton btnSave = new ModernButton(existingTeacher == null ? "ADD TEACHER" : "UPDATE TEACHER", ModernButton.ButtonType.PRIMARY);

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> saveTeacher());

        btnBar.add(btnCancel);
        btnBar.add(btnSave);
        add(btnBar, BorderLayout.SOUTH);
    }

    private void saveTeacher() {
        String empId = txtEmpId.getText().trim();
        String name = txtName.getText().trim();
        String dept = (String) cmbDept.getSelectedItem();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();

        Teacher t = existingTeacher != null ? existingTeacher : new Teacher();
        t.setEmployeeId(empId);
        t.setTeacherName(name);
        t.setDepartment(dept);
        t.setEmail(email);
        t.setPhone(phone);

        try {
            if (existingTeacher == null) {
                teacherService.addTeacher(t);
                JOptionPane.showMessageDialog(this, "Teacher added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                teacherService.updateTeacher(t);
                JOptionPane.showMessageDialog(this, "Teacher updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            saved = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "⚠ " + ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
