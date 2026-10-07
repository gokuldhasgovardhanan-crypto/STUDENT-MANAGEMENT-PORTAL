package com.attendance.ui;

import com.attendance.model.AppSettings;
import com.attendance.model.AttendanceCalculatorResult;
import com.attendance.model.Student;
import com.attendance.model.StudentAttendanceSummary;
import com.attendance.service.*;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;
import com.attendance.ui.components.SimpleBarChart;
import com.attendance.ui.components.StatCard;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Dedicated Analytics & Risk Prediction Panel.
 * Demonstrates:
 * 1. Department Attendance Comparison
 * 2. Attendance Tier Distribution (90-100%, 80-89%, 75-79%, 65-74%, <65%)
 * 3. Monthly Attendance Trend
 * 4. Subject Comparison
 * 5. Interactive Shortage Calculator with mathematical formulas:
 *    x = ceil((R*T - A) / (1 - R)) and m = floor((A - R*T) / R)
 * 6. Rule-Based Explainable Attendance Risk Prediction (SAFE, AT RISK, CRITICAL).
 */
public class AnalyticsPanel extends JPanel {

    private final AttendanceService attendanceService;
    private final AttendanceCalculatorService calculatorService;
    private final StudentService studentService;
    private final SettingsService settingsService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    // Charts & Displays
    private SimpleBarChart deptChart;
    private JPanel distributionPanel;
    private DefaultTableModel monthlyModel;
    private DefaultTableModel subjectModel;

    // Calculator inputs & outputs
    private JComboBox<Student> cmbStudentPicker;
    private JSpinner spinConducted;
    private JSpinner spinAttended;
    private JSpinner spinRequiredPct;
    private JLabel lblCalcCurrentPct;
    private JLabel lblCalcClassesNeeded;
    private JLabel lblCalcMaxMissable;
    private JLabel lblCalcRiskBadge;
    private JTextArea txtCalcAdvice;

    private StatCard statCollegeAvg;
    private StatCard statCriticalCount;
    private StatCard statRiskCount;
    private StatCard statSafeCount;

    public AnalyticsPanel() {
        this.attendanceService = new AttendanceService();
        this.calculatorService = new AttendanceCalculatorService();
        this.studentService = new StudentService();
        this.settingsService = new SettingsService();
        this.excelExportService = new ExcelExportService();
        this.pdfExportService = new PdfExportService();

        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initUI();
        refreshAnalyticsData();
    }

