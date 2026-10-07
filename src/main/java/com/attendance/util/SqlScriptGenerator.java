package com.attendance.util;

import com.attendance.service.DatabaseInitService;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Generator to output database/demo_data.sql and database/setup.sql.
 */
public class SqlScriptGenerator {

    public static void main(String[] args) throws Exception {
        String baseDir = args.length > 0 ? args[0] : ".";
        File dbDir = new File(baseDir, "database");
        dbDir.mkdirs();

        File demoFile = new File(dbDir, "demo_data.sql");
        File setupFile = new File(dbDir, "setup.sql");
        File schemaFile = new File(dbDir, "schema.sql");

        System.out.println("Generating " + demoFile.getAbsolutePath() + "...");

        try (PrintWriter pw = new PrintWriter(new FileWriter(demoFile))) {
            pw.println("-- ===================================================================");
            pw.println("-- Student Attendance Management System - Demo Data");
            pw.println("-- 100 Demo Students, Faculty, Settings, Users, and 40 Days Attendance");
            pw.println("-- ===================================================================");
            pw.println();
            pw.println("USE student_attendance_db;");
            pw.println();

            // 1. Settings
            pw.println("-- 1. Default Settings");
            pw.println("INSERT INTO settings (setting_key, setting_value, description) VALUES");
            pw.println("('college_name', 'KIT ENGINEERING COLLEGE', 'Configured Institution Name'),");
            pw.println("('academic_year', '2026-27', 'Current Academic Year'),");
            pw.println("('semester', 'V', 'Current Active Semester'),");
            pw.println("('required_attendance_pct', '75.0', 'Mandatory attendance requirement'),");
            pw.println("('working_days_target', '60', 'Semester working days target'),");
            pw.println("('departments_list', 'CSE,IT,AI&DS,ECE,EEE,MECH', 'Offered Degree Departments'),");
            pw.println("('years_list', '1,2,3,4', 'Academic Years'),");
            pw.println("('sections_list', 'A,B', 'Class Sections')");
            pw.println("ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);");
            pw.println();

            // 2. Users (BCrypt hashes for admin123 and teacher123)
            String adminHash = PasswordUtil.hashPassword("admin123");
            String teacherHash = PasswordUtil.hashPassword("teacher123");
            pw.println("-- 2. Default Users");
            pw.println("INSERT INTO users (username, password_hash, full_name, role, email) VALUES");
            pw.printf("('admin', '%s', 'System Administrator', 'ADMIN', 'admin@kit.edu.in'),%n", adminHash);
            pw.printf("('teacher', '%s', 'Prof. Rajesh Sharma', 'TEACHER', 'teacher@kit.edu.in')%n", teacherHash);
            pw.println("ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);");
            pw.println();

            // 3. Departments
            pw.println("-- 3. Departments");
            pw.println("INSERT INTO departments (dept_code, dept_name) VALUES");
            pw.println("('CSE', 'Computer Science and Engineering'),");
            pw.println("('IT', 'Information Technology'),");
            pw.println("('AI&DS', 'Artificial Intelligence and Data Science'),");
            pw.println("('ECE', 'Electronics and Communication Engineering'),");
            pw.println("('EEE', 'Electrical and Electronics Engineering'),");
            pw.println("('MECH', 'Mechanical Engineering')");
            pw.println("ON DUPLICATE KEY UPDATE dept_name = VALUES(dept_name);");
            pw.println();

            // 4. Teachers
            pw.println("-- 4. Teachers / Faculty");
            pw.println("INSERT INTO teachers (employee_id, teacher_name, department, email, phone) VALUES");
            pw.println("('EMP101', 'Dr. R. Ramanathan', 'CSE', 'ramanathan.r@kit.edu.in', '9840123451'),");
            pw.println("('EMP102', 'Dr. S. Gayathri', 'IT', 'gayathri.s@kit.edu.in', '9840123452'),");
            pw.println("('EMP103', 'Prof. K. Venkatesh', 'AI&DS', 'venkatesh.k@kit.edu.in', '9840123453'),");
            pw.println("('EMP104', 'Dr. P. Meenakshi', 'ECE', 'meenakshi.p@kit.edu.in', '9840123454'),");
            pw.println("('EMP105', 'Prof. M. Suresh', 'EEE', 'suresh.m@kit.edu.in', '9840123455'),");
            pw.println("('EMP106', 'Dr. T. Revathi', 'MECH', 'revathi.t@kit.edu.in', '9840123456')");
            pw.println("ON DUPLICATE KEY UPDATE teacher_name = VALUES(teacher_name);");
            pw.println();

            // 5. 100 Demo Students
            pw.println("-- 5. 100 Realistic Demo Students");
            pw.println("INSERT INTO students (student_id, register_no, student_name, gender, date_of_birth, department, year_of_study, section, email, phone_number, address, admission_date) VALUES");
            List<DatabaseInitService.DemoStudentSeed> students = DatabaseInitService.generate100StudentDefinitions();
            for (int i = 0; i < students.size(); i++) {
                DatabaseInitService.DemoStudentSeed s = students.get(i);
                s.studentId = i + 1; // Explicit ID for reliable demo data SQL
                String delim = (i == students.size() - 1) ? ";" : ",";
                pw.printf("(%d, '%s', '%s', '%s', '%s', '%s', %d, '%s', '%s', '%s', '%s', '%s')%s%n",
                        s.studentId, s.regNo, s.name.replace("'", "''"), s.gender, s.dob,
                        s.dept, s.year, s.section, s.email, s.phone, s.address.replace("'", "''"), s.admissionDate, delim);
            }
            pw.println();

            // 6. 40 Days of Attendance
            pw.println("-- 6. Historical Attendance Records (40 Working Days)");
            List<LocalDate> workingDays = DatabaseInitService.generate40WorkingDays();

            pw.println("INSERT INTO attendance (student_id, attendance_date, status) VALUES");
            int totalInserted = 0;
            int totalExpected = students.size() * workingDays.size();

            for (DatabaseInitService.DemoStudentSeed s : students) {
                Random rnd = new Random(s.regNo.hashCode());
                for (LocalDate day : workingDays) {
                    boolean present = rnd.nextDouble() < s.targetAttendanceRate;
                    totalInserted++;
                    String delim = (totalInserted == totalExpected) ? ";" : ",";
                    pw.printf("(%d, '%s', '%s')%s%n", s.studentId, day, present ? "PRESENT" : "ABSENT", delim);
                }
            }
        }

        // Now create setup.sql = schema.sql + demo_data.sql
        try (PrintWriter pw = new PrintWriter(new FileWriter(setupFile))) {
            if (schemaFile.exists()) {
                String schemaSql = Files.readString(Paths.get(schemaFile.getAbsolutePath()));
                pw.println(schemaSql);
                pw.println();
            }
            String demoSql = Files.readString(Paths.get(demoFile.getAbsolutePath()));
            pw.println(demoSql);
        }

        System.out.println("SQL scripts generated successfully!");
    }
}
