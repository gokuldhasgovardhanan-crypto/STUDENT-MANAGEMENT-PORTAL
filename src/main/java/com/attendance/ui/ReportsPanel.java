package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.Attendance;
import com.attendance.model.DashboardStats;
import com.attendance.model.Student;
import com.attendance.model.StudentAttendanceSummary;
import com.attendance.service.*;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;
import com.attendance.util.DateUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reports Panel supporting Daily, Student-wise, Monthly, Date-Range, Department,
 * and Low Attendance reports, with real Excel (.xlsx) and PDF export.
 */
public class ReportsPanel extends JPanel {

    private final ReportService reportService;
    private final StudentService studentService;
    private final SettingsService settingsService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final AttendanceService attendanceService;

    private final JComboBox<String> cmbReportType;

    // Filter controls
    private final JTextField txtDate1; // Date or From Date
    private final JTextField txtDate2; // To Date
    private final JComboBox<String> cmbMonth;
    private final JComboBox<Integer> cmbYearNum;
    private final JComboBox<String> cmbDept;
    private final JComboBox<String> cmbClassYear;
    private final JComboBox<String> cmbSection;
    private final JComboBox<Student> cmbStudent;

    private final JPanel filterRowDate1;
    private final JPanel filterRowDate2;
    private final JPanel filterRowMonthYear;
    private final JPanel filterRowStudent;
    private final JLabel lblDate1;

    private final ModernTable reportTable;
    private final DefaultTableModel tableModel;
    private final JLabel lblReportMeta;

    // Cached report data
    private List<Attendance> currentAttendanceList = new ArrayList<>();
    private List<StudentAttendanceSummary> currentSummaryList = new ArrayList<>();

    public ReportsPanel() {
        this.reportService = new ReportService();
        this.studentService = new StudentService();
        this.settingsService = new SettingsService();
        this.excelExportService = new ExcelExportService();
        this.pdfExportService = new PdfExportService();
        this.attendanceService = new AttendanceService();

        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        AppSettings settings = settingsService.getSettings();

        // Top Filter Card
        JPanel topCard = new JPanel(new BorderLayout(10, 12));
        topCard.setBackground(Color.WHITE);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        // Row 1: Report Type Selection & Actions
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        typePanel.setOpaque(false);
        typePanel.add(new JLabel("Select Report Type:"));

        String[] reportTypes = {
                "Daily Attendance Report",
                "Monthly Attendance Report",
                "Date Range Report",
                "Student-wise Attendance Report",
                "Department-wise Summary",
                "Low Attendance Report (< 75%)"
        };
        cmbReportType = new JComboBox<>(reportTypes);
        cmbReportType.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cmbReportType.addActionListener(e -> updateFilterVisibility());
        typePanel.add(cmbReportType);

        row1.add(typePanel, BorderLayout.WEST);

        // Export Buttons
        JPanel exportBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        exportBtns.setOpaque(false);

        ModernButton btnGen = new ModernButton("GENERATE REPORT", ModernButton.ButtonType.PRIMARY);
        ModernButton btnExcel = new ModernButton("EXPORT TO EXCEL", ModernButton.ButtonType.SUCCESS);
        ModernButton btnPdf = new ModernButton("EXPORT TO PDF", ModernButton.ButtonType.PRIMARY);
        ModernButton btnCert = new ModernButton("CERTIFICATE", ModernButton.ButtonType.SECONDARY);
        ModernButton btnSummaryWb = new ModernButton("COMPLETE WORKBOOK", ModernButton.ButtonType.SECONDARY);

        btnGen.addActionListener(e -> generateReport());
        btnExcel.addActionListener(e -> exportExcel());
        btnPdf.addActionListener(e -> exportPdf());
        btnCert.addActionListener(e -> exportCertificate());
        btnSummaryWb.addActionListener(e -> exportCompleteWorkbook());

        exportBtns.add(btnGen);
        exportBtns.add(btnExcel);
        exportBtns.add(btnPdf);
        exportBtns.add(btnCert);
        exportBtns.add(btnSummaryWb);

        row1.add(exportBtns, BorderLayout.EAST);
        topCard.add(row1, BorderLayout.NORTH);

        // Row 2: Dynamic Filters
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        row2.setOpaque(false);

        lblDate1 = new JLabel("Date (dd-MM-yyyy):");
        txtDate1 = new JTextField(LocalDate.now().format(DateUtil.DISPLAY_DATE_FORMAT), 8);
        filterRowDate1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterRowDate1.setOpaque(false);
        filterRowDate1.add(lblDate1);
        filterRowDate1.add(txtDate1);

        txtDate2 = new JTextField(LocalDate.now().format(DateUtil.DISPLAY_DATE_FORMAT), 8);
        filterRowDate2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterRowDate2.setOpaque(false);
        filterRowDate2.add(new JLabel("To Date:"));
        filterRowDate2.add(txtDate2);

        cmbMonth = new JComboBox<>(new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        });
        cmbMonth.setSelectedIndex(LocalDate.now().getMonthValue() - 1);

