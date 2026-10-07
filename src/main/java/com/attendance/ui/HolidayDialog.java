package com.attendance.ui;

import com.attendance.model.Holiday;
import com.attendance.service.HolidayService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;
import com.attendance.util.DateUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Dialog for managing academic and institutional holidays.
 * Holidays configured here are excluded from working attendance days.
 */
public class HolidayDialog extends JDialog {

    private final HolidayService holidayService;
    private final DefaultTableModel tableModel;
    private final ModernTable holidayTable;

    private final JTextField txtDate;
    private final JTextField txtName;
    private final JTextField txtDesc;

    public HolidayDialog(Window owner) {
        super(owner, "Institutional Holiday Management", ModalityType.APPLICATION_MODAL);
        this.holidayService = new HolidayService();

        setSize(700, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 12));
        getContentPane().setBackground(new Color(248, 250, 252));

        // Top Form Card
        JPanel topCard = new JPanel(new BorderLayout(8, 8));
        topCard.setBackground(Color.WHITE);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblTitle = new JLabel("Academic Calendar & Declared Holidays");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(168, 28, 28)); // Crimson
        topCard.add(lblTitle, BorderLayout.NORTH);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        inputRow.setOpaque(false);

        txtDate = new JTextField(LocalDate.now().format(DateUtil.DISPLAY_DATE_FORMAT), 8);
        txtName = new JTextField(16);
        txtDesc = new JTextField(14);

        inputRow.add(new JLabel("Date (dd-MM-yyyy):"));
        inputRow.add(txtDate);
        inputRow.add(new JLabel("Holiday Name:"));
        inputRow.add(txtName);
        inputRow.add(new JLabel("Description:"));
        inputRow.add(txtDesc);

        ModernButton btnAdd = new ModernButton("+ ADD HOLIDAY", ModernButton.ButtonType.PRIMARY);
        btnAdd.addActionListener(e -> onAddHoliday());
        inputRow.add(btnAdd);

        topCard.add(inputRow, BorderLayout.CENTER);
        add(topCard, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"ID", "Date", "Holiday Name", "Description"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        holidayTable = new ModernTable(tableModel);
        holidayTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        holidayTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        holidayTable.getColumnModel().getColumn(2).setPreferredWidth(240);
        holidayTable.getColumnModel().getColumn(3).setPreferredWidth(260);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        centerPanel.setOpaque(false);
        centerPanel.add(new JScrollPane(holidayTable), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        bottomBar.setBackground(new Color(248, 250, 252));
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnDelete = new ModernButton("DELETE SELECTED", ModernButton.ButtonType.DANGER);
        ModernButton btnClose = new ModernButton("CLOSE", ModernButton.ButtonType.SECONDARY);

        btnDelete.addActionListener(e -> onDeleteHoliday());
        btnClose.addActionListener(e -> dispose());

        bottomBar.add(btnDelete);
        bottomBar.add(btnClose);
        add(bottomBar, BorderLayout.SOUTH);

        loadHolidays();
    }

    private void loadHolidays() {
        tableModel.setRowCount(0);
        List<Holiday> list = holidayService.getAllHolidays();
        for (Holiday h : list) {
            tableModel.addRow(new Object[]{
                    h.getHolidayId(),
                    DateUtil.formatDisplayDate(h.getHolidayDate()),
                    h.getHolidayName(),
                    h.getDescription() != null ? h.getDescription() : ""
            });
        }
    }

    private void onAddHoliday() {
        String dateStr = txtDate.getText().trim();
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a holiday name.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate date = DateUtil.parseDisplayDate(dateStr);
        if (date == null) {
            JOptionPane.showMessageDialog(this, "Please enter a valid date in dd-MM-yyyy format.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Holiday h = new Holiday(date, name, desc);
            boolean ok = holidayService.addHoliday(h);
            if (ok) {
                JOptionPane.showMessageDialog(this, "✓ Holiday added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                txtName.setText("");
                txtDesc.setText("");
                loadHolidays();
            } else {
                JOptionPane.showMessageDialog(this, "Could not add holiday (possibly duplicate date).", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save holiday: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDeleteHoliday() {
        int row = holidayTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a holiday row to delete.", "Select Row", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int holidayId = (Integer) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete holiday: " + name + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean ok = holidayService.deleteHoliday(holidayId);
                if (ok) {
                    loadHolidays();
                    JOptionPane.showMessageDialog(this, "✓ Holiday removed successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error deleting holiday: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
