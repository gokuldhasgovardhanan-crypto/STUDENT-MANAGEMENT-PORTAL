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
                     "WHERE table_schema = ? AND table_name IN ('users', 'students', 'teachers', 'attendance', 'settings')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DatabaseConnection.getDbName());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) >= 5;
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
                stmt.executeUpdate("DROP TABLE IF EXISTS attendance");
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
                    "role ENUM('ADMIN', 'TEACHER') NOT NULL DEFAULT 'TEACHER', " +
                    "email VARCHAR(100) NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "INDEX idx_users_username (username)" +
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

            // Create attendance
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS attendance (" +
                    "attendance_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_id INT NOT NULL, " +
                    "attendance_date DATE NOT NULL, " +
                    "status ENUM('PRESENT', 'ABSENT') NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY uq_student_date (student_id, attendance_date), " +
                    "INDEX idx_attendance_date (attendance_date), " +
                    "INDEX idx_attendance_student (student_id), " +
                    "CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE ON UPDATE CASCADE" +
                    ") ENGINE=InnoDB"
            );

            LOGGER.info("Tables created or verified successfully.");
        }

        // Seed data if empty
        seedDataIfNecessary();
    }

    private static void seedDataIfNecessary() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Seed Settings
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM settings")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertSettings(conn);
                }
            }

            // 2. Seed Users (admin & teacher)
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
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

            // 5. Seed 100 Students and attendance
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM students")) {
                if (rs.next() && rs.getInt(1) < 100) {
                    insert100DemoStudentsAndAttendance(conn);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while seeding database data", e);
        }
    }

    private static void insertSettings(Connection conn) throws SQLException {
        String sql = "INSERT INTO settings (setting_key, setting_value, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Object[][] defaultSettings = {
                    {"college_name", "ABC Engineering College", "Configured Institution Name"},
                    {"academic_year", "2026-27", "Current Academic Year"},
                    {"semester", "V", "Current Active Semester"},
                    {"required_attendance_pct", "75.0", "Mandatory attendance requirement"},
                    {"working_days_target", "60", "Semester working days target"},
                    {"departments_list", "CSE,IT,AI&DS,ECE,EEE,MECH", "Offered Degree Departments"},
                    {"years_list", "1,2,3,4", "Academic Years"},
                    {"sections_list", "A,B", "Class Sections"}
            };
            for (Object[] row : defaultSettings) {
                ps.setString(1, (String) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.addBatch();
            }
            ps.executeBatch();
            LOGGER.info("Default settings inserted.");
        }
    }

    private static void insertUsers(Connection conn) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, email) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Admin: admin / admin123
            String adminHash = PasswordUtil.hashPassword("admin123");
            ps.setString(1, "admin");
            ps.setString(2, adminHash);
            ps.setString(3, "System Administrator");
            ps.setString(4, "ADMIN");
            ps.setString(5, "admin@abc.edu.in");
            ps.addBatch();

            // Teacher: teacher / teacher123
            String teacherHash = PasswordUtil.hashPassword("teacher123");
            ps.setString(1, "teacher");
            ps.setString(2, teacherHash);
            ps.setString(3, "Prof. Rajesh Sharma");
            ps.setString(4, "TEACHER");
            ps.setString(5, "teacher@abc.edu.in");
            ps.addBatch();

            ps.executeBatch();
            LOGGER.info("Default users (admin, teacher) created.");
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
                    {"EMP101", "Dr. R. Ramanathan", "CSE", "ramanathan.r@abc.edu.in", "9840123451"},
                    {"EMP102", "Dr. S. Gayathri", "IT", "gayathri.s@abc.edu.in", "9840123452"},
                    {"EMP103", "Prof. K. Venkatesh", "AI&DS", "venkatesh.k@abc.edu.in", "9840123453"},
                    {"EMP104", "Dr. P. Meenakshi", "ECE", "meenakshi.p@abc.edu.in", "9840123454"},
                    {"EMP105", "Prof. M. Suresh", "EEE", "suresh.m@abc.edu.in", "9840123455"},
                    {"EMP106", "Dr. T. Revathi", "MECH", "revathi.t@abc.edu.in", "9840123456"}
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

        String attSql = "INSERT INTO attendance (student_id, attendance_date, status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(attSql)) {
            for (DemoStudentSeed s : seedList) {
                if (s.studentId == 0) continue;

                // Determine target present probability based on student's assigned group
                double targetRate = s.targetAttendanceRate;
                Random rnd = new Random(s.regNo.hashCode());

                for (LocalDate day : workingDays) {
                    boolean present = rnd.nextDouble() < targetRate;
                    ps.setInt(1, s.studentId);
                    ps.setDate(2, Date.valueOf(day));
                    ps.setString(3, present ? "PRESENT" : "ABSENT");
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
                s.email = cleanName + "@abc.edu.in";
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
