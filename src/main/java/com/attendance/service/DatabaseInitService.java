package com.attendance.service;

import com.attendance.config.DatabaseConnection;
import com.attendance.model.Student;
import com.attendance.util.DateUtil;
import com.attendance.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service to verify database readiness, create schema, and seed 100 demo students,
 * faculty, and 40 working days of realistic historical attendance.
 */
public class DatabaseInitService {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitService.class.getName());

    public static boolean checkTablesExist() {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                     "WHERE table_schema = ? AND table_name IN ('users', 'students', 'teachers', 'attendance', 'settings', 'subjects', 'periods', 'timetable', 'leave_requests')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DatabaseConnection.getDbName());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) >= 9;
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Could not check table existence: " + e.getMessage());
            return false;
        }
        return false;
    }

    public static void initializeDatabaseAndSeed(boolean forceRecreate) throws SQLException {
        // 1. Ensure database exists
        try (Connection sConn = DatabaseConnection.getServerConnection();
             Statement stmt = sConn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DatabaseConnection.getDbName() +
                    " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        // 2. Connect to the database and create tables
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            if (forceRecreate) {
                stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
                stmt.executeUpdate("DROP TABLE IF EXISTS notifications");
                stmt.executeUpdate("DROP TABLE IF EXISTS audit_logs");
                stmt.executeUpdate("DROP TABLE IF EXISTS attendance_audit");
                stmt.executeUpdate("DROP TABLE IF EXISTS leave_requests");
                stmt.executeUpdate("DROP TABLE IF EXISTS attendance_session");
                stmt.executeUpdate("DROP TABLE IF EXISTS timetable");
                stmt.executeUpdate("DROP TABLE IF EXISTS periods");
                stmt.executeUpdate("DROP TABLE IF EXISTS student_subjects");
                stmt.executeUpdate("DROP TABLE IF EXISTS attendance");
                stmt.executeUpdate("DROP TABLE IF EXISTS subjects");
                stmt.executeUpdate("DROP TABLE IF EXISTS students");
                stmt.executeUpdate("DROP TABLE IF EXISTS teachers");
                stmt.executeUpdate("DROP TABLE IF EXISTS departments");
                stmt.executeUpdate("DROP TABLE IF EXISTS users");
                stmt.executeUpdate("DROP TABLE IF EXISTS settings");
                stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
            }

            // Create settings
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS settings (" +
                    "setting_key VARCHAR(50) PRIMARY KEY, " +
                    "setting_value VARCHAR(255) NOT NULL, " +
                    "description VARCHAR(255) NULL, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB"
            );

            // Create users
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100) NOT NULL, " +
                    "role ENUM('ADMIN', 'TEACHER', 'STUDENT') NOT NULL DEFAULT 'TEACHER', " +
                    "email VARCHAR(100) NULL, " +
                    "student_id INT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "INDEX idx_users_username (username), " +
                    "INDEX idx_users_role (role)" +
                    ") ENGINE=InnoDB"
            );

            // Create departments
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS departments (" +
                    "dept_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "dept_code VARCHAR(20) NOT NULL UNIQUE, " +
                    "dept_name VARCHAR(100) NOT NULL, " +
                    "INDEX idx_dept_code (dept_code)" +
                    ") ENGINE=InnoDB"
            );

            // Create teachers
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS teachers (" +
                    "teacher_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "employee_id VARCHAR(50) NOT NULL UNIQUE, " +
                    "teacher_name VARCHAR(100) NOT NULL, " +
                    "department VARCHAR(50) NOT NULL, " +
                    "email VARCHAR(100) NOT NULL, " +
                    "phone VARCHAR(20) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "INDEX idx_teacher_empid (employee_id), " +
                    "INDEX idx_teacher_dept (department)" +
                    ") ENGINE=InnoDB"
            );

            // Create students
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS students (" +
                    "student_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "register_no VARCHAR(50) NOT NULL UNIQUE, " +
                    "student_name VARCHAR(100) NOT NULL, " +
                    "gender ENUM('Male', 'Female', 'Other') NOT NULL, " +
                    "date_of_birth DATE NOT NULL, " +
                    "department VARCHAR(50) NOT NULL, " +
                    "year_of_study INT NOT NULL, " +
                    "section VARCHAR(10) NOT NULL, " +
                    "email VARCHAR(100) NOT NULL, " +
                    "phone_number VARCHAR(20) NOT NULL, " +
                    "address TEXT NULL, " +
                    "admission_date DATE NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "INDEX idx_students_regno (register_no), " +
                    "INDEX idx_students_name (student_name), " +
                    "INDEX idx_students_dept (department), " +
                    "INDEX idx_students_year_sec (year_of_study, section)" +
                    ") ENGINE=InnoDB"
            );

            // Create subjects
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS subjects (" +
                    "subject_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "subject_code VARCHAR(20) NOT NULL UNIQUE, " +
                    "subject_name VARCHAR(100) NOT NULL, " +
                    "department VARCHAR(50) NOT NULL, " +
                    "year_of_study INT NOT NULL, " +
                    "semester VARCHAR(20) NOT NULL, " +
                    "credits INT DEFAULT 3, " +
                    "teacher_id INT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "INDEX idx_subject_code (subject_code), " +
                    "INDEX idx_subject_dept_year (department, year_of_study)" +
                    ") ENGINE=InnoDB"
            );

            // Create student_subjects
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS student_subjects (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_id INT NOT NULL, " +
                    "subject_id INT NOT NULL, " +
                    "enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY uq_student_subject (student_id, subject_id), " +
                    "INDEX idx_ss_student (student_id), " +
                    "INDEX idx_ss_subject (subject_id)" +
                    ") ENGINE=InnoDB"
            );

            // Create periods
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS periods (" +
                    "period_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "period_number INT NOT NULL UNIQUE, " +
                    "period_name VARCHAR(50) NOT NULL, " +
                    "start_time VARCHAR(20) NOT NULL, " +
                    "end_time VARCHAR(20) NOT NULL, " +
                    "is_active BOOLEAN DEFAULT TRUE" +
                    ") ENGINE=InnoDB"
            );

            // Create timetable
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS timetable (" +
                    "timetable_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "day_of_week VARCHAR(20) NOT NULL, " +
                    "period_id INT NOT NULL, " +
                    "subject_id INT NOT NULL, " +
                    "teacher_id INT NULL, " +
                    "department VARCHAR(50) NOT NULL, " +
                    "year_of_study INT NOT NULL, " +
                    "section VARCHAR(10) NOT NULL, " +
                    "room VARCHAR(50) DEFAULT 'Room 101', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY uq_timetable_slot (day_of_week, period_id, department, year_of_study, section)" +
                    ") ENGINE=InnoDB"
            );

            // Create attendance
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS attendance (" +
                    "attendance_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_id INT NOT NULL, " +
                    "subject_id INT NULL, " +
                    "attendance_date DATE NOT NULL, " +
                    "period VARCHAR(20) DEFAULT 'Period 1', " +
                    "status ENUM('PRESENT', 'ABSENT') NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "INDEX idx_attendance_date (attendance_date), " +
                    "INDEX idx_attendance_student (student_id)" +
                    ") ENGINE=InnoDB"
            );

            // Create attendance_session
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS attendance_session (" +
                    "session_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "token VARCHAR(64) NOT NULL UNIQUE, " +
                    "subject_id INT NOT NULL, " +
                    "teacher_id INT NOT NULL, " +
                    "attendance_date DATE NOT NULL, " +
                    "period VARCHAR(20) NOT NULL, " +
                    "department VARCHAR(50) NOT NULL, " +
                    "year_of_study INT NOT NULL, " +
                    "section VARCHAR(10) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "expires_at TIMESTAMP NOT NULL, " +
                    "status ENUM('ACTIVE', 'EXPIRED', 'CLOSED') DEFAULT 'ACTIVE'" +
                    ") ENGINE=InnoDB"
            );

            // Create leave_requests
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS leave_requests (" +
                    "leave_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_id INT NOT NULL, " +
                    "leave_type ENUM('Medical', 'Personal', 'College Event', 'Emergency', 'Other') NOT NULL, " +
                    "from_date DATE NOT NULL, " +
                    "to_date DATE NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "supporting_doc VARCHAR(255) NULL, " +
                    "status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING', " +
                    "approved_by VARCHAR(100) NULL, " +
                    "approved_at TIMESTAMP NULL, " +
                    "remarks TEXT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB"
            );

            // Create attendance_audit
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS attendance_audit (" +
                    "audit_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "attendance_id INT NULL, " +
                    "student_id INT NOT NULL, " +
                    "old_status VARCHAR(20) NOT NULL, " +
                    "new_status VARCHAR(20) NOT NULL, " +
                    "changed_by VARCHAR(100) NOT NULL, " +
                    "changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "reason TEXT NOT NULL" +
                    ") ENGINE=InnoDB"
            );

            // Create audit_logs
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id VARCHAR(50) NOT NULL, " +
                    "action VARCHAR(50) NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "ip_address VARCHAR(50) DEFAULT '127.0.0.1'" +
                    ") ENGINE=InnoDB"
            );

            // Create notifications
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS notifications (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "title VARCHAR(100) NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "target_role VARCHAR(20) DEFAULT 'ALL', " +
                    "is_read BOOLEAN DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB"
            );

            // If tables existed from v1, ensure new columns are added safely
            try {
                stmt.executeUpdate("ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN', 'TEACHER', 'STUDENT') NOT NULL DEFAULT 'TEACHER'");
            } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN student_id INT NULL");
            } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate("ALTER TABLE attendance ADD COLUMN subject_id INT NULL");
            } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate("ALTER TABLE attendance ADD COLUMN period VARCHAR(20) DEFAULT 'Period 1'");
            } catch (SQLException ignored) {}

            LOGGER.info("All 15 tables created or verified successfully.");
        }

        // Seed data if empty
        seedDataIfNecessary();
    }

    private static void seedDataIfNecessary() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Seed Settings
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM settings")) {
                if (rs.next() && rs.getInt(1) < 12) {
                    insertSettings(conn);
                }
            }

            // 2. Seed Users (admin, teacher, student)
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) < 3) {
                    insertUsers(conn);
                }
            }

            // 3. Seed Departments
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM departments")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertDepartments(conn);
                }
            }

            // 4. Seed Teachers
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM teachers")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertTeachers(conn);
                }
            }

            // 5. Seed Periods
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM periods")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertPeriods(conn);
                }
            }

            // 6. Seed Subjects
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM subjects")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertSubjects(conn);
                }
            }

            // 7. Seed 100 Students and attendance
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM students")) {
                if (rs.next() && rs.getInt(1) < 100) {
                    insert100DemoStudentsAndAttendance(conn);
                }
            }

            // 8. Seed Student-Subject enrollments
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM student_subjects")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    enrollStudentsInSubjects(conn);
                }
            }

            // 9. Seed Timetable
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM timetable")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertTimetable(conn);
                }
            }

            // 10. Seed Leave Requests
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM leave_requests")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertLeaveRequests(conn);
                }
            }

            // 11. Seed Notifications & Audit Logs
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM notifications")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertNotificationsAndAudits(conn);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while seeding database data", e);
        }
    }

    private static void insertSettings(Connection conn) throws SQLException {
        String sql = "INSERT INTO settings (setting_key, setting_value, description) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Object[][] defaultSettings = {
                    {"college_name", "KIT ENGINEERING COLLEGE", "Configured Institution Name"},
                    {"academic_year", "2026-27", "Current Academic Year"},
                    {"semester", "V", "Current Active Semester"},
                    {"required_attendance_pct", "75.0", "Mandatory attendance requirement"},
                    {"working_days_target", "60", "Semester working days target"},
                    {"departments_list", "CSE,IT,AI&DS,ECE,EEE,MECH", "Offered Degree Departments"},
                    {"years_list", "1,2,3,4", "Academic Years"},
                    {"sections_list", "A,B", "Class Sections"},
                    {"count_approved_leave_as_present", "NO", "Count approved student leave as attendance present"},
                    {"qr_expiration_minutes", "5", "QR code attendance session expiry time in minutes"},
                    {"smtp_host", "smtp.gmail.com", "Outgoing SMTP mail server host"},
                    {"smtp_port", "587", "Outgoing SMTP mail server port"},
                    {"smtp_username", "", "SMTP mail user authentication account"},
                    {"smtp_password", "", "SMTP mail user authentication password"},
                    {"email_notifications_enabled", "NO", "Enable automatic email notifications"},
                    {"app_theme", "LIGHT", "UI Theme Mode (LIGHT / DARK)"}
            };
            for (Object[] row : defaultSettings) {
                ps.setString(1, (String) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.addBatch();
            }
            ps.executeBatch();
            LOGGER.info("Default settings inserted/updated.");
        }
    }

    private static void insertUsers(Connection conn) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, email, student_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE full_name = VALUES(full_name)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Admin: admin / admin123
            String adminHash = PasswordUtil.hashPassword("admin123");
            ps.setString(1, "admin");
            ps.setString(2, adminHash);
            ps.setString(3, "System Administrator");
            ps.setString(4, "ADMIN");
            ps.setString(5, "admin@kit.edu.in");
            ps.setNull(6, java.sql.Types.INTEGER);
            ps.addBatch();

            // Teacher: teacher / teacher123
            String teacherHash = PasswordUtil.hashPassword("teacher123");
            ps.setString(1, "teacher");
            ps.setString(2, teacherHash);
            ps.setString(3, "Prof. Rajesh Sharma");
            ps.setString(4, "TEACHER");
            ps.setString(5, "teacher@kit.edu.in");
            ps.setNull(6, java.sql.Types.INTEGER);
            ps.addBatch();

            // Student: student / student123 (mapped to student_id 1 / 24CSE001)
            String studentHash = PasswordUtil.hashPassword("student123");
            ps.setString(1, "student");
            ps.setString(2, studentHash);
            ps.setString(3, "Aarav Kumar (Student)");
            ps.setString(4, "STUDENT");
            ps.setString(5, "aarav.kumar@kit.edu.in");
            ps.setInt(6, 1);
            ps.addBatch();

            ps.executeBatch();
            LOGGER.info("Default users (admin, teacher, student) created.");
        }
    }

    private static void insertDepartments(Connection conn) throws SQLException {
        String sql = "INSERT INTO departments (dept_code, dept_name) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String[][] depts = {
                    {"CSE", "Computer Science and Engineering"},
                    {"IT", "Information Technology"},
                    {"AI&DS", "Artificial Intelligence and Data Science"},
                    {"ECE", "Electronics and Communication Engineering"},
                    {"EEE", "Electrical and Electronics Engineering"},
                    {"MECH", "Mechanical Engineering"}
            };
            for (String[] d : depts) {
                ps.setString(1, d[0]);
                ps.setString(2, d[1]);
                ps.addBatch();
            }
            ps.executeBatch();
            LOGGER.info("Departments inserted.");
        }
    }

    private static void insertTeachers(Connection conn) throws SQLException {
        String sql = "INSERT INTO teachers (employee_id, teacher_name, department, email, phone) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String[][] teachers = {
                    {"EMP101", "Dr. R. Ramanathan", "CSE", "ramanathan.r@kit.edu.in", "9840123451"},
                    {"EMP102", "Dr. S. Gayathri", "IT", "gayathri.s@kit.edu.in", "9840123452"},
                    {"EMP103", "Prof. K. Venkatesh", "AI&DS", "venkatesh.k@kit.edu.in", "9840123453"},
                    {"EMP104", "Dr. P. Meenakshi", "ECE", "meenakshi.p@kit.edu.in", "9840123454"},
                    {"EMP105", "Prof. M. Suresh", "EEE", "suresh.m@kit.edu.in", "9840123455"},
                    {"EMP106", "Dr. T. Revathi", "MECH", "revathi.t@kit.edu.in", "9840123456"}
            };
            for (String[] t : teachers) {
                ps.setString(1, t[0]);
                ps.setString(2, t[1]);
                ps.setString(3, t[2]);
                ps.setString(4, t[3]);
                ps.setString(5, t[4]);
                ps.addBatch();
            }
            ps.executeBatch();
            LOGGER.info("Demo teachers inserted.");
        }
    }

    private static void insertPeriods(Connection conn) throws SQLException {
        String sql = "INSERT INTO periods (period_number, period_name, start_time, end_time, is_active) VALUES (?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE period_name = VALUES(period_name)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Object[][] periods = {
                    {1, "Period 1", "09:00", "10:00", true},
                    {2, "Period 2", "10:00", "11:00", true},
                    {3, "Period 3", "11:15", "12:15", true},
                    {4, "Period 4", "12:15", "13:15", true},
                    {5, "Period 5", "14:00", "15:00", true},
                    {6, "Period 6", "15:00", "16:00", true}
            };
            for (Object[] p : periods) {
                ps.setInt(1, (Integer) p[0]);
                ps.setString(2, (String) p[1]);
                ps.setString(3, (String) p[2]);
                ps.setString(4, (String) p[3]);
                ps.setBoolean(5, (Boolean) p[4]);
                ps.addBatch();
            }
            ps.executeBatch();
            LOGGER.info("Periods inserted.");
        }
    }

    private static void insertSubjects(Connection conn) throws SQLException {
        String sql = "INSERT INTO subjects (subject_code, subject_name, department, year_of_study, semester, credits, teacher_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE subject_name = VALUES(subject_name)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Object[][] subjects = {
                    {"CS501", "Object Oriented Programming Java", "CSE", 3, "V", 4, 1},
                    {"CS502", "Database Management Systems", "CSE", 3, "V", 4, 1},
                    {"CS503", "Computer Networks", "CSE", 3, "V", 3, 1},
                    {"CS504", "Operating Systems & Cloud", "CSE", 3, "V", 3, 1},
                    {"IT501", "Web Technology & Frameworks", "IT", 3, "V", 4, 2},
                    {"IT502", "Cloud Computing & DevOps", "IT", 3, "V", 3, 2},
                    {"AD501", "Machine Learning Foundations", "AI&DS", 3, "V", 4, 3},
                    {"AD502", "Deep Learning & Neural Networks", "AI&DS", 3, "V", 4, 3},
                    {"EC501", "Digital Signal Processing", "ECE", 3, "V", 4, 4},
                    {"EC502", "VLSI Design & Embedded Systems", "ECE", 3, "V", 3, 4},
                    {"EE501", "Power Electronics & Drives", "EEE", 3, "V", 4, 5},
                    {"EE502", "Control Systems & Automation", "EEE", 3, "V", 3, 5},
                    {"ME501", "Applied Thermodynamics", "MECH", 3, "V", 4, 6},
                    {"ME502", "Fluid Mechanics & Machinery", "MECH", 3, "V", 3, 6}
            };
            for (Object[] s : subjects) {
                ps.setString(1, (String) s[0]);
                ps.setString(2, (String) s[1]);
                ps.setString(3, (String) s[2]);
                ps.setInt(4, (Integer) s[3]);
                ps.setString(5, (String) s[4]);
                ps.setInt(6, (Integer) s[5]);
                ps.setInt(7, (Integer) s[6]);
                ps.addBatch();
            }
            ps.executeBatch();
            LOGGER.info("Subjects inserted.");
        }
    }

    private static void enrollStudentsInSubjects(Connection conn) throws SQLException {
        String enrollSql = "INSERT IGNORE INTO student_subjects (student_id, subject_id) " +
                           "SELECT s.student_id, sub.subject_id FROM students s " +
                           "JOIN subjects sub ON s.department = sub.department";
        try (Statement stmt = conn.createStatement()) {
            int count = stmt.executeUpdate(enrollSql);
            LOGGER.info("Enrolled students into subjects. Inserted records: " + count);
        }
    }

    private static void insertTimetable(Connection conn) throws SQLException {
        String sql = "INSERT INTO timetable (day_of_week, period_id, subject_id, teacher_id, department, year_of_study, section, room) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE room = VALUES(room)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
            int[][] cseGrid = {
                    {1, 2, 3, 4, 1, 2},
                    {2, 3, 4, 1, 3, 4},
                    {3, 4, 1, 2, 2, 1},
                    {4, 1, 2, 3, 4, 3},
                    {1, 2, 3, 4, 1, 2}
            };
            for (int d = 0; d < days.length; d++) {
                for (int p = 1; p <= 6; p++) {
                    int subId = cseGrid[d][p - 1];
                    ps.setString(1, days[d]);
                    ps.setInt(2, p);
                    ps.setInt(3, subId);
                    ps.setInt(4, 1);
                    ps.setString(5, "CSE");
                    ps.setInt(6, 3);
                    ps.setString(7, "A");
                    ps.setString(8, "Lab Block LH-" + (100 + p));
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            LOGGER.info("Timetable slots inserted.");
        }
    }

    private static void insertLeaveRequests(Connection conn) throws SQLException {
        String sql = "INSERT INTO leave_requests (student_id, leave_type, from_date, to_date, reason, status, approved_by, approved_at, remarks) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Student 1 (Aarav Kumar) - Approved Medical Leave
            ps.setInt(1, 1);
            ps.setString(2, "Medical");
            ps.setDate(3, Date.valueOf(LocalDate.of(2026, 8, 17)));
            ps.setDate(4, Date.valueOf(LocalDate.of(2026, 8, 18)));
            ps.setString(5, "Viral fever and doctor prescribed bed rest.");
            ps.setString(6, "APPROVED");
            ps.setString(7, "Prof. Rajesh Sharma");
            ps.setTimestamp(8, java.sql.Timestamp.valueOf("2026-08-16 10:00:00"));
            ps.setString(9, "Medical certificate verified. Approved.");
            ps.addBatch();

            // Student 2 (Arjun Raj) - Pending Event Leave
            ps.setInt(1, 2);
            ps.setString(2, "College Event");
            ps.setDate(3, Date.valueOf(LocalDate.of(2026, 9, 10)));
            ps.setDate(4, Date.valueOf(LocalDate.of(2026, 9, 11)));
            ps.setString(5, "Participating in Inter-Collegiate Hackathon at IIT Madras.");
            ps.setString(6, "PENDING");
            ps.setNull(7, java.sql.Types.VARCHAR);
            ps.setNull(8, java.sql.Types.TIMESTAMP);
            ps.setString(9, "Awaiting department HOD nod.");
            ps.addBatch();

            // Student 3 (Aditya Iyer) - Rejected Leave
            ps.setInt(1, 3);
            ps.setString(2, "Personal");
            ps.setDate(3, Date.valueOf(LocalDate.of(2026, 9, 21)));
            ps.setDate(4, Date.valueOf(LocalDate.of(2026, 9, 22)));
            ps.setString(5, "Family function out of town.");
            ps.setString(6, "REJECTED");
            ps.setString(7, "Prof. Rajesh Sharma");
            ps.setTimestamp(8, java.sql.Timestamp.valueOf("2026-09-20 16:30:00"));
            ps.setString(9, "Attendance shortage; internal exam week.");
            ps.addBatch();

            ps.executeBatch();
            LOGGER.info("Demo leave requests inserted.");
        }
    }

    private static void insertNotificationsAndAudits(Connection conn) throws SQLException {
        // Notifications
        String notifSql = "INSERT INTO notifications (title, message, category, target_role, is_read) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(notifSql)) {
            ps.setString(1, "Welcome to KIT Engineering College Portal");
            ps.setString(2, "Semester V attendance management system is live with QR scanning and subject-wise logs.");
            ps.setString(3, "System");
            ps.setString(4, "ALL");
            ps.setBoolean(5, false);
            ps.addBatch();

            ps.setString(1, "Low Attendance Alert Threshold");
            ps.setString(2, "12 students have attendance below the mandatory 75% threshold in CSE/IT.");
            ps.setString(3, "Attendance");
            ps.setString(4, "TEACHER");
            ps.setBoolean(5, false);
            ps.addBatch();

            ps.setString(1, "Pending Leave Applications");
            ps.setString(2, "1 leave request is currently pending your faculty mentor review.");
            ps.setString(3, "Leave");
            ps.setString(4, "TEACHER");
            ps.setBoolean(5, false);
            ps.addBatch();

            ps.executeBatch();
        }

        // Audit Log
        String auditSql = "INSERT INTO audit_logs (user_id, action, description, ip_address) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(auditSql)) {
            ps.setString(1, "system");
            ps.setString(2, "SYSTEM_INIT");
            ps.setString(3, "Initialized KIT Engineering College Student Attendance Management System v2.0");
            ps.setString(4, "127.0.0.1");
            ps.addBatch();

            ps.setString(1, "admin");
            ps.setString(2, "CONFIG_UPDATE");
            ps.setString(3, "Configured institution profile and semester periods");
            ps.setString(4, "127.0.0.1");
            ps.addBatch();

            ps.executeBatch();
        }
        LOGGER.info("Notifications and Audit Logs seeded.");
    }

    private static void insert100DemoStudentsAndAttendance(Connection conn) throws SQLException {
        List<DemoStudentSeed> seedList = generate100StudentDefinitions();
        String studentSql = "INSERT INTO students (register_no, student_name, gender, date_of_birth, " +
                "department, year_of_study, section, email, phone_number, address, admission_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(studentSql, Statement.RETURN_GENERATED_KEYS)) {
            for (DemoStudentSeed s : seedList) {
                ps.setString(1, s.regNo);
                ps.setString(2, s.name);
                ps.setString(3, s.gender);
                ps.setDate(4, Date.valueOf(s.dob));
                ps.setString(5, s.dept);
                ps.setInt(6, s.year);
                ps.setString(7, s.section);
                ps.setString(8, s.email);
                ps.setString(9, s.phone);
                ps.setString(10, s.address);
                ps.setDate(11, Date.valueOf(s.admissionDate));
                ps.addBatch();
            }
            ps.executeBatch();

            // Retrieve generated student IDs
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int idx = 0;
                while (rs.next() && idx < seedList.size()) {
                    seedList.get(idx).studentId = rs.getInt(1);
                    idx++;
                }
            }
        }

        // If batch generated keys didn't populate for all, query them by register_no
        for (DemoStudentSeed s : seedList) {
            if (s.studentId == 0) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT student_id FROM students WHERE register_no = ?")) {
                    ps.setString(1, s.regNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) s.studentId = rs.getInt(1);
                    }
                }
            }
        }

        // Generate 40 working days of attendance
        // Between Aug 03, 2026 and Sep 25, 2026 (exact 40 weekdays)
        List<LocalDate> workingDays = generate40WorkingDays();

        String attSql = "INSERT INTO attendance (student_id, subject_id, attendance_date, period, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(attSql)) {
            for (DemoStudentSeed s : seedList) {
                if (s.studentId == 0) continue;

                int defaultSubId = 1;
                if ("IT".equalsIgnoreCase(s.dept)) defaultSubId = 5;
                else if ("AI&DS".equalsIgnoreCase(s.dept)) defaultSubId = 7;
                else if ("ECE".equalsIgnoreCase(s.dept)) defaultSubId = 9;
                else if ("EEE".equalsIgnoreCase(s.dept)) defaultSubId = 11;
                else if ("MECH".equalsIgnoreCase(s.dept)) defaultSubId = 13;

                // Determine target present probability based on student's assigned group
                double targetRate = s.targetAttendanceRate;
                Random rnd = new Random(s.regNo.hashCode());

                for (LocalDate day : workingDays) {
                    boolean present = rnd.nextDouble() < targetRate;
                    ps.setInt(1, s.studentId);
                    ps.setInt(2, defaultSubId);
                    ps.setDate(3, Date.valueOf(day));
                    ps.setString(4, "Period 1");
                    ps.setString(5, present ? "PRESENT" : "ABSENT");
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        conn.commit();
        conn.setAutoCommit(true);
        LOGGER.info("100 demo students and 40 days of historical attendance successfully seeded!");
    }

    public static List<LocalDate> generate40WorkingDays() {
        List<LocalDate> days = new ArrayList<>();
        LocalDate curr = LocalDate.of(2026, 8, 3); // Monday
        while (days.size() < 40) {
            DayOfWeek dow = curr.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                days.add(curr);
            }
            curr = curr.plusDays(1);
        }
        return days;
    }

    public static class DemoStudentSeed {
        public int studentId;
        public String regNo;
        public String name;
        public String gender;
        public LocalDate dob;
        public String dept;
        public int year;
        public String section;
        public String email;
        public String phone;
        public String address;
        public LocalDate admissionDate;
        public double targetAttendanceRate;
    }

    public static List<DemoStudentSeed> generate100StudentDefinitions() {
        List<DemoStudentSeed> list = new ArrayList<>();

        String[] firstNamesMale = {
                "Aarav", "Arjun", "Aditya", "Rohan", "Karthik", "Siddharth", "Varun", "Rahul",
                "Kavin", "Gowtham", "Vikram", "Pranav", "Harish", "Sai", "Manoj", "Dinesh",
                "Surya", "Naveen", "Ashwin", "Vishal", "Vijay", "Anand", "Deepak", "Ganesh",
                "Madhav", "Nikhil", "Praveen", "Rishi", "Sanjay", "Tarun", "Ajay", "Alok",
                "Amit", "Bala", "Bhuvnesh", "Charan", "Dev", "Dhruv", "Girish", "Hemant",
                "Jitendra", "Kishore", "Lokesh", "Manish", "Mayank", "Mohit", "Mukesh", "Nitesh",
                "Pradeep", "Raghav"
        };
        String[] firstNamesFemale = {
                "Ananya", "Priya", "Sneha", "Divya", "Pooja", "Meera", "Swetha", "Nithya",
                "Deepa", "Kavya", "Keerthana", "Shreya", "Rhea", "Sandhya", "Shruthi", "Lakshmi",
                "Harini", "Pavithra", "Aishwarya", "Janani", "Ishwarya", "Bhavana", "Lavanya", "Malini",
                "Preethi", "Roshni", "Swathi", "Vaishnavi", "Vidhya", "Yamini", "Anitha", "Aparna",
                "Archana", "Charulatha", "Geetha", "Hema", "Indira", "Kalyani", "Madhumitha", "Nandhini",
                "Padma", "Radhika", "Rekha", "Revathi", "Sangeetha", "Saranya", "Shobana", "Sowmya",
                "Sujatha", "Vandana"
        };
        String[] lastNames = {
                "Kumar", "Raj", "Iyer", "Nair", "Patel", "Sharma", "Verma", "Reddy",
                "Sundaram", "Krishnan", "Deshmukh", "Subramanian", "Nambiar", "Praneeth", "Balan",
                "Ranganathan", "Varadarajan", "Murugan", "Ramachandran", "Balaji", "Natarajan", "Gopal",
                "Srinivasan", "Sankaran", "Chakravarthy", "Menon", "Pillai", "Chettiar", "Muthusamy", "Venkatesan",
                "Bose", "Choudhury", "Das", "Dutta", "Ghosh", "Gupta", "Jadhav", "Joshi", "Kapoor", "Kulkarni",
                "Mahajan", "Mehta", "Mishra", "Pandey", "Rao", "Sen", "Seth", "Shah", "Singh", "Tiwari"
        };

        String[] cities = {"Chennai", "Coimbatore", "Madurai", "Salem", "Trichy", "Tirunelveli", "Erode", "Vellore"};

        // Distribution of departments, years, and sections
        // Total 100 students:
        // CSE: 20 students (Reg: 24CSE001 - 24CSE020)
        // IT: 20 students (Reg: 24IT001 - 24IT020)
        // AI&DS: 15 students (Reg: 24AIDS001 - 24AIDS015)
        // ECE: 20 students (Reg: 24ECE001 - 24ECE020)
        // EEE: 13 students (Reg: 24EEE001 - 24EEE013)
        // MECH: 12 students (Reg: 24MECH001 - 24MECH012)
        // Total: 20 + 20 + 15 + 20 + 13 + 12 = 100 students!

        String[][] deptAllocations = {
                {"CSE", "20"},
                {"IT", "20"},
                {"AI&DS", "15"},
                {"ECE", "20"},
                {"EEE", "13"},
                {"MECH", "12"}
        };

        int globalIndex = 0;

        // Realistic attendance target bands:
        // Indices:
        // 0-29 (30 students): Excellent (92% - 98%)
        // 30-64 (35 students): Good (81% - 88%)
        // 65-79 (15 students): Borderline safe (75% - 79%)
        // 80-91 (12 students): Warning below 75% (66% - 73%)
        // 92-99 (8 students): Critical (45% - 62%)
        double[] targetRates = new double[100];
        for (int i = 0; i < 30; i++) targetRates[i] = 0.92 + (i % 7) * 0.01;
        for (int i = 30; i < 65; i++) targetRates[i] = 0.81 + (i % 8) * 0.01;
        for (int i = 65; i < 80; i++) targetRates[i] = 0.75 + (i % 5) * 0.01;
        for (int i = 80; i < 92; i++) targetRates[i] = 0.66 + (i % 8) * 0.01;
        for (int i = 92; i < 100; i++) targetRates[i] = 0.45 + (i % 6) * 0.03;

        for (String[] alloc : deptAllocations) {
            String dept = alloc[0];
            int count = Integer.parseInt(alloc[1]);
            String prefix = dept.replace("&", "");

            for (int j = 1; j <= count; j++) {
                int idx = globalIndex;
                DemoStudentSeed s = new DemoStudentSeed();
                s.regNo = String.format("24%s%03d", prefix, j);

                boolean isMale = (idx % 2 == 0);
                String first = isMale ? firstNamesMale[(idx / 2) % firstNamesMale.length]
                                      : firstNamesFemale[(idx / 2) % firstNamesFemale.length];
                String last = lastNames[idx % lastNames.length];
                s.name = first + " " + last;
                s.gender = isMale ? "Male" : "Female";

                int year = 1 + ((j - 1) % 4);
                String sec = ((j - 1) / 2) % 2 == 0 ? "A" : "B";
                s.dept = dept;
                s.year = year;
                s.section = sec;

                s.dob = LocalDate.of(2003 + (4 - year), 1 + (idx % 12), 1 + (idx % 28));
                s.admissionDate = LocalDate.of(2023 + (year == 4 ? 0 : (4 - year)), 8, 1);

                String cleanName = first.toLowerCase() + "." + last.toLowerCase();
                s.email = cleanName + "@kit.edu.in";
                s.phone = String.format("98%02d%06d", 40 + (idx % 50), 100000 + idx * 73);
                String city = cities[idx % cities.length];
                s.address = (12 + idx) + ", Gandhi Street, " + city + ", Tamil Nadu - 6000" + (10 + (idx % 50));

                s.targetAttendanceRate = targetRates[idx];

                list.add(s);
                globalIndex++;
            }
        }

        return list;
    }
}
