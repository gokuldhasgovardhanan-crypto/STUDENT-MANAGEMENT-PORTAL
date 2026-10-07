# Student Attendance Management System

A production-grade, full-featured desktop application built with **Java 17**, **Java Swing (FlatLaf Modern UI)**, **JDBC**, **MySQL 8.x**, **Apache POI**, and **OpenPDF**.

Designed with clean MVC architecture (Model, DAO, Service, and UI layers), this application provides an end-to-end solution for academic institutions to manage students, faculty, daily attendance marking, dynamic analytics, executive reports, and professional Excel/PDF exports.

---

## 1. Key Features

### 🔐 Secure Login & Role-Based Access Control
- Stored in MySQL `users` table with **BCrypt password hashing** (`$2a$10$...`).
- **Admin Role**: Full access to Dashboard, Students, Attendance, Reports, Teachers, and Settings.
- **Teacher Role**: Access to Dashboard, Students, Attendance, and Reports.
- Quick credentials autofill on the login screen for testing.
- Change password dialog accessible right from the header.
- Real-time MySQL connectivity telemetry indicator on login.

### 📊 Modern Executive Dashboard
- **Sidebar Navigation**: Dashboard, Students, Attendance, Reports, Teachers, Settings, and Logout.
- **Dynamic Live KPI Metric Cards** (sourced 100% from MySQL):
  - *Total Students* (enrolled count)
  - *Present Today*
  - *Absent Today*
  - *Attendance Not Marked* (pending marking today)
  - *Average Attendance* (college-wide dynamic percentage)
  - *Students Below 75%* (requiring immediate academic intervention)
- **Department Attendance Comparison Chart**:
  - Custom Java Swing 2D vector chart rendering attendance performance per department with target threshold indicator.
- **Low Attendance Quick Alert Table**:
  - Live preview table showing students below required percentage with one-click export to Excel and PDF.

### 👥 Student Management
- **Add Student**:
  - Fields: Auto-generated Student ID, Register Number, Full Name, Gender, Date of Birth, Department, Year of Study (1–4), Section (A/B), Email, Phone, Address, Admission Date.
  - Strict input validations (Register number uniqueness, email format, phone digits, name pattern, required fields).
- **View Students**:
  - Interactive `JTable` with 32px row height, sorting, and responsive layout.
  - Live search by Name and Register Number.
  - Dropdown filters by Department, Year of Study, and Section.
- **Edit Student**: Update student particulars with immediate MySQL reflection.
- **Delete Student**: Confirmation modal (`YES / NO`) with foreign-key cascade safety.
- **Student Profile**:
  - Comprehensive modal showing student academic details, working days, present/absent count, attendance %, standing badge, and full historical log.
  - Export individual student profile to Excel (`.xlsx`) and PDF.

### 👨‍🏫 Teacher Management
- Fields: Employee ID, Teacher Name, Department, Email, Phone.
- Features: Add Teacher, Search Faculty, Edit Profile, Delete Faculty with database validation.

### 📝 Attendance Module
- Top filter bar: **Date**, **Department**, **Year**, and **Section**.
- Automatically queries and loads enrolled students for the class.
- Status toggle: `PRESENT` / `ABSENT` via dropdown badge editor.
- Ergonomic batch buttons:
  - `MARK ALL PRESENT`
  - `MARK ALL ABSENT`
  - `SAVE ATTENDANCE`
  - `RESET`
- **Duplicate Attendance Prevention**:
  - Enforced by MySQL `UNIQUE (student_id, attendance_date)` constraint.
  - If attendance already exists for that date, prompts:
    ```text
    Attendance already recorded for this date.
    Do you want to edit it?
    YES / NO
    ```
  - Batch updates execute atomically inside a single JDBC transaction.

### 📈 Automatic Attendance Calculation & Dynamic Status
- Formula:
  $$\text{Attendance Percentage} = \left(\frac{\text{Present Days}}{\text{Total Working Days}}\right) \times 100$$
- Dynamic status tiers (configurable in Settings):
  - **$\ge$ 85%**: Excellent (Green badge)
  - **75% – 84.99%**: Good (Blue badge)
  - **65% – 74.99%**: Warning (Yellow badge)
  - **$<$ 65%**: Critical (Red badge)

