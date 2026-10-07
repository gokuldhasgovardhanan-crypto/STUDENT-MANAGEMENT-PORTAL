-- ===================================================================
-- Student Attendance Management System - Database Migration Script
-- Version: 2.0.0 (Upgrade)
-- Preserves all existing data (100 students, attendance, teachers, settings)
-- ===================================================================

USE student_attendance_db;

-- 1. Upgrade Users Role to include STUDENT
ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN', 'TEACHER', 'STUDENT') NOT NULL DEFAULT 'TEACHER';

-- 2. Create Subjects Table
CREATE TABLE IF NOT EXISTS subjects (
    subject_id INT AUTO_INCREMENT PRIMARY KEY,
    subject_code VARCHAR(20) NOT NULL UNIQUE,
    subject_name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    year_of_study INT NOT NULL,
    semester VARCHAR(20) NOT NULL,
    credits INT DEFAULT 3,
    teacher_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subject_code (subject_code),
    INDEX idx_subject_dept_year (department, year_of_study),
    CONSTRAINT fk_subjects_teacher
        FOREIGN KEY (teacher_id)
        REFERENCES teachers(teacher_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 3. Create Student-Subjects Relationship Table
CREATE TABLE IF NOT EXISTS student_subjects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    subject_id INT NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_student_subject (student_id, subject_id),
    INDEX idx_ss_student (student_id),
    INDEX idx_ss_subject (subject_id),
    CONSTRAINT fk_ss_student
        FOREIGN KEY (student_id)
        REFERENCES students(student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ss_subject
        FOREIGN KEY (subject_id)
        REFERENCES subjects(subject_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 4. Create Periods Table
CREATE TABLE IF NOT EXISTS periods (
    period_id INT AUTO_INCREMENT PRIMARY KEY,
    period_number INT NOT NULL UNIQUE,
    period_name VARCHAR(50) NOT NULL,
    start_time VARCHAR(20) NOT NULL,
    end_time VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;

-- Insert Default Periods if empty
INSERT IGNORE INTO periods (period_number, period_name, start_time, end_time) VALUES
(1, 'Period 1', '09:00', '10:00'),
(2, 'Period 2', '10:00', '11:00'),
(3, 'Period 3', '11:15', '12:15'),
(4, 'Period 4', '12:15', '13:15'),
(5, 'Period 5', '14:00', '15:00'),
(6, 'Period 6', '15:00', '16:00');

-- 5. Create Timetable Table
CREATE TABLE IF NOT EXISTS timetable (
    timetable_id INT AUTO_INCREMENT PRIMARY KEY,
    day_of_week VARCHAR(20) NOT NULL,
    period_id INT NOT NULL,
    subject_id INT NOT NULL,
    teacher_id INT NULL,
    department VARCHAR(50) NOT NULL,
    year_of_study INT NOT NULL,
    section VARCHAR(10) NOT NULL,
    room VARCHAR(50) DEFAULT 'Room 101',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_timetable_slot (day_of_week, period_id, department, year_of_study, section),
    INDEX idx_tt_dept_year_sec (department, year_of_study, section),
    INDEX idx_tt_teacher (teacher_id),
    CONSTRAINT fk_tt_period
        FOREIGN KEY (period_id)
        REFERENCES periods(period_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_tt_subject
        FOREIGN KEY (subject_id)
        REFERENCES subjects(subject_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_tt_teacher
        FOREIGN KEY (teacher_id)
        REFERENCES teachers(teacher_id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- 6. Upgrade Attendance Table for Subject-Wise and Period Support
-- Add subject_id and period columns safely if not present
SET @dbname = DATABASE();
SET @tablename = 'attendance';
SET @columnname = 'subject_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  'ALTER TABLE attendance ADD COLUMN subject_id INT NULL AFTER student_id;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'period';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  'ALTER TABLE attendance ADD COLUMN period VARCHAR(20) DEFAULT "Period 1" AFTER attendance_date;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Safely add Foreign Key to subject_id
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE
      CONSTRAINT_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND CONSTRAINT_NAME = 'fk_attendance_subject'
  ) > 0,
  'SELECT 1',
  'ALTER TABLE attendance ADD CONSTRAINT fk_attendance_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE SET NULL;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 7. Create QR Attendance Session Table
CREATE TABLE IF NOT EXISTS attendance_session (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    subject_id INT NOT NULL,
    teacher_id INT NOT NULL,
    attendance_date DATE NOT NULL,
    period VARCHAR(20) NOT NULL,
    department VARCHAR(50) NOT NULL,
    year_of_study INT NOT NULL,
    section VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    status ENUM('ACTIVE', 'EXPIRED', 'CLOSED') DEFAULT 'ACTIVE',
    INDEX idx_as_token (token),
    INDEX idx_as_status (status),
    CONSTRAINT fk_as_subject
        FOREIGN KEY (subject_id)
        REFERENCES subjects(subject_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_as_teacher
        FOREIGN KEY (teacher_id)
        REFERENCES teachers(teacher_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Create Leave Requests Table
CREATE TABLE IF NOT EXISTS leave_requests (
    leave_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    leave_type ENUM('Medical', 'Personal', 'College Event', 'Emergency', 'Other') NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    reason TEXT NOT NULL,
    supporting_doc VARCHAR(255) NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    approved_by VARCHAR(100) NULL,
    approved_at TIMESTAMP NULL,
    remarks TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_leave_student (student_id),
    INDEX idx_leave_status (status),
    CONSTRAINT fk_leave_student
        FOREIGN KEY (student_id)
        REFERENCES students(student_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. Create Attendance Audit Table
CREATE TABLE IF NOT EXISTS attendance_audit (
    audit_id INT AUTO_INCREMENT PRIMARY KEY,
    attendance_id INT NULL,
    student_id INT NOT NULL,
    old_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason TEXT NOT NULL,
    INDEX idx_audit_att (attendance_id),
    INDEX idx_audit_student (student_id),
    CONSTRAINT fk_audit_student
        FOREIGN KEY (student_id)
        REFERENCES students(student_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 10. Create General Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50) DEFAULT '127.0.0.1',
    INDEX idx_logs_user (user_id),
    INDEX idx_logs_action (action),
    INDEX idx_logs_time (timestamp)
) ENGINE=InnoDB;

-- 11. Create Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    target_role VARCHAR(20) DEFAULT 'ALL',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notif_role (target_role),
    INDEX idx_notif_read (is_read)
) ENGINE=InnoDB;

-- 12. Insert New System Settings
INSERT INTO settings (setting_key, setting_value, description) VALUES
('count_approved_leave_as_present', 'NO', 'Count approved student leave as attendance present (YES/NO)'),
('qr_expiration_minutes', '5', 'QR code attendance session expiry time in minutes'),
('smtp_host', 'smtp.gmail.com', 'Outgoing SMTP mail server host'),
('smtp_port', '587', 'Outgoing SMTP mail server port'),
('smtp_username', '', 'SMTP mail user authentication account'),
('smtp_password', '', 'SMTP mail user authentication password'),
('email_notifications_enabled', 'NO', 'Enable automatic email notifications for low attendance and leaves'),
('app_theme', 'LIGHT', 'UI Theme Mode (LIGHT / DARK)')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- 13. Create Institutional Holidays Table
CREATE TABLE IF NOT EXISTS holidays (
    holiday_id INT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    holiday_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_holiday_date (holiday_date)
) ENGINE=InnoDB;

-- 14. Seed Institutional Holidays
INSERT IGNORE INTO holidays (holiday_date, holiday_name, description) VALUES
('2026-08-15', 'Independence Day', 'National Holiday'),
('2026-08-27', 'Janmashtami', 'Festival Holiday'),
('2026-09-07', 'Vinayaka Chaturthi', 'Festival Holiday'),
('2026-09-16', 'Milad-un-Nabi', 'Declared Holiday'),
('2026-10-02', 'Gandhi Jayanti', 'National Holiday'),
('2026-10-20', 'Ayutha Pooja', 'Festival Holiday'),
('2026-10-21', 'Vijaya Dashami', 'Festival Holiday'),
('2026-11-08', 'Deepavali / Diwali', 'Festival Holiday'),
('2026-12-25', 'Christmas Day', 'Festival Holiday'),
('2027-01-01', 'New Year\'s Day', 'Annual Holiday'),
('2027-01-14', 'Pongal / Makar Sankranti', 'Harvest Festival'),
('2027-01-26', 'Republic Day', 'National Holiday');
