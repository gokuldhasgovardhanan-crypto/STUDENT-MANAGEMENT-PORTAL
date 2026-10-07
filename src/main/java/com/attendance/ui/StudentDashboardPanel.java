package com.attendance.ui;

import com.attendance.model.*;
import com.attendance.service.*;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;
import com.attendance.util.DateUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Dedicated Student Portal Dashboard.
 * Displays personal attendance percentage, shortage calculator, subject breakdown,
 * timetable, leave requests, QR check-in, and Attendance Card export.
 */
public class StudentDashboardPanel extends JPanel {

    private final StudentService studentService;
    private final AttendanceService attendanceService;
    private final AttendanceCalculatorService calculatorService;
    private final TimetableService timetableService;
    private final LeaveService leaveService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final SettingsService settingsService;

    private final int studentId;
    private Student currentStudent;
    private StudentAttendanceSummary summary;
    private List<Map<String, Object>> subjectBreakdown;

    private JLabel lblName;
    private JLabel lblReg;
    private JLabel lblDept;
    private JLabel lblOverallPct;
    private JLabel lblStatusBadge;
    private JLabel lblCalcAdvice;

    private DefaultTableModel subjectModel;
    private DefaultTableModel timetableModel;
    private DefaultTableModel leaveModel;

    public StudentDashboardPanel(int studentId) {
        this.studentId = studentId;
        this.studentService = new StudentService();
        this.attendanceService = new AttendanceService();
        this.calculatorService = new AttendanceCalculatorService();
        this.timetableService = new TimetableService();
        this.leaveService = new LeaveService();
        this.excelExportService = new ExcelExportService();
        this.pdfExportService = new PdfExportService();
        this.settingsService = new SettingsService();

        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        loadStudentData();
        initUI();
    }

    private void loadStudentData() {
        this.currentStudent = studentService.getStudentById(studentId);
        if (currentStudent == null) {
            // Fallback to first student if studentId is 0 or unmatched
            List<Student> all = studentService.getAllStudents();
            if (!all.isEmpty()) this.currentStudent = all.get(0);
        }
        if (currentStudent != null) {
            this.summary = attendanceService.getStudentAttendanceSummary(currentStudent.getStudentId());
            this.subjectBreakdown = attendanceService.getSubjectWiseAttendanceForStudent(currentStudent.getStudentId());
        }
    }