### 📑 Reports Module
Supports 6 dedicated report types:
1. **Daily Attendance Report**: Filter by Date, Department, Year, Section.
2. **Monthly Attendance Report**: Filter by Month (1–12), Year, Department, Year of Study, Section.
3. **Date Range Report**: Filter by From Date, To Date, Department, Year, Section.
4. **Student-wise Attendance Report**: Select individual student and date range.
5. **Department-wise Summary**: College-wide comparison of attendance rates across engineering branches.
6. **Low Attendance Report**: Lists all students below configured criteria (default 75%).

### 📊 Real Excel Export (Apache POI `.xlsx`)
- Generates genuine, styled Microsoft Excel `.xlsx` files:
  - Header styling with Royal Blue fill, bold white typography.
  - Auto-sized columns and thin cell borders.
  - Freeze panes on header row.
  - Date formats (`dd-MM-yyyy`) and Percentage formats (`0.00%`).
- **Complete Attendance Summary Workbook**:
  - **Sheet 1**: `Summary` (Institution info, total students, working days, averages, daily counts).
  - **Sheet 2**: `Student Attendance` (Consolidated performance of all students).
  - **Sheet 3**: `Daily Attendance` (Date-wise logs).
  - **Sheet 4**: `Low Attendance` (Intervention list).
  - **Sheet 5**: `Student List` (Master directory).

### 📄 Real PDF Export (OpenPDF)
- Clean, print-ready PDF reports with institution header, generated date/time stamps, filter parameters, styled table cells, alternating row colors, and summary totals.

### ⚙️ System & Academic Settings
- Configurable settings stored in MySQL `settings` table:
  - Institution Name (Default: `ABC Engineering College`)
  - Academic Year (Default: `2026-27`)
  - Current Semester (Default: `V`)
  - Required Attendance Percentage (Default: `75%`)
  - Semester Working Days Target (Default: `60`)
  - Departments, Years, and Sections.
- One-click **Database Connection Settings** dialog and **Re-seed Demo Data** utility.

---

## 2. Technology Stack

| Component | Technology | Version |
|---|---|---|
| **Language** | Java (JDK) | 17 LTS or later |
| **GUI Framework** | Java Swing + FlatLaf | FlatLaf 3.5.4 |
| **Database** | MySQL Server | 8.x |
| **Database Driver** | MySQL Connector/J | 8.3.0 |
| **Excel Library** | Apache POI / POI-OOXML | 5.2.5 |
| **PDF Library** | OpenPDF | 1.3.39 |
| **Security** | jBCrypt | 0.4 |
| **Build Tool** | Apache Maven | 3.9+ |
| **Testing** | JUnit Jupiter | 5.10.2 |

---

## 3. Preloaded 100 Demo Students & Historical Attendance

The database includes **100 realistic demo students** with diverse Indian names, emails, phones, and addresses distributed across:
- **Departments**: CSE (20), IT (20), AI&DS (15), ECE (20), EEE (13), MECH (12)
- **Years of Study**: 1, 2, 3, 4
- **Sections**: A, B
- **Attendance History**: **40 working days** (weekdays only, August 3, 2026 to September 25, 2026) with realistic distributions:
  - ~30 students: **90% – 100%** (High performers)
  - ~35 students: **80% – 89%** (Good attendance)
  - ~15 students: **75% – 79%** (Borderline compliant)
  - ~12 students: **65% – 74%** (Warning - below threshold)
  - ~8 students: **< 65%** (Critical low attendance)

---

## 4. Default Login Credentials

| Role | Username | Password | Access Level |
|---|---|---|---|
| **Administrator** | `admin` | `admin123` | Full Access (Dashboard, Students, Attendance, Reports, Teachers, Settings) |
| **Teacher / Faculty** | `teacher` | `teacher123` | Standard Access (Dashboard, Students, Attendance, Reports) |

---

## 5. Database Setup Instructions

### Option A: Automatic Application Setup (Fastest & Easiest)
1. Make sure MySQL Server is running locally on port `3306`.
2. Start the application (see section 6).
3. On the login screen, click the **`DB Settings`** button at the bottom right.
4. Verify your MySQL username and password (e.g., `root` / your password).
5. Click **`Initialize / Seed DB`**. The application will automatically create the database, tables, default settings, demo users, 100 students, faculty, and 40 days of historical attendance!

### Option B: Manual Setup via MySQL Command Line or Workbench
Open MySQL Workbench or your terminal and execute:

