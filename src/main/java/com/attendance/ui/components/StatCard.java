package com.attendance.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Modern KPI dashboard card displaying a title, bold statistic value, and accent color.
 */
public class StatCard extends JPanel {

    private final JLabel lblTitle;
    private final JLabel lblValue;
    private final JLabel lblSubtitle;
    private final Color accentColor;

    public StatCard(String title, String initialValue, String subtitle, Color accentColor) {
        this.accentColor = accentColor;
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(new Color(100, 116, 139));

        lblValue = new JLabel(initialValue);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblValue.setForeground(new Color(15, 23, 42));

        lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(148, 163, 184));

        textPanel.add(lblTitle);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(lblValue);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(lblSubtitle);

        add(textPanel, BorderLayout.CENTER);
    }

    public void setValue(String val) {
        lblValue.setText(val);
        repaint();
    }

    public void setSubtitle(String sub) {
        lblSubtitle.setText(sub);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Card background
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

        // Left accent bar
        g2.setColor(accentColor);
        g2.fillRoundRect(0, 0, 6, getHeight(), 6, 6);

        // Subtle border
        g2.setColor(new Color(226, 232, 240));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

        g2.dispose();
    }
}
