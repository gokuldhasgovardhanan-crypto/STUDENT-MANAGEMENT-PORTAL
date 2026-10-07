package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.User;
import com.attendance.service.AuthenticationService;
import com.attendance.service.NotificationService;
import com.attendance.service.SettingsService;
import com.attendance.ui.components.ModernButton;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Main application frame featuring 3-tier RBAC navigation, dynamic notifications,
 * light/dark theme switching, and seamless panel integration.
 */
public class DashboardFrame extends JFrame {

    private final AuthenticationService authService;
    private final SettingsService settingsService;
    private final NotificationService notificationService;

    private final CardLayout cardLayout;
    private final JPanel mainContent;

    // Sub-panels
    private DashboardPanel dashboardPanel;
    private StudentPanel studentPanel;
    private AttendancePanel attendancePanel;
    private SubjectPanel subjectPanel;
    private TimetablePanel timetablePanel;
    private LeaveManagementPanel leavePanel;
    private ReportsPanel reportsPanel;
    private TeacherPanel teacherPanel;
    private AuditLogPanel auditLogPanel;
    private SettingsPanel settingsPanel;
    private StudentDashboardPanel studentDashboardPanel;

    private final JLabel lblCollegeHeader;
    private final JLabel lblUserInfo;
    private final ModernButton btnNotif;
    private final ModernButton btnTheme;

    private boolean isDarkMode = false;
    private final java.util.List<JButton> sidebarButtons = new java.util.ArrayList<>();

