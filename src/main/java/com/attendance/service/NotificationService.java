package com.attendance.service;

import com.attendance.dao.NotificationDAO;
import com.attendance.model.Notification;

import java.sql.SQLException;
import java.util.List;

/**
 * Service for in-app Notifications & Alerts.
 */
public class NotificationService {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    public void send(String title, String message, String category, String targetRole) {
        Notification n = new Notification(0, title, message, category, targetRole, false, null);
        notificationDAO.create(n);
    }

    public List<Notification> getNotificationsForRole(String role) {
        try {
            return notificationDAO.findForRole(role);
        } catch (SQLException e) {
            return List.of();
        }
    }

    public int getUnreadCount(String role) {
        try {
            return notificationDAO.getUnreadCount(role);
        } catch (SQLException e) {
            return 0;
        }
    }

    public boolean markAsRead(int id) {
        try {
            return notificationDAO.markAsRead(id);
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean markAllAsRead(String role) {
        try {
            return notificationDAO.markAllAsRead(role);
        } catch (SQLException e) {
            return false;
        }
    }
}
