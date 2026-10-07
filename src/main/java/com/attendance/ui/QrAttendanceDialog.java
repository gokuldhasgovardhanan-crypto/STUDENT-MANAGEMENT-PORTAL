package com.attendance.ui;

import com.attendance.model.AttendanceSession;
import com.attendance.model.Student;
import com.attendance.service.QrAttendanceService;
import com.attendance.service.StudentService;
import com.attendance.ui.components.ModernButton;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Interactive QR Code Attendance Dialog for Teachers.
 * Generates dynamic session QR, displays countdown timer, and tracks live scan entries.
 */
public class QrAttendanceDialog extends JDialog {

    private final QrAttendanceService qrService;
    private final StudentService studentService;
    private final AttendanceSession session;
    private final BufferedImage qrImage;

    private JLabel lblTimer;
    private JLabel lblScanCount;
    private Timer countdownTimer;
    private Timer pollingTimer;

    public QrAttendanceDialog(Window owner, int subjectId, String subjectName, int teacherId,
                              java.time.LocalDate date, String period, String dept, int year, String section) throws Exception {
        super(owner, "Dynamic QR Code Attendance Session", ModalityType.APPLICATION_MODAL);
        this.qrService = new QrAttendanceService();
        this.studentService = new StudentService();

        // 1. Create session token (default 5 min = 300 seconds)
        this.session = qrService.createSession(subjectId, teacherId, date, period, dept, year, section, 300);
        this.qrImage = qrService.generateQrCodeImage(session.getToken(), 320, 320);

        setSize(480, 680);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(168, 28, 28)); // KIT Crimson Red
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel lblTitle = new JLabel("📱 Live QR Code Attendance");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel(subjectName + " | " + period + " (" + dept + " Y" + year + " " + section + ")");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(254, 226, 226));

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Instructions
        JLabel lblInstruct = new JLabel("Students can scan this QR code using their student portal to mark attendance.");
        lblInstruct.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblInstruct.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInstruct.setForeground(new Color(100, 116, 139));
        body.add(lblInstruct);
        body.add(Box.createVerticalStrut(12));

        // QR Image Label
        JLabel lblQr = new JLabel(new ImageIcon(qrImage));
        lblQr.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblQr.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        body.add(lblQr);
        body.add(Box.createVerticalStrut(14));

        // Countdown Timer & Live Count
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        statsPanel.setOpaque(false);
        statsPanel.setMaximumSize(new Dimension(400, 50));

        JPanel timerCard = createStatCard("Expires In:", "05:00", new Color(185, 28, 28));
        lblTimer = (JLabel) timerCard.getComponent(1);

        JPanel countCard = createStatCard("Students Checked In:", "0", new Color(22, 101, 52));
        lblScanCount = (JLabel) countCard.getComponent(1);

        statsPanel.add(timerCard);
        statsPanel.add(countCard);
        body.add(statsPanel);
        body.add(Box.createVerticalStrut(14));

        // Manual Fallback Check-in (for students without camera)
        JPanel manualPanel = new JPanel(new BorderLayout(8, 0));
        manualPanel.setOpaque(false);
        manualPanel.setMaximumSize(new Dimension(400, 34));
        JTextField txtManualReg = new JTextField();
        txtManualReg.putClientProperty("JTextField.placeholderText", "Manual Register No (e.g. 24CSE001)...");
        ModernButton btnManual = new ModernButton("Mark", ModernButton.ButtonType.SECONDARY);
        btnManual.addActionListener(e -> {
            String reg = txtManualReg.getText().trim();
            if (!reg.isEmpty()) {
                Student s = studentService.getStudentByRegisterNo(reg);
                if (s != null) {
                    String res = qrService.validateAndRecordAttendance(session.getToken(), s.getStudentId());
                    JOptionPane.showMessageDialog(this, res, "Manual Check-In", JOptionPane.INFORMATION_MESSAGE);
                    txtManualReg.setText("");
                    updateScanCount();
                } else {
                    JOptionPane.showMessageDialog(this, "Student not found with Reg No: " + reg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        manualPanel.add(txtManualReg, BorderLayout.CENTER);
        manualPanel.add(btnManual, BorderLayout.EAST);
        body.add(manualPanel);

        add(body, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnClose = new ModernButton("End Session & Close", ModernButton.ButtonType.PRIMARY);
        btnClose.addActionListener(e -> closeSession());
        footer.add(btnClose);
        add(footer, BorderLayout.SOUTH);

        // Start Countdown & Poller
        startTimers();
    }

    private JPanel createStatCard(String label, String value, Color valColor) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setBackground(new Color(248, 250, 252));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(100, 116, 139));

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 18));
        val.setForeground(valColor);

        p.add(lbl);
        p.add(val);
        return p;
    }

    private void startTimers() {
        countdownTimer = new Timer(1000, e -> {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(session.getExpiresAt())) {
                lblTimer.setText("EXPIRED");
                lblTimer.setForeground(Color.RED);
                countdownTimer.stop();
            } else {
                Duration rem = Duration.between(now, session.getExpiresAt());
                long mins = rem.toMinutes();
                long secs = rem.minusMinutes(mins).getSeconds();
                lblTimer.setText(String.format("%02d:%02d", mins, secs));
            }
        });
        countdownTimer.start();

        pollingTimer = new Timer(3000, e -> updateScanCount());
        pollingTimer.start();
    }

    private void updateScanCount() {
        try {
            int count = qrService.getSessionPresentCount(session.getSubjectId(), session.getAttendanceDate(), session.getPeriod());
            lblScanCount.setText(String.valueOf(count));
        } catch (Exception ignored) {}
    }

    private void closeSession() {
        if (countdownTimer != null) countdownTimer.stop();
        if (pollingTimer != null) pollingTimer.stop();
        try {
            qrService.closeSession(session.getToken());
        } catch (Exception ignored) {}
        dispose();
    }
}
