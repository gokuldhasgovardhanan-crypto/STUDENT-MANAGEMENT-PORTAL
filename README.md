# SMART STUDENT ATTENDANCE MANAGEMENT SYSTEM USING JAVA, JDBC AND MYSQL
### A Database-Driven Attendance Monitoring and Reporting Application
**Institution:** KIT ENGINEERING COLLEGE  
**Academic Year:** 2026-2027  
**Degree / Program:** Bachelor of Engineering (B.E. / B.Tech) — Computer Science & Engineering  
**Curriculum Focus:** Academic Credit / Capstone Viva-Voce Evaluation  

---

## 🏛️ Executive Summary

The **Smart Student Attendance Management System** is a production-grade, viva-friendly academic software application developed for **KIT ENGINEERING COLLEGE**. Built using **Java 17 LTS**, **Java Swing (FlatLaf Modern UI)**, **JDBC**, **MySQL 8.x**, **Apache POI**, and **OpenPDF**, the system enforces strict relational database normalization (3NF), prepared-statement SQL security, transaction integrity, explainable attendance risk analytics, and formal academic reporting.

In addition to the primary standalone Java Desktop GUI application, the system features a synchronized **Full-Stack Web Portal** (HTML5, Vanilla CSS, Vite, SheetJS, jsPDF) and a comprehensive 670+ line [ACADEMIC_DOCUMENTATION.md](file:///C:/Users/GOVARDHANAN/.gemini/antigravity-ide/scratch/student-attendance-system/ACADEMIC_DOCUMENTATION.md) written specifically for academic project evaluations, viva-voce defense, and external examiners.

---

## 🎯 Key Academic & Architectural Highlights

1. **Clean Multi-Tier MVC Architecture:** Strict separation between Presentation (`ui`), Business Logic (`service`), Data Access Objects (`dao`), Relational Entities (`model`), and Configuration (`config`).
2. **Normalized Relational Schema (16 Tables in 3NF):** Full relational integrity with foreign keys, composite primary keys, triggers, audit logging, and cascading constraints.
3. **Robust JDBC Implementation:** 100% Prepared Statements preventing SQL injection; explicit ACID transaction management (`setAutoCommit(false)`, `commit()`, `rollback()`).
4. **Mathematical Attendance Recovery & Buffer Algorithms:**
   - **Shortage Recovery:** $x = \max\left(0, \left\lceil \frac{R \cdot T - A}{1 - R} \right\rceil\right)$ (classes needed to reach cutoff $R$).
   - **Missable Margin:** $m = \max\left(0, \left\lfloor \frac{A - R \cdot T}{R} \right\rfloor\right)$ (classes allowed to miss while remaining $\ge R$).
5. **Rule-Based Explainable Risk Predictor:** Transparent risk classification (*SAFE*, *AT RISK*, *CRITICAL*) avoiding opaque blackbox AI.
6. **Official Attendance Certificate Generator:** Print-ready official clearance certificates with tamper-evident digital verification tokens.
7. **Collegiate Red & White UI:** Premium KIT Engineering College branding (Collegiate Crimson `#8B0000`, Deep Burgundy `#5C0000`, Warm Gold `#D4AF37`, and Crisp Alabaster `#F8FAFC`).
8. **Academic Holiday Management:** Declare institutional holidays and dynamically exclude them from instructional working day tallies.
9. **Dual Platform Access:**
   - Standalone Desktop Fat JAR (`target/StudentAttendanceSystem.jar`)
   - Companion Responsive Web Portal (`web/dist/` or `http://localhost:3000`)

---

## 🗄️ Relational Database Schema (16 Tables in 3NF)

| # | Table Name | Purpose / Relations |
|---|---|---|
| 1 | `settings` | System-wide academic configurations (college name, cutoff %, working days). |
| 2 | `users` | Role-based user credentials (`admin`, `teacher`, `student`) with BCrypt hashes. |
| 3 | `departments` | Engineering branches (CSE, IT, AI&DS, ECE, EEE, MECH). |
| 4 | `teachers` | Faculty directory with employee codes, departments, and designations. |
| 5 | `students` | Master student directory with register numbers, semesters, and contact details. |
| 6 | `subjects` | Course registry with subject codes, credits, and semester mappings. |
| 7 | `student_subjects` | Course enrollment mappings linking students to registered subjects. |
| 8 | `periods` | Daily class time-slots (Period 1 to Period 7 with start/end times). |
| 9 | `timetable` | Scheduled class allocation linking department, year, section, day, period, subject, and faculty. |
| 10 | `attendance_session` | Master session record for period-wise or daily class attendance. |
| 11 | `attendance` | Daily/period-wise attendance entries (`PRESENT`, `ABSENT`, `OD`, `LATE`, `LEAVE`). |
| 12 | `leave_requests` | Formal leave application workflow (Applied, Approved, Rejected). |
| 13 | `attendance_audit` | Historical record of manual attendance modifications and corrections. |
| 14 | `audit_logs` | System-wide operational logs (logins, exports, data updates). |
| 15 | `notifications` | Role-targeted administrative announcements and alerts. |
| 16 | `holidays` | Declared public and academic holidays excluded from working day calculations. |

---

## 👥 Demo Preloaded Data (100 Students & 40 Days History)

The database includes realistic demo data tailored for an instant project presentation:
- **100 Realistic Students** across 6 departments with Indian names, valid register numbers, and emails.
- **10+ Faculty Members** across disciplines.
- **15+ Engineering Subjects** with course codes, credits, and syllabus hours.
- **40 Instructional Working Days** (August 3, 2026 to September 25, 2026, Monday–Friday only):
  - ~30 students: **90% – 100%** (High Performers)
  - ~35 students: **80% – 89%** (Good Standing)
  - ~15 students: **75% – 79%** (Borderline Compliant)
  - ~12 students: **65% – 74%** (At Risk / Intervention Required)
  - ~8 students: **< 65%** (Critical Condonation / Detained)

---

## 🔑 Default Login Credentials

| Role | Username | Password | Access Capabilities |
|---|---|---|---|
| **Administrator** | `admin` | `admin123` | Full control: Analytics, Students, Faculty, Attendance, Holidays, Settings, Reports. |
| **Faculty / Teacher** | `teacher` | `teacher123` | Daily attendance marking, Student search, Risk reports, Certificate generation. |
| **Student** | `student` | `student123` | Personal attendance percentage, shortage recovery calculator, leave status. |

*(Quick-fill demo buttons are provided on the login window for 1-click evaluation).*

---

## 🚀 How to Build and Run

### Prerequisites
- **Java Development Kit (JDK):** Version 17 LTS or higher (`java -version`)
- **Apache Maven:** Version 3.8+ (`mvn -version`)
- **MySQL Server:** Version 8.0+ running on port `3306`

### Option 1: Quick Run on Windows (One-Click)
Double-click `run.bat` or open PowerShell / Command Prompt and run:
```cmd
run.bat
```

### Option 2: Build & Run Standalone Fat JAR
```bash
# Package into executable fat JAR with all dependencies
mvn clean package -DskipTests

# Execute the standalone JAR
java -jar target/StudentAttendanceSystem.jar
```

### Option 3: Run via Maven Exec Plugin
```bash
mvn compile exec:java
```

### Option 4: Companion Web Portal
```bash
cd web
npm install
npm run dev
# Open http://localhost:3000 in your browser
```

---

## 📊 Analytics & Academic Innovation Modules

### 1. Interactive Analytics & Risk Prediction Panel
Navigate to **📈 Analytics & Risk** in the sidebar:
- **Department Attendance Comparison Bar Chart:** Dynamic Swing 2D vector bars showing average attendance per department with 75% cutoff line.
- **Tier Distribution:** Real-time headcount across 5 performance bands (90–100%, 80–89%, 75–79%, 65–74%, <65%).
- **Subject-wise Performance:** Tabular breakdown of subject attendance averages.
- **Explainable Risk Predictor:** Table classifying students into *SAFE* (green), *AT RISK* (amber), or *CRITICAL* (red) based on deterministic thresholds.
- **Live Shortage & Buffer Calculator:** Select any student to instantly compute:
  - Required consecutive classes ($x$) to achieve 75% / 85%.
  - Permissible future absences ($m$) without dropping below cutoff.

### 2. Official Attendance Certificate Generator
Available in **Reports Panel** (`📜 Generate Certificate`) and **Student Profile Dialog**:
- Produces a formal, print-ready PDF certificate on KIT Engineering College letterhead.
- Lists student particulars, semester, working days, attendance percentage, and eligibility standing.
- Embeds verification hash and authorization signatures for university exam entry.

### 3. Holiday Management
Available under **Settings Panel** (`📅 Manage Holidays`):
- Declare and view public, institutional, and examination holidays.
- Integrated into `DateUtil` and `AttendanceService` to automatically omit holidays from working days.

---

## 📑 Viva-Voce Technical Preparation Guide

A dedicated, comprehensive academic report is available in [ACADEMIC_DOCUMENTATION.md](file:///C:/Users/GOVARDHANAN/.gemini/antigravity-ide/scratch/student-attendance-system/ACADEMIC_DOCUMENTATION.md). Key topics covered for external examiners:

1. **Why JDBC over ORM (Hibernate)?**  
   *Direct SQL control, zero caching opacity, minimal memory footprint, and explicit transaction safety needed for academic clarity.*
2. **How is SQL Injection prevented?**  
   *Pre-compiled `PreparedStatement` with parameterized placeholders (`?`), treating input strictly as literals rather than executable SQL.*
3. **How is Duplicate Attendance prevented?**  
   *Enforced at both the DB tier via `UNIQUE KEY (student_id, attendance_date)` and the UI tier via duplicate-checking confirmation dialogs.*
4. **How does the Shortage Recovery Formula work?**  
   *Derived from $\frac{A + x}{T + x} \ge R \implies x \ge \frac{R \cdot T - A}{1 - R}$. Since already missed classes cannot be undone, each recovery class adds $+1$ to both numerator and denominator.*
5. **How is Password Security managed?**  
   *Salted BCrypt hashing with standard work factor 10, preventing rainbow-table attacks.*

---

## 🧪 Automated Testing

Run the test suite via Maven:
```bash
mvn test
```
**Test Coverage Includes:**
- BCrypt cryptographic hash verification.
- Relational input validators (email, phone, register numbers).
- Working day calculation excluding weekends and declared holidays.
- Attendance percentage and tier classification algorithms.
- Shortage recovery formula edge cases (100% attendance, 0% attendance, already compliant).
- Real Apache POI `.xlsx` generation and validation.
- Real OpenPDF document creation with valid `%PDF-` header.

---

## 📂 Project Structure

```text
student-attendance-system/
│
├── pom.xml                               # Maven project configuration & dependencies
├── README.md                             # Comprehensive project manual & setup
├── ACADEMIC_DOCUMENTATION.md             # Complete college submission documentation & viva guide
├── run.bat                               # Windows one-click executable launcher
│
├── database/
│   ├── schema.sql                        # 16 Relational tables (DDL, constraints, triggers)
│   ├── demo_data.sql                     # Seed data: 100 students, 10 faculty, 40 days logs
│   ├── migration.sql                     # Schema migration script for upgrades
│   └── setup.sql                         # Complete unified MySQL initialization script
│
├── web/                                  # Full-Stack Web Portal Companion
│   ├── index.html                        # Modern responsive web interface
│   ├── package.json                      # Web portal dependencies (Vite, SheetJS, jsPDF)
│   ├── src/                              # Vanilla JS modular components
│   └── dist/                             # Pre-built production bundle
│
└── src/
    ├── main/
    │   ├── java/com/attendance/
    │   │   ├── Main.java                 # Desktop application entry point & FlatLaf initialization
    │   │   ├── config/
    │   │   │   └── DatabaseConnection.java# JDBC connection management & pooling
    │   │   ├── model/                    # Relational data entities (Student, Attendance, Holiday, etc.)
    │   │   ├── dao/                      # Data Access Objects with parameterized SQL
    │   │   │   ├── AttendanceDAO.java    # Marking, calculations, distributions, trends
    │   │   │   ├── StudentDAO.java       # Student queries, filters, CRUD
    │   │   │   ├── HolidayDAO.java       # Holiday persistence & working day exclusion
    │   │   │   └── ...
    │   │   ├── service/                  # Business logic & algorithms
    │   │   │   ├── AttendanceService.java# Batch marking & percentage logic
    │   │   │   ├── AttendanceCalculatorService.java # Shortage & buffer math formulas
    │   │   │   ├── ExcelExportService.java   # Apache POI multi-sheet export
    │   │   │   ├── PdfExportService.java     # OpenPDF reports & Attendance Certificates
    │   │   │   └── ...
    │   │   └── ui/                       # Modern Swing GUI (FlatLaf, KIT Red/Gold theme)
    │   │       ├── DashboardFrame.java   # Executive dashboard with role-based sidebar
    │   │       ├── AnalyticsPanel.java   # Charts, risk table, shortage calculator
    │   │       ├── AttendancePanel.java  # Daily attendance marking interface
    │   │       ├── ReportsPanel.java     # 6 Report types + Certificate generator
    │   │       ├── HolidayDialog.java    # Academic holiday management
    │   │       └── ...
    │   └── resources/
    │       └── application.properties    # Database configuration & defaults
    └── test/
        └── java/com/attendance/
            └── SystemUnitTest.java       # Unit & integration test suite
```

---

## 🏆 Academic Evaluation Readiness

- **Working Functionality:** 100% verified with live database interactions.
- **Architecture:** Standard Java MVC design pattern.
- **Viva Voce:** Complete Q&A, mathematical formulas, and DFDs documented in [ACADEMIC_DOCUMENTATION.md](file:///C:/Users/GOVARDHANAN/.gemini/antigravity-ide/scratch/student-attendance-system/ACADEMIC_DOCUMENTATION.md).
- **Code Repository:** Pushed to GitHub: `https://github.com/gokuldhasgovardhanan-crypto/STUDENT-MANAGEMENT-PORTAL.git`.