        cmbYearNum = new JComboBox<>(new Integer[]{2025, 2026, 2027, 2028});
        cmbYearNum.setSelectedItem(LocalDate.now().getYear());

        filterRowMonthYear = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterRowMonthYear.setOpaque(false);
        filterRowMonthYear.add(new JLabel("Month:"));
        filterRowMonthYear.add(cmbMonth);
        filterRowMonthYear.add(new JLabel("Year:"));
        filterRowMonthYear.add(cmbYearNum);

        List<String> depts = new ArrayList<>();
        depts.add("All");
        depts.addAll(settings.getDepartments());
        cmbDept = new JComboBox<>(depts.toArray(new String[0]));

        cmbClassYear = new JComboBox<>(new String[]{"All", "1", "2", "3", "4"});
        cmbSection = new JComboBox<>(new String[]{"All", "A", "B"});

        cmbStudent = new JComboBox<>();
        cmbStudent.setPreferredSize(new Dimension(220, 28));
        loadStudentListCombo();

        filterRowStudent = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterRowStudent.setOpaque(false);
        filterRowStudent.add(new JLabel("Student:"));
        filterRowStudent.add(cmbStudent);

        row2.add(filterRowDate1);
        row2.add(filterRowDate2);
        row2.add(filterRowMonthYear);
        row2.add(filterRowStudent);
        row2.add(new JLabel("Dept:"));
        row2.add(cmbDept);
        row2.add(new JLabel("Class Year:"));
        row2.add(cmbClassYear);
        row2.add(new JLabel("Section:"));
        row2.add(cmbSection);

        topCard.add(row2, BorderLayout.SOUTH);

        add(topCard, BorderLayout.NORTH);

        // Center Table
        tableModel = new DefaultTableModel();
        reportTable = new ModernTable(tableModel);
        add(new JScrollPane(reportTable), BorderLayout.CENTER);

        // Bottom status / report meta
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        lblReportMeta = new JLabel("Click 'GENERATE REPORT' to query records.");
        lblReportMeta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblReportMeta.setForeground(new Color(100, 116, 139));
        bottomBar.add(lblReportMeta, BorderLayout.WEST);

        add(bottomBar, BorderLayout.SOUTH);

