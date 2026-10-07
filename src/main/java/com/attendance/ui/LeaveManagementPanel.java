package com.attendance.ui;

import com.attendance.model.LeaveRequest;
import com.attendance.model.User;
import com.attendance.service.AuthenticationService;
import com.attendance.service.LeaveService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;
import com.attendance.util.DateUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel for managing student leave applications and processing approvals.
 */
public class LeaveManagementPanel extends JPanel {

    private final LeaveService leaveService;
    private final AuthenticationService authService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel lblStatus;
    private JComboBox<String> cmbStatusFilter;

    public LeaveManagementPanel() {
        this.leaveService = new LeaveService();
        this.authService = AuthenticationService.getInstance();

        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // 1. TOP HEADER & ACTIONS
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JLabel lblTitle = new JLabel("Student Leave & OD Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 41, 59));

        JLabel lblSubtitle = new JLabel("Review, approve, or reject student medical and on-duty leave applications");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitle.setForeground(new Color(100, 116, 139));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);
        titleBlock.add(lblTitle);
        titleBlock.add(lblSubtitle);
        headerBar.add(titleBlock, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtns.setOpaque(false);

        ModernButton btnApply = new ModernButton("+ Apply Leave", ModernButton.ButtonType.PRIMARY);
        ModernButton btnApprove = new ModernButton("✓ Approve", ModernButton.ButtonType.SUCCESS);
        ModernButton btnReject = new ModernButton("✗ Reject", ModernButton.ButtonType.DANGER);
        ModernButton btnRefresh = new ModernButton("Refresh", ModernButton.ButtonType.SECONDARY);

        btnApply.addActionListener(e -> onApplyLeave());
        btnApprove.addActionListener(e -> onProcessLeave(true));
        btnReject.addActionListener(e -> onProcessLeave(false));
        btnRefresh.addActionListener(e -> loadData());

        actionBtns.add(btnApply);
        actionBtns.add(btnApprove);
        actionBtns.add(btnReject);
        actionBtns.add(btnRefresh);
        headerBar.add(actionBtns, BorderLayout.EAST);

        topContainer.add(headerBar);
        topContainer.add(Box.createVerticalStrut(14));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));

        filterBar.add(new JLabel("Status:"));
        cmbStatusFilter = new JComboBox<>(new String[]{"All", "PENDING", "APPROVED", "REJECTED"});
        cmbStatusFilter.addActionListener(e -> loadData());
        filterBar.add(cmbStatusFilter);

        topContainer.add(filterBar);
        add(topContainer, BorderLayout.NORTH);

        // 2. TABLE
        String[] cols = {"ID", "Reg No", "Student Name", "Dept", "Year", "Type", "From Date", "To Date", "Reason", "Status", "Approver", "Remarks"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new ModernTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(95);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(50);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setPreferredWidth(85);
        table.getColumnModel().getColumn(7).setPreferredWidth(85);
        table.getColumnModel().getColumn(8).setPreferredWidth(180);
        table.getColumnModel().getColumn(9).setPreferredWidth(90);
        table.getColumnModel().getColumn(10).setPreferredWidth(120);
        table.getColumnModel().getColumn(11).setPreferredWidth(140);

        // Status column custom renderer
        table.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                String st = value != null ? value.toString() : "";
                if ("APPROVED".equalsIgnoreCase(st)) {
                    c.setForeground(new Color(22, 101, 52));
                } else if ("REJECTED".equalsIgnoreCase(st)) {
                    c.setForeground(new Color(185, 28, 28));
                } else {
                    c.setForeground(new Color(194, 65, 12));
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        add(scrollPane, BorderLayout.CENTER);

        // 3. FOOTER
        lblStatus = new JLabel("Loading leaves...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(100, 116, 139));
        add(lblStatus, BorderLayout.SOUTH);

        loadData();
    }

    public void loadData() {
        tableModel.setRowCount(0);
        String filter = (String) cmbStatusFilter.getSelectedItem();

        try {
            List<LeaveRequest> list = leaveService.getAllLeaveRequests();
            int count = 0;
            for (LeaveRequest r : list) {
                if (!"All".equalsIgnoreCase(filter) && !filter.equalsIgnoreCase(r.getStatus())) {
                    continue;
                }

                tableModel.addRow(new Object[]{
                        r.getLeaveId(),
                        r.getRegisterNo(),
                        r.getStudentName(),
                        r.getDepartment(),
                        r.getYearOfStudy(),
                        r.getLeaveType(),
                        DateUtil.formatDisplayDate(r.getFromDate()),
                        DateUtil.formatDisplayDate(r.getToDate()),
                        r.getReason(),
                        r.getStatus(),
                        r.getApprovedBy() != null ? r.getApprovedBy() : "—",
                        r.getRemarks() != null ? r.getRemarks() : "—"
                });
                count++;
            }
            lblStatus.setText(String.format("Showing %d leave records", count));
        } catch (Exception ex) {
            lblStatus.setText("Error loading leave requests: " + ex.getMessage());
        }
    }

    private void onApplyLeave() {
        ApplyLeaveDialog dlg = new ApplyLeaveDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            loadData();
        }
    }

    private void onProcessLeave(boolean approve) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a leave request from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int leaveId = (Integer) tableModel.getValueAt(row, 0);
        String studentName = (String) tableModel.getValueAt(row, 2);
        String currStatus = (String) tableModel.getValueAt(row, 9);

        if (!"PENDING".equalsIgnoreCase(currStatus)) {
            JOptionPane.showMessageDialog(this, "This leave request has already been " + currStatus + ".", "Already Processed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String action = approve ? "Approve" : "Reject";
        String prompt = "Enter remarks or reason to " + action + " leave for " + studentName + ":";
        String remarks = JOptionPane.showInputDialog(this, prompt, action + " Leave Request", JOptionPane.PLAIN_MESSAGE);

        if (remarks == null) return; // user cancelled

        User currentUser = authService.getCurrentUser();
        String approverName = currentUser != null ? currentUser.getFullName() : "Administrator";

        try {
            if (approve) {
                leaveService.approveLeave(leaveId, approverName, remarks);
                JOptionPane.showMessageDialog(this, "Leave request approved successfully.", "Approved", JOptionPane.INFORMATION_MESSAGE);
            } else {
                leaveService.rejectLeave(leaveId, approverName, remarks);
                JOptionPane.showMessageDialog(this, "Leave request rejected.", "Rejected", JOptionPane.INFORMATION_MESSAGE);
            }
            loadData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to process leave request: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
