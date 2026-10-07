package com.attendance.ui;

import com.attendance.model.AttendanceAudit;
import com.attendance.model.AuditLog;
import com.attendance.service.AuditService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;
import com.attendance.util.DateUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Audit Trail & Security Panel for System Operations and Attendance Corrections.
 */
public class AuditLogPanel extends JPanel {

    private final AuditService auditService;

    private final DefaultTableModel sysModel;
    private final JTable sysTable;

    private final DefaultTableModel corrModel;
    private final JTable corrTable;

    public AuditLogPanel() {
        this.auditService = new AuditService();

        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);
        JLabel lblTitle = new JLabel("System Audit Trail & Compliance Log");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 41, 59));

        JLabel lblSub = new JLabel("Immutable log of administrative operations, security events, and attendance override corrections");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(100, 116, 139));
        titleBlock.add(lblTitle);
        titleBlock.add(lblSub);
        header.add(titleBlock, BorderLayout.WEST);

        ModernButton btnRefresh = new ModernButton("Refresh Logs", ModernButton.ButtonType.SECONDARY);
        btnRefresh.addActionListener(e -> loadData());
        header.add(btnRefresh, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: System Activity Logs
        String[] sysCols = {"Log ID", "User ID", "Action", "Description", "IP Address", "Timestamp"};
        sysModel = new DefaultTableModel(sysCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        sysTable = new ModernTable(sysModel);
        sysTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        sysTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        sysTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        sysTable.getColumnModel().getColumn(3).setPreferredWidth(320);
        sysTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        sysTable.getColumnModel().getColumn(5).setPreferredWidth(150);

        tabs.addTab("🛡️  System Operations Trail", new JScrollPane(sysTable));

        // Tab 2: Attendance Corrections
        String[] corrCols = {"Audit ID", "Student ID", "Old Status", "New Status", "Changed By", "Reason for Correction", "Changed At"};
        corrModel = new DefaultTableModel(corrCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        corrTable = new ModernTable(corrModel);
        corrTable.getColumnModel().getColumn(0).setPreferredWidth(65);
        corrTable.getColumnModel().getColumn(1).setPreferredWidth(75);
        corrTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        corrTable.getColumnModel().getColumn(3).setPreferredWidth(85);
        corrTable.getColumnModel().getColumn(4).setPreferredWidth(140);
        corrTable.getColumnModel().getColumn(5).setPreferredWidth(280);
        corrTable.getColumnModel().getColumn(6).setPreferredWidth(140);

        tabs.addTab("✏️  Attendance Override Corrections", new JScrollPane(corrTable));

        add(tabs, BorderLayout.CENTER);

        loadData();
    }

    public void loadData() {
        sysModel.setRowCount(0);
        corrModel.setRowCount(0);

        try {
            List<AuditLog> sysLogs = auditService.getRecentLogs(100);
            for (AuditLog l : sysLogs) {
                sysModel.addRow(new Object[]{
                        l.getLogId(),
                        l.getUserId(),
                        l.getAction(),
                        l.getDescription(),
                        l.getIpAddress(),
                        DateUtil.formatDisplayDateTime(l.getTimestamp())
                });
            }

            List<AttendanceAudit> corrLogs = auditService.getAttendanceAudits();
            for (AttendanceAudit a : corrLogs) {
                corrModel.addRow(new Object[]{
                        a.getAuditId(),
                        a.getStudentId(),
                        a.getOldStatus(),
                        a.getNewStatus(),
                        a.getChangedBy(),
                        a.getReason(),
                        DateUtil.formatDisplayDateTime(a.getChangedAt())
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load audit logs: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