    public DashboardFrame() {
        this.authService = AuthenticationService.getInstance();
        this.settingsService = new SettingsService();
        this.notificationService = new NotificationService();

        AppSettings settings = settingsService.getSettings();
        User currentUser = authService.getCurrentUser();
        String role = currentUser != null ? currentUser.getRole() : "TEACHER";

        setTitle("KIT Engineering College — Student Attendance Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 840);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. TOP HEADER BAR
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(Color.WHITE);
        topHeader.setPreferredSize(new Dimension(0, 58));
        topHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(0, 20, 0, 20)
        ));

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 14));
        leftHeader.setOpaque(false);

        JLabel lblAppName = new JLabel("KIT ENGINEERING COLLEGE");
        lblAppName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblAppName.setForeground(new Color(168, 28, 28)); // KIT Crimson Red

        lblCollegeHeader = new JLabel("•   Student Attendance & Self-Service Portal");
        lblCollegeHeader.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCollegeHeader.setForeground(new Color(100, 116, 139));

        leftHeader.add(lblAppName);
        leftHeader.add(lblCollegeHeader);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 11));
        rightHeader.setOpaque(false);

        String userDisplay = currentUser != null ? currentUser.getFullName() + " [" + currentUser.getRole() + "]" : "User";
        lblUserInfo = new JLabel("👤 " + userDisplay);
        lblUserInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUserInfo.setForeground(new Color(30, 41, 59));

        // Notifications button with unread count
        int unread = notificationService.getUnreadCount(role);
        btnNotif = new ModernButton(unread > 0 ? ("🔔 (" + unread + ")") : "🔔 Alerts", unread > 0 ? ModernButton.ButtonType.DANGER : ModernButton.ButtonType.SECONDARY);
        btnNotif.addActionListener(e -> {
            NotificationDialog dlg = new NotificationDialog(this, role);
            dlg.setVisible(true);
            updateNotificationCount();
        });

        // Theme toggle
        btnTheme = new ModernButton("🌙 Dark Mode", ModernButton.ButtonType.SECONDARY);
        btnTheme.addActionListener(e -> toggleTheme());

        ModernButton btnChangePwd = new ModernButton("Password", ModernButton.ButtonType.SECONDARY);
        ModernButton btnLogout = new ModernButton("Logout", ModernButton.ButtonType.DANGER);

        btnChangePwd.addActionListener(e -> changePassword());
        btnLogout.addActionListener(e -> logout());

        rightHeader.add(lblUserInfo);
        rightHeader.add(btnNotif);
        rightHeader.add(btnTheme);
        rightHeader.add(btnChangePwd);
        rightHeader.add(btnLogout);

        topHeader.add(leftHeader, BorderLayout.WEST);
        topHeader.add(rightHeader, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // 2. LEFT SIDEBAR NAVIGATION
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(15, 23, 42)); // Collegiate Navy / Slate
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        JLabel lblNav = new JLabel("PORTAL NAVIGATION");
        lblNav.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblNav.setForeground(new Color(148, 163, 184));
        lblNav.setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));
        sidebar.add(lblNav);

        cardLayout = new CardLayout();
        mainContent = new JPanel(cardLayout);

        boolean isStudent = currentUser != null && currentUser.isStudent();
        boolean isAdmin = authService.isAdmin();

        JButton initialActiveBtn;

        if (isStudent) {
            // Student-only view
            int sid = currentUser.getStudentId() != null ? currentUser.getStudentId() : 1;
            studentDashboardPanel = new StudentDashboardPanel(sid);
            mainContent.add(studentDashboardPanel, "STUDENT_DASH");

            JButton btnMyPortal = createNavButton("🏠  My Attendance Card", "STUDENT_DASH");
            sidebar.add(btnMyPortal);
            sidebar.add(Box.createVerticalStrut(4));
            initialActiveBtn = btnMyPortal;

        } else {
            // Staff & Admin Views
            dashboardPanel = new DashboardPanel();
            studentPanel = new StudentPanel();
            attendancePanel = new AttendancePanel();
            subjectPanel = new SubjectPanel();
            timetablePanel = new TimetablePanel();
            leavePanel = new LeaveManagementPanel();
            reportsPanel = new ReportsPanel();
            teacherPanel = new TeacherPanel();
            auditLogPanel = new AuditLogPanel();
            settingsPanel = new SettingsPanel();

            mainContent.add(dashboardPanel, "DASHBOARD");
            mainContent.add(studentPanel, "STUDENTS");
            mainContent.add(attendancePanel, "ATTENDANCE");
            mainContent.add(subjectPanel, "SUBJECTS");
            mainContent.add(timetablePanel, "TIMETABLE");
            mainContent.add(leavePanel, "LEAVES");
            mainContent.add(reportsPanel, "REPORTS");
            mainContent.add(teacherPanel, "TEACHERS");
            mainContent.add(auditLogPanel, "AUDIT_LOGS");
            mainContent.add(settingsPanel, "SETTINGS");

            JButton btnNavDash = createNavButton("📊  Dashboard", "DASHBOARD");
            JButton btnNavAttendance = createNavButton("📝  Attendance & QR", "ATTENDANCE");
            JButton btnNavStudents = createNavButton("👥  Students", "STUDENTS");
            JButton btnNavSubjects = createNavButton("📚  Subjects", "SUBJECTS");
            JButton btnNavTimetable = createNavButton("🗓️  Timetable", "TIMETABLE");
            JButton btnNavLeaves = createNavButton("🏖️  Leave Requests", "LEAVES");
            JButton btnNavReports = createNavButton("📑  Reports & Export", "REPORTS");
            JButton btnNavTeachers = createNavButton("👨‍🏫  Faculty / Staff", "TEACHERS");
            JButton btnNavAudit = createNavButton("🛡️  Audit Logs", "AUDIT_LOGS");
            JButton btnNavSettings = createNavButton("⚙️  Settings", "SETTINGS");

            sidebar.add(btnNavDash);
            sidebar.add(Box.createVerticalStrut(4));
            sidebar.add(btnNavAttendance);
            sidebar.add(Box.createVerticalStrut(4));
            sidebar.add(btnNavStudents);
            sidebar.add(Box.createVerticalStrut(4));
            sidebar.add(btnNavSubjects);
            sidebar.add(Box.createVerticalStrut(4));
            sidebar.add(btnNavTimetable);
            sidebar.add(Box.createVerticalStrut(4));
            sidebar.add(btnNavLeaves);
            sidebar.add(Box.createVerticalStrut(4));
            sidebar.add(btnNavReports);
            sidebar.add(Box.createVerticalStrut(4));

            if (isAdmin) {
                sidebar.add(btnNavTeachers);
                sidebar.add(Box.createVerticalStrut(4));
                sidebar.add(btnNavAudit);
                sidebar.add(Box.createVerticalStrut(4));
                sidebar.add(btnNavSettings);
                sidebar.add(Box.createVerticalStrut(4));
            }

            initialActiveBtn = btnNavDash;
        }

        JButton btnNavLogout = createNavButton("🚪  Logout", "LOGOUT");
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(btnNavLogout);

        add(sidebar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);

        setActiveNavButton(initialActiveBtn);
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

                if ("DASHBOARD".equals(cardName) && dashboardPanel != null) {
                    dashboardPanel.refreshData();
                } else if ("STUDENTS".equals(cardName) && studentPanel != null) {
                    studentPanel.applyFilter();
                } else if ("SUBJECTS".equals(cardName) && subjectPanel != null) {
                    subjectPanel.loadData();
                } else if ("TIMETABLE".equals(cardName) && timetablePanel != null) {
                    timetablePanel.loadData();
                } else if ("LEAVES".equals(cardName) && leavePanel != null) {
                    leavePanel.loadData();
                } else if ("AUDIT_LOGS".equals(cardName) && auditLogPanel != null) {
                    auditLogPanel.loadData();
                } else if ("REPORTS".equals(cardName) && reportsPanel != null) {
                    reportsPanel.generateReport();
                } else if ("TEACHERS".equals(cardName) && teacherPanel != null) {
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
        activeBtn.setBackground(new Color(168, 28, 28)); // Active KIT Crimson Red
        activeBtn.setForeground(Color.WHITE);
    }

    private void updateNotificationCount() {
        User u = authService.getCurrentUser();
        String role = u != null ? u.getRole() : "ALL";
        int unread = notificationService.getUnreadCount(role);
        btnNotif.setText(unread > 0 ? ("🔔 (" + unread + ")") : "🔔 Alerts");
    }

    private void toggleTheme() {
        try {
            if (isDarkMode) {
                UIManager.setLookAndFeel(new FlatLightLaf());
                isDarkMode = false;
                btnTheme.setText("🌙 Dark Mode");
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                isDarkMode = true;
                btnTheme.setText("☀️ Light Mode");
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not toggle theme: " + ex.getMessage(), "Theme Error", JOptionPane.ERROR_MESSAGE);
        }
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
