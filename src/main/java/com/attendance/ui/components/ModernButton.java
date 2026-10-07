package com.attendance.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Modern styled button with custom color schemes and smooth hover transitions.
 */
public class ModernButton extends JButton {

    public enum ButtonType {
        PRIMARY(new Color(30, 64, 175), new Color(29, 78, 216), Color.WHITE),
        SUCCESS(new Color(22, 101, 52), new Color(21, 128, 61), Color.WHITE),
        DANGER(new Color(153, 27, 27), new Color(185, 28, 28), Color.WHITE),
        SECONDARY(new Color(241, 245, 249), new Color(226, 232, 240), new Color(51, 65, 85)),
        OUTLINE(Color.WHITE, new Color(241, 245, 249), new Color(30, 64, 175));

        final Color normalBg;
        final Color hoverBg;
        final Color textCol;

        ButtonType(Color normal, Color hover, Color text) {
            this.normalBg = normal;
            this.hoverBg = hover;
            this.textCol = text;
        }
    }

    private final ButtonType type;

    public ModernButton(String text) {
        this(text, ButtonType.PRIMARY);
    }

    public ModernButton(String text, ButtonType type) {
        super(text);
        this.type = type;

        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setForeground(type.textCol);
        setBackground(type.normalBg);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMargin(new java.awt.Insets(8, 16, 8, 16));

        getModel().addChangeListener(e -> repaint());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        ButtonModel model = getModel();
        Color bg = model.isRollover() || model.isArmed() ? type.hoverBg : type.normalBg;

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        if (type == ButtonType.OUTLINE) {
            g2.setColor(new Color(203, 213, 225));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
