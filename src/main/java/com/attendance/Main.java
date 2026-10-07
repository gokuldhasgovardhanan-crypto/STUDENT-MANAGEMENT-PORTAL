package com.attendance;

import com.attendance.config.DatabaseConnection;
import com.attendance.service.DatabaseInitService;
import com.attendance.ui.LoginFrame;
import com.attendance.util.SqlScriptGenerator;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application entry point for the Student Attendance Management System.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // 1. Process CLI switches if passed
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if ("--init-db".equalsIgnoreCase(arg)) {
                    System.out.println("Running database initialization...");
                    try {
                        DatabaseInitService.initializeDatabaseAndSeed(false);
                        System.out.println("Database initialization completed successfully!");
                    } catch (Exception e) {
                        System.err.println("Database initialization failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return;
                } else if ("--export-sql".equalsIgnoreCase(arg)) {
                    System.out.println("Exporting SQL scripts...");
                    try {
                        SqlScriptGenerator.main(new String[]{"."});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }
        }

        // 2. Setup FlatLaf Modern Look & Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "FlatLaf look and feel not loaded, defaulting to system L&F", e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // 3. Pre-flight check database in background
        new Thread(() -> {
            try {
                if (DatabaseConnection.isConnected()) {
                    if (!DatabaseInitService.checkTablesExist()) {
                        LOGGER.info("Database connected but tables missing. Automatically initializing schema and demo data...");
                        DatabaseInitService.initializeDatabaseAndSeed(false);
                        LOGGER.info("Automatic database initialization complete.");
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Auto database initialization could not complete: " + e.getMessage());
            }
        }).start();

        // 4. Launch Desktop GUI
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
