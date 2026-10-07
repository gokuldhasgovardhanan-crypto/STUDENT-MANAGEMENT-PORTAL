package com.attendance.ui;

import com.attendance.model.Period;
import com.attendance.model.TimetableEntry;
import com.attendance.service.TimetableService;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel providing visual weekly timetable schedule view and period management.
 */
public class TimetablePanel extends JPanel {

    private final TimetableService timetableService;
    private final DefaultTableModel gridModel;
    private final JTable gridTable;

    private final DefaultTableModel listModel;
    private final JTable listTable;

    private JComboBox<String> cmbDept;
    private JComboBox<Integer> cmbYear;
    private JComboBox<String> cmbSection;
    private JTabbedPane tabbedPane;

    public TimetablePanel() {
        this.timetableService = new TimetableService();
        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // 1. TOP HEADER & FILTER
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JLabel lblTitle = new JLabel("Class Timetable & Schedule Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 41, 59));

        JLabel lblSubtitle = new JLabel("Manage class schedules, period timings, room allocations, and faculty assignments");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitle.setForeground(new Color(100, 116, 139));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);
        titleBlock.add(lblTitle);
        titleBlock.add(lblSubtitle);
        headerBar.add(titleBlock, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtns.setOpaque(false);

        ModernButton btnAdd = new ModernButton("+ Add Slot", ModernButton.ButtonType.PRIMARY);
        ModernButton btnDelete = new ModernButton("Delete Slot", ModernButton.ButtonType.DANGER);
        ModernButton btnRefresh = new ModernButton("Refresh", ModernButton.ButtonType.SECONDARY);

        btnAdd.addActionListener(e -> onAddSlot());
        btnDelete.addActionListener(e -> onDeleteSlot());
        btnRefresh.addActionListener(e -> loadData());

        actionBtns.add(btnAdd);
        actionBtns.add(btnDelete);
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

        filterBar.add(new JLabel("Department:"));
        cmbDept = new JComboBox<>(new String[]{"CSE", "IT", "AI&DS", "ECE", "EEE", "MECH"});
        cmbDept.addActionListener(e -> loadData());
        filterBar.add(cmbDept);

        filterBar.add(Box.createHorizontalStrut(8));
        filterBar.add(new JLabel("Year:"));
        cmbYear = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        cmbYear.setSelectedItem(3);
        cmbYear.addActionListener(e -> loadData());
        filterBar.add(cmbYear);

        filterBar.add(Box.createHorizontalStrut(8));
        filterBar.add(new JLabel("Section:"));
        cmbSection = new JComboBox<>(new String[]{"A", "B"});
        cmbSection.addActionListener(e -> loadData());
        filterBar.add(cmbSection);

        topContainer.add(filterBar);
        add(topContainer, BorderLayout.NORTH);

        // 2. TABBED VIEW: WEEKLY GRID vs LIST VIEW
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // TAB 1: Weekly Grid
        String[] gridCols = {"Day", "Period 1 (09:00-10:00)", "Period 2 (10:00-11:00)", "Period 3 (11:15-12:15)",
                "Period 4 (12:15-13:15)", "Period 5 (14:00-15:00)", "Period 6 (15:00-16:00)"};
        gridModel = new DefaultTableModel(gridCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        gridTable = new ModernTable(gridModel);
        gridTable.setRowHeight(48);
        gridTable.getColumnModel().getColumn(0).setPreferredWidth(100);

        JScrollPane gridScroll = new JScrollPane(gridTable);
        gridScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        tabbedPane.addTab("🗓️  Weekly Schedule Grid", gridScroll);

        // TAB 2: Detailed List
        String[] listCols = {"Slot ID", "Day", "Period", "Subject Code", "Subject Name", "Faculty", "Room"};
        listModel = new DefaultTableModel(listCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        listTable = new ModernTable(listModel);
        listTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        listTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        listTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        listTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        listTable.getColumnModel().getColumn(4).setPreferredWidth(240);
        listTable.getColumnModel().getColumn(5).setPreferredWidth(160);
        listTable.getColumnModel().getColumn(6).setPreferredWidth(90);

        JScrollPane listScroll = new JScrollPane(listTable);
        listScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        tabbedPane.addTab("📋  Detailed Slot List", listScroll);

        add(tabbedPane, BorderLayout.CENTER);

        loadData();
    }

    public void loadData() {
        gridModel.setRowCount(0);
        listModel.setRowCount(0);

        String dept = (String) cmbDept.getSelectedItem();
        Integer year = (Integer) cmbYear.getSelectedItem();
        String sec = (String) cmbSection.getSelectedItem();

        if (dept == null || year == null || sec == null) return;

        try {
            List<TimetableEntry> entries = timetableService.getTimetableByClass(dept, year, sec);

            // Populate List View
            for (TimetableEntry t : entries) {
                listModel.addRow(new Object[]{
                        t.getTimetableId(),
                        t.getDayOfWeek(),
                        t.getPeriodName() != null ? t.getPeriodName() : ("Period " + t.getPeriodNumber()),
                        t.getSubjectCode(),
                        t.getSubjectName(),
                        t.getTeacherName() != null ? t.getTeacherName() : "Unassigned",
                        t.getRoom()
                });
            }

            // Populate Grid View (Monday to Saturday)
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
            // Map: day -> periodNumber (1..6) -> TimetableEntry
            Map<String, Map<Integer, TimetableEntry>> gridMap = new HashMap<>();
            for (String d : days) gridMap.put(d, new HashMap<>());

            for (TimetableEntry t : entries) {
                if (gridMap.containsKey(t.getDayOfWeek())) {
                    gridMap.get(t.getDayOfWeek()).put(t.getPeriodNumber(), t);
                }
            }

            for (String d : days) {
                Object[] rowData = new Object[7];
                rowData[0] = d;
                Map<Integer, TimetableEntry> dayMap = gridMap.get(d);
                for (int p = 1; p <= 6; p++) {
                    TimetableEntry slot = dayMap.get(p);
                    if (slot != null) {
                        rowData[p] = "<html><b>" + slot.getSubjectCode() + "</b><br><font color='#64748b'>" + slot.getRoom() + "</font></html>";
                    } else {
                        rowData[p] = "—";
                    }
                }
                gridModel.addRow(rowData);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load timetable: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAddSlot() {
        String dept = (String) cmbDept.getSelectedItem();
        Integer year = (Integer) cmbYear.getSelectedItem();
        String sec = (String) cmbSection.getSelectedItem();

        TimetableFormDialog dlg = new TimetableFormDialog(
                SwingUtilities.getWindowAncestor(this),
                null,
                dept,
                year != null ? year : 1,
                sec
        );
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            loadData();
        }
    }

    private void onDeleteSlot() {
        int row = listTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a slot from the 'Detailed Slot List' tab to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            tabbedPane.setSelectedIndex(1);
            return;
        }

        int slotId = (Integer) listModel.getValueAt(row, 0);
        String day = (String) listModel.getValueAt(row, 1);
        String period = (String) listModel.getValueAt(row, 2);
        String sub = (String) listModel.getValueAt(row, 3);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Remove slot " + day + " " + period + " (" + sub + ")?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                timetableService.deleteEntry(slotId);
                loadData();
                JOptionPane.showMessageDialog(this, "Timetable slot deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error deleting slot: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
