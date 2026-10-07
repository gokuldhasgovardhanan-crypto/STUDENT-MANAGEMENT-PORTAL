package com.attendance.service;

import com.attendance.dao.UserDAO;
import com.attendance.model.User;
import com.attendance.util.PasswordUtil;

import java.util.logging.Logger;

/**
 * Service managing user authentication, sessions, and security.
 */
public class AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());
    private static AuthenticationService instance;

    private final UserDAO userDAO;
    private User currentUser;

    public AuthenticationService() {
        this.userDAO = new UserDAO();
    }

    public static synchronized AuthenticationService getInstance() {
        if (instance == null) {
            instance = new AuthenticationService();
        }
        return instance;
    }

    /**
     * Authenticates username and password against users table in MySQL.
     */
    public boolean login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return false;
        }

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            return false;
        }

        if (PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            this.currentUser = user;
            LOGGER.info("User authenticated successfully: " + username + " (" + user.getRole() + ")");
            return true;
        }

        return false;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        User user = userDAO.findById(userId);
        if (user == null) return false;

        if (!PasswordUtil.checkPassword(oldPassword, user.getPasswordHash())) {
            return false;
        }

        String newHash = PasswordUtil.hashPassword(newPassword);
        return userDAO.updatePassword(userId, newHash);
    }
}
