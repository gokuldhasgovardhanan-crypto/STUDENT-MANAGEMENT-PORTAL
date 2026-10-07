package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.Attendance;
import com.attendance.model.Student;
import com.attendance.model.StudentAttendanceSummary;
import com.attendance.service.AttendanceService;
import com.attendance.service.ExcelExportService;
import com.attendance.service.PdfExportService;
import com.attendance.service.SettingsService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;
import com.attendance.ui.components.StatCard;
import com.attendance.util.DateUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Modal dialog displaying detailed student profile, dynamic attendance KPI summary,
 * attendance history log, and Excel / PDF export capabilities.
 */
public class StudentProfileDialog extends JDialog {

    private final Student student;
    private final AttendanceService attendanceService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final AppSettings settings;

    private StudentAttendanceSummary summary;
    private List<Attendance> history;

    public StudentProfileDialog(Frame owner, Student student) {
        super(owner, "Student Profile - " + student.getRegisterNo() + " (" + student.getStudentName() + ")", true);
        this.student = student;
        this.attendanceService = new AttendanceService();
        this.excelExportService = new ExcelExportService();
        this.pdfExportService = new PdfExportService();
        this.settings = new SettingsService().getSettings();

        setSize(780, 680);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        loadData();
        initUI();
    }

    private void loadData() {
        this.summary = attendanceService.getStudentAttendanceSummary(student.getStudentId());
        if (this.summary == null) {
            this.summary = new StudentAttendanceSummary(
                    student.getStudentId(), student.getRegisterNo(), student.getStudentName(),
                    student.getDepartment(), student.getYearOfStudy(), student.getSection(),
                    0, 0, 0, 0.0, "N/A"
            );
        }
        this.history = attendanceService.getStudentAttendanceHistory(student.getStudentId(), null, null);
    }

    private void initUI() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        main.setBackground(new Color(248, 250, 252));

        // 1. Student Details Card
        JPanel infoCard = new JPanel(new GridLayout(4, 2, 12, 8));
        infoCard.setBackground(Color.WHITE);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        addInfoItem(infoCard, "Register Number:", student.getRegisterNo());
        addInfoItem(infoCard, "Student Name:", student.getStudentName());
        addInfoItem(infoCard, "Department:", student.getDepartment());
        addInfoItem(infoCard, "Year & Section:", "Year " + student.getYearOfStudy() + ", Sec " + student.getSection());
        addInfoItem(infoCard, "Email Address:", student.getEmail());
        addInfoItem(infoCard, "Phone Number:", student.getPhoneNumber());
        addInfoItem(infoCard, "Date of Birth:", DateUtil.formatDisplayDate(student.getDateOfBirth()));
        addInfoItem(infoCard, "Admission Date:", DateUtil.formatDisplayDate(student.getAdmissionDate()));

        main.add(infoCard);
        main.add(Box.createVerticalStrut(14));

        // 2. Attendance Summary Metric Cards
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        statsPanel.setOpaque(false);

        StatCard cardWorking = new StatCard("Working Days", String.valueOf(summary.getTotalWorkingDays()), "Total Recorded", new Color(59, 130, 246));
        StatCard cardPresent = new StatCard("Present Days", String.valueOf(summary.getPresentDays()), "Attended", new Color(16, 185, 129));
        StatCard cardAbsent = new StatCard("Absent Days", String.valueOf(summary.getAbsentDays()), "Missed", new Color(239, 68, 68));

        Color pctCol = summary.getAttendancePercentage() >= 75.0 ? new Color(16, 185, 129) : new Color(249, 115, 22);
        StatCard cardPct = new StatCard("Attendance %", String.format("%.1f%%", summary.getAttendancePercentage()), summary.getStatus(), pctCol);

        statsPanel.add(cardWorking);
        statsPanel.add(cardPresent);
        statsPanel.add(cardAbsent);
        statsPanel.add(cardPct);

        main.add(statsPanel);
        main.add(Box.createVerticalStrut(14));

        // 3. Attendance History Table
        JLabel lblHistory = new JLabel("Attendance History Records (" + history.size() + " days)");
        lblHistory.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblHistory.setForeground(new Color(30, 41, 59));
        main.add(lblHistory);
        main.add(Box.createVerticalStrut(6));

        String[] cols = {"S.No", "Attendance Date", "Day of Week", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        int sno = 1;
        for (Attendance a : history) {
            String dayOfWeek = a.getAttendanceDate() != null ? a.getAttendanceDate().getDayOfWeek().toString() : "";
            model.addRow(new Object[]{
                    sno++,
                    DateUtil.formatDisplayDate(a.getAttendanceDate()),
                    dayOfWeek,
                    a.getStatus()
            });
        }

        ModernTable table = new ModernTable(model);
        table.getColumnModel().getColumn(3).setCellRenderer(new ModernTable.StatusBadgeRenderer());
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setPreferredSize(new Dimension(740, 240));
        main.add(scrollTable);

        add(main, BorderLayout.CENTER);

        // Buttons Bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnBar.setBackground(new Color(248, 250, 252));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnExcel = new ModernButton("EXPORT TO EXCEL", ModernButton.ButtonType.SUCCESS);
        ModernButton btnPdf = new ModernButton("EXPORT TO PDF", ModernButton.ButtonType.PRIMARY);
        ModernButton btnCert = new ModernButton("ATTENDANCE CERTIFICATE", ModernButton.ButtonType.SECONDARY);
        ModernButton btnClose = new ModernButton("CLOSE", ModernButton.ButtonType.SECONDARY);

        btnExcel.addActionListener(e -> exportExcel());
        btnPdf.addActionListener(e -> exportPdf());
        btnCert.addActionListener(e -> exportCertificate());
        btnClose.addActionListener(e -> dispose());

        btnBar.add(btnExcel);
        btnBar.add(btnPdf);
        btnBar.add(btnCert);
        btnBar.add(btnClose);

        add(btnBar, BorderLayout.SOUTH);
    }

    private void addInfoItem(JPanel p, String label, String val) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(71, 85, 105));
        JLabel v = new JLabel(val != null ? val : "-");
        v.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        v.setForeground(new Color(15, 23, 42));
        item.add(l);
        item.add(v);
        p.add(item);
    }

    private void exportExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Student_Attendance_" + student.getRegisterNo() + ".xlsx"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".xlsx")) {
                target = new File(target.getParentFile(), target.getName() + ".xlsx");
            }
            try {
                excelExportService.exportStudentProfileAttendance(student, summary, history, target);
                JOptionPane.showMessageDialog(this, "Excel file exported successfully!\nSaved to: " + target.getAbsolutePath(),
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export Excel: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Student_Profile_" + student.getRegisterNo() + ".pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".pdf")) {
                target = new File(target.getParentFile(), target.getName() + ".pdf");
            }
            try {
                pdfExportService.exportStudentProfilePdf(settings.getCollegeName(), student, summary, history, target);
                JOptionPane.showMessageDialog(this, "PDF file exported successfully!\nSaved to: " + target.getAbsolutePath(),
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export PDF: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCertificate() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Attendance_Certificate_" + student.getRegisterNo() + ".pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".pdf")) {
                target = new File(target.getParentFile(), target.getName() + ".pdf");
            }
            try {
                pdfExportService.exportAttendanceCertificatePdf(settings.getCollegeName(), student, summary, settings.getAcademicYear(), target);
                JOptionPane.showMessageDialog(this, "Attendance Certificate exported successfully!\nSaved to: " + target.getAbsolutePath(),
                        "Certificate Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export certificate: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