    private void initUI() {
        // TOP HEADER
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);
        JLabel lblTitle = new JLabel("KIT ENGINEERING COLLEGE — Attendance Analytics & Risk Prediction");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(168, 28, 28)); // Crimson Red

        JLabel lblSub = new JLabel("Predictive Modeling, Shortage Calculations & Distribution Trends (Live MySQL Grounding)");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));
        titleBlock.add(lblTitle);
        titleBlock.add(lblSub);
        topHeader.add(titleBlock, BorderLayout.WEST);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setOpaque(false);
        ModernButton btnRefresh = new ModernButton("Refresh Analytics", ModernButton.ButtonType.SECONDARY);
        btnRefresh.addActionListener(e -> refreshAnalyticsData());
        topActions.add(btnRefresh);
        topHeader.add(topActions, BorderLayout.EAST);

        add(topHeader, BorderLayout.NORTH);

        // KPI CARDS BANNER
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiGrid.setOpaque(false);

        statCollegeAvg = new StatCard("College Average", "0.0%", "Across all semesters", new Color(37, 99, 235));
        statSafeCount = new StatCard("Safe Standing", "0", "Students >= 75%", new Color(22, 163, 74));
        statRiskCount = new StatCard("At Risk (Warning)", "0", "Students 65% - 74%", new Color(234, 88, 12));
        statCriticalCount = new StatCard("Critical Shortage", "0", "Students < 65%", new Color(220, 38, 38));

        kpiGrid.add(statCollegeAvg);
        kpiGrid.add(statSafeCount);
        kpiGrid.add(statRiskCount);
        kpiGrid.add(statCriticalCount);

        // TABBED PANE FOR (1) CHARTS & DISTRIBUTIONS, (2) SHORTAGE CALCULATOR & RISK PREDICTION
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("📊  Institutional Analytics & Charts", createChartsTab());
        tabbedPane.addTab("🧮  Shortage Calculator & Risk Engine", createCalculatorTab());

        JPanel centerContainer = new JPanel(new BorderLayout(0, 14));
        centerContainer.setOpaque(false);
        centerContainer.add(kpiGrid, BorderLayout.NORTH);
        centerContainer.add(tabbedPane, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);
    }

    private JPanel createChartsTab() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 14, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));

        // 1. Department Attendance Comparison (Top-Left)
        JPanel deptCard = createCardContainer("Department Attendance Comparison");
        deptChart = new SimpleBarChart();
        deptCard.add(deptChart, BorderLayout.CENTER);
        panel.add(deptCard);

        // 2. Attendance Tier Distribution (Top-Right)
        JPanel distCard = createCardContainer("Attendance Distribution (5 Tiers)");
        distributionPanel = new JPanel();
        distributionPanel.setLayout(new BoxLayout(distributionPanel, BoxLayout.Y_AXIS));
        distributionPanel.setOpaque(false);
        distributionPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        distCard.add(new JScrollPane(distributionPanel), BorderLayout.CENTER);
        panel.add(distCard);

        // 3. Monthly Attendance Trend (Bottom-Left)
        JPanel monthCard = createCardContainer("Monthly Attendance Trend");
        String[] monthCols = {"Month", "Average Attendance %", "Academic Status"};
        monthlyModel = new DefaultTableModel(monthCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ModernTable monthTable = new ModernTable(monthlyModel);
        monthTable.getColumnModel().getColumn(2).setCellRenderer(new ModernTable.StatusBadgeRenderer());
        monthCard.add(new JScrollPane(monthTable), BorderLayout.CENTER);
        panel.add(monthCard);

        // 4. Subject Comparison (Bottom-Right)
        JPanel subCard = createCardContainer("Subject-Wise Average Attendance");
        String[] subCols = {"Subject Code", "Subject Attendance %", "Standing"};
        subjectModel = new DefaultTableModel(subCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ModernTable subTable = new ModernTable(subjectModel);
        subTable.getColumnModel().getColumn(2).setCellRenderer(new ModernTable.StatusBadgeRenderer());
        subCard.add(new JScrollPane(subTable), BorderLayout.CENTER);
        panel.add(subCard);

        return panel;
    }

    private JPanel createCalculatorTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // LEFT CARD: INPUT CONTROLS
        JPanel leftCard = new JPanel();
        leftCard.setLayout(new BoxLayout(leftCard, BoxLayout.Y_AXIS));
        leftCard.setBackground(Color.WHITE);
        leftCard.setPreferredSize(new Dimension(420, 0));
        leftCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        JLabel lblCalcHeader = new JLabel("Attendance Shortage Parameters");
        lblCalcHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblCalcHeader.setForeground(new Color(15, 23, 42));
        leftCard.add(lblCalcHeader);
        leftCard.add(Box.createVerticalStrut(14));

        // Student quick-filler
        JLabel lblStudent = new JLabel("Quick-Fill From Student Record:");
        lblStudent.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cmbStudentPicker = new JComboBox<>();
        loadStudentsToPicker();
        cmbStudentPicker.addActionListener(e -> onStudentPicked());

        leftCard.add(lblStudent);
        leftCard.add(Box.createVerticalStrut(4));
        leftCard.add(cmbStudentPicker);
        leftCard.add(Box.createVerticalStrut(14));

        // Conducted Classes
        JLabel lblConducted = new JLabel("Total Classes Conducted (T):");
        lblConducted.setFont(new Font("Segoe UI", Font.BOLD, 12));
        spinConducted = new JSpinner(new SpinnerNumberModel(50, 1, 500, 1));
        leftCard.add(lblConducted);
        leftCard.add(Box.createVerticalStrut(4));
        leftCard.add(spinConducted);
        leftCard.add(Box.createVerticalStrut(12));

        // Attended Classes
        JLabel lblAttended = new JLabel("Classes Attended (A):");
        lblAttended.setFont(new Font("Segoe UI", Font.BOLD, 12));
        spinAttended = new JSpinner(new SpinnerNumberModel(38, 0, 500, 1));
        leftCard.add(lblAttended);
        leftCard.add(Box.createVerticalStrut(4));
        leftCard.add(spinAttended);
        leftCard.add(Box.createVerticalStrut(12));

        // Required Attendance Percentage
        JLabel lblReq = new JLabel("Required Cutoff Percentage (R):");
        lblReq.setFont(new Font("Segoe UI", Font.BOLD, 12));
        spinRequiredPct = new JSpinner(new SpinnerNumberModel(75.0, 50.0, 100.0, 1.0));
        leftCard.add(lblReq);
        leftCard.add(Box.createVerticalStrut(4));
        leftCard.add(spinRequiredPct);
        leftCard.add(Box.createVerticalStrut(18));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        ModernButton btnCalculate = new ModernButton("CALCULATE RISK", ModernButton.ButtonType.PRIMARY);
        ModernButton btnReset = new ModernButton("RESET", ModernButton.ButtonType.SECONDARY);
        btnCalculate.addActionListener(e -> computeShortageAndRisk());
        btnReset.addActionListener(e -> resetCalculator());

        btnRow.add(btnCalculate);
        btnRow.add(btnReset);
        leftCard.add(btnRow);

        leftCard.add(Box.createVerticalGlue());

        // RIGHT CARD: FORMULAS & PREDICTIVE RESULTS
        JPanel rightCard = new JPanel(new BorderLayout(0, 14));
        rightCard.setBackground(Color.WHITE);
        rightCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JLabel lblFormulaTitle = new JLabel("Mathematically Rigorous Attendance Analytics");
        lblFormulaTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblFormulaTitle.setForeground(new Color(168, 28, 28));

        JLabel lblFormulaHelp = new JLabel("<html><b>Mathematical Formulations:</b><br>" +
                "• Shortage Recovery: <code>x = ⌈(R·T - A) / (1 - R)⌉</code> (consecutive classes needed with zero absences)<br>" +
                "• Missable Margin: <code>m = ⌊(A - R·T) / R⌋</code> (maximum upcoming classes student may miss while maintaining R)</html>");
        lblFormulaHelp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFormulaHelp.setForeground(new Color(71, 85, 105));

        JPanel formulaTop = new JPanel(new GridLayout(2, 1, 0, 4));
        formulaTop.setOpaque(false);
        formulaTop.add(lblFormulaTitle);
        formulaTop.add(lblFormulaHelp);
        rightCard.add(formulaTop, BorderLayout.NORTH);

        // Result KPI Cards in Center
        JPanel resultsGrid = new JPanel(new GridLayout(2, 2, 14, 14));
        resultsGrid.setOpaque(false);

        JPanel p1 = createResultBox("CURRENT PERCENTAGE", "0.0%");
        lblCalcCurrentPct = (JLabel) p1.getClientProperty("valLabel");

        JPanel p2 = createResultBox("PREDICTIVE RISK LEVEL", "SAFE");
        lblCalcRiskBadge = (JLabel) p2.getClientProperty("valLabel");

        JPanel p3 = createResultBox("CLASSES NEEDED TO HIT CUTOFF", "0");
        lblCalcClassesNeeded = (JLabel) p3.getClientProperty("valLabel");

        JPanel p4 = createResultBox("MAX CLASSES CAN BE MISSED", "0");
        lblCalcMaxMissable = (JLabel) p4.getClientProperty("valLabel");

        resultsGrid.add(p1);
        resultsGrid.add(p2);
        resultsGrid.add(p3);
        resultsGrid.add(p4);

        // Bottom Advice Box
        JPanel adviceBox = new JPanel(new BorderLayout(0, 6));
        adviceBox.setOpaque(false);
        JLabel lblAdviceTitle = new JLabel("Academic Counseling Recommendation & Explanation:");
        lblAdviceTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblAdviceTitle.setForeground(new Color(30, 41, 59));

        txtCalcAdvice = new JTextArea(4, 40);
        txtCalcAdvice.setEditable(false);
        txtCalcAdvice.setLineWrap(true);
        txtCalcAdvice.setWrapStyleWord(true);
        txtCalcAdvice.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtCalcAdvice.setBackground(new Color(248, 250, 252));
        txtCalcAdvice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        adviceBox.add(lblAdviceTitle, BorderLayout.NORTH);
        adviceBox.add(new JScrollPane(txtCalcAdvice), BorderLayout.CENTER);

        JPanel centerRight = new JPanel(new BorderLayout(0, 16));
        centerRight.setOpaque(false);
        centerRight.add(resultsGrid, BorderLayout.NORTH);
        centerRight.add(adviceBox, BorderLayout.CENTER);

        rightCard.add(centerRight, BorderLayout.CENTER);

        panel.add(leftCard, BorderLayout.WEST);
        panel.add(rightCard, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createResultBox(String title, String initialVal) {
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setBackground(new Color(248, 250, 252));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblT = new JLabel(title);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblT.setForeground(new Color(100, 116, 139));

        JLabel lblV = new JLabel(initialVal);
        lblV.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblV.setForeground(new Color(15, 23, 42));

        box.add(lblT, BorderLayout.NORTH);
        box.add(lblV, BorderLayout.CENTER);
        box.putClientProperty("valLabel", lblV);
        return box;
    }

    private JPanel createCardContainer(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(new Color(30, 41, 59));
        card.add(lbl, BorderLayout.NORTH);
        return card;
    }

    public void refreshAnalyticsData() {
        SwingUtilities.invokeLater(() -> {
            try {
                AppSettings settings = settingsService.getSettings();
                double req = settings.getRequiredAttendancePct();

                // 1. Department averages & Bar chart
                Map<String, Double> deptAverages = attendanceService.getDashboardStats().getDepartmentAttendance();
                if (deptChart != null) {
                    deptChart.setData(deptAverages, req);
                }

                // 2. Attendance Distribution (5 tiers)
                Map<String, Integer> dist = attendanceService.getAttendanceDistributionCounts();
                int totalStudents = dist.values().stream().mapToInt(Integer::intValue).sum();
                if (distributionPanel != null) {
                    distributionPanel.removeAll();
                    for (Map.Entry<String, Integer> entry : dist.entrySet()) {
                        distributionPanel.add(createDistributionBar(entry.getKey(), entry.getValue(), totalStudents));
                        distributionPanel.add(Box.createVerticalStrut(8));
                    }
                    distributionPanel.revalidate();
                    distributionPanel.repaint();
                }

                // 3. Monthly Trend
                Map<String, Double> monthly = attendanceService.getMonthlyAttendanceTrend();
                monthlyModel.setRowCount(0);
                for (Map.Entry<String, Double> e : monthly.entrySet()) {
                    String st = StudentAttendanceSummary.calculateStatus(e.getValue());
                    monthlyModel.addRow(new Object[]{e.getKey(), String.format("%.1f%%", e.getValue()), st});
                }

                // 4. Subject Comparison
                Map<String, Double> subjects = attendanceService.getSubjectAverageAttendance();
                subjectModel.setRowCount(0);
                for (Map.Entry<String, Double> e : subjects.entrySet()) {
                    String st = StudentAttendanceSummary.calculateStatus(e.getValue());
                    subjectModel.addRow(new Object[]{e.getKey(), String.format("%.1f%%", e.getValue()), st});
                }

                // Top KPIs
                double collegeAvg = attendanceService.getDashboardStats().getAverageAttendancePct();
                statCollegeAvg.setValue(String.format("%.1f%%", collegeAvg));

                int safe = dist.getOrDefault("90–100%", 0) + dist.getOrDefault("80–89%", 0) + dist.getOrDefault("75–79%", 0);
                int risk = dist.getOrDefault("65–74%", 0);
                int critical = dist.getOrDefault("Below 65%", 0);

                statSafeCount.setValue(String.valueOf(safe));
                statRiskCount.setValue(String.valueOf(risk));
                statCriticalCount.setValue(String.valueOf(critical));

                // Execute initial calculation
                computeShortageAndRisk();

            } catch (Exception ignored) {}
        });
    }

    private JPanel createDistributionBar(String tier, int count, int total) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        double pct = total > 0 ? ((double) count / total) * 100.0 : 0.0;

        JLabel lblTier = new JLabel(tier);
        lblTier.setPreferredSize(new Dimension(85, 20));
        lblTier.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTier.setForeground(new Color(71, 85, 105));

        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue((int) Math.round(pct));
        pb.setStringPainted(false);
        pb.setPreferredSize(new Dimension(160, 14));

        Color col = tier.startsWith("90") || tier.startsWith("80")
                ? new Color(22, 163, 74)
                : tier.startsWith("75")
                ? new Color(37, 99, 235)
                : tier.startsWith("65")
                ? new Color(234, 88, 12)
                : new Color(220, 38, 38);
        pb.setForeground(col);

        JLabel lblCount = new JLabel(count + " students (" + String.format("%.1f%%", pct) + ")");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(15, 23, 42));

        row.add(lblTier, BorderLayout.WEST);
        row.add(pb, BorderLayout.CENTER);
        row.add(lblCount, BorderLayout.EAST);
        return row;
    }

    private void loadStudentsToPicker() {
        try {
            List<Student> students = studentService.getAllStudents();
            cmbStudentPicker.removeAllItems();
            cmbStudentPicker.addItem(null); // Option for custom values
            for (Student s : students) {
                cmbStudentPicker.addItem(s);
            }
        } catch (Exception ignored) {}
    }

    private void onStudentPicked() {
        Student s = (Student) cmbStudentPicker.getSelectedItem();
        if (s != null) {
            StudentAttendanceSummary sum = attendanceService.getStudentAttendanceSummary(s.getStudentId());
            if (sum != null) {
                spinConducted.setValue(Math.max(1, sum.getTotalWorkingDays()));
                spinAttended.setValue(sum.getPresentDays());
                computeShortageAndRisk();
            }
        }
    }

    private void computeShortageAndRisk() {
        int conducted = (Integer) spinConducted.getValue();
        int attended = (Integer) spinAttended.getValue();
        double reqPct = (Double) spinRequiredPct.getValue();

        if (attended > conducted) {
            attended = conducted;
            spinAttended.setValue(attended);
        }

        AttendanceCalculatorResult res = calculatorService.calculateShortage(attended, conducted, reqPct);

        double pct = res.getCurrentPercentage();
        lblCalcCurrentPct.setText(String.format("%.2f%%", pct));

        // Determine Risk Category
        String risk;
        Color riskColor;
        if (pct >= 85.0) {
            risk = "SAFE (EXCELLENT)";
            riskColor = new Color(22, 163, 74);
        } else if (pct >= reqPct) {
            risk = "SAFE (BORDERLINE)";
            riskColor = new Color(37, 99, 235);
        } else if (pct >= (reqPct - 10.0)) {
            risk = "AT RISK (WARNING)";
            riskColor = new Color(234, 88, 12);
        } else {
            risk = "CRITICAL (SHORTAGE)";
            riskColor = new Color(220, 38, 38);
        }

        lblCalcRiskBadge.setText(risk);
        lblCalcRiskBadge.setForeground(riskColor);

        lblCalcClassesNeeded.setText(String.valueOf(res.getClassesNeededToReachRequired()));
        lblCalcClassesNeeded.setForeground(res.getClassesNeededToReachRequired() > 0 ? new Color(220, 38, 38) : new Color(22, 163, 74));

        lblCalcMaxMissable.setText(String.valueOf(res.getMaxClassesCanBeMissed()));
        lblCalcMaxMissable.setForeground(res.getMaxClassesCanBeMissed() > 0 ? new Color(22, 163, 74) : new Color(100, 116, 139));

        txtCalcAdvice.setText(res.getAdvice() + "\n\n" +
                "Detailed Breakdown:\n" +
                "• Attended " + attended + " out of " + conducted + " conducted sessions.\n" +
                "• Minimum compliance target: " + String.format("%.0f%%", reqPct) + ".\n" +
                (res.isShortage()
                        ? ("• Academic Alert: The student must attend the next " + res.getClassesNeededToReachRequired() + " consecutive classes without missing any to restore eligibility.")
                        : ("• Compliance Confirmed: The student currently holds a safety buffer of " + res.getMaxClassesCanBeMissed() + " classes.")));
    }

    private void resetCalculator() {
        cmbStudentPicker.setSelectedIndex(0);
        spinConducted.setValue(50);
        spinAttended.setValue(38);
        spinRequiredPct.setValue(75.0);
        computeShortageAndRisk();
    }
}
