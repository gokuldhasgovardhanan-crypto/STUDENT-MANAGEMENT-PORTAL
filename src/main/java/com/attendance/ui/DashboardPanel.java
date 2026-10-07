package com.attendance.ui;

import com.attendance.model.DashboardStats;
import com.attendance.model.StudentAttendanceSummary;
import com.attendance.service.AttendanceService;
import com.attendance.service.ExcelExportService;
import com.attendance.service.PdfExportService;
import com.attendance.service.SettingsService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;
import com.attendance.ui.components.SimpleBarChart;
import com.attendance.ui.components.StatCard;
import com.attendance.util.DateUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Modern Dashboard panel displaying live statistics cards, department chart,
 * and low attendance alerts dynamically loaded from MySQL.
 */
public class DashboardPanel extends JPanel {

    private final AttendanceService attendanceService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final SettingsService settingsService;

    private StatCard cardTotal;
    private StatCard cardPresent;
    private StatCard cardAbsent;
    private StatCard cardNotMarked;
    private StatCard cardAverage;
    private StatCard cardLowAtt;

    private SimpleBarChart chart;
    private ModernTable lowAttTable;
    private DefaultTableModel lowAttModel;
    private JLabel lblMetaInfo;

    private DashboardStats currentStats;

    public DashboardPanel() {
        this.attendanceService = new AttendanceService();
        this.excelExportService = new ExcelExportService();
        this.pdfExportService = new PdfExportService();
        this.settingsService = new SettingsService();

        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initUI();
        refreshData();
    }

    private void initUI() {
        // Top Header
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel titleBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("Academic Attendance Dashboard");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(15, 23, 42));

        lblMetaInfo = new JLabel("Today: " + DateUtil.formatDisplayDate(LocalDate.now()));
        lblMetaInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMetaInfo.setForeground(new Color(100, 116, 139));

