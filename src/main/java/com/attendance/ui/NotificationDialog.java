package com.attendance.ui;

import com.attendance.model.Notification;
import com.attendance.service.NotificationService;
import com.attendance.ui.components.ModernButton;
import com.attendance.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Modal dialog displaying institutional notifications, attendance alerts, and leave updates.
 */
public class NotificationDialog extends JDialog {

    private final NotificationService notificationService;
    private final String role;
    private final JPanel listContainer;

    public NotificationDialog(Window owner, String role) {
        super(owner, "Notification Center & System Alerts", ModalityType.APPLICATION_MODAL);
        this.notificationService = new NotificationService();
        this.role = role != null ? role : "ALL";

        setSize(520, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 14));
        header.setBackground(new Color(168, 28, 28)); // KIT Crimson Red
        JLabel lblTitle = new JLabel("🔔  Notifications & Important Alerts");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle);
        add(header, BorderLayout.NORTH);

        // Body with Scrollable List
        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Color.WHITE);
        listContainer.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnMarkAll = new ModernButton("Mark All as Read", ModernButton.ButtonType.SECONDARY);
        ModernButton btnClose = new ModernButton("Close", ModernButton.ButtonType.PRIMARY);

        btnMarkAll.addActionListener(e -> {
            notificationService.markAllAsRead(role);
            loadNotifications();
        });
        btnClose.addActionListener(e -> dispose());

        footer.add(btnMarkAll);
        footer.add(btnClose);
        add(footer, BorderLayout.SOUTH);

        loadNotifications();
    }

    private void loadNotifications() {
        listContainer.removeAll();
        List<Notification> list = notificationService.getNotificationsForRole(role);

        if (list.isEmpty()) {
            JLabel empty = new JLabel("No notifications at this time.");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            empty.setForeground(new Color(100, 116, 139));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
            listContainer.add(empty);
        } else {
            for (Notification n : list) {
                listContainer.add(createNotificationCard(n));
                listContainer.add(Box.createVerticalStrut(10));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createNotificationCard(Notification n) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(n.isRead() ? new Color(248, 250, 252) : new Color(254, 242, 242));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(n.isRead() ? new Color(226, 232, 240) : new Color(252, 165, 165), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel((n.isRead() ? "" : "● ") + n.getTitle());
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(n.isRead() ? new Color(30, 41, 59) : new Color(153, 27, 27));

        JLabel cat = new JLabel("[" + n.getCategory() + "]");
        cat.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cat.setForeground(new Color(100, 116, 139));

        top.add(title, BorderLayout.WEST);
        top.add(cat, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JLabel msg = new JLabel("<html>" + n.getMessage() + "</html>");
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        msg.setForeground(new Color(71, 85, 105));
        card.add(msg, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        String timeStr = n.getCreatedAt() != null ? DateUtil.formatDisplayDateTime(n.getCreatedAt()) : "";
        JLabel time = new JLabel(timeStr);
        time.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        time.setForeground(new Color(148, 163, 184));
        bottom.add(time, BorderLayout.WEST);

        if (!n.isRead()) {
            JButton btnRead = new JButton("Mark Read");
            btnRead.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnRead.setMargin(new Insets(1, 6, 1, 6));
            btnRead.addActionListener(e -> {
                notificationService.markAsRead(n.getId());
                loadNotifications();
            });
            bottom.add(btnRead, BorderLayout.EAST);
        }

        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }
}
