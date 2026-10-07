package com.attendance.ui;

import com.attendance.config.DatabaseConnection;
import com.attendance.service.AuthenticationService;
import com.attendance.service.DatabaseInitService;
import com.attendance.ui.components.ModernButton;

import javax.swing.*;
import java.awt.*;

/**
 * Professional Login Frame with demo account quick-fill, connection telemetry,
 * and database auto-configuration access.
 */
public class LoginFrame extends JFrame {

    private final AuthenticationService authService;

    private final JTextField txtUsername;
    private final JPasswordField txtPassword;
    private final JLabel lblStatus;
    private final JLabel lblDbStatus;

    public LoginFrame() {
        this.authService = AuthenticationService.getInstance();

        setTitle("Login - KIT ENGINEERING COLLEGE - Student Attendance System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 550);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header Banner with KIT Engineering College Crimson Red Branding
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(139, 0, 0)); // Crimson Dark Red #8B0000
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel lblCollege = new JLabel("KIT ENGINEERING COLLEGE");
        lblCollege.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblCollege.setForeground(new Color(254, 243, 199)); // Warm amber/gold accent
        lblCollege.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblApp = new JLabel("Student Attendance Management System");
        lblApp.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblApp.setForeground(Color.WHITE);
        lblApp.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Enterprise Academic Portal v2.0");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(254, 202, 202));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblCollege);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblApp);
        headerPanel.add(Box.createVerticalStrut(2));
        headerPanel.add(lblSub);

        add(headerPanel, BorderLayout.NORTH);

        // Center Card Panel
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(Color.WHITE);
        center.setBorder(BorderFactory.createEmptyBorder(20, 36, 16, 36));

        JLabel lblSign = new JLabel("Sign In to Your Account");
        lblSign.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSign.setForeground(new Color(30, 41, 59));
        lblSign.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(lblSign);
        center.add(Box.createVerticalStrut(14));

        // Form Fields
        JLabel lblUser = new JLabel("Username / Reg No:");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(new Color(71, 85, 105));
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtUsername = new JTextField("admin");
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPass.setForeground(new Color(71, 85, 105));
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword = new JPasswordField("admin123");
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword.addActionListener(e -> performLogin());

        center.add(lblUser);
        center.add(Box.createVerticalStrut(4));
        center.add(txtUsername);
        center.add(Box.createVerticalStrut(12));
        center.add(lblPass);
        center.add(Box.createVerticalStrut(4));
        center.add(txtPassword);
        center.add(Box.createVerticalStrut(14));

        // Quick demo buttons (Admin, Teacher, Student)
        JPanel demoBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        demoBox.setOpaque(false);
        demoBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDemo = new JLabel("Quick Fill:");
        lblDemo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDemo.setForeground(new Color(100, 116, 139));

        ModernButton btnFillAdmin = new ModernButton("Admin", ModernButton.ButtonType.SECONDARY);
        ModernButton btnFillTeacher = new ModernButton("Teacher", ModernButton.ButtonType.SECONDARY);
        ModernButton btnFillStudent = new ModernButton("Student", ModernButton.ButtonType.SECONDARY);

        btnFillAdmin.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnFillTeacher.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnFillStudent.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        btnFillAdmin.addActionListener(e -> {
            txtUsername.setText("admin");
            txtPassword.setText("admin123");
        });

        btnFillTeacher.addActionListener(e -> {
            txtUsername.setText("teacher");
            txtPassword.setText("teacher123");
        });

        btnFillStudent.addActionListener(e -> {
            txtUsername.setText("student");
            txtPassword.setText("student123");
        });

        demoBox.add(lblDemo);
        demoBox.add(btnFillAdmin);
        demoBox.add(btnFillTeacher);
        demoBox.add(btnFillStudent);
        center.add(demoBox);
        center.add(Box.createVerticalStrut(14));

        // Status Label
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(new Color(220, 38, 38));
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(lblStatus);
        center.add(Box.createVerticalStrut(10));

        // Action Buttons
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton btnLogin = new ModernButton("Login", ModernButton.ButtonType.PRIMARY);
        ModernButton btnExit = new ModernButton("Exit", ModernButton.ButtonType.SECONDARY);

        btnLogin.addActionListener(e -> performLogin());
        btnExit.addActionListener(e -> System.exit(0));

        btnPanel.add(btnLogin);
        btnPanel.add(btnExit);
        center.add(btnPanel);

        add(center, BorderLayout.CENTER);

        // Footer Bar with DB connection status & Settings link
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        lblDbStatus = new JLabel("Checking MySQL...");
        lblDbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        ModernButton btnDbSettings = new ModernButton("DB Settings", ModernButton.ButtonType.SECONDARY);
        btnDbSettings.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnDbSettings.addActionListener(e -> openDbConfig());

        footer.add(lblDbStatus, BorderLayout.WEST);
        footer.add(btnDbSettings, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        checkDatabaseTelemetry();
    }

    private void checkDatabaseTelemetry() {
        new Thread(() -> {
            boolean connected = DatabaseConnection.isConnected();
            SwingUtilities.invokeLater(() -> {
                if (connected) {
                    boolean tables = DatabaseInitService.checkTablesExist();
                    if (tables) {
                        lblDbStatus.setText("● Connected to MySQL (Database ready)");
                        lblDbStatus.setForeground(new Color(22, 101, 52));
                    } else {
                        lblDbStatus.setText("⚠ Connected, but tables not found. Click 'DB Settings' -> 'Initialize'.");
                        lblDbStatus.setForeground(new Color(180, 83, 9));
                    }
                } else {
                    lblDbStatus.setText("● MySQL Offline or Access Denied. Click 'DB Settings'.");
                    lblDbStatus.setForeground(new Color(185, 28, 28));
                }
            });
        }).start();
    }

    private void openDbConfig() {
        DatabaseConfigDialog dlg = new DatabaseConfigDialog(this);
        dlg.setVisible(true);
        if (dlg.isConfigUpdated()) {
            checkDatabaseTelemetry();
        }
    }

    private void performLogin() {
        String u = txtUsername.getText().trim();
        String p = new String(txtPassword.getPassword());

        if (u.isEmpty() || p.isEmpty()) {
            lblStatus.setText("Please enter username and password.");
            return;
        }

        lblStatus.setText("Authenticating...");
        lblStatus.setForeground(new Color(100, 116, 139));

        new Thread(() -> {
            try {
                boolean ok = authService.login(u, p);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        dispose();
                        DashboardFrame dash = new DashboardFrame();
                        dash.setVisible(true);
                    } else {
                        lblStatus.setText("Invalid username or password.");
                        lblStatus.setForeground(new Color(220, 38, 38));
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Database error. Verify DB connection settings below.");
                    lblStatus.setForeground(new Color(220, 38, 38));
                });
            }
        }).start();
    }
}
