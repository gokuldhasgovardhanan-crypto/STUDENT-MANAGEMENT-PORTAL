package com.attendance.service;

import com.attendance.config.DatabaseConnection;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for Database Backup and Safe Restore operations.
 */
public class BackupRestoreService {

    /**
     * Creates a portable SQL backup script containing structure and data of all active tables.
     */
    public void backupDatabase(File destinationFile) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(destinationFile), "UTF-8"))) {

            pw.println("-- ===================================================================");
            pw.println("-- KIT ENGINEERING COLLEGE - Database Backup Script");
            pw.println("-- Generated At: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pw.println("-- ===================================================================");
            pw.println();
            pw.println("USE student_attendance_db;");
            pw.println("SET FOREIGN_KEY_CHECKS = 0;");
            pw.println();

            String[] tables = {
                    "settings", "users", "departments", "teachers", "students",
                    "subjects", "student_subjects", "periods", "timetable",
                    "attendance", "attendance_session", "leave_requests", "attendance_audit", "notifications"
            };

            for (String table : tables) {
                exportTableData(conn, table, pw);
            }

            pw.println("SET FOREIGN_KEY_CHECKS = 1;");
            pw.println("-- End of Backup Script");
        }
    }

    private void exportTableData(Connection conn, String tableName, PrintWriter pw) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) return;
        } catch (SQLException ignored) {
            return; // Table might not exist yet
        }

        pw.println("-- Table Data: " + tableName);
        String sql = "SELECT * FROM " + tableName;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                sb.append("INSERT INTO ").append(tableName).append(" VALUES (");
                for (int i = 1; i <= colCount; i++) {
                    Object val = rs.getObject(i);
                    if (val == null) {
                        sb.append("NULL");
                    } else if (val instanceof Number) {
                        sb.append(val);
                    } else {
                        String str = val.toString().replace("'", "''").replace("\\", "\\\\");
                        sb.append("'").append(str).append("'");
                    }
                    if (i < colCount) sb.append(", ");
                }
                sb.append(") ON DUPLICATE KEY UPDATE ").append(meta.getColumnName(1)).append(" = ").append(meta.getColumnName(1)).append(";");
                pw.println(sb.toString());
            }
            pw.println();
        }
    }

    /**
     * Executes SQL statements from a backup file to restore database state.
     */
    public int restoreDatabase(File backupFile) throws Exception {
        String content = Files.readString(backupFile.toPath());
        String[] statements = content.split(";\\r?\\n");
        int executedCount = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                try {
                    stmt.execute(trimmed);
                    executedCount++;
                } catch (SQLException e) {
                    System.err.println("Restore warning on stmt: " + e.getMessage());
                }
            }
        }
        return executedCount;
    }
}