        updateFilterVisibility();
        generateReport();
    }

    private void loadStudentListCombo() {
        try {
            List<Student> students = studentService.getAllStudents();
            cmbStudent.removeAllItems();
            for (Student s : students) {
                cmbStudent.addItem(s);
            }
        } catch (Exception ignored) {}
    }

    private void updateFilterVisibility() {
        int idx = cmbReportType.getSelectedIndex();
        // 0: Daily, 1: Monthly, 2: Date Range, 3: Student-wise, 4: Dept-wise, 5: Low Att
        filterRowDate1.setVisible(idx == 0 || idx == 2 || idx == 3);
        filterRowDate2.setVisible(idx == 2 || idx == 3);
        filterRowMonthYear.setVisible(idx == 1);
        filterRowStudent.setVisible(idx == 3);

        lblDate1.setText(idx == 2 || idx == 3 ? "From Date:" : "Date:");
        revalidate();
        repaint();
    }

    public void generateReport() {
        int idx = cmbReportType.getSelectedIndex();
        String dept = (String) cmbDept.getSelectedItem();
        String yrStr = (String) cmbClassYear.getSelectedItem();
        String sec = (String) cmbSection.getSelectedItem();
        Integer year = (yrStr != null && !yrStr.equalsIgnoreCase("All")) ? Integer.parseInt(yrStr) : null;

        currentAttendanceList.clear();
        currentSummaryList.clear();

        try {
            switch (idx) {
                case 0: // Daily Attendance
                    generateDailyReport(dept, year, sec);
                    break;
                case 1: // Monthly Report
                    generateMonthlyReport(dept, year, sec);
                    break;
                case 2: // Date Range Report
                    generateDateRangeReport(dept, year, sec);
                    break;
                case 3: // Student-wise Report
                    generateStudentWiseReport();
                    break;
                case 4: // Department-wise Report
                    generateDepartmentReport();
                    break;
                case 5: // Low Attendance Report
                    generateLowAttendanceReport(dept, year, sec);
                    break;
            }
        } catch (Exception ex) {
            lblReportMeta.setText("Error generating report: " + ex.getMessage());
        }
    }

    private void generateDailyReport(String dept, Integer year, String sec) {
        LocalDate date = DateUtil.parseDisplayDate(txtDate1.getText().trim());
        if (date == null) {
            JOptionPane.showMessageDialog(this, "Please enter a valid date in dd-MM-yyyy format.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentAttendanceList = reportService.getDailyAttendanceReport(date, dept, year, sec);

        String[] cols = {"S.No", "Register No", "Student Name", "Department", "Year", "Section", "Date", "Status"};
        tableModel.setDataVector(new Object[0][0], cols);

        int sno = 1;
        for (Attendance a : currentAttendanceList) {
            tableModel.addRow(new Object[]{
                    sno++, a.getRegisterNo(), a.getStudentName(), a.getDepartment(),
                    a.getYearOfStudy(), a.getSection(), DateUtil.formatDisplayDate(a.getAttendanceDate()), a.getStatus()
            });
        }
        reportTable.getColumnModel().getColumn(7).setCellRenderer(new ModernTable.StatusBadgeRenderer());
        lblReportMeta.setText("Generated: " + DateUtil.formatCurrentTimestamp() + " | Records found: " + currentAttendanceList.size());
    }

    private void generateMonthlyReport(String dept, Integer year, String sec) {
        int month = cmbMonth.getSelectedIndex() + 1;
        int yr = (Integer) cmbYearNum.getSelectedItem();

        currentSummaryList = reportService.getMonthlyReport(month, yr, dept, year, sec);
        populateSummaryTable(currentSummaryList, "Month: " + cmbMonth.getSelectedItem() + " " + yr);
    }

    private void generateDateRangeReport(String dept, Integer year, String sec) {
        LocalDate start = DateUtil.parseDisplayDate(txtDate1.getText().trim());
        LocalDate end = DateUtil.parseDisplayDate(txtDate2.getText().trim());
        if (start == null || end == null) {
            JOptionPane.showMessageDialog(this, "Please enter valid From and To dates.", "Invalid Dates", JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentSummaryList = reportService.getDateRangeReport(start, end, dept, year, sec);
        populateSummaryTable(currentSummaryList, "Range: " + DateUtil.formatDisplayDate(start) + " to " + DateUtil.formatDisplayDate(end));
    }

    private void generateStudentWiseReport() {
        Student s = (Student) cmbStudent.getSelectedItem();
        if (s == null) {
            JOptionPane.showMessageDialog(this, "Please select a student.", "Missing Student", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate start = DateUtil.parseDisplayDate(txtDate1.getText().trim());
        LocalDate end = DateUtil.parseDisplayDate(txtDate2.getText().trim());

        currentAttendanceList = reportService.getStudentReport(s.getStudentId(), start, end);

        String[] cols = {"S.No", "Date", "Day of Week", "Status"};
        tableModel.setDataVector(new Object[0][0], cols);

        int sno = 1;
        for (Attendance a : currentAttendanceList) {
            String day = a.getAttendanceDate() != null ? a.getAttendanceDate().getDayOfWeek().toString() : "";
            tableModel.addRow(new Object[]{sno++, DateUtil.formatDisplayDate(a.getAttendanceDate()), day, a.getStatus()});
        }
        reportTable.getColumnModel().getColumn(3).setCellRenderer(new ModernTable.StatusBadgeRenderer());
        lblReportMeta.setText("Student: " + s.getRegisterNo() + " - " + s.getStudentName() + " | Generated: " + DateUtil.formatCurrentTimestamp());
    }

    private void generateDepartmentReport() {
        Map<String, Double> deptStats = reportService.getDepartmentWiseStats();
        String[] cols = {"S.No", "Department Code", "Average Attendance Percentage", "Performance Status"};
        tableModel.setDataVector(new Object[0][0], cols);

        int sno = 1;
        for (Map.Entry<String, Double> e : deptStats.entrySet()) {
            double pct = e.getValue();
            String st = StudentAttendanceSummary.calculateStatus(pct);
            tableModel.addRow(new Object[]{sno++, e.getKey(), String.format("%.2f%%", pct), st});
        }
        reportTable.getColumnModel().getColumn(3).setCellRenderer(new ModernTable.StatusBadgeRenderer());
        lblReportMeta.setText("Department Statistics | Generated: " + DateUtil.formatCurrentTimestamp());
    }

    private void generateLowAttendanceReport(String dept, Integer year, String sec) {
        AppSettings settings = settingsService.getSettings();
        currentSummaryList = reportService.getLowAttendanceReport(settings.getRequiredAttendancePct(), dept, year, sec);
        populateSummaryTable(currentSummaryList, "Low Attendance (< " + settings.getRequiredAttendancePct() + "%)");
    }

    private void populateSummaryTable(List<StudentAttendanceSummary> list, String desc) {
        String[] cols = {
                "S.No", "Register No", "Student Name", "Dept", "Year", "Section",
                "Working Days", "Present", "Absent", "Attendance %", "Status"
        };
        tableModel.setDataVector(new Object[0][0], cols);

        int sno = 1;
        for (StudentAttendanceSummary s : list) {
            tableModel.addRow(new Object[]{
                    sno++, s.getRegisterNo(), s.getStudentName(), s.getDepartment(),
                    s.getYearOfStudy(), s.getSection(), s.getTotalWorkingDays(),
                    s.getPresentDays(), s.getAbsentDays(), String.format("%.2f%%", s.getAttendancePercentage()),
                    s.getStatus()
            });
        }
        reportTable.getColumnModel().getColumn(10).setCellRenderer(new ModernTable.StatusBadgeRenderer());
        lblReportMeta.setText(desc + " | Total Records: " + list.size() + " | Generated: " + DateUtil.formatCurrentTimestamp());
    }

    private void exportExcel() {
        int idx = cmbReportType.getSelectedIndex();
        JFileChooser chooser = new JFileChooser();

        String defaultName = switch (idx) {
            case 0 -> "Daily_Attendance_" + txtDate1.getText().trim() + ".xlsx";
            case 1 -> "Monthly_Attendance_" + (cmbMonth.getSelectedIndex() + 1) + "_" + cmbYearNum.getSelectedItem() + ".xlsx";
            case 2 -> "Date_Range_Attendance.xlsx";
            case 3 -> "Student_Attendance_Report.xlsx";
            case 4 -> "Department_Attendance_Summary.xlsx";
            default -> "Low_Attendance_Report.xlsx";
        };

        chooser.setSelectedFile(new File(defaultName));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".xlsx")) {
                target = new File(target.getParentFile(), target.getName() + ".xlsx");
            }

            try {
                if (idx == 0) {
                    LocalDate date = DateUtil.parseDisplayDate(txtDate1.getText().trim());
                    excelExportService.exportDailyAttendance(date, (String) cmbDept.getSelectedItem(), null, null, currentAttendanceList, target);
                } else if (idx == 3) {
                    Student s = (Student) cmbStudent.getSelectedItem();
                    StudentAttendanceSummary sum = attendanceService.getStudentAttendanceSummary(s.getStudentId());
                    excelExportService.exportStudentProfileAttendance(s, sum, currentAttendanceList, target);
                } else {
                    excelExportService.exportMonthlyAttendance((String) cmbReportType.getSelectedItem(), currentSummaryList, target);
                }

                JOptionPane.showMessageDialog(this, "✓ Report exported to Excel successfully!\nFile: " + target.getAbsolutePath(),
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export Excel: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportPdf() {
        int idx = cmbReportType.getSelectedIndex();
        AppSettings settings = settingsService.getSettings();
        JFileChooser chooser = new JFileChooser();

        String defaultName = switch (idx) {
            case 0 -> "Daily_Attendance_Report.pdf";
            case 1 -> "Monthly_Attendance_Report.pdf";
            case 2 -> "Date_Range_Report.pdf";
            case 3 -> "Student_Report.pdf";
            case 4 -> "Department_Summary.pdf";
            default -> "Low_Attendance_Report.pdf";
        };

        chooser.setSelectedFile(new File(defaultName));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".pdf")) {
                target = new File(target.getParentFile(), target.getName() + ".pdf");
            }

            try {
                if (idx == 0) {
                    LocalDate date = DateUtil.parseDisplayDate(txtDate1.getText().trim());
                    pdfExportService.exportDailyAttendancePdf(settings.getCollegeName(), date,
                            (String) cmbDept.getSelectedItem(), null, null, currentAttendanceList, target);
                } else if (idx == 3) {
                    Student s = (Student) cmbStudent.getSelectedItem();
                    StudentAttendanceSummary sum = attendanceService.getStudentAttendanceSummary(s.getStudentId());
                    pdfExportService.exportStudentProfilePdf(settings.getCollegeName(), s, sum, currentAttendanceList, target);
                } else {
                    pdfExportService.exportAttendanceSummaryPdf(settings.getCollegeName(),
                            (String) cmbReportType.getSelectedItem(),
                            "Academic Year: " + settings.getAcademicYear() + " | Semester: " + settings.getSemester(),
                            currentSummaryList, target);
                }

                JOptionPane.showMessageDialog(this, "✓ Report exported to PDF successfully!\nFile: " + target.getAbsolutePath(),
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCompleteWorkbook() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Attendance_Report.xlsx"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".xlsx")) {
                target = new File(target.getParentFile(), target.getName() + ".xlsx");
            }

            try {
                AppSettings settings = settingsService.getSettings();
                DashboardStats stats = attendanceService.getDashboardStats();
                List<StudentAttendanceSummary> summaries = attendanceService.getAllStudentSummaries(null, null, null, null, null);
                List<Attendance> recent = attendanceService.getDailyAttendanceReport(LocalDate.now(), null, null, null);
                List<StudentAttendanceSummary> low = attendanceService.getLowAttendanceStudents(settings.getRequiredAttendancePct(), null, null, null);
                List<Student> students = studentService.getAllStudents();

                excelExportService.exportCompleteAttendanceSummaryWorkbook(settings, stats, summaries, recent, low, students, target);
                JOptionPane.showMessageDialog(this, "✓ Complete multi-sheet attendance workbook generated!\n" +
                        "Includes: Summary, Student Attendance, Daily Attendance, Low Attendance, Student List.\n" +
                        "File: " + target.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to generate workbook: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCertificate() {
        Student s = (Student) cmbStudent.getSelectedItem();
        if (s == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a student from the Student dropdown filter to generate their official attendance certificate.",
                    "Select Student", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AppSettings settings = settingsService.getSettings();
        StudentAttendanceSummary sum = attendanceService.getStudentAttendanceSummary(s.getStudentId());
        if (sum == null) {
            JOptionPane.showMessageDialog(this, "No attendance summary data found for student.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Attendance_Certificate_" + s.getRegisterNo() + ".pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".pdf")) {
                target = new File(target.getParentFile(), target.getName() + ".pdf");
            }
            try {
                pdfExportService.exportAttendanceCertificatePdf(settings.getCollegeName(), s, sum, settings.getAcademicYear(), target);
                JOptionPane.showMessageDialog(this, "✓ Official Attendance Certificate generated successfully!\nFile: " + target.getAbsolutePath(),
                        "Certificate Generated", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to generate certificate: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
