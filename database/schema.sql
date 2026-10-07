-- ===================================================================
-- Student Attendance Management System - Database Schema
-- Database: student_attendance_db
-- ===================================================================

CREATE DATABASE IF NOT EXISTS student_attendance_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE student_attendance_db;

-- 1. System Settings Table
CREATE TABLE IF NOT EXISTS settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Users Table for Authentication & Roles
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'TEACHER') NOT NULL DEFAULT 'TEACHER',
    email VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username)
) ENGINE=InnoDB;

-- 3. Departments Table
CREATE TABLE IF NOT EXISTS departments (
    dept_id INT AUTO_INCREMENT PRIMARY KEY,
    dept_code VARCHAR(20) NOT NULL UNIQUE,
    dept_name VARCHAR(100) NOT NULL,
    INDEX idx_dept_code (dept_code)
) ENGINE=InnoDB;

-- 4. Teachers Table
CREATE TABLE IF NOT EXISTS teachers (
    teacher_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    teacher_name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_teacher_empid (employee_id),
    INDEX idx_teacher_dept (department)
) ENGINE=InnoDB;

-- 5. Students Table
CREATE TABLE IF NOT EXISTS students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    register_no VARCHAR(50) NOT NULL UNIQUE,
    student_name VARCHAR(100) NOT NULL,
    gender ENUM('Male', 'Female', 'Other') NOT NULL,
    date_of_birth DATE NOT NULL,
    department VARCHAR(50) NOT NULL,
    year_of_study INT NOT NULL,
    section VARCHAR(10) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address TEXT NULL,
    admission_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_students_regno (register_no),
    INDEX idx_students_name (student_name),
    INDEX idx_students_dept (department),
    INDEX idx_students_year_sec (year_of_study, section)
) ENGINE=InnoDB;

-- 6. Attendance Table
CREATE TABLE IF NOT EXISTS attendance (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    attendance_date DATE NOT NULL,
    status ENUM('PRESENT', 'ABSENT') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_student_date (student_id, attendance_date),
    INDEX idx_attendance_date (attendance_date),
    INDEX idx_attendance_student (student_id),
    CONSTRAINT fk_attendance_student
        FOREIGN KEY (student_id)
        REFERENCES students(student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;