    private void initUI() {
        removeAll();

        // 1. TOP HEADER & PROFILE CARD
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Header bar with actions
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);
        JLabel lblTitle = new JLabel("KIT ENGINEERING COLLEGE — Student Portal");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(168, 28, 28)); // KIT Crimson

        JLabel lblSub = new JLabel("Semester V Academic Attendance & Self-Service Portal");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(100, 116, 139));
        titleBlock.add(lblTitle);
        titleBlock.add(lblSub);
        headerBar.add(titleBlock, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtns.setOpaque(false);

        ModernButton btnQrCheckIn = new ModernButton("📷 QR Check-in", ModernButton.ButtonType.PRIMARY);
        ModernButton btnApplyLeave = new ModernButton("📝 Apply Leave", ModernButton.ButtonType.SUCCESS);
        ModernButton btnExportPdf = new ModernButton("📄 PDF Card", ModernButton.ButtonType.SECONDARY);
        ModernButton btnExportExcel = new ModernButton("📊 Excel Card", ModernButton.ButtonType.SECONDARY);

        btnQrCheckIn.addActionListener(e -> onQrCheckIn());
        btnApplyLeave.addActionListener(e -> onApplyLeave());
        btnExportPdf.addActionListener(e -> onExportPdf());
        btnExportExcel.addActionListener(e -> onExportExcel());

        actionBtns.add(btnQrCheckIn);
        actionBtns.add(btnApplyLeave);
        actionBtns.add(btnExportPdf);
        actionBtns.add(btnExportExcel);
        headerBar.add(actionBtns, BorderLayout.EAST);

        topContainer.add(headerBar);
        topContainer.add(Box.createVerticalStrut(14));

        // Profile & KPI Banner
        JPanel banner = new JPanel(new GridLayout(1, 4, 16, 0));
        banner.setOpaque(false);

        String name = currentStudent != null ? currentStudent.getStudentName() : "Student";
        String reg = currentStudent != null ? currentStudent.getRegisterNo() : "—";
        String dept = currentStudent != null ? (currentStudent.getDepartment() + " Y" + currentStudent.getYearOfStudy() + " " + currentStudent.getSection()) : "—";
        double pct = summary != null ? summary.getAttendancePercentage() : 0.0;
        int conducted = summary != null ? summary.getTotalWorkingDays() : 0;
        int attended = summary != null ? summary.getPresentDays() : 0;
        int absent = summary != null ? summary.getAbsentDays() : 0;

        banner.add(createCard("Student Profile", name, reg + " • " + dept, new Color(30, 41, 59)));
        banner.add(createCard("Overall Attendance", String.format("%.1f%%", pct), summary != null ? summary.getStatus() : "—", pct >= 75.0 ? new Color(22, 101, 52) : new Color(185, 28, 28)));
        banner.add(createCard("Classes Attended", String.valueOf(attended) + " / " + conducted, "Conducted Periods", new Color(30, 64, 175)));
        banner.add(createCard("Classes Absent", String.valueOf(absent), "Absences Recorded", new Color(194, 65, 12)));

        topContainer.add(banner);
        topContainer.add(Box.createVerticalStrut(12));

        // Shortage Calculator Box
        AppSettings settings = settingsService.getSettings();
        double reqPct = settings.getRequiredAttendancePct();
        AttendanceCalculatorResult calcResult = calculatorService.calculateShortage(attended, conducted, reqPct);

        JPanel calcBanner = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        calcBanner.setBackground(calcResult.isShortage() ? new Color(254, 242, 242) : new Color(240, 253, 244));
        calcBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(calcResult.isShortage() ? new Color(252, 165, 165) : new Color(134, 239, 172), 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        JLabel lblCalcIcon = new JLabel(calcResult.isShortage() ? "⚠️ Attendance Requirement Notice:" : "✅ Good Standing:");
        lblCalcIcon.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCalcIcon.setForeground(calcResult.isShortage() ? new Color(153, 27, 27) : new Color(22, 101, 52));

        JLabel lblCalcText = new JLabel(calcResult.getAdvice() + " (Minimum criteria: " + reqPct + "%)");
        lblCalcText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCalcText.setForeground(new Color(30, 41, 59));

        calcBanner.add(lblCalcIcon);
        calcBanner.add(lblCalcText);
        topContainer.add(calcBanner);

        add(topContainer, BorderLayout.NORTH);

        // 2. TABS: SUBJECTS, TIMETABLE, LEAVES
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // TAB 1: Subject-wise Breakdown
        String[] subCols = {"S.No", "Subject Code", "Subject Name", "Faculty", "Conducted", "Attended", "Attendance %", "Status"};
        subjectModel = new DefaultTableModel(subCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable subTable = new ModernTable(subjectModel);
        subTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        subTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        subTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        subTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        subTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        subTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        subTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        subTable.getColumnModel().getColumn(7).setPreferredWidth(90);

        int sno = 1;
        if (subjectBreakdown != null) {
            for (Map<String, Object> map : subjectBreakdown) {
                double p = (Double) map.get("percentage");
                String st = p >= 85 ? "Excellent" : p >= 75 ? "Good" : p >= 65 ? "Warning" : "Critical";
                subjectModel.addRow(new Object[]{
                        sno++,
                        map.get("subjectCode"),
                        map.get("subjectName"),
                        map.get("teacherName") != null ? map.get("teacherName") : "Department Faculty",
                        map.get("conducted"),
                        map.get("attended"),
                        String.format("%.1f%%", p),
                        st
                });
            }
        }
        tabs.addTab("📚  Subject-Wise Attendance", new JScrollPane(subTable));

        // TAB 2: Class Timetable
        String[] ttCols = {"Day", "Period", "Subject Code", "Subject Name", "Faculty", "Room"};
        timetableModel = new DefaultTableModel(ttCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable ttTable = new ModernTable(timetableModel);
        if (currentStudent != null) {
            try {
                List<TimetableEntry> ttList = timetableService.getTimetableByClass(
                        currentStudent.getDepartment(), currentStudent.getYearOfStudy(), currentStudent.getSection());
                for (TimetableEntry t : ttList) {
                    timetableModel.addRow(new Object[]{
                            t.getDayOfWeek(),
                            t.getPeriodName() != null ? t.getPeriodName() : ("Period " + t.getPeriodNumber()),
                            t.getSubjectCode(),
                            t.getSubjectName(),
                            t.getTeacherName() != null ? t.getTeacherName() : "Faculty",
                            t.getRoom()
                    });
                }
            } catch (Exception ignored) {}
        }
        tabs.addTab("🗓️  My Class Schedule", new JScrollPane(ttTable));

        // TAB 3: My Leaves
        String[] leaveCols = {"ID", "Leave Type", "From Date", "To Date", "Reason", "Status", "Approved By", "Remarks"};
        leaveModel = new DefaultTableModel(leaveCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable lvTable = new ModernTable(leaveModel);
        if (currentStudent != null) {
            try {
                List<LeaveRequest> lvs = leaveService.getLeaveRequestsByStudent(currentStudent.getStudentId());
                for (LeaveRequest l : lvs) {
                    leaveModel.addRow(new Object[]{
                            l.getLeaveId(),
                            l.getLeaveType(),
                            DateUtil.formatDisplayDate(l.getFromDate()),
                            DateUtil.formatDisplayDate(l.getToDate()),
                            l.getReason(),
                            l.getStatus(),
                            l.getApprovedBy() != null ? l.getApprovedBy() : "—",
                            l.getRemarks() != null ? l.getRemarks() : "—"
                    });
                }
            } catch (Exception ignored) {}
        }
        tabs.addTab("📋  My Leave History", new JScrollPane(lvTable));

        add(tabs, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel createCard(String title, String val, String sub, Color valColor) {
        JPanel card = new JPanel(new GridLayout(3, 1, 0, 3));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setForeground(new Color(100, 116, 139));

        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 20));
        v.setForeground(valColor);

        JLabel s = new JLabel(sub);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        s.setForeground(new Color(100, 116, 139));

        card.add(t);
        card.add(v);
        card.add(s);
        return card;
    }

    private void onQrCheckIn() {
        if (currentStudent == null) return;
        String token = JOptionPane.showInputDialog(
                this,
                "Enter or scan the 16-character Session Code / QR Token from your teacher:",
                "📱 Student QR Attendance Check-In",
                JOptionPane.PLAIN_MESSAGE
        );
        if (token != null && !token.trim().isEmpty()) {
            QrAttendanceService qrService = new QrAttendanceService();
            String res = qrService.validateAndRecordAttendance(token.trim(), currentStudent.getStudentId());
            JOptionPane.showMessageDialog(this, res, "Attendance Check-In", JOptionPane.INFORMATION_MESSAGE);
            loadStudentData();
            initUI();
        }
    }

    private void onApplyLeave() {
        if (currentStudent == null) return;
        ApplyLeaveDialog dlg = new ApplyLeaveDialog(SwingUtilities.getWindowAncestor(this), currentStudent.getStudentId());
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            loadStudentData();
            initUI();
        }
    }

    private void onExportPdf() {
        if (currentStudent == null || subjectBreakdown == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(currentStudent.getRegisterNo() + "_Attendance_Card.pdf"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                pdfExportService.exportStudentAttendanceCardPdf(
                        "KIT ENGINEERING COLLEGE",
                        currentStudent,
                        subjectBreakdown,
                        summary != null ? summary.getAttendancePercentage() : 0.0,
                        summary != null ? summary.getStatus() : "Normal",
                        chooser.getSelectedFile()
                );
                JOptionPane.showMessageDialog(this, "Student Attendance Card PDF exported successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error exporting PDF: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onExportExcel() {
        if (currentStudent == null || subjectBreakdown == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(currentStudent.getRegisterNo() + "_Attendance_Card.xlsx"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                excelExportService.exportStudentAttendanceCard(
                        currentStudent,
                        subjectBreakdown,
                        summary != null ? summary.getAttendancePercentage() : 0.0,
                        summary != null ? summary.getStatus() : "Normal",
                        chooser.getSelectedFile()
                );
                JOptionPane.showMessageDialog(this, "Student Attendance Card Excel exported successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error exporting Excel: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
