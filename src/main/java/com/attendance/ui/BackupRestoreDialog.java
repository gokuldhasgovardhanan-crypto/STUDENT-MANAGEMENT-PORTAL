package com.attendance.ui;

import com.attendance.service.BackupRestoreService;
import com.attendance.ui.components.ModernButton;
import com.attendance.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;

/**
 * Dialog for generating portable SQL database backups and restoring data state.
 */
public class BackupRestoreDialog extends JDialog {

    private final BackupRestoreService backupRestoreService;
    private JLabel lblStatus;

    public BackupRestoreDialog(Window owner) {
        super(owner, "Database Backup & Disaster Recovery", ModalityType.APPLICATION_MODAL);
        this.backupRestoreService = new BackupRestoreService();

        setSize(520, 360);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 14));
        header.setBackground(new Color(168, 28, 28)); // KIT Crimson Red
        JLabel lblTitle = new JLabel("💾  Database Backup & Recovery");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle);
        add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new GridLayout(2, 1, 14, 14));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Backup Card
        JPanel pnlBackup = createActionCard(
                "Export Database Backup (.sql)",
                "Creates an encrypted, portable SQL script containing tables, enrolled students, attendance logs, and settings.",
                "Export Backup",
                ModernButton.ButtonType.PRIMARY,
                e -> handleBackup()
        );

        // Restore Card
        JPanel pnlRestore = createActionCard(
                "Restore Database from SQL (.sql)",
                "Restores tables and records from a previous SQL backup file. Warning: Existing matching records will be updated.",
                "Select File to Restore",
                ModernButton.ButtonType.DANGER,
                e -> handleRestore()
        );

        body.add(pnlBackup);
        body.add(pnlRestore);
        add(body, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        lblStatus = new JLabel("Ready for backup/restore operation.");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(100, 116, 139));
        footer.add(lblStatus, BorderLayout.WEST);

        ModernButton btnClose = new ModernButton("Close", ModernButton.ButtonType.SECONDARY);
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createActionCard(String title, String desc, String btnLabel, ModernButton.ButtonType btnType, java.awt.event.ActionListener action) {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(new Color(248, 250, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JPanel textBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        textBlock.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(new Color(30, 41, 59));

        JLabel d = new JLabel("<html>" + desc + "</html>");
        d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        d.setForeground(new Color(100, 116, 139));

        textBlock.add(t);
        textBlock.add(d);
        card.add(textBlock, BorderLayout.CENTER);

        ModernButton btn = new ModernButton(btnLabel, btnType);
        btn.addActionListener(action);
        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        btnWrapper.setOpaque(false);
        btnWrapper.add(btn);
        card.add(btnWrapper, BorderLayout.EAST);

        return card;
    }

    private void handleBackup() {
        JFileChooser chooser = new JFileChooser();
        String defaultName = "KIT_Attendance_Backup_" + LocalDate.now() + ".sql";
        chooser.setSelectedFile(new File(defaultName));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            try {
                lblStatus.setText("Exporting backup...");
                backupRestoreService.backupDatabase(target);
                lblStatus.setText("Backup created at: " + target.getName());
                JOptionPane.showMessageDialog(this,
                        "Database successfully exported to:\n" + target.getAbsolutePath(),
                        "Backup Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                lblStatus.setText("Backup failed: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Failed to create backup: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleRestore() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Restoring from a backup will execute database SQL statements and overwrite conflicting rows.\nDo you want to proceed?",
                "Confirm Database Restore",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File source = chooser.getSelectedFile();
            try {
                lblStatus.setText("Restoring database...");
                int count = backupRestoreService.restoreDatabase(source);
                lblStatus.setText("Restored " + count + " statements.");
                JOptionPane.showMessageDialog(this,
                        "Database state successfully restored from:\n" + source.getName() + "\nExecuted statements: " + count,
                        "Restore Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                lblStatus.setText("Restore error: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Failed to restore database: " + ex.getMessage(), "Restore Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
