package com.attendance.ui.components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Modern styled JTable with FlatLaf typography, alternating rows, and status badges.
 */
public class ModernTable extends JTable {

    public ModernTable() {
        super();
        initStyle();
    }

    public ModernTable(TableModel model) {
        super(model);
        initStyle();
    }

    private void initStyle() {
        setRowHeight(32);
        setFont(new Font("Segoe UI", Font.PLAIN, 13));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoCreateRowSorter(true);
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setGridColor(new Color(241, 245, 249));

        setSelectionBackground(new Color(224, 231, 255));
        setSelectionForeground(new Color(30, 27, 75));

        // Header Style
        JTableHeader header = getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(new Color(71, 85, 105));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(203, 213, 225)));
        header.setPreferredSize(new Dimension(0, 36));

        // Default cell renderer with alternating row colors
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(new Color(30, 41, 59));
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
    }

    /**
     * Renders a status badge cell for attendance percentage standing.
     */
    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

            String status = value != null ? value.toString() : "";
            if (!isSelected) {
                label.setOpaque(true);
                switch (status.toUpperCase()) {
                    case "EXCELLENT":
                    case "PRESENT":
                        label.setBackground(new Color(220, 252, 231));
                        label.setForeground(new Color(22, 101, 52));
                        break;
                    case "GOOD":
                        label.setBackground(new Color(224, 242, 254));
                        label.setForeground(new Color(3, 105, 161));
                        break;
                    case "WARNING":
                        label.setBackground(new Color(254, 243, 199));
                        label.setForeground(new Color(180, 83, 9));
                        break;
                    case "CRITICAL":
                    case "ABSENT":
                        label.setBackground(new Color(254, 226, 226));
                        label.setForeground(new Color(185, 28, 28));
                        break;
                    default:
                        label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                        label.setForeground(new Color(71, 85, 105));
                        break;
                }
            }
            return label;
        }
    }
}
