package com.attendance.ui;

import com.attendance.service.AuthenticationService;
import com.attendance.ui.components.ModernButton;

import javax.swing.*;
import java.awt.*;

/**
 * Modal dialog for changing current user's password.
 */
public class ChangePasswordDialog extends JDialog {

    private final JPasswordField txtOld;
    private final JPasswordField txtNew;
    private final JPasswordField txtConfirm;

    public ChangePasswordDialog(Frame owner) {
        super(owner, "Change Password", true);
        setSize(380, 290);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        txtOld = new JPasswordField();
        txtNew = new JPasswordField();
        txtConfirm = new JPasswordField();

        panel.add(new JLabel("Current Password:"));
        panel.add(txtOld);
        panel.add(new JLabel("New Password:"));
        panel.add(txtNew);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(txtConfirm);

        add(panel, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnBar.setBackground(new Color(248, 250, 252));
        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);
        ModernButton btnSave = new ModernButton("Update Password", ModernButton.ButtonType.PRIMARY);

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> updatePassword());

        btnBar.add(btnCancel);
        btnBar.add(btnSave);
        add(btnBar, BorderLayout.SOUTH);
    }

    private void updatePassword() {
        String oldP = new String(txtOld.getPassword());
        String newP = new String(txtNew.getPassword());
        String confP = new String(txtConfirm.getPassword());

        if (oldP.isEmpty() || newP.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (newP.length() < 6) {
            JOptionPane.showMessageDialog(this, "New password must be at least 6 characters long.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!newP.equals(confP)) {
            JOptionPane.showMessageDialog(this, "New password and confirmation do not match.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AuthenticationService auth = AuthenticationService.getInstance();
        if (auth.getCurrentUser() == null) {
            dispose();
            return;
        }

        boolean ok = auth.changePassword(auth.getCurrentUser().getUserId(), oldP, newP);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Current password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
