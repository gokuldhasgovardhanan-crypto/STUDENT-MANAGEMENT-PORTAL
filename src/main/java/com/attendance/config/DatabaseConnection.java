package com.attendance.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages JDBC connection pooling/creation to MySQL 8.x with dynamic configuration support.
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String EXTERNAL_CONFIG_FILE = "db.properties";

    private static String host = "localhost";
    private static int port = 3306;
    private static String dbName = "student_attendance_db";
    private static String username = "root";
    private static String password = "root";
    private static String extraParams = "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

    static {
        loadProperties();
    }

    public static synchronized void loadProperties() {
        Properties props = new Properties();

        // 1. Try internal application.properties
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load internal application.properties", e);
        }

        // 2. Override with external db.properties if exists
        Path externalPath = Paths.get(EXTERNAL_CONFIG_FILE);
        if (Files.exists(externalPath)) {
            try (InputStream in = Files.newInputStream(externalPath)) {
                props.load(in);
                LOGGER.info("Loaded overrides from " + EXTERNAL_CONFIG_FILE);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not load external db.properties", e);
            }
        }

        host = props.getProperty("db.host", host);
        try {
            port = Integer.parseInt(props.getProperty("db.port", String.valueOf(port)));
        } catch (NumberFormatException ignored) {}
        dbName = props.getProperty("db.name", dbName);
        username = props.getProperty("db.username", username);
        password = props.getProperty("db.password", password);
        extraParams = props.getProperty("db.params", extraParams);
    }

    public static synchronized void saveExternalProperties(String h, int p, String db, String u, String pwd) {
        host = h;
        port = p;
        dbName = db;
        username = u;
        password = pwd;

        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", String.valueOf(port));
        props.setProperty("db.name", dbName);
        props.setProperty("db.username", username);
        props.setProperty("db.password", password);
        props.setProperty("db.params", extraParams);

        try (OutputStream out = Files.newOutputStream(Paths.get(EXTERNAL_CONFIG_FILE))) {
            props.store(out, "Student Attendance System - User Database Configuration");
            LOGGER.info("Saved user database properties to " + EXTERNAL_CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to persist external db.properties", e);
        }
    }

    public static String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?%s", host, port, dbName, extraParams);
    }

    public static String getServerJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/?%s", host, port, extraParams);
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found on classpath", e);
        }
        return DriverManager.getConnection(getJdbcUrl(), username, password);
    }

    public static Connection getServerConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found on classpath", e);
        }
        return DriverManager.getConnection(getServerJdbcUrl(), username, password);
    }

    public static boolean testConnection(String h, int p, String db, String u, String pwd) {
        String url = String.format("jdbc:mysql://%s:%d/%s?%s", h, p, db, extraParams);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, u, pwd)) {
                return conn != null && !conn.isClosed();
            }
        } catch (Exception e) {
            LOGGER.warning("Test connection failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean testServerConnection(String h, int p, String u, String pwd) {
        String url = String.format("jdbc:mysql://%s:%d/?%s", h, p, extraParams);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, u, pwd)) {
                return conn != null && !conn.isClosed();
            }
        } catch (Exception e) {
            LOGGER.warning("Test server connection failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    public static String getHost() { return host; }
    public static int getPort() { return port; }
    public static String getDbName() { return dbName; }
    public static String getUsername() { return username; }
    public static String getPassword() { return password; }
}
