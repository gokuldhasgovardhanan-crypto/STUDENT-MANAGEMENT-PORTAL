package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.Student;
import com.attendance.service.SettingsService;
import com.attendance.service.StudentService;
import com.attendance.ui.components.ModernButton;
import com.attendance.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * Dialog for adding or editing student records with comprehensive field validation.
 */
public class StudentFormDialog extends JDialog {

    private final StudentService studentService;
    private final Student existingStudent;
    private boolean saved = false;

    private final JTextField txtRegNo;
    private final JTextField txtName;
    private final JComboBox<String> cmbGender;
    private final JTextField txtDob;
    private final JComboBox<String> cmbDept;
    private final JComboBox<Integer> cmbYear;
    private final JComboBox<String> cmbSection;
    private final JTextField txtEmail;
    private final JTextField txtPhone;
    private final JTextArea txtAddress;
    private final JTextField txtAdmissionDate;

    public StudentFormDialog(Frame owner, Student studentToEdit) {
        super(owner, studentToEdit == null ? "Add New Student" : "Edit Student - " + studentToEdit.getRegisterNo(), true);
        this.studentService = new StudentService();
        this.existingStudent = studentToEdit;

        setSize(520, 620);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        setResizable(false);

        AppSettings settings = new SettingsService().getSettings();

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtRegNo = new JTextField(20);
        txtName = new JTextField(20);
        cmbGender = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        txtDob = new JTextField(LocalDate.now().minusYears(19).format(DateUtil.DISPLAY_DATE_FORMAT));

        String[] depts = settings.getDepartments().toArray(new String[0]);
        cmbDept = new JComboBox<>(depts);

        cmbYear = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        cmbSection = new JComboBox<>(new String[]{"A", "B"});

        txtEmail = new JTextField(20);
        txtPhone = new JTextField(20);
        txtAddress = new JTextArea(3, 20);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        JScrollPane scrollAddr = new JScrollPane(txtAddress);

        txtAdmissionDate = new JTextField(LocalDate.now().format(DateUtil.DISPLAY_DATE_FORMAT));

        int r = 0;
        addFormField(formPanel, gbc, r++, "Register Number *:", txtRegNo);
        addFormField(formPanel, gbc, r++, "Student Full Name *:", txtName);
        addFormField(formPanel, gbc, r++, "Gender *:", cmbGender);
        addFormField(formPanel, gbc, r++, "Date of Birth (dd-MM-yyyy) *:", txtDob);
        addFormField(formPanel, gbc, r++, "Department *:", cmbDept);
        addFormField(formPanel, gbc, r++, "Year of Study *:", cmbYear);
        addFormField(formPanel, gbc, r++, "Section *:", cmbSection);
        addFormField(formPanel, gbc, r++, "Email Address *:", txtEmail);
        addFormField(formPanel, gbc, r++, "Phone Number *:", txtPhone);
        addFormField(formPanel, gbc, r++, "Admission Date (dd-MM-yyyy) *:", txtAdmissionDate);

        gbc.gridx = 0; gbc.gridy = r; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Residential Address:"), gbc);
        gbc.gridx = 1; gbc.gridy = r++; gbc.weightx = 0.7;
        formPanel.add(scrollAddr, gbc);

        // Prepopulate if editing
        if (existingStudent != null) {
            txtRegNo.setText(existingStudent.getRegisterNo());
            txtName.setText(existingStudent.getStudentName());
            cmbGender.setSelectedItem(existingStudent.getGender());
            if (existingStudent.getDateOfBirth() != null) {
                txtDob.setText(DateUtil.formatDisplayDate(existingStudent.getDateOfBirth()));
            }
            cmbDept.setSelectedItem(existingStudent.getDepartment());
            cmbYear.setSelectedItem(existingStudent.getYearOfStudy());
            cmbSection.setSelectedItem(existingStudent.getSection());
            txtEmail.setText(existingStudent.getEmail());
            txtPhone.setText(existingStudent.getPhoneNumber());
            txtAddress.setText(existingStudent.getAddress() != null ? existingStudent.getAddress() : "");
            if (existingStudent.getAdmissionDate() != null) {
                txtAdmissionDate.setText(DateUtil.formatDisplayDate(existingStudent.getAdmissionDate()));
            }
        }

        add(new JScrollPane(formPanel), BorderLayout.CENTER);

        // Buttons
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnBar.setBackground(new Color(248, 250, 252));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnClear = new ModernButton("CLEAR", ModernButton.ButtonType.SECONDARY);
        ModernButton btnCancel = new ModernButton("CANCEL", ModernButton.ButtonType.SECONDARY);
        ModernButton btnSave = new ModernButton(existingStudent == null ? "ADD STUDENT" : "SAVE CHANGES", ModernButton.ButtonType.PRIMARY);

        btnClear.addActionListener(e -> clearForm());
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> saveStudent());

        btnBar.add(btnClear);
        btnBar.add(btnCancel);
        btnBar.add(btnSave);
        add(btnBar, BorderLayout.SOUTH);
    }

    private void addFormField(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.65;
        p.add(comp, gbc);
    }

    private void clearForm() {
        if (existingStudent == null) {
            txtRegNo.setText("");
            txtName.setText("");
            txtEmail.setText("");
            txtPhone.setText("");
            txtAddress.setText("");
        } else {
            txtName.setText("");
            txtEmail.setText("");
            txtPhone.setText("");
            txtAddress.setText("");
        }
    }

    private void saveStudent() {
        String reg = txtRegNo.getText().trim();
        String name = txtName.getText().trim();
        String gender = (String) cmbGender.getSelectedItem();
        String dobStr = txtDob.getText().trim();
        String dept = (String) cmbDept.getSelectedItem();
        int year = (Integer) cmbYear.getSelectedItem();
        String sec = (String) cmbSection.getSelectedItem();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String addr = txtAddress.getText().trim();
        String admStr = txtAdmissionDate.getText().trim();

        LocalDate dob = DateUtil.parseDisplayDate(dobStr);
        if (dob == null) {
            JOptionPane.showMessageDialog(this, "Please enter a valid Date of Birth in dd-MM-yyyy format.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate adm = DateUtil.parseDisplayDate(admStr);
        if (adm == null) {
            JOptionPane.showMessageDialog(this, "Please enter a valid Admission Date in dd-MM-yyyy format.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Student s = existingStudent != null ? existingStudent : new Student();
        s.setRegisterNo(reg);
        s.setStudentName(name);
        s.setGender(gender);
        s.setDateOfBirth(dob);
        s.setDepartment(dept);
        s.setYearOfStudy(year);
        s.setSection(sec);
        s.setEmail(email);
        s.setPhoneNumber(phone);
        s.setAddress(addr);
        s.setAdmissionDate(adm);

        try {
            if (existingStudent == null) {
                studentService.addStudent(s);
                JOptionPane.showMessageDialog(this, "Student added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                studentService.updateStudent(s);
                JOptionPane.showMessageDialog(this, "Student updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            saved = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "⚠ " + ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving student: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
