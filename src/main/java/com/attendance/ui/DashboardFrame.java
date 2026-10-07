package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.User;
import com.attendance.service.AuthenticationService;
import com.attendance.service.SettingsService;
import com.attendance.ui.components.ModernButton;

import javax.swing.*;
import java.awt.*;

/**
 * Main application frame featuring modern sidebar navigation, role-aware menus,
 * dynamic header information, and seamless panel switching.
 */
public class DashboardFrame extends JFrame {

    private final AuthenticationService authService;
    private final SettingsService settingsService;

    private final CardLayout cardLayout;
    private final JPanel mainContent;

    private final DashboardPanel dashboardPanel;
    private final StudentPanel studentPanel;
    private final AttendancePanel attendancePanel;
    private final ReportsPanel reportsPanel;
    private final TeacherPanel teacherPanel;
    private final SettingsPanel settingsPanel;

    private final JLabel lblCollegeHeader;
    private final JLabel lblUserInfo;

    // Sidebar buttons
    private final java.util.List<JButton> sidebarButtons = new java.util.ArrayList<>();

    public DashboardFrame() {
        this.authService = AuthenticationService.getInstance();
        this.settingsService = new SettingsService();

        setTitle("Student Attendance Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 800);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        AppSettings settings = settingsService.getSettings();
        User currentUser = authService.getCurrentUser();

        // 1. TOP HEADER BAR
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(Color.WHITE);
        topHeader.setPreferredSize(new Dimension(0, 56));
        topHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(0, 20, 0, 20)
        ));

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 14));
        leftHeader.setOpaque(false);

        JLabel lblAppName = new JLabel("Student Attendance Management System");
        lblAppName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblAppName.setForeground(new Color(30, 41, 59));

        lblCollegeHeader = new JLabel("•   " + settings.getCollegeName());
        lblCollegeHeader.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblCollegeHeader.setForeground(new Color(100, 116, 139));

        leftHeader.add(lblAppName);
        leftHeader.add(lblCollegeHeader);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        rightHeader.setOpaque(false);

        String userDisplay = currentUser != null ? currentUser.getFullName() + " (" + currentUser.getRole() + ")" : "User";
        lblUserInfo = new JLabel("👤 " + userDisplay);
        lblUserInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUserInfo.setForeground(new Color(30, 64, 175));

        ModernButton btnChangePwd = new ModernButton("Change Password", ModernButton.ButtonType.SECONDARY);
        ModernButton btnLogout = new ModernButton("Logout", ModernButton.ButtonType.DANGER);

        btnChangePwd.addActionListener(e -> changePassword());
        btnLogout.addActionListener(e -> logout());

        rightHeader.add(lblUserInfo);
        rightHeader.add(btnChangePwd);
        rightHeader.add(btnLogout);

        topHeader.add(leftHeader, BorderLayout.WEST);
        topHeader.add(rightHeader, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // 2. LEFT SIDEBAR NAVIGATION
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(15, 23, 42)); // Dark Navy/Slate
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        JLabel lblNav = new JLabel("NAVIGATION");
        lblNav.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblNav.setForeground(new Color(148, 163, 184));
        lblNav.setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));
        sidebar.add(lblNav);

        JButton btnNavDash = createNavButton("📊  Dashboard", "DASHBOARD");
        JButton btnNavStudents = createNavButton("👥  Students", "STUDENTS");
        JButton btnNavAttendance = createNavButton("📝  Attendance", "ATTENDANCE");
        JButton btnNavReports = createNavButton("📑  Reports", "REPORTS");
        JButton btnNavTeachers = createNavButton("👨‍🏫  Teachers", "TEACHERS");
        JButton btnNavSettings = createNavButton("⚙️  Settings", "SETTINGS");
        JButton btnNavLogout = createNavButton("🚪  Logout", "LOGOUT");

        sidebar.add(btnNavDash);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavStudents);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavAttendance);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavReports);
        sidebar.add(Box.createVerticalStrut(4));

        // Teachers & Settings visible for ADMIN
        boolean isAdmin = authService.isAdmin();
        if (isAdmin) {
            sidebar.add(btnNavTeachers);
            sidebar.add(Box.createVerticalStrut(4));
            sidebar.add(btnNavSettings);
            sidebar.add(Box.createVerticalStrut(4));
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(btnNavLogout);

        add(sidebar, BorderLayout.WEST);

        // 3. CENTER CONTENT AREA (CARD LAYOUT)
        cardLayout = new CardLayout();
        mainContent = new JPanel(cardLayout);

        dashboardPanel = new DashboardPanel();
        studentPanel = new StudentPanel();
        attendancePanel = new AttendancePanel();
        reportsPanel = new ReportsPanel();
        teacherPanel = new TeacherPanel();
        settingsPanel = new SettingsPanel();

        mainContent.add(dashboardPanel, "DASHBOARD");
        mainContent.add(studentPanel, "STUDENTS");
        mainContent.add(attendancePanel, "ATTENDANCE");
        mainContent.add(reportsPanel, "REPORTS");
        mainContent.add(teacherPanel, "TEACHERS");
        mainContent.add(settingsPanel, "SETTINGS");

        add(mainContent, BorderLayout.CENTER);

        // Set initial active button
        setActiveNavButton(btnNavDash);
    }

    private JButton createNavButton(String title, String cardName) {
        JButton btn = new JButton(title);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(new Color(203, 213, 225));
        btn.setBackground(new Color(15, 23, 42));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMargin(new Insets(10, 14, 10, 14));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            if ("LOGOUT".equals(cardName)) {
                logout();
            } else {
                setActiveNavButton(btn);
                cardLayout.show(mainContent, cardName);

                if ("DASHBOARD".equals(cardName)) {
                    dashboardPanel.refreshData();
                } else if ("STUDENTS".equals(cardName)) {
                    studentPanel.applyFilter();
                } else if ("REPORTS".equals(cardName)) {
                    reportsPanel.generateReport();
                } else if ("TEACHERS".equals(cardName)) {
                    teacherPanel.applyFilter();
                }
            }
        });

        sidebarButtons.add(btn);
        return btn;
    }

    private void setActiveNavButton(JButton activeBtn) {
        for (JButton b : sidebarButtons) {
            b.setBackground(new Color(15, 23, 42));
            b.setForeground(new Color(203, 213, 225));
        }
        activeBtn.setBackground(new Color(30, 64, 175)); // Active Royal Blue
        activeBtn.setForeground(Color.WHITE);
    }

    private void changePassword() {
        ChangePasswordDialog dlg = new ChangePasswordDialog(this);
        dlg.setVisible(true);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out of the system?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            authService.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
