package com.attendance.ui;

import com.attendance.model.LeaveRequest;
import com.attendance.model.Student;
import com.attendance.service.LeaveService;
import com.attendance.service.StudentService;
import com.attendance.ui.components.ModernButton;
import com.attendance.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Modal dialog for students or advisors to apply for student leave.
 */
public class ApplyLeaveDialog extends JDialog {

    private final LeaveService leaveService;
    private final StudentService studentService;
    private final Integer presetStudentId;
    private boolean saved = false;

    private JTextField txtRegisterNo;
    private JComboBox<String> cmbType;
    private JTextField txtFromDate;
    private JTextField txtToDate;
    private JTextArea txtReason;

    public ApplyLeaveDialog(Window owner, Integer studentId) {
        super(owner, "Apply for Student Leave", ModalityType.APPLICATION_MODAL);
        this.leaveService = new LeaveService();
        this.studentService = new StudentService();
        this.presetStudentId = studentId;

        setSize(460, 480);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 14));
        header.setBackground(new Color(168, 28, 28)); // KIT Crimson Red
        JLabel lblTitle = new JLabel("📝  Student Leave Application");
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

        txtRegisterNo = new JTextField();
        cmbType = new JComboBox<>(new String[]{"Medical", "Personal", "College Event", "Emergency", "Other"});
        txtFromDate = new JTextField(DateUtil.formatDisplayDate(LocalDate.now()));
        txtToDate = new JTextField(DateUtil.formatDisplayDate(LocalDate.now()));
        txtReason = new JTextArea(4, 20);
        txtReason.setLineWrap(true);
        txtReason.setWrapStyleWord(true);
        JScrollPane reasonScroll = new JScrollPane(txtReason);

        if (presetStudentId != null && presetStudentId > 0) {
            Student s = studentService.getStudentById(presetStudentId);
            if (s != null) {
                txtRegisterNo.setText(s.getRegisterNo());
                txtRegisterNo.setEditable(false);
            }
        }

        addFormField(form, gbc, 0, "Student Register No:", txtRegisterNo);
        addFormField(form, gbc, 1, "Leave Type:", cmbType);
        addFormField(form, gbc, 2, "From Date (dd-MM-yyyy):", txtFromDate);
        addFormField(form, gbc, 3, "To Date (dd-MM-yyyy):", txtToDate);
        addFormField(form, gbc, 4, "Reason / Justification:", reasonScroll);

        add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);
        ModernButton btnSubmit = new ModernButton("Submit Application", ModernButton.ButtonType.PRIMARY);

        btnCancel.addActionListener(e -> dispose());
        btnSubmit.addActionListener(e -> handleSubmit());

        footer.add(btnCancel);
        footer.add(btnSubmit);
        add(footer, BorderLayout.SOUTH);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.38;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.62;
        panel.add(comp, gbc);
    }

    private void handleSubmit() {
        String reg = txtRegisterNo.getText().trim();
        String reason = txtReason.getText().trim();

        if (reg.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Student register number is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Reason for leave application is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Student student = studentService.getStudentByRegisterNo(reg);
        if (student == null) {
            JOptionPane.showMessageDialog(this, "No student found with Register No: " + reg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate from, to;
        try {
            from = DateUtil.parseDisplayDate(txtFromDate.getText().trim());
            to = DateUtil.parseDisplayDate(txtToDate.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid dates in dd-MM-yyyy format.", "Format Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (to.isBefore(from)) {
            JOptionPane.showMessageDialog(this, "To Date cannot be before From Date.", "Date Order Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LeaveRequest req = new LeaveRequest();
        req.setStudentId(student.getStudentId());
        req.setLeaveType((String) cmbType.getSelectedItem());
        req.setFromDate(from);
        req.setToDate(to);
        req.setReason(reason);

        try {
            leaveService.applyLeave(req);
            saved = true;
            JOptionPane.showMessageDialog(this, "Leave application submitted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to apply leave: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