        titleBlock.add(lblTitle);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblMetaInfo);

        ModernButton btnRefresh = new ModernButton("Refresh Data", ModernButton.ButtonType.SECONDARY);
        btnRefresh.addActionListener(e -> refreshData());

        topBar.add(titleBlock, BorderLayout.WEST);
        topBar.add(btnRefresh, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Center Content: Cards + Chart/Table
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        // 6 Cards Grid (2 rows x 3 columns)
        JPanel cardsGrid = new JPanel(new GridLayout(2, 3, 14, 14));
        cardsGrid.setOpaque(false);
        cardsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        cardTotal = new StatCard("Total Students", "0", "Enrolled across all depts", new Color(59, 130, 246));
        cardPresent = new StatCard("Present Today", "0", "Marked present", new Color(16, 185, 129));
        cardAbsent = new StatCard("Absent Today", "0", "Marked absent", new Color(239, 68, 68));
        cardNotMarked = new StatCard("Attendance Not Marked", "0", "Pending marking today", new Color(245, 158, 11));
        cardAverage = new StatCard("Average Attendance", "0.0%", "College-wide semester average", new Color(99, 102, 241));
        cardLowAtt = new StatCard("Students Below 75%", "0", "Requiring intervention", new Color(225, 29, 72));

        cardsGrid.add(cardTotal);
        cardsGrid.add(cardPresent);
        cardsGrid.add(cardAbsent);
        cardsGrid.add(cardNotMarked);
        cardsGrid.add(cardAverage);
        cardsGrid.add(cardLowAtt);

        center.add(cardsGrid);
        center.add(Box.createVerticalStrut(18));

        // Bottom Split: Left = Chart, Right = Low Attendance Table
        JPanel bottomSplit = new JPanel(new GridLayout(1, 2, 16, 0));
        bottomSplit.setOpaque(false);

        // Chart Container Card
        JPanel chartCard = new JPanel(new BorderLayout());
        chartCard.setBackground(Color.WHITE);
        chartCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblChartTitle = new JLabel("Department Attendance Comparison");
        lblChartTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblChartTitle.setForeground(new Color(30, 41, 59));
        chartCard.add(lblChartTitle, BorderLayout.NORTH);

        chart = new SimpleBarChart();
        chartCard.add(chart, BorderLayout.CENTER);
        bottomSplit.add(chartCard);

        // Low Attendance Table Card
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        JLabel lblTableTitle = new JLabel("Low Attendance Students Alert (< 75%)");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTableTitle.setForeground(new Color(185, 28, 28));

        JPanel quickExport = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        quickExport.setOpaque(false);
        ModernButton btnExportExcel = new ModernButton("Excel", ModernButton.ButtonType.SECONDARY);
        ModernButton btnExportPdf = new ModernButton("PDF", ModernButton.ButtonType.SECONDARY);
        btnExportExcel.addActionListener(e -> exportLowExcel());
        btnExportPdf.addActionListener(e -> exportLowPdf());
        quickExport.add(btnExportExcel);
        quickExport.add(btnExportPdf);

        tableHeader.add(lblTableTitle, BorderLayout.WEST);
        tableHeader.add(quickExport, BorderLayout.EAST);
        tableCard.add(tableHeader, BorderLayout.NORTH);

        String[] cols = {"Reg No", "Student Name", "Dept", "Year", "Attendance %", "Status"};
        lowAttModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        lowAttTable = new ModernTable(lowAttModel);
        lowAttTable.getColumnModel().getColumn(5).setCellRenderer(new ModernTable.StatusBadgeRenderer());
        JScrollPane scroll = new JScrollPane(lowAttTable);
        tableCard.add(scroll, BorderLayout.CENTER);

        bottomSplit.add(tableCard);

        center.add(bottomSplit);

        add(new JScrollPane(center), BorderLayout.CENTER);
    }

    public void refreshData() {
        SwingUtilities.invokeLater(() -> {
            try {
                this.currentStats = attendanceService.getDashboardStats();

                lblMetaInfo.setText(String.format("Today: %s  |  Academic Year: %s  |  Semester: %s  |  College: %s",
                        DateUtil.formatDisplayDate(currentStats.getTodayDate()),
                        currentStats.getAcademicYear(),
                        currentStats.getSemester(),
                        currentStats.getCollegeName()));

                cardTotal.setValue(String.valueOf(currentStats.getTotalStudents()));
                cardPresent.setValue(String.valueOf(currentStats.getPresentToday()));
                cardAbsent.setValue(String.valueOf(currentStats.getAbsentToday()));
                cardNotMarked.setValue(String.valueOf(currentStats.getAttendanceNotMarked()));
                cardAverage.setValue(String.format("%.2f%%", currentStats.getAverageAttendancePct()));
                cardLowAtt.setValue(String.valueOf(currentStats.getStudentsBelowThresholdCount()));
                cardLowAtt.setSubtitle("Below " + (int) currentStats.getRequiredAttendancePct() + "% Threshold");

                chart.setData(currentStats.getDepartmentAttendance(), currentStats.getRequiredAttendancePct());

                // Update Low Attendance Table
                lowAttModel.setRowCount(0);
                List<StudentAttendanceSummary> lowList = currentStats.getLowAttendanceStudents();
                if (lowList != null) {
                    for (StudentAttendanceSummary s : lowList) {
                        lowAttModel.addRow(new Object[]{
                                s.getRegisterNo(),
                                s.getStudentName(),
                                s.getDepartment(),
                                s.getYearOfStudy(),
                                String.format("%.2f%%", s.getAttendancePercentage()),
                                s.getStatus()
                        });
                    }
                }
            } catch (Exception e) {
                // Ignore transient db failure when not connected yet
            }
        });
    }

    private void exportLowExcel() {
        if (currentStats == null || currentStats.getLowAttendanceStudents() == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Low_Attendance_Report.xlsx"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".xlsx")) {
                target = new File(target.getParentFile(), target.getName() + ".xlsx");
            }
            try {
                excelExportService.exportLowAttendanceReport(currentStats.getRequiredAttendancePct(), currentStats.getLowAttendanceStudents(), target);
                JOptionPane.showMessageDialog(this, "Exported successfully to " + target.getName(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportLowPdf() {
        if (currentStats == null || currentStats.getLowAttendanceStudents() == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Low_Attendance_Report.pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".pdf")) {
                target = new File(target.getParentFile(), target.getName() + ".pdf");
            }
            try {
                pdfExportService.exportAttendanceSummaryPdf(currentStats.getCollegeName(),
                        "Low Attendance Students Report",
                        "Threshold: Below " + currentStats.getRequiredAttendancePct() + "%",
                        currentStats.getLowAttendanceStudents(), target);
                JOptionPane.showMessageDialog(this, "Exported successfully to " + target.getName(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
