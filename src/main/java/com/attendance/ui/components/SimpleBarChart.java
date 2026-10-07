package com.attendance.ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Modern custom-rendered bar chart displaying departmental attendance metrics.
 */
public class SimpleBarChart extends JPanel {

    private Map<String, Double> data = new LinkedHashMap<>();
    private double targetThreshold = 75.0;

    public SimpleBarChart() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(500, 260));
        setMinimumSize(new Dimension(350, 200));
    }

    public void setData(Map<String, Double> data, double targetThreshold) {
        this.data = data != null ? data : new LinkedHashMap<>();
        this.targetThreshold = targetThreshold;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Chart padding
        int padLeft = 50;
        int padRight = 30;
        int padTop = 30;
        int padBottom = 40;

        int chartW = w - padLeft - padRight;
        int chartH = h - padTop - padBottom;

        if (chartW <= 0 || chartH <= 0) {
            g2.dispose();
            return;
        }

        // Draw horizontal grid lines (0%, 25%, 50%, 75%, 100%)
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        for (int p = 0; p <= 100; p += 25) {
            int y = padTop + chartH - (int) ((p / 100.0) * chartH);
            g2.setColor(new Color(241, 245, 249));
            g2.drawLine(padLeft, y, padLeft + chartW, y);

            g2.setColor(new Color(148, 163, 184));
            g2.drawString(p + "%", 15, y + 4);
        }

        // Draw Target Threshold Dotted Line
        if (targetThreshold > 0 && targetThreshold <= 100) {
            int targetY = padTop + chartH - (int) ((targetThreshold / 100.0) * chartH);
            Stroke originalStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0));
            g2.setColor(new Color(239, 68, 68));
            g2.drawLine(padLeft, targetY, padLeft + chartW, targetY);
            g2.drawString("Req: " + (int) targetThreshold + "%", padLeft + chartW - 55, targetY - 4);
            g2.setStroke(originalStroke);
        }

        // Draw Bars
        if (!data.isEmpty()) {
            int barCount = data.size();
            int slotW = chartW / barCount;
            int barW = Math.max(16, Math.min(48, (int) (slotW * 0.55)));

            int idx = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                String label = entry.getKey();
                double val = Math.max(0.0, Math.min(100.0, entry.getValue()));

                int barH = (int) ((val / 100.0) * chartH);
                int x = padLeft + (idx * slotW) + (slotW - barW) / 2;
                int y = padTop + chartH - barH;

                // Color based on performance
                Color topCol = val >= targetThreshold ? new Color(59, 130, 246) : new Color(249, 115, 22);
                Color botCol = val >= targetThreshold ? new Color(29, 78, 216) : new Color(234, 88, 12);

                GradientPaint gp = new GradientPaint(x, y, topCol, x, y + barH, botCol);
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, barW, barH, 6, 6);

                // Value on top of bar
                g2.setColor(new Color(30, 41, 59));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                String valStr = String.format("%.1f%%", val);
                FontMetrics fm = g2.getFontMetrics();
                int strW = fm.stringWidth(valStr);
                g2.drawString(valStr, x + (barW - strW) / 2, y - 6);

                // Department code below bar
                g2.setColor(new Color(71, 85, 105));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                FontMetrics fmLabel = g2.getFontMetrics();
                int lblW = fmLabel.stringWidth(label);
                g2.drawString(label, x + (barW - lblW) / 2, padTop + chartH + 18);

                idx++;
            }
        } else {
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            String msg = "No attendance data available yet";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, padLeft + (chartW - fm.stringWidth(msg)) / 2, padTop + chartH / 2);
        }

        // Draw Base Axis Line
        g2.setColor(new Color(203, 213, 225));
        g2.drawLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH);

        g2.dispose();
    }
}