```bash
# 1. Login to MySQL
mysql -u root -p

# 2. Execute setup.sql
source database/setup.sql;
```

Or run via shell:
```bash
mysql -u root -p < database/setup.sql
```

The script `database/setup.sql` will:
1. Create `student_attendance_db` with `utf8mb4` character set.
2. Create tables: `settings`, `users`, `departments`, `teachers`, `students`, `attendance`.
3. Add unique indexes and constraints (`student_id, attendance_date`).
4. Insert default academic settings.
5. Insert `admin` and `teacher` accounts with BCrypt hashes.
6. Insert 6 departments and 6 demo faculty members.
7. Insert 100 demo students with realistic particulars.
8. Insert 4,000 attendance records across 40 working days.

---

## 6. Configuration

### Connection Properties
Configuration is located in `src/main/resources/application.properties`:
```properties
db.host=localhost
db.port=3306
db.name=student_attendance_db
db.username=root
db.password=root
db.params=useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8

app.name=Student Attendance Management System
app.version=1.0.0
app.default.college=ABC Engineering College
app.default.academic_year=2026-27
app.default.semester=V
app.default.required_attendance=75
app.default.working_days=60
```

> **Note:** If you change database credentials using the in-app **DB Settings** dialog, they are automatically persisted to `db.properties` in the application directory without modifying internal resources.

---

## 7. How to Build and Run

### Run on Windows using `run.bat`
Simply double-click `run.bat` or run:
```cmd
run.bat
```

### Build Executable JAR with Maven
```bash
mvn clean package -DskipTests
```
This produces the standalone fat JAR at `target/StudentAttendanceSystem.jar`.

### Run the Standalone JAR
```bash
java -jar target/StudentAttendanceSystem.jar
```

### Run with Maven Exec Plugin
```bash
mvn compile exec:java
```

### Run in IDE (IntelliJ IDEA / Eclipse / NetBeans)
1. Open the IDE and select **Open / Import Project**.
2. Select the directory `student-attendance-system` as a **Maven project**.
3. Allow the IDE to import `pom.xml`.
4. Locate and run `src/main/java/com/attendance/Main.java`.

---

## 8. Automated Testing

Run the automated test suite with Maven:
```bash
mvn test
```
The test suite validates:
- BCrypt password hashing and verification for `admin123` and `teacher123`.
- Input validation routines (names, emails, phones, years, statuses).
- Date utilities and working days calculation (ensuring 0 weekend days).
- Attendance percentages and tier thresholds (Excellent, Good, Warning, Critical).
- 100 Demo student definitions (uniqueness of names and register numbers).
- Real Apache POI Excel export (creates and reads back `.xlsx` worksheets).
- Real OpenPDF export (creates valid PDF documents with `%PDF-` header).

---

## 9. Project Structure

