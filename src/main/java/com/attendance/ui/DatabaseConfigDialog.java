package com.attendance.ui;

import com.attendance.config.DatabaseConnection;
import com.attendance.service.DatabaseInitService;
import com.attendance.ui.components.ModernButton;

import javax.swing.*;
import java.awt.*;

/**
 * Modal dialog for inspecting and adjusting MySQL database credentials and initializing schema.
 */
public class DatabaseConfigDialog extends JDialog {

    private final JTextField txtHost;
    private final JTextField txtPort;
    private final JTextField txtDbName;
    private final JTextField txtUsername;
    private final JPasswordField txtPassword;
    private final JLabel lblStatus;
    private boolean configUpdated = false;

    public DatabaseConfigDialog(Frame owner) {
        super(owner, "Database Connection Settings", true);
        setSize(480, 440);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        content.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("MySQL Database Configuration");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(15, 23, 42));
        content.add(lblTitle);

        JLabel lblDesc = new JLabel("Configure your local MySQL 8.x server connection parameters.");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(new Color(100, 116, 139));
        content.add(lblDesc);
        content.add(Box.createVerticalStrut(16));

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 12));
        form.setOpaque(false);

        txtHost = new JTextField(DatabaseConnection.getHost());
        txtPort = new JTextField(String.valueOf(DatabaseConnection.getPort()));
        txtDbName = new JTextField(DatabaseConnection.getDbName());
        txtUsername = new JTextField(DatabaseConnection.getUsername());
        txtPassword = new JPasswordField(DatabaseConnection.getPassword());

        form.add(new JLabel("Host:"));
        form.add(txtHost);
        form.add(new JLabel("Port:"));
        form.add(txtPort);
        form.add(new JLabel("Database Name:"));
        form.add(txtDbName);
        form.add(new JLabel("Username:"));
        form.add(txtUsername);
        form.add(new JLabel("Password:"));
        form.add(txtPassword);

        content.add(form);
        content.add(Box.createVerticalStrut(14));

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        content.add(lblStatus);

        add(content, BorderLayout.CENTER);

        // Buttons Bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnBar.setBackground(new Color(248, 250, 252));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnTest = new ModernButton("Test Connection", ModernButton.ButtonType.SECONDARY);
        ModernButton btnInit = new ModernButton("Initialize / Seed DB", ModernButton.ButtonType.SUCCESS);
        ModernButton btnSave = new ModernButton("Save & Connect", ModernButton.ButtonType.PRIMARY);

        btnTest.addActionListener(e -> testConn());
        btnInit.addActionListener(e -> initDatabase());
        btnSave.addActionListener(e -> saveAndClose());

        btnBar.add(btnTest);
        btnBar.add(btnInit);
        btnBar.add(btnSave);

        add(btnBar, BorderLayout.SOUTH);
    }

    private void testConn() {
        lblStatus.setText("Testing connection...");
        lblStatus.setForeground(new Color(100, 116, 139));

        String h = txtHost.getText().trim();
        int p = Integer.parseInt(txtPort.getText().trim());
        String db = txtDbName.getText().trim();
        String u = txtUsername.getText().trim();
        String pwd = new String(txtPassword.getPassword());

        boolean ok = DatabaseConnection.testConnection(h, p, db, u, pwd);
        if (ok) {
            lblStatus.setText("✓ Connection successful!");
            lblStatus.setForeground(new Color(22, 101, 52));
        } else {
            // Check if server is at least reachable without DB
            boolean serverOk = DatabaseConnection.testServerConnection(h, p, u, pwd);
            if (serverOk) {
                lblStatus.setText("⚠ MySQL Server connected, but database '" + db + "' does not exist yet. Click 'Initialize / Seed DB'.");
                lblStatus.setForeground(new Color(180, 83, 9));
            } else {
                lblStatus.setText("✗ Cannot connect. Verify host, port, username, and password.");
                lblStatus.setForeground(new Color(185, 28, 28));
            }
        }
    }

    private void initDatabase() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "This will create the database, tables, and automatically seed 100 demo students, faculty, and historical attendance.\nProceed?",
                "Initialize Database", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        saveSettingsValues();
        try {
            DatabaseInitService.initializeDatabaseAndSeed(false);
            lblStatus.setText("✓ Database & 100 Demo Students Initialized!");
            lblStatus.setForeground(new Color(22, 101, 52));
            configUpdated = true;
            JOptionPane.showMessageDialog(this, "Database initialized and seeded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            lblStatus.setText("✗ Initialization failed: " + ex.getMessage());
            lblStatus.setForeground(new Color(185, 28, 28));
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSettingsValues() {
        String h = txtHost.getText().trim();
        int p = Integer.parseInt(txtPort.getText().trim());
        String db = txtDbName.getText().trim();
        String u = txtUsername.getText().trim();
        String pwd = new String(txtPassword.getPassword());
        DatabaseConnection.saveExternalProperties(h, p, db, u, pwd);
    }

    private void saveAndClose() {
        saveSettingsValues();
        configUpdated = true;
        dispose();
    }

    public boolean isConfigUpdated() {
        return configUpdated;
    }
}