```text
student-attendance-system/
│
├── pom.xml                               # Maven project descriptor & dependencies
├── README.md                             # Complete project documentation
├── run.bat                               # Windows launcher script
│
├── database/
│   ├── schema.sql                        # DDL table creation and index definitions
│   ├── demo_data.sql                     # 100 students, users, faculty & attendance seed
│   └── setup.sql                         # Complete unified setup script
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/attendance/
    │   │       ├── Main.java             # Desktop application launcher & L&F setup
    │   │       │
    │   │       ├── config/
    │   │       │   └── DatabaseConnection.java   # JDBC connection pooling & telemetry
    │   │       │
    │   │       ├── model/
    │   │       │   ├── User.java                 # User account entity
    │   │       │   ├── Student.java              # Student record entity
    │   │       │   ├── Teacher.java              # Faculty entity
    │   │       │   ├── Department.java           # Department entity
    │   │       │   ├── Attendance.java           # Daily attendance entry
    │   │       │   ├── AppSettings.java          # Persistent settings model
    │   │       │   ├── DashboardStats.java       # Executive analytics model
    │   │       │   └── StudentAttendanceSummary.java # Calculated attendance metrics
    │   │       │
    │   │       ├── dao/
    │   │       │   ├── UserDAO.java              # Users table CRUD & authentication
    │   │       │   ├── StudentDAO.java           # Student SQL queries & filters
    │   │       │   ├── TeacherDAO.java           # Teacher SQL queries
    │   │       │   ├── DepartmentDAO.java        # Department SQL operations
    │   │       │   ├── AttendanceDAO.java        # Attendance marking & calculations
    │   │       │   └── SettingsDAO.java          # Settings table persistence
    │   │       │
    │   │       ├── service/
    │   │       │   ├── AuthenticationService.java# Session & credential validation
    │   │       │   ├── StudentService.java       # Student business validations
    │   │       │   ├── TeacherService.java       # Faculty management logic
    │   │       │   ├── AttendanceService.java    # Marking rules & calculations
    │   │       │   ├── ReportService.java        # Aggregated reporting queries
    │   │       │   ├── SettingsService.java      # Settings validation & storage
    │   │       │   ├── DatabaseInitService.java  # Auto-schema creator & seeder
    │   │       │   ├── ExcelExportService.java   # Apache POI multi-sheet generator
    │   │       │   └── PdfExportService.java     # OpenPDF formatted reports
    │   │       │
    │   │       ├── ui/
    │   │       │   ├── LoginFrame.java           # Modern login window
    │   │       │   ├── DashboardFrame.java       # Main frame with sidebar navigation
    │   │       │   ├── DashboardPanel.java       # KPI cards, bar chart, low-att alert
    │   │       │   ├── StudentPanel.java         # Student search, filters, CRUD table
    │   │       │   ├── StudentFormDialog.java    # Add/Edit student modal dialog
    │   │       │   ├── StudentProfileDialog.java # Individual profile & attendance stats
    │   │       │   ├── AttendancePanel.java      # Daily class attendance marking
    │   │       │   ├── ReportsPanel.java         # 6 report types, Excel & PDF exports
    │   │       │   ├── TeacherPanel.java         # Faculty management panel
    │   │       │   ├── TeacherFormDialog.java    # Add/Edit teacher modal dialog
    │   │       │   ├── SettingsPanel.java        # Academic configuration panel
    │   │       │   ├── DatabaseConfigDialog.java # MySQL connection settings modal
    │   │       │   ├── ChangePasswordDialog.java # User password modification modal
    │   │       │   │
    │   │       │   └── components/
    │   │       │       ├── ModernButton.java     # Custom styled rounded buttons
    │   │       │       ├── ModernTable.java      # Styled JTable with status badges
    │   │       │       ├── StatCard.java         # KPI dashboard card component
    │   │       │       └── SimpleBarChart.java   # Vector department comparison chart
    │   │       │
    │   │       └── util/
    │   │           ├── DateUtil.java             # Date formatting & working days
    │   │           ├── ValidationUtil.java       # Field & regex validators
    │   │           ├── PasswordUtil.java         # BCrypt hashing & checking
    │   │           └── SqlScriptGenerator.java   # SQL schema & demo data exporter
    │   │
    │   └── resources/
    │       └── application.properties            # Default connection parameters
    │
    └── test/
        └── java/
            └── com/attendance/
                └── SystemUnitTest.java           # Automated test suite
```

---

## 10. Verification & Quality Checklist

- [x] Java 17 compatibility verified.
- [x] Java Swing with FlatLaf modern UI implemented.
- [x] JDBC MySQL 8.x connectivity with prepared statements.
- [x] Complete MVC architecture with DAO, Service, Model, and UI layers.
- [x] Login system with BCrypt hashing (`admin` / `admin123`, `teacher` / `teacher123`).
- [x] Interactive Dashboard with 6 dynamic KPI cards and department comparison chart.
- [x] Student Management with search, filters, CRUD, and profile viewer.
- [x] Teacher Management module.
- [x] Daily class attendance marking with duplicate prevention (`UNIQUE(student_id, attendance_date)`).
- [x] Dynamic attendance percentage calculation: `(Present / Total Working Days) * 100`.
- [x] Low attendance alerts section with export options.
- [x] 6 Report types (Daily, Student, Monthly, Date Range, Department, Low Attendance).
- [x] Apache POI `.xlsx` Excel export with multi-sheet summary workbook.
- [x] OpenPDF report generation with institutional headers and timestamps.
- [x] Configurable academic settings saved in MySQL.
- [x] 100 Demo students with realistic Indian names across CSE, IT, AI&DS, ECE, EEE, MECH.
- [x] 40 Days of historical attendance with realistic distribution (no weekends).
- [x] Executable fat JAR built at `target/StudentAttendanceSystem.jar`.
- [x] All 7 automated unit and integration tests passing (`mvn test`).
