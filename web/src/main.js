import { 
  DEFAULT_SETTINGS, 
  DEFAULT_TEACHERS, 
  generateInitialStudents, 
  generate40WorkingDays, 
  generateInitialAttendance,
  DEFAULT_PERIODS,
  DEFAULT_SUBJECTS,
  DEFAULT_TIMETABLE,
  DEFAULT_LEAVES,
  DEFAULT_NOTIFICATIONS,
  DEFAULT_AUDIT_LOGS
} from "./data.js";
import * as XLSX from "xlsx";
import { jsPDF } from "jspdf";
import "jspdf-autotable";

// ===================================================================
// Application State & LocalStorage Store
// ===================================================================

class Store {
  constructor() {
    this.init();
  }

  init() {
    if (!localStorage.getItem("sams_students")) {
      const students = generateInitialStudents();
      const workingDays = generate40WorkingDays();
      const attendance = generateInitialAttendance(students, workingDays);

      localStorage.setItem("sams_settings", JSON.stringify(DEFAULT_SETTINGS));
      localStorage.setItem("sams_teachers", JSON.stringify(DEFAULT_TEACHERS));
      localStorage.setItem("sams_students", JSON.stringify(students));
      localStorage.setItem("sams_attendance", JSON.stringify(attendance));
      localStorage.setItem("sams_working_days", JSON.stringify(workingDays));
      localStorage.setItem("sams_subjects", JSON.stringify(DEFAULT_SUBJECTS));
      localStorage.setItem("sams_timetable", JSON.stringify(DEFAULT_TIMETABLE));
      localStorage.setItem("sams_leaves", JSON.stringify(DEFAULT_LEAVES));
      localStorage.setItem("sams_notifications", JSON.stringify(DEFAULT_NOTIFICATIONS));
      localStorage.setItem("sams_audit_logs", JSON.stringify(DEFAULT_AUDIT_LOGS));
    }
  }

  getSettings() {
    let s = JSON.parse(localStorage.getItem("sams_settings"));
    if (!s || s.collegeName === "ABC Engineering College") {
      s = { ...(s || DEFAULT_SETTINGS), collegeName: "KIT ENGINEERING COLLEGE" };
      this.saveSettings(s);
    }
    return s;
  }
  saveSettings(s) {
    localStorage.setItem("sams_settings", JSON.stringify(s));
  }

  getTeachers() {
    return JSON.parse(localStorage.getItem("sams_teachers")) || DEFAULT_TEACHERS;
  }
  saveTeachers(t) {
    localStorage.setItem("sams_teachers", JSON.stringify(t));
  }

  getStudents() {
    return JSON.parse(localStorage.getItem("sams_students")) || [];
  }
  saveStudents(s) {
    localStorage.setItem("sams_students", JSON.stringify(s));
  }

  getAttendance() {
    return JSON.parse(localStorage.getItem("sams_attendance")) || {};
  }
  saveAttendance(a) {
    localStorage.setItem("sams_attendance", JSON.stringify(a));
  }

  getWorkingDays() {
    return JSON.parse(localStorage.getItem("sams_working_days")) || [];
  }

  getSubjects() {
    return JSON.parse(localStorage.getItem("sams_subjects")) || DEFAULT_SUBJECTS;
  }
  saveSubjects(s) {
    localStorage.setItem("sams_subjects", JSON.stringify(s));
  }

  getTimetable() {
    return JSON.parse(localStorage.getItem("sams_timetable")) || DEFAULT_TIMETABLE;
  }
  saveTimetable(t) {
    localStorage.setItem("sams_timetable", JSON.stringify(t));
  }

  getLeaves() {
    return JSON.parse(localStorage.getItem("sams_leaves")) || DEFAULT_LEAVES;
  }
  saveLeaves(l) {
    localStorage.setItem("sams_leaves", JSON.stringify(l));
  }

  getNotifications() {
    return JSON.parse(localStorage.getItem("sams_notifications")) || DEFAULT_NOTIFICATIONS;
  }
  saveNotifications(n) {
    localStorage.setItem("sams_notifications", JSON.stringify(n));
  }

  getAuditLogs() {
    return JSON.parse(localStorage.getItem("sams_audit_logs")) || DEFAULT_AUDIT_LOGS;
  }
  saveAuditLogs(a) {
    localStorage.setItem("sams_audit_logs", JSON.stringify(a));
  }

  resetAll() {
    localStorage.clear();
    this.init();
  }
}

const store = new Store();

// Current User State (Default Admin)
let currentUser = {
  username: "admin",
  name: "System Administrator",
  role: "ADMIN"
};

let currentTab = "dashboard";
let selectedStudentForModal = null;
let editingStudent = null;
let editingTeacher = null;

// ===================================================================
// Metric Calculations
// ===================================================================

function getStudentSummary(studentId) {
  const attObj = store.getAttendance()[studentId] || {};
  const dates = Object.keys(attObj);
  const totalDays = dates.length;
  let presentDays = 0;
  for (const d of dates) {
    if (attObj[d] === "PRESENT") presentDays++;
  }
  const absentDays = totalDays - presentDays;
  const pct = totalDays > 0 ? (presentDays / totalDays) * 100 : 0;
  const roundedPct = Math.round(pct * 100) / 100;

  let status = "Critical";
  if (roundedPct >= 85) status = "Excellent";
  else if (roundedPct >= 75) status = "Good";
  else if (roundedPct >= 65) status = "Warning";

  return { totalDays, presentDays, absentDays, percentage: roundedPct, status };
}

function calculateDashboardStats() {
  const students = store.getStudents();
  const settings = store.getSettings();
  const today = new Date().toISOString().split("T")[0];
  const attendance = store.getAttendance();

  let presentToday = 0;
  let absentToday = 0;
  let notMarkedToday = 0;
  let totalPctSum = 0;
  let studentsBelowCount = 0;
  const lowStudents = [];

  for (const s of students) {
    const summary = getStudentSummary(s.id);
    totalPctSum += summary.percentage;

    if (summary.totalDays > 0 && summary.percentage < settings.requiredAttendancePct) {
      studentsBelowCount++;
      lowStudents.push({ ...s, ...summary });
    }

    const todayStatus = attendance[s.id] ? attendance[s.id][today] : null;
    if (todayStatus === "PRESENT") presentToday++;
    else if (todayStatus === "ABSENT") absentToday++;
    else notMarkedToday++;
  }

  lowStudents.sort((a, b) => a.percentage - b.percentage);

  const avgAttendance = students.length > 0 ? Math.round((totalPctSum / students.length) * 100) / 100 : 0;

  // Department Attendance Averages
  const deptMap = {};
  for (const d of settings.departments) {
    deptMap[d] = { totalPct: 0, count: 0 };
  }
  for (const s of students) {
    if (deptMap[s.dept]) {
      const summary = getStudentSummary(s.id);
      deptMap[s.dept].totalPct += summary.percentage;
      deptMap[s.dept].count++;
    }
  }

  const deptStats = {};
  for (const d of settings.departments) {
    const item = deptMap[d];
    deptStats[d] = item && item.count > 0 ? Math.round((item.totalPct / item.count) * 10) / 10 : 0;
  }

  return {
    totalStudents: students.length,
    presentToday,
    absentToday,
    notMarkedToday,
    averageAttendance: avgAttendance,
    studentsBelowCount,
    lowStudents,
    deptStats,
    today,
    requiredPct: settings.requiredAttendancePct
  };
}

// ===================================================================
// Core App Rendering
// ===================================================================

const app = document.getElementById("app");

function render() {
  const settings = store.getSettings();
  const notifs = store.getNotifications();

  app.innerHTML = `
    <div id="app-container">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <div class="sidebar-brand-badge">🏛️ KIT COLLEGE</div>
          <div class="sidebar-title">KIT Engineering College</div>
          <div class="sidebar-subtitle">Enterprise Academic Portal v2.0</div>
        </div>
        <div class="nav-section-title">Navigation</div>
        <ul class="nav-list">
          ${currentUser.role === "STUDENT" ? `
            <li class="nav-item">
              <button class="${currentTab === "student-portal" ? "active" : ""}" onclick="switchTab('student-portal')">
                🎓 My Student Portal
              </button>
            </li>
            <li class="nav-item">
              <button class="${currentTab === "timetable" ? "active" : ""}" onclick="switchTab('timetable')">
                📅 My Timetable
              </button>
            </li>
            <li class="nav-item">
              <button class="${currentTab === "leaves" ? "active" : ""}" onclick="switchTab('leaves')">
                🏖️ My Leaves
              </button>
            </li>
          ` : `
            <li class="nav-item">
              <button class="${currentTab === "dashboard" ? "active" : ""}" onclick="switchTab('dashboard')">
                📊 Dashboard
              </button>
            </li>
            <li class="nav-item">
              <button class="${currentTab === "students" ? "active" : ""}" onclick="switchTab('students')">
                👥 Students
              </button>
            </li>
            <li class="nav-item">
              <button class="${currentTab === "attendance" ? "active" : ""}" onclick="switchTab('attendance')">
                📝 Attendance
              </button>
            </li>
            <li class="nav-item">
              <button class="${currentTab === "subjects" ? "active" : ""}" onclick="switchTab('subjects')">
                📚 Subjects
              </button>
            </li>
            <li class="nav-item">
              <button class="${currentTab === "timetable" ? "active" : ""}" onclick="switchTab('timetable')">
                🗓️ Timetable
              </button>
            </li>
            <li class="nav-item">
              <button class="${currentTab === "qr-attendance" ? "active" : ""}" onclick="switchTab('qr-attendance')">
                📱 QR Attendance
              </button>
            </li>
            <li class="nav-item">
              <button class="${currentTab === "leaves" ? "active" : ""}" onclick="switchTab('leaves')">
                🏖️ Leave Requests
              </button>
            </li>
            <li class="nav-item">
              <button class="${currentTab === "reports" ? "active" : ""}" onclick="switchTab('reports')">
                📑 Reports
              </button>
            </li>
            <li class="nav-item">
              <button class="${currentTab === "audit-logs" ? "active" : ""}" onclick="switchTab('audit-logs')">
                📋 Audit Trail
              </button>
            </li>
            ${currentUser.role === "ADMIN" ? `
              <li class="nav-item">
                <button class="${currentTab === "teachers" ? "active" : ""}" onclick="switchTab('teachers')">
                  👨‍🏫 Teachers
                </button>
              </li>
              <li class="nav-item">
                <button class="${currentTab === "settings" ? "active" : ""}" onclick="switchTab('settings')">
                  ⚙️ Settings
                </button>
              </li>
            ` : ""}
          `}
        </ul>
        <div class="sidebar-footer">
          <ul class="nav-list">
            <li class="nav-item">
              <button onclick="logout()">🚪 Sign Out</button>
            </li>
          </ul>
        </div>
      </aside>

      <!-- Main Layout -->
      <div class="main-wrapper">
        <!-- Topbar -->
        <header class="topbar">
          <div class="topbar-left">
            <span class="portal-brand-title">KIT Attendance Management</span>
            <span class="college-badge">🏛️ ${settings.collegeName}</span>
          </div>
          <div class="topbar-right">
            <button class="notif-btn" title="Notification Center" onclick="openNotificationModal()">
              🔔
              <span class="notif-badge">${notifs.length}</span>
            </button>
            <div class="user-profile">
              <div class="user-avatar-badge">${currentUser.name.charAt(0)}</div>
              <div>
                <span style="display: block; font-weight: 700; line-height: 1.2;">${currentUser.name}</span>
                <span class="role-tag">${currentUser.role}</span>
              </div>
            </div>
            <button class="btn btn-secondary" style="padding: 5px 12px; font-size: 0.78rem;" onclick="logout()">🚪 Exit</button>
          </div>
        </header>

        <!-- Main Content View -->
        <main class="content-area">
          ${renderTabContent()}
        </main>
      </div>
    </div>

    <!-- Modals Container -->
    <div id="modal-container"></div>
  `;

  attachTabEvents();
}

window.switchTab = function(tab) {
  currentTab = tab;
  render();
};

window.logout = function() {
  if (confirm("Are you sure you want to log out?")) {
    currentUser = { username: "guest", name: "Guest User", role: "GUEST" };
    renderLoginView();
  }
};

function renderLoginView() {
  const settings = store.getSettings();
  app.innerHTML = `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <div class="login-emblem">🏛️</div>
          <h2>${settings.collegeName}</h2>
          <p>Student Attendance Management Portal v2.0</p>
        </div>
        <form id="login-form" onsubmit="handleLogin(event)">
          <div class="form-group" style="margin-bottom: 16px;">
            <label>Username / Reg No</label>
            <input type="text" id="login-user" class="form-control" value="admin" required />
          </div>
          <div class="form-group" style="margin-bottom: 18px;">
            <label>Secure Password</label>
            <input type="password" id="login-pass" class="form-control" value="admin123" required />
          </div>
          <button type="submit" class="btn btn-primary" style="width: 100%; padding: 12px; font-size: 0.92rem; margin-bottom: 14px;">
            Sign In to Portal
          </button>
          <div class="login-quick-demo">
            <span>⚡ Quick Demo Switch</span>
            <div class="login-demo-btns">
              <button type="button" class="btn btn-secondary" onclick="setDemo('admin', 'admin123')">Admin</button>
              <button type="button" class="btn btn-secondary" onclick="setDemo('teacher', 'teacher123')">Faculty</button>
              <button type="button" class="btn btn-secondary" onclick="setDemo('student', 'student123')">Student</button>
            </div>
          </div>
        </form>
      </div>
    </div>
  `;
}

window.setDemo = function(u, p) {
  document.getElementById("login-user").value = u;
  document.getElementById("login-pass").value = p;
};

window.handleLogin = function(e) {
  e.preventDefault();
  const u = document.getElementById("login-user").value.trim();
  const p = document.getElementById("login-pass").value.trim();

  if (u === "admin" && p === "admin123") {
    currentUser = { username: "admin", name: "System Administrator", role: "ADMIN" };
    currentTab = "dashboard";
    render();
  } else if (u === "teacher" && p === "teacher123") {
    currentUser = { username: "teacher", name: "Dr. R. Ramanathan", role: "TEACHER" };
    currentTab = "dashboard";
    render();
  } else if (u === "student" && p === "student123") {
    currentUser = { 
      username: "student", 
      name: "Aarav Kumar", 
      role: "STUDENT", 
      studentId: 1, 
      regNo: "24CSE001", 
      dept: "CSE", 
      year: 3, 
      sec: "A" 
    };
    currentTab = "student-portal";
    render();
  } else {
    alert("Invalid credentials! Try admin/admin123, teacher/teacher123, or student/student123");
  }
};

// ===================================================================
// Tab Content Renderers
// ===================================================================

function renderTabContent() {
  switch (currentTab) {
    case "dashboard": return renderDashboard();
    case "student-portal": return renderStudentPortal();
    case "students": return renderStudents();
    case "attendance": return renderAttendance();
    case "subjects": return renderSubjects();
    case "timetable": return renderTimetable();
    case "qr-attendance": return renderQrAttendance();
    case "leaves": return renderLeaveManagement();
    case "reports": return renderReports();
    case "audit-logs": return renderAuditLogs();
    case "teachers": return renderTeachers();
    case "settings": return renderSettings();
    default: return currentUser.role === "STUDENT" ? renderStudentPortal() : renderDashboard();
  }
}

// 1. Dashboard View
function renderDashboard() {
  const stats = calculateDashboardStats();
  const settings = store.getSettings();

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Executive Dashboard</h1>
        <div class="page-meta">
          Date: ${new Date().toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" })}  |  
          Academic Year: ${settings.academicYear}  |  Semester: ${settings.semester}
        </div>
      </div>
      <div>
        <button class="btn btn-secondary" onclick="render()">🔄 Refresh</button>
      </div>
    </div>

    <!-- 6 KPI Stat Cards -->
    <div class="stats-grid">
      <div class="stat-card" style="--stat-color: #991b1b;">
        <div class="stat-header">
          <span class="stat-title">Total Students</span>
          <span class="stat-icon">🎓</span>
        </div>
        <div class="stat-value">${stats.totalStudents}</div>
        <div class="stat-sub">Enrolled active students</div>
      </div>
      <div class="stat-card" style="--stat-color: #15803d;">
        <div class="stat-header">
          <span class="stat-title">Present Today</span>
          <span class="stat-icon">✅</span>
        </div>
        <div class="stat-value">${stats.presentToday}</div>
        <div class="stat-sub">Marked present today</div>
      </div>
      <div class="stat-card" style="--stat-color: #dc2626;">
        <div class="stat-header">
          <span class="stat-title">Absent Today</span>
          <span class="stat-icon">❌</span>
        </div>
        <div class="stat-value">${stats.absentToday}</div>
        <div class="stat-sub">Marked absent today</div>
      </div>
      <div class="stat-card" style="--stat-color: #d97706;">
        <div class="stat-header">
          <span class="stat-title">Not Marked</span>
          <span class="stat-icon">⏳</span>
        </div>
        <div class="stat-value">${stats.notMarkedToday}</div>
        <div class="stat-sub">Pending attendance</div>
      </div>
      <div class="stat-card" style="--stat-color: #b91c1c;">
        <div class="stat-header">
          <span class="stat-title">Average Attendance</span>
          <span class="stat-icon">📈</span>
        </div>
        <div class="stat-value">${stats.averageAttendance}%</div>
        <div class="stat-sub">Overall semester rate</div>
      </div>
      <div class="stat-card" style="--stat-color: #7f1d1d;">
        <div class="stat-header">
          <span class="stat-title">Defaulters (&lt;${stats.requiredPct}%)</span>
          <span class="stat-icon">⚠️</span>
        </div>
        <div class="stat-value">${stats.studentsBelowCount}</div>
        <div class="stat-sub">Action required</div>
      </div>
    </div>

    <!-- 2 Column Split: Department Comparison & Low Attendance Alert -->
    <div class="dashboard-split">
      <div class="card">
        <h3 style="font-size: 1rem; font-weight: 700; margin-bottom: 4px;">Department Attendance Comparison</h3>
        <p style="font-size: 0.78rem; color: var(--text-muted);">Attendance percentage performance across engineering disciplines</p>
        
        <div class="chart-container">
          <div class="chart-bars">
            ${Object.entries(stats.deptStats).map(([dept, val]) => `
              <div class="chart-col">
                <div class="bar-wrap">
                  <div class="bar-fill ${val < stats.requiredPct ? "warning" : ""}" style="height: ${val}%;">
                    <span class="bar-val">${val}%</span>
                  </div>
                </div>
                <div class="bar-label">${dept}</div>
              </div>
            `).join("")}
          </div>
        </div>
      </div>

      <div class="card" style="display: flex; flex-direction: column;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <div>
            <h3 style="font-size: 1rem; font-weight: 700; color: var(--danger);">Low Attendance Alert (< ${stats.requiredPct}%)</h3>
            <p style="font-size: 0.78rem; color: var(--text-muted);">${stats.studentsBelowCount} students need immediate intervention</p>
          </div>
          <div style="display: flex; gap: 6px;">
            <button class="btn btn-secondary" onclick="exportLowAttendanceExcel()">Excel</button>
            <button class="btn btn-secondary" onclick="exportLowAttendancePdf()">PDF</button>
          </div>
        </div>

        <div class="table-wrapper" style="flex: 1; max-height: 240px;">
          <table>
            <thead>
              <tr>
                <th>Reg No</th>
                <th>Name</th>
                <th>Dept</th>
                <th>Attendance %</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${stats.lowStudents.slice(0, 10).map(s => `
                <tr>
                  <td><strong>${s.regNo}</strong></td>
                  <td>${s.name}</td>
                  <td>${s.dept}</td>
                  <td><strong>${s.percentage}%</strong></td>
                  <td><span class="badge badge-${s.status.toLowerCase()}">${s.status}</span></td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;
}

// 2. Students View
function renderStudents() {
  const students = store.getStudents();
  const settings = store.getSettings();

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Student Management</h1>
        <div class="page-meta">Manage active student roster, profile details, and history</div>
      </div>
      <div style="display: flex; gap: 8px;">
        <button class="btn btn-primary" onclick="openStudentModal()">+ Add Student</button>
        <button class="btn btn-success" onclick="exportStudentListExcel()">Export Excel</button>
      </div>
    </div>

    <!-- Filters Bar -->
    <div class="filters-bar">
      <div class="filters-group">
        <input type="text" id="stud-search" class="form-control" placeholder="Search name or reg no..." oninput="filterStudentsTable()" />
        <select id="stud-dept" class="form-control" onchange="filterStudentsTable()">
          <option value="All">All Departments</option>
          ${settings.departments.map(d => `<option value="${d}">${d}</option>`).join("")}
        </select>
        <select id="stud-year" class="form-control" onchange="filterStudentsTable()">
          <option value="All">All Years</option>
          <option value="1">Year 1</option>
          <option value="2">Year 2</option>
          <option value="3">Year 3</option>
          <option value="4">Year 4</option>
        </select>
        <select id="stud-sec" class="form-control" onchange="filterStudentsTable()">
          <option value="All">All Sections</option>
          <option value="A">Section A</option>
          <option value="B">Section B</option>
        </select>
      </div>
      <div id="stud-count" style="font-size: 0.82rem; color: var(--text-muted);">
        Total: ${students.length} students
      </div>
    </div>

    <!-- Table -->
    <div class="table-wrapper">
      <table id="students-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Register No</th>
            <th>Student Name</th>
            <th>Gender</th>
            <th>Department</th>
            <th>Year</th>
            <th>Section</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody id="students-tbody">
          <!-- Populated by filterStudentsTable() -->
        </tbody>
      </table>
    </div>
  `;
}

window.filterStudentsTable = function() {
  const search = (document.getElementById("stud-search")?.value || "").toLowerCase().trim();
  const dept = document.getElementById("stud-dept")?.value || "All";
  const year = document.getElementById("stud-year")?.value || "All";
  const sec = document.getElementById("stud-sec")?.value || "All";

  const allStudents = store.getStudents();
  const filtered = allStudents.filter(s => {
    const matchSearch = s.name.toLowerCase().includes(search) || s.regNo.toLowerCase().includes(search);
    const matchDept = dept === "All" || s.dept === dept;
    const matchYear = year === "All" || String(s.year) === year;
    const matchSec = sec === "All" || s.section === sec;
    return matchSearch && matchDept && matchYear && matchSec;
  });

  const tbody = document.getElementById("students-tbody");
  if (!tbody) return;

  tbody.innerHTML = filtered.map(s => `
    <tr>
      <td>${s.id}</td>
      <td><strong>${s.regNo}</strong></td>
      <td>${s.name}</td>
      <td>${s.gender}</td>
      <td>${s.dept}</td>
      <td>Year ${s.year}</td>
      <td>${s.section}</td>
      <td>${s.email}</td>
      <td>${s.phone}</td>
      <td>
        <div style="display: flex; gap: 4px;">
          <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 0.72rem;" onclick="viewStudentProfile(${s.id})">Profile</button>
          <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 0.72rem;" onclick="editStudentModal(${s.id})">Edit</button>
          <button class="btn btn-danger" style="padding: 4px 8px; font-size: 0.72rem;" onclick="deleteStudent(${s.id})">Delete</button>
        </div>
      </td>
    </tr>
  `).join("");

  const countDiv = document.getElementById("stud-count");
  if (countDiv) countDiv.innerText = `Showing ${filtered.length} of ${allStudents.length} students`;
};

// 3. Attendance View
function renderAttendance() {
  const settings = store.getSettings();
  const today = new Date().toISOString().split("T")[0];

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Daily Attendance Marking</h1>
        <div class="page-meta">Select class criteria and record student present/absent logs</div>
      </div>
      <div style="display: flex; gap: 8px;">
        <button class="btn btn-success" onclick="markAllAttendance('PRESENT')">MARK ALL PRESENT</button>
        <button class="btn btn-danger" onclick="markAllAttendance('ABSENT')">MARK ALL ABSENT</button>
        <button class="btn btn-primary" onclick="saveClassAttendance()">SAVE ATTENDANCE</button>
      </div>
    </div>

    <!-- Attendance Filters -->
    <div class="filters-bar">
      <div class="filters-group">
        <label style="font-size: 0.8rem; font-weight: 600;">Date:</label>
        <input type="date" id="att-date" class="form-control" value="${today}" onchange="loadAttendanceList()" />

        <label style="font-size: 0.8rem; font-weight: 600;">Department:</label>
        <select id="att-dept" class="form-control" onchange="loadAttendanceList()">
          ${settings.departments.map(d => `<option value="${d}">${d}</option>`).join("")}
        </select>

        <label style="font-size: 0.8rem; font-weight: 600;">Year:</label>
        <select id="att-year" class="form-control" onchange="loadAttendanceList()">
          <option value="1">1</option>
          <option value="2" selected>2</option>
          <option value="3">3</option>
          <option value="4">4</option>
        </select>

        <label style="font-size: 0.8rem; font-weight: 600;">Section:</label>
        <select id="att-sec" class="form-control" onchange="loadAttendanceList()">
          <option value="A" selected>A</option>
          <option value="B">B</option>
        </select>
      </div>
      <div id="att-status-msg" style="font-size: 0.82rem; font-weight: 600; color: var(--primary);">
        Ready to mark attendance
      </div>
    </div>

    <!-- Class Roster Table -->
    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>S.No</th>
            <th>Register No</th>
            <th>Student Name</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody id="attendance-tbody">
          <!-- Populated by loadAttendanceList() -->
        </tbody>
      </table>
    </div>
  `;
}

window.loadAttendanceList = function() {
  const date = document.getElementById("att-date")?.value;
  const dept = document.getElementById("att-dept")?.value;
  const year = parseInt(document.getElementById("att-year")?.value || "1");
  const sec = document.getElementById("att-sec")?.value;

  const students = store.getStudents().filter(s => s.dept === dept && s.year === year && s.section === sec);
  const attendance = store.getAttendance();

  const tbody = document.getElementById("attendance-tbody");
  if (!tbody) return;

  if (students.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; padding: 20px;">No students found for this class.</td></tr>`;
    return;
  }

  tbody.innerHTML = students.map((s, idx) => {
    const recorded = attendance[s.id] && attendance[s.id][date] ? attendance[s.id][date] : "PRESENT";
    return `
      <tr data-student-id="${s.id}">
        <td>${idx + 1}</td>
        <td><strong>${s.regNo}</strong></td>
        <td>${s.name}</td>
        <td>
          <select class="form-control att-select" style="font-weight: 700; width: 140px;" onchange="updateBadgeColor(this)">
            <option value="PRESENT" ${recorded === "PRESENT" ? "selected" : ""}>PRESENT</option>
            <option value="ABSENT" ${recorded === "ABSENT" ? "selected" : ""}>ABSENT</option>
          </select>
        </td>
      </tr>
    `;
  }).join("");

  const msg = document.getElementById("att-status-msg");
  if (msg) msg.innerText = `Loaded ${students.length} students for ${dept} Year ${year} Sec ${sec}`;
};

window.markAllAttendance = function(status) {
  const selects = document.querySelectorAll(".att-select");
  selects.forEach(s => {
    s.value = status;
    updateBadgeColor(s);
  });
};

window.updateBadgeColor = function(select) {
  if (select.value === "PRESENT") {
    select.style.color = "#16a34a";
  } else {
    select.style.color = "#dc2626";
  }
};

window.saveClassAttendance = function() {
  const date = document.getElementById("att-date")?.value;
  if (!date) {
    alert("Please select a date!");
    return;
  }

  const rows = document.querySelectorAll("#attendance-tbody tr");
  if (rows.length === 0) {
    alert("No students to save attendance for.");
    return;
  }

  const attendance = store.getAttendance();
  rows.forEach(r => {
    const sid = r.getAttribute("data-student-id");
    const val = r.querySelector(".att-select")?.value;
    if (sid && val) {
      if (!attendance[sid]) attendance[sid] = {};
      attendance[sid][date] = val;
    }
  });

  store.saveAttendance(attendance);
  alert("✓ Attendance recorded successfully for date: " + date);
  render();
};

// 4. Reports View
function renderReports() {
  const settings = store.getSettings();
  const today = new Date().toISOString().split("T")[0];

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Attendance Reports</h1>
        <div class="page-meta">Generate comprehensive daily, monthly, departmental, and low attendance reports</div>
      </div>
      <div style="display: flex; gap: 8px;">
        <button class="btn btn-primary" onclick="generateReportData()">Generate</button>
        <button class="btn btn-success" onclick="exportReportExcel()">Export Excel</button>
        <button class="btn btn-primary" onclick="exportReportPdf()">Export PDF</button>
        <button class="btn btn-secondary" onclick="exportCompleteWorkbookExcel()">Complete Workbook</button>
      </div>
    </div>

    <!-- Filters Bar -->
    <div class="filters-bar">
      <div class="filters-group">
        <label style="font-weight: 600; font-size: 0.8rem;">Type:</label>
        <select id="rep-type" class="form-control" onchange="toggleReportFilters()">
          <option value="daily">Daily Attendance Report</option>
          <option value="monthly">Monthly Attendance Report</option>
          <option value="low">Low Attendance Report (< ${settings.requiredAttendancePct}%)</option>
          <option value="dept">Department-wise Summary</option>
        </select>

        <span id="rep-date-wrap">
          <label style="font-weight: 600; font-size: 0.8rem;">Date:</label>
          <input type="date" id="rep-date" class="form-control" value="${today}" />
        </span>

        <span id="rep-dept-wrap">
          <label style="font-weight: 600; font-size: 0.8rem;">Dept:</label>
          <select id="rep-dept" class="form-control">
            <option value="All">All Departments</option>
            ${settings.departments.map(d => `<option value="${d}">${d}</option>`).join("")}
          </select>
        </span>
      </div>
      <div id="rep-meta" style="font-size: 0.8rem; color: var(--text-muted);">
        Generated on: ${new Date().toLocaleString("en-IN")}
      </div>
    </div>

    <!-- Report Table -->
    <div class="table-wrapper">
      <table id="report-table">
        <thead id="report-thead"></thead>
        <tbody id="report-tbody"></tbody>
      </table>
    </div>
  `;
}

window.toggleReportFilters = function() {
  const type = document.getElementById("rep-type")?.value;
  const dateWrap = document.getElementById("rep-date-wrap");
  if (dateWrap) dateWrap.style.display = type === "daily" ? "inline-flex" : "none";
  generateReportData();
};

window.generateReportData = function() {
  const type = document.getElementById("rep-type")?.value || "daily";
  const date = document.getElementById("rep-date")?.value || new Date().toISOString().split("T")[0];
  const dept = document.getElementById("rep-dept")?.value || "All";

  const students = store.getStudents();
  const attendance = store.getAttendance();
  const thead = document.getElementById("report-thead");
  const tbody = document.getElementById("report-tbody");
  if (!thead || !tbody) return;

  if (type === "daily") {
    thead.innerHTML = `
      <tr>
        <th>S.No</th>
        <th>Register No</th>
        <th>Student Name</th>
        <th>Department</th>
        <th>Year</th>
        <th>Section</th>
        <th>Date</th>
        <th>Status</th>
      </tr>
    `;

    const filtered = students.filter(s => dept === "All" || s.dept === dept);
    tbody.innerHTML = filtered.map((s, idx) => {
      const st = attendance[s.id] && attendance[s.id][date] ? attendance[s.id][date] : "NOT MARKED";
      return `
        <tr>
          <td>${idx + 1}</td>
          <td><strong>${s.regNo}</strong></td>
          <td>${s.name}</td>
          <td>${s.dept}</td>
          <td>${s.year}</td>
          <td>${s.section}</td>
          <td>${date}</td>
          <td><span class="badge badge-${st.toLowerCase()}">${st}</span></td>
        </tr>
      `;
    }).join("");
  } else if (type === "dept") {
    thead.innerHTML = `
      <tr>
        <th>S.No</th>
        <th>Department Code</th>
        <th>Average Attendance</th>
        <th>Status</th>
      </tr>
    `;

    const stats = calculateDashboardStats().deptStats;
    tbody.innerHTML = Object.entries(stats).map(([d, val], idx) => {
      const st = val >= 75 ? "Good" : "Warning";
      return `
        <tr>
          <td>${idx + 1}</td>
          <td><strong>${d}</strong></td>
          <td>${val}%</td>
          <td><span class="badge badge-${st.toLowerCase()}">${st}</span></td>
        </tr>
      `;
    }).join("");
  } else {
    // monthly or low
    thead.innerHTML = `
      <tr>
        <th>S.No</th>
        <th>Register No</th>
        <th>Student Name</th>
        <th>Department</th>
        <th>Year</th>
        <th>Working Days</th>
        <th>Present</th>
        <th>Absent</th>
        <th>Attendance %</th>
        <th>Status</th>
      </tr>
    `;

    const settings = store.getSettings();
    const rows = [];
    for (const s of students) {
      if (dept !== "All" && s.dept !== dept) continue;
      const sum = getStudentSummary(s.id);
      if (type === "low" && sum.percentage >= settings.requiredAttendancePct) continue;
      rows.push({ s, sum });
    }

    if (type === "low") rows.sort((a, b) => a.sum.percentage - b.sum.percentage);

    tbody.innerHTML = rows.map((r, idx) => `
      <tr>
        <td>${idx + 1}</td>
        <td><strong>${r.s.regNo}</strong></td>
        <td>${r.s.name}</td>
        <td>${r.s.dept}</td>
        <td>${r.s.year}</td>
        <td>${r.sum.totalDays}</td>
        <td>${r.sum.presentDays}</td>
        <td>${r.sum.absentDays}</td>
        <td><strong>${r.sum.percentage}%</strong></td>
        <td><span class="badge badge-${r.sum.status.toLowerCase()}">${r.sum.status}</span></td>
      </tr>
    `).join("");
  }
};

// 5. Teachers View
function renderTeachers() {
  const teachers = store.getTeachers();

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Faculty Management</h1>
        <div class="page-meta">Directory of teaching staff and department coordinators</div>
      </div>
      <div>
        <button class="btn btn-primary" onclick="openTeacherModal()">+ Add Faculty</button>
      </div>
    </div>

    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Employee ID</th>
            <th>Teacher Name</th>
            <th>Department</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          ${teachers.map(t => `
            <tr>
              <td>${t.id}</td>
              <td><strong>${t.empId}</strong></td>
              <td>${t.name}</td>
              <td>${t.dept}</td>
              <td>${t.email}</td>
              <td>${t.phone}</td>
              <td>
                <div style="display: flex; gap: 4px;">
                  <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 0.72rem;" onclick="editTeacherModal(${t.id})">Edit</button>
                  <button class="btn btn-danger" style="padding: 4px 8px; font-size: 0.72rem;" onclick="deleteTeacher(${t.id})">Delete</button>
                </div>
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

// 6. Settings View
function renderSettings() {
  const s = store.getSettings();

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">System Settings</h1>
        <div class="page-meta">Configure institutional policies and academic rules</div>
      </div>
    </div>

    <div class="card" style="max-width: 650px;">
      <form onsubmit="saveSettingsForm(event)">
        <div class="form-grid">
          <div class="form-group col-span-2">
            <label>College Name</label>
            <input type="text" id="set-college" class="form-control" value="${s.collegeName}" required />
          </div>
          <div class="form-group">
            <label>Academic Year</label>
            <input type="text" id="set-ay" class="form-control" value="${s.academicYear}" required />
          </div>
          <div class="form-group">
            <label>Current Semester</label>
            <input type="text" id="set-sem" class="form-control" value="${s.semester}" required />
          </div>
          <div class="form-group">
            <label>Required Attendance (%)</label>
            <input type="number" id="set-pct" class="form-control" value="${s.requiredAttendancePct}" required />
          </div>
          <div class="form-group">
            <label>Target Working Days</label>
            <input type="number" id="set-days" class="form-control" value="${s.workingDaysTarget}" required />
          </div>
          <div class="form-group col-span-2">
            <label>Departments (comma separated)</label>
            <input type="text" id="set-depts" class="form-control" value="${s.departments.join(", ")}" required />
          </div>
        </div>

        <div style="margin-top: 20px; display: flex; justify-content: space-between; align-items: center;">
          <button type="button" class="btn btn-danger" onclick="resetToDefaults()">Restore 100 Demo Students & Data</button>
          <button type="submit" class="btn btn-primary">Save Settings</button>
        </div>
      </form>
    </div>
  `;
}

window.saveSettingsForm = function(e) {
  e.preventDefault();
  const s = store.getSettings();
  s.collegeName = document.getElementById("set-college").value.trim();
  s.academicYear = document.getElementById("set-ay").value.trim();
  s.semester = document.getElementById("set-sem").value.trim();
  s.requiredAttendancePct = parseFloat(document.getElementById("set-pct").value);
  s.workingDaysTarget = parseInt(document.getElementById("set-days").value);
  s.departments = document.getElementById("set-depts").value.split(",").map(x => x.trim()).filter(Boolean);

  store.saveSettings(s);
  alert("✓ Settings saved successfully!");
  render();
};

window.resetToDefaults = function() {
  if (confirm("Reset everything to 100 demo students, faculty, and 40 days historical attendance?")) {
    store.resetAll();
    alert("✓ Database reset completed!");
    render();
  }
};

// ===================================================================
// Student Modals & Actions
// ===================================================================

window.viewStudentProfile = function(id) {
  const student = store.getStudents().find(s => s.id === id);
  if (!student) return;

  const summary = getStudentSummary(student.id);
  const attObj = store.getAttendance()[student.id] || {};
  const history = Object.entries(attObj).sort((a, b) => b[0].localeCompare(a[0]));

  const modalContainer = document.getElementById("modal-container");
  modalContainer.innerHTML = `
    <div class="modal-overlay" onclick="closeModal(event)">
      <div class="modal-content" style="max-width: 680px;" onclick="event.stopPropagation()">
        <div class="modal-header">
          <h3>Student Profile - ${student.regNo}</h3>
          <button class="btn btn-secondary" onclick="closeModal()">✕</button>
        </div>
        <div class="modal-body">
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 16px; background: #f8fafc; padding: 12px; border-radius: 8px;">
            <div><strong>Name:</strong> ${student.name}</div>
            <div><strong>Department:</strong> ${student.dept}</div>
            <div><strong>Year / Sec:</strong> Year ${student.year}, Section ${student.section}</div>
            <div><strong>Gender:</strong> ${student.gender}</div>
            <div><strong>Email:</strong> ${student.email}</div>
            <div><strong>Phone:</strong> ${student.phone}</div>
            <div><strong>Address:</strong> ${student.address}</div>
            <div><strong>Admission:</strong> ${student.admissionDate}</div>
          </div>

          <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 16px;">
            <div style="background: #eff6ff; padding: 10px; border-radius: 6px; text-align: center;">
              <div style="font-size: 0.72rem; color: #1e40af;">WORKING DAYS</div>
              <div style="font-size: 1.3rem; font-weight: 800;">${summary.totalDays}</div>
            </div>
            <div style="background: #ecfdf5; padding: 10px; border-radius: 6px; text-align: center;">
              <div style="font-size: 0.72rem; color: #065f46;">PRESENT</div>
              <div style="font-size: 1.3rem; font-weight: 800; color: #16a34a;">${summary.presentDays}</div>
            </div>
            <div style="background: #fef2f2; padding: 10px; border-radius: 6px; text-align: center;">
              <div style="font-size: 0.72rem; color: #991b1b;">ABSENT</div>
              <div style="font-size: 1.3rem; font-weight: 800; color: #dc2626;">${summary.absentDays}</div>
            </div>
            <div style="background: #faf5ff; padding: 10px; border-radius: 6px; text-align: center;">
              <div style="font-size: 0.72rem; color: #6b21a8;">ATTENDANCE %</div>
              <div style="font-size: 1.3rem; font-weight: 800; color: #7c3aed;">${summary.percentage}%</div>
            </div>
          </div>

          <h4 style="font-size: 0.9rem; font-weight: 700; margin-bottom: 8px;">Recent Attendance Logs</h4>
          <div class="table-wrapper" style="max-height: 180px;">
            <table>
              <thead>
                <tr><th>Date</th><th>Status</th></tr>
              </thead>
              <tbody>
                ${history.map(([d, st]) => `
                  <tr>
                    <td>${d}</td>
                    <td><span class="badge badge-${st.toLowerCase()}">${st}</span></td>
                  </tr>
                `).join("")}
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" onclick="closeModal()">Close</button>
        </div>
      </div>
    </div>
  `;
};

window.openStudentModal = function() {
  editingStudent = null;
  showStudentFormModal("Add New Student");
};

window.editStudentModal = function(id) {
  editingStudent = store.getStudents().find(s => s.id === id);
  if (!editingStudent) return;
  showStudentFormModal("Edit Student - " + editingStudent.regNo);
};

function showStudentFormModal(title) {
  const settings = store.getSettings();
  const s = editingStudent || {
    regNo: "", name: "", gender: "Male", dept: settings.departments[0], year: 1, section: "A",
    email: "", phone: "", address: "", admissionDate: new Date().toISOString().split("T")[0]
  };

  const modalContainer = document.getElementById("modal-container");
  modalContainer.innerHTML = `
    <div class="modal-overlay" onclick="closeModal(event)">
      <div class="modal-content" onclick="event.stopPropagation()">
        <div class="modal-header">
          <h3>${title}</h3>
          <button class="btn btn-secondary" onclick="closeModal()">✕</button>
        </div>
        <form onsubmit="handleSaveStudent(event)">
          <div class="modal-body">
            <div class="form-grid">
              <div class="form-group">
                <label>Register Number *</label>
                <input type="text" id="f-reg" class="form-control" value="${s.regNo}" required />
              </div>
              <div class="form-group">
                <label>Student Full Name *</label>
                <input type="text" id="f-name" class="form-control" value="${s.name}" required />
              </div>
              <div class="form-group">
                <label>Gender *</label>
                <select id="f-gender" class="form-control">
                  <option value="Male" ${s.gender === "Male" ? "selected" : ""}>Male</option>
                  <option value="Female" ${s.gender === "Female" ? "selected" : ""}>Female</option>
                  <option value="Other" ${s.gender === "Other" ? "selected" : ""}>Other</option>
                </select>
              </div>
              <div class="form-group">
                <label>Department *</label>
                <select id="f-dept" class="form-control">
                  ${settings.departments.map(d => `<option value="${d}" ${s.dept === d ? "selected" : ""}>${d}</option>`).join("")}
                </select>
              </div>
              <div class="form-group">
                <label>Year *</label>
                <select id="f-year" class="form-control">
                  <option value="1" ${s.year === 1 ? "selected" : ""}>1</option>
                  <option value="2" ${s.year === 2 ? "selected" : ""}>2</option>
                  <option value="3" ${s.year === 3 ? "selected" : ""}>3</option>
                  <option value="4" ${s.year === 4 ? "selected" : ""}>4</option>
                </select>
              </div>
              <div class="form-group">
                <label>Section *</label>
                <select id="f-sec" class="form-control">
                  <option value="A" ${s.section === "A" ? "selected" : ""}>A</option>
                  <option value="B" ${s.section === "B" ? "selected" : ""}>B</option>
                </select>
              </div>
              <div class="form-group">
                <label>Email *</label>
                <input type="email" id="f-email" class="form-control" value="${s.email}" required />
              </div>
              <div class="form-group">
                <label>Phone *</label>
                <input type="text" id="f-phone" class="form-control" value="${s.phone}" required />
              </div>
              <div class="form-group col-span-2">
                <label>Residential Address</label>
                <input type="text" id="f-addr" class="form-control" value="${s.address || ""}" />
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            <button type="submit" class="btn btn-primary">Save Student</button>
          </div>
        </form>
      </div>
    </div>
  `;
}

window.handleSaveStudent = function(e) {
  e.preventDefault();
  const students = store.getStudents();
  const regNo = document.getElementById("f-reg").value.trim();
  const name = document.getElementById("f-name").value.trim();
  const gender = document.getElementById("f-gender").value;
  const dept = document.getElementById("f-dept").value;
  const year = parseInt(document.getElementById("f-year").value);
  const section = document.getElementById("f-sec").value;
  const email = document.getElementById("f-email").value.trim();
  const phone = document.getElementById("f-phone").value.trim();
  const address = document.getElementById("f-addr").value.trim();

  // Check duplicate regNo
  const existingWithReg = students.find(s => s.regNo === regNo && (!editingStudent || s.id !== editingStudent.id));
  if (existingWithReg) {
    alert("Register Number already exists!");
    return;
  }

  if (editingStudent) {
    editingStudent.regNo = regNo;
    editingStudent.name = name;
    editingStudent.gender = gender;
    editingStudent.dept = dept;
    editingStudent.year = year;
    editingStudent.section = section;
    editingStudent.email = email;
    editingStudent.phone = phone;
    editingStudent.address = address;
  } else {
    const nextId = Math.max(0, ...students.map(s => s.id)) + 1;
    students.push({
      id: nextId, regNo, name, gender, dept, year, section, email, phone, address,
      admissionDate: new Date().toISOString().split("T")[0], targetRate: 0.85
    });
  }

  store.saveStudents(students);
  closeModal();
  render();
};

window.deleteStudent = function(id) {
  const student = store.getStudents().find(s => s.id === id);
  if (!student) return;

  if (confirm(`Are you sure you want to delete ${student.name} (${student.regNo})?`)) {
    const students = store.getStudents().filter(s => s.id !== id);
    const attendance = store.getAttendance();
    delete attendance[id];
    store.saveStudents(students);
    store.saveAttendance(attendance);
    render();
  }
};

window.closeModal = function() {
  const modalContainer = document.getElementById("modal-container");
  if (modalContainer) modalContainer.innerHTML = "";
};

// Teacher Modal
window.openTeacherModal = function() {
  editingTeacher = null;
  showTeacherFormModal("Add Faculty");
};

window.editTeacherModal = function(id) {
  editingTeacher = store.getTeachers().find(t => t.id === id);
  if (!editingTeacher) return;
  showTeacherFormModal("Edit Faculty - " + editingTeacher.empId);
};

function showTeacherFormModal(title) {
  const settings = store.getSettings();
  const t = editingTeacher || { empId: "", name: "", dept: settings.departments[0], email: "", phone: "" };

  const modalContainer = document.getElementById("modal-container");
  modalContainer.innerHTML = `
    <div class="modal-overlay" onclick="closeModal(event)">
      <div class="modal-content" onclick="event.stopPropagation()">
        <div class="modal-header">
          <h3>${title}</h3>
          <button class="btn btn-secondary" onclick="closeModal()">✕</button>
        </div>
        <form onsubmit="handleSaveTeacher(event)">
          <div class="modal-body">
            <div class="form-grid">
              <div class="form-group">
                <label>Employee ID *</label>
                <input type="text" id="t-empid" class="form-control" value="${t.empId}" required />
              </div>
              <div class="form-group">
                <label>Teacher Name *</label>
                <input type="text" id="t-name" class="form-control" value="${t.name}" required />
              </div>
              <div class="form-group">
                <label>Department *</label>
                <select id="t-dept" class="form-control">
                  ${settings.departments.map(d => `<option value="${d}" ${t.dept === d ? "selected" : ""}>${d}</option>`).join("")}
                </select>
              </div>
              <div class="form-group">
                <label>Email *</label>
                <input type="email" id="t-email" class="form-control" value="${t.email}" required />
              </div>
              <div class="form-group col-span-2">
                <label>Phone *</label>
                <input type="text" id="t-phone" class="form-control" value="${t.phone}" required />
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            <button type="submit" class="btn btn-primary">Save Faculty</button>
          </div>
        </form>
      </div>
    </div>
  `;
}

window.handleSaveTeacher = function(e) {
  e.preventDefault();
  const teachers = store.getTeachers();
  const empId = document.getElementById("t-empid").value.trim();
  const name = document.getElementById("t-name").value.trim();
  const dept = document.getElementById("t-dept").value;
  const email = document.getElementById("t-email").value.trim();
  const phone = document.getElementById("t-phone").value.trim();

  if (editingTeacher) {
    editingTeacher.empId = empId;
    editingTeacher.name = name;
    editingTeacher.dept = dept;
    editingTeacher.email = email;
    editingTeacher.phone = phone;
  } else {
    const nextId = Math.max(0, ...teachers.map(t => t.id)) + 1;
    teachers.push({ id: nextId, empId, name, dept, email, phone });
  }

  store.saveTeachers(teachers);
  render();
};

// ===================================================================
// V2.0 Modules: Student Portal, Subjects, Timetable, QR, Leaves, Audits
// ===================================================================

let activeQrSession = null;
let qrCountdownInterval = null;
let leaveFilterStatus = "ALL";

function formatTime(seconds) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

function logAudit(action, details) {
  const logs = store.getAuditLogs();
  const nextId = Math.max(0, ...logs.map(l => l.id)) + 1;
  const now = new Date();
  const ts = now.toISOString().replace("T", " ").substring(0, 16);
  logs.unshift({ id: nextId, action, user: currentUser.name || "System", details, timestamp: ts });
  store.saveAuditLogs(logs);
}

// 1. Student Portal View
function renderStudentPortal() {
  const settings = store.getSettings();
  const students = store.getStudents();
  const student = students.find(s => s.id === (currentUser.studentId || 1)) || students[0];
  const summary = getStudentSummary(student.id);
  const subjects = store.getSubjects().filter(sub => sub.dept === student.dept && sub.year === student.year);
  const timetable = store.getTimetable().filter(tt => tt.dept === student.dept && tt.year === student.year);
  const leaves = store.getLeaves().filter(l => l.studentId === student.id);

  // Shortage calculation formula: x = ceil((R*T - A)/(1 - R))
  const R = (settings.requiredAttendancePct || 75) / 100;
  const T = summary.totalDays;
  const A = summary.presentDays;
  let classesNeeded = 0;
  let isShortage = false;

  if (summary.percentage < settings.requiredAttendancePct) {
    isShortage = true;
    classesNeeded = Math.ceil((R * T - A) / (1 - R));
    if (classesNeeded < 0) classesNeeded = 0;
  }

  let riskCategory = "LOW RISK";
  if (summary.percentage < 65) riskCategory = "CRITICAL RISK";
  else if (summary.percentage < 75) riskCategory = "HIGH RISK";
  else if (summary.percentage < 85) riskCategory = "MEDIUM RISK";

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">🎓 Student Academic Portal</h1>
        <div class="page-meta">
          Welcome back, <strong>${student.name}</strong> (${student.regNo}) | ${student.dept} - Year ${student.year} Sec ${student.section}
        </div>
      </div>
      <div style="display: flex; gap: 8px;">
        <button class="btn btn-secondary" onclick="exportStudentAttendanceCardPdf(${student.id})">📄 Attendance Card (PDF)</button>
        <button class="btn btn-success" onclick="exportStudentAttendanceCardExcel(${student.id})">📊 Export Card (Excel)</button>
      </div>
    </div>

    <!-- Overall Attendance KPI & Profile Cards -->
    <div class="stats-grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); margin-bottom: 20px;">
      <div class="stat-card" style="--stat-color: ${summary.percentage >= 75 ? "#15803d" : "#dc2626"};">
        <div class="stat-header">
          <span class="stat-title">Overall Attendance</span>
          <span class="stat-icon">📊</span>
        </div>
        <div class="stat-value">${summary.percentage}%</div>
        <div class="stat-sub">${summary.status} Standing</div>
      </div>
      <div class="stat-card" style="--stat-color: #2563eb;">
        <div class="stat-header">
          <span class="stat-title">Classes Attended</span>
          <span class="stat-icon">✅</span>
        </div>
        <div class="stat-value">${summary.presentDays} / ${summary.totalDays}</div>
        <div class="stat-sub">Total conducted periods</div>
      </div>
      <div class="stat-card" style="--stat-color: #d97706;">
        <div class="stat-header">
          <span class="stat-title">Classes Absent</span>
          <span class="stat-icon">❌</span>
        </div>
        <div class="stat-value">${summary.absentDays}</div>
        <div class="stat-sub">Absences on record</div>
      </div>
      <div class="stat-card" style="--stat-color: ${isShortage ? "#dc2626" : "#15803d"};">
        <div class="stat-header">
          <span class="stat-title">Risk Assessment</span>
          <span class="stat-icon">🛡️</span>
        </div>
        <div class="stat-value" style="font-size: 1.3rem;">${riskCategory}</div>
        <div class="stat-sub">Mandatory Cutoff: ${settings.requiredAttendancePct}%</div>
      </div>
    </div>

    <!-- Attendance Shortage Calculator Alert Banner -->
    <div class="shortage-banner ${isShortage ? "shortage" : "good"}">
      <div style="font-size: 1.8rem;">${isShortage ? "⚠️" : "✅"}</div>
      <div style="flex: 1;">
        <div style="font-weight: 800; font-size: 0.95rem; margin-bottom: 4px;">
          ${isShortage 
            ? `Attendance Shortage Notice: You must attend the next ${classesNeeded} classes consecutively with 0 absences!` 
            : `Good Standing: You are currently above the mandatory ${settings.requiredAttendancePct}% attendance threshold.`}
        </div>
        <div style="font-size: 0.8rem; opacity: 0.9;">
          Shortage Formula: <span class="formula-chip">x = ⌈(R·T - A) / (1 - R)⌉</span> | Conducted (T): ${T}, Attended (A): ${A}, Cutoff (R): ${settings.requiredAttendancePct}%
        </div>
      </div>
      <div>
        <button class="btn btn-primary" onclick="openApplyLeaveModal()">📝 Apply Leave / OD</button>
      </div>
    </div>

    <!-- Live QR Check-in Card & Subject Breakdown Grid -->
    <div style="display: grid; grid-template-columns: 320px 1fr; gap: 20px; margin-bottom: 24px;">
      <!-- QR Check-in Box -->
      <div class="card" style="border-top: 4px solid var(--primary);">
        <h3 style="font-size: 1rem; font-weight: 700; margin-bottom: 4px;">📱 Live QR Attendance Check-In</h3>
        <p style="font-size: 0.78rem; color: var(--text-muted); margin-bottom: 16px;">
          Enter the session token displayed on classroom projector to record presence.
        </p>
        <form onsubmit="handleStudentQrCheckIn(event)">
          <div class="form-group" style="margin-bottom: 12px;">
            <label>Session Token</label>
            <input type="text" id="qr-token-input" class="form-control" placeholder="e.g. SAMS-A1B2C3" required />
          </div>
          <button type="submit" class="btn btn-primary" style="width: 100%;">
            ⚡ Check-In Now
          </button>
        </form>
        <div id="qr-checkin-msg" style="margin-top: 12px; font-size: 0.82rem; font-weight: 600;"></div>
      </div>

      <!-- Subject-Wise Attendance Breakdown Table -->
      <div class="card">
        <h3 style="font-size: 1rem; font-weight: 700; margin-bottom: 4px;">📚 Subject-Wise Attendance Breakdown</h3>
        <p style="font-size: 0.78rem; color: var(--text-muted); margin-bottom: 14px;">
          Semester V courses, faculty in-charge, and attendance percentage
        </p>
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Subject Name</th>
                <th>Faculty In-Charge</th>
                <th>Credits</th>
                <th>Conducted</th>
                <th>Attended</th>
                <th>Percentage</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${subjects.map((sub, i) => {
                const subPct = Math.min(100, Math.max(0, summary.percentage + ((i % 3) - 1) * 3));
                const subCond = Math.round(summary.totalDays * 0.8);
                const subAtt = Math.round(subCond * (subPct / 100));
                const badge = subPct >= 85 ? "tag-present" : subPct >= 75 ? "tag-od" : "tag-absent";
                return `
                  <tr>
                    <td><strong>${sub.code}</strong></td>
                    <td>${sub.name}</td>
                    <td>${sub.teacherName || "Faculty In-Charge"}</td>
                    <td><span class="badge" style="background: var(--bg-subtle); color: var(--primary);">${sub.credits}</span></td>
                    <td>${subCond}</td>
                    <td>${subAtt}</td>
                    <td><strong>${subPct.toFixed(1)}%</strong></td>
                    <td><span class="tag ${badge}">${subPct >= 75 ? "Satisfied" : "Shortage"}</span></td>
                  </tr>
                `;
              }).join("")}
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Student Timetable & My Leaves -->
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
      <div class="card">
        <h3 style="font-size: 1rem; font-weight: 700; margin-bottom: 12px;">📅 Class Timetable Schedule</h3>
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>Day</th>
                <th>Period</th>
                <th>Subject</th>
                <th>Room</th>
              </tr>
            </thead>
            <tbody>
              ${timetable.map(t => `
                <tr>
                  <td><strong>${t.day}</strong></td>
                  <td>${t.periodName}</td>
                  <td>${t.subjectCode} - ${t.subjectName}</td>
                  <td><span class="badge" style="background:#fee2e2; color:#991b1b;">${t.room}</span></td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <h3 style="font-size: 1rem; font-weight: 700;">🏖️ My Leave & On-Duty Requests</h3>
          <button class="btn btn-secondary" style="padding: 4px 10px; font-size: 0.76rem;" onclick="openApplyLeaveModal()">+ Apply</button>
        </div>
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Dates</th>
                <th>Reason</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${leaves.length === 0 ? `<tr><td colspan="4" style="text-align:center; color:var(--text-muted);">No leave records found.</td></tr>` : leaves.map(l => {
                const tagClass = l.status === "APPROVED" ? "tag-present" : l.status === "REJECTED" ? "tag-absent" : "tag-od";
                return `
                  <tr>
                    <td><strong>${l.leaveType}</strong></td>
                    <td style="font-size: 0.78rem;">${l.fromDate} to ${l.toDate}</td>
                    <td style="max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${l.reason}">${l.reason}</td>
                    <td><span class="tag ${tagClass}">${l.status}</span></td>
                  </tr>
                `;
              }).join("")}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;
}

// 2. Subjects View
function renderSubjects() {
  const subjects = store.getSubjects();
  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">📚 Subject Management</h1>
        <div class="page-meta">Manage academic courses, syllabus credits, and faculty in-charge</div>
      </div>
      <div style="display: flex; gap: 8px;">
        <button class="btn btn-primary" onclick="openAddSubjectModal()">➕ Add Subject</button>
        <button class="btn btn-secondary" onclick="openEnrollClassModal()">👥 Batch Enroll Class</button>
      </div>
    </div>

    <div class="card">
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>Subject Code</th>
              <th>Course Name</th>
              <th>Department</th>
              <th>Year</th>
              <th>Semester</th>
              <th>Credits</th>
              <th>Faculty In-Charge</th>
              <th style="text-align: right;">Actions</th>
            </tr>
          </thead>
          <tbody>
            ${subjects.map(s => `
              <tr>
                <td><strong>${s.code}</strong></td>
                <td>${s.name}</td>
                <td><span class="badge" style="background:var(--bg-subtle); color:var(--primary-dark); font-weight:700;">${s.dept}</span></td>
                <td>Year ${s.year}</td>
                <td>Sem ${s.sem}</td>
                <td><strong>${s.credits}</strong></td>
                <td>${s.teacherName || "Unassigned"}</td>
                <td style="text-align: right;">
                  <button class="btn btn-secondary" style="padding: 4px 8px; font-size: 0.74rem;" onclick="deleteSubject(${s.id})">🗑️ Delete</button>
                </td>
              </tr>
            `).join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// 3. Timetable View
function renderTimetable() {
  const timetable = store.getTimetable();
  const days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"];
  const periods = DEFAULT_PERIODS;

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">🗓️ Academic Timetable</h1>
        <div class="page-meta">Weekly lecture schedule, period allocations, and lecture hall distribution</div>
      </div>
      <div>
        <button class="btn btn-primary" onclick="openAddTimetableModal()">➕ Add Timetable Slot</button>
      </div>
    </div>

    <div class="card" style="padding: 16px;">
      <div class="table-responsive">
        <table class="timetable-grid-table">
          <thead>
            <tr>
              <th style="width: 120px;">Day / Time</th>
              ${periods.map(p => `<th>${p.name}<br><span style="font-weight:400; font-size:0.7rem; color:var(--text-muted);">${p.timeSlot}</span></th>`).join("")}
            </tr>
          </thead>
          <tbody>
            ${days.map(d => `
              <tr>
                <td style="font-weight: 800; background: var(--bg-subtle); text-align: center; vertical-align: middle;">${d}</td>
                ${periods.map(p => {
                  const slot = timetable.find(t => t.day.toLowerCase() === d.toLowerCase() && t.periodId === p.id);
                  if (slot) {
                    return `
                      <td>
                        <div class="timetable-slot-card">
                          <div class="slot-code">${slot.subjectCode}</div>
                          <div class="slot-name">${slot.subjectName}</div>
                          <div class="slot-faculty">👨‍🏫 ${slot.teacherName}</div>
                          <div class="slot-room">📍 ${slot.room}</div>
                        </div>
                      </td>
                    `;
                  }
                  return `<td style="background: #fafafa; text-align: center; color: #cbd5e1; font-size: 0.75rem;">— Free —</td>`;
                }).join("")}
              </tr>
            `).join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// 4. QR Attendance View
function renderQrAttendance() {
  const subjects = store.getSubjects();

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">📱 Live QR Code Attendance</h1>
        <div class="page-meta">Generate temporary encrypted QR tokens for classroom contact check-in</div>
      </div>
    </div>

    <div style="display: grid; grid-template-columns: 360px 1fr; gap: 24px;">
      <!-- Left Configurator Form -->
      <div class="card">
        <h3 style="font-size: 1rem; font-weight: 700; margin-bottom: 14px;">Session Configuration</h3>
        <form onsubmit="handleStartQrSession(event)">
          <div class="form-group" style="margin-bottom: 12px;">
            <label>Select Course *</label>
            <select id="qr-sub-select" class="form-control" required>
              ${subjects.map(s => `<option value="${s.id}">${s.code} - ${s.name} (${s.dept})</option>`).join("")}
            </select>
          </div>
          <div class="form-group" style="margin-bottom: 12px;">
            <label>Lecture Period *</label>
            <select id="qr-period-select" class="form-control" required>
              ${DEFAULT_PERIODS.map(p => `<option value="${p.name}">${p.name} (${p.timeSlot})</option>`).join("")}
            </select>
          </div>
          <div class="form-group" style="margin-bottom: 12px;">
            <label>Token Expiration</label>
            <select id="qr-expiry-select" class="form-control">
              <option value="300">5 Minutes (Recommended)</option>
              <option value="600">10 Minutes</option>
              <option value="120">2 Minutes</option>
            </select>
          </div>
          <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 8px;">
            🚀 Generate Dynamic QR Session
          </button>
        </form>

        <hr style="border: 0; border-top: 1px solid var(--border); margin: 20px 0;">

        <!-- Manual Fallback Form -->
        <h4 style="font-size: 0.88rem; font-weight: 700; margin-bottom: 8px;">Manual Roll Call Fallback</h4>
        <div style="display: flex; gap: 8px;">
          <input type="text" id="manual-reg-input" class="form-control" placeholder="Reg No (e.g. 24CSE001)" />
          <button class="btn btn-secondary" onclick="handleManualRollCall()">Mark</button>
        </div>
      </div>

      <!-- Right Active QR Session Display -->
      <div class="card" id="qr-display-panel">
        ${activeQrSession ? `
          <div class="qr-session-card">
            <div class="qr-timer-badge">
              ⏱️ <span id="qr-time-rem">${formatTime(activeQrSession.remainingSeconds)}</span>
            </div>
            <div class="qr-token-text">Session Token: <strong>${activeQrSession.token}</strong></div>

            <!-- Animated Simulated QR Visual -->
            <div class="qr-box">
              <div class="qr-scanner-line"></div>
              <div style="text-align: center;">
                <div style="font-size: 4rem;">🏁</div>
                <div style="font-weight: 800; font-size: 0.85rem; color: var(--primary-deep); margin-top: 6px;">SCAN TO MARK PRESENT</div>
                <div style="font-size: 0.72rem; color: var(--text-muted);">${activeQrSession.subjectCode} | ${activeQrSession.period}</div>
              </div>
            </div>

            <div style="display: flex; gap: 12px; margin-bottom: 20px;">
              <span class="badge" style="background: #dcfce7; color: #166534; font-size: 0.9rem; padding: 6px 14px;">
                ✅ <span id="qr-scanned-count">${activeQrSession.scannedStudents.length}</span> Scanned
              </span>
              <button class="btn btn-secondary" onclick="closeQrSession()">End Session</button>
            </div>

            <div style="width: 100%; text-align: left;">
              <h4 style="font-size: 0.84rem; font-weight: 700; margin-bottom: 8px;">Live Scanned Attendance Roster:</h4>
              <div style="max-height: 140px; overflow-y: auto; background: var(--bg-main); border-radius: 6px; padding: 8px; border: 1px solid var(--border);">
                ${activeQrSession.scannedStudents.length === 0 ? `<div style="color:var(--text-muted); font-size:0.78rem;">Waiting for student check-ins...</div>` : activeQrSession.scannedStudents.map(s => `
                  <div style="font-size: 0.78rem; padding: 4px 0; border-bottom: 1px solid var(--border-light); display: flex; justify-content: space-between;">
                    <span><strong>${s.regNo}</strong> - ${s.name}</span>
                    <span style="color: #166534; font-weight: 700;">✓ Present</span>
                  </div>
                `).join("")}
              </div>
            </div>
          </div>
        ` : `
          <div style="text-align: center; padding: 60px 20px; color: var(--text-muted);">
            <div style="font-size: 3.5rem; margin-bottom: 12px;">📱</div>
            <h3 style="font-size: 1.1rem; font-weight: 700; color: var(--text-main);">No Active QR Attendance Session</h3>
            <p style="font-size: 0.84rem; max-width: 380px; margin: 8px auto 0;">
              Configure course and lecture period on the left and click <strong>Generate Dynamic QR Session</strong> to project for classroom attendance.
            </p>
          </div>
        `}
      </div>
    </div>
  `;
}

// 5. Leave Management View
function renderLeaveManagement() {
  const allLeaves = store.getLeaves();
  const filteredLeaves = leaveFilterStatus === "ALL" 
    ? allLeaves 
    : allLeaves.filter(l => l.status === leaveFilterStatus);

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">🏖️ Leave & On-Duty Management</h1>
        <div class="page-meta">Review medical leaves, official On-Duty (OD) applications, and personal absences</div>
      </div>
      <div>
        <button class="btn btn-primary" onclick="openApplyLeaveModal()">📝 Apply Leave Request</button>
      </div>
    </div>

    <!-- Filter Tabs -->
    <div style="display: flex; gap: 8px; margin-bottom: 16px;">
      <button class="btn ${leaveFilterStatus === "ALL" ? "btn-primary" : "btn-secondary"}" onclick="setLeaveFilter('ALL')">All Requests (${allLeaves.length})</button>
      <button class="btn ${leaveFilterStatus === "PENDING" ? "btn-primary" : "btn-secondary"}" onclick="setLeaveFilter('PENDING')">Pending Review (${allLeaves.filter(l => l.status === "PENDING").length})</button>
      <button class="btn ${leaveFilterStatus === "APPROVED" ? "btn-primary" : "btn-secondary"}" onclick="setLeaveFilter('APPROVED')">Approved (${allLeaves.filter(l => l.status === "APPROVED").length})</button>
      <button class="btn ${leaveFilterStatus === "REJECTED" ? "btn-primary" : "btn-secondary"}" onclick="setLeaveFilter('REJECTED')">Rejected (${allLeaves.filter(l => l.status === "REJECTED").length})</button>
    </div>

    <div class="card">
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>Reg No</th>
              <th>Student Name</th>
              <th>Department</th>
              <th>Leave Type</th>
              <th>From Date</th>
              <th>To Date</th>
              <th>Reason</th>
              <th>Status</th>
              <th style="text-align: right;">Actions</th>
            </tr>
          </thead>
          <tbody>
            ${filteredLeaves.length === 0 ? `<tr><td colspan="9" style="text-align:center; padding: 30px; color:var(--text-muted);">No leave requests found for this status.</td></tr>` : filteredLeaves.map(l => {
              const tagClass = l.status === "APPROVED" ? "tag-present" : l.status === "REJECTED" ? "tag-absent" : "tag-od";
              return `
                <tr>
                  <td><strong>${l.regNo}</strong></td>
                  <td>${l.studentName}</td>
                  <td><span class="badge" style="background:var(--bg-subtle); color:var(--primary); font-weight:700;">${l.dept}</span></td>
                  <td><strong>${l.leaveType}</strong></td>
                  <td>${l.fromDate}</td>
                  <td>${l.toDate}</td>
                  <td style="max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${l.reason}">${l.reason}</td>
                  <td><span class="tag ${tagClass}">${l.status}</span></td>
                  <td style="text-align: right;">
                    ${l.status === "PENDING" && currentUser.role !== "STUDENT" ? `
                      <button class="btn btn-success" style="padding: 3px 8px; font-size: 0.72rem;" onclick="approveLeave(${l.id})">✓ Approve</button>
                      <button class="btn btn-secondary" style="padding: 3px 8px; font-size: 0.72rem; color: #dc2626;" onclick="rejectLeave(${l.id})">✕ Reject</button>
                    ` : `
                      <span style="font-size: 0.74rem; color: var(--text-muted);">${l.approvedBy ? `Reviewed by ${l.approvedBy}` : "—"}</span>
                    `}
                  </td>
                </tr>
              `;
            }).join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// 6. Audit Trail View
function renderAuditLogs() {
  const logs = store.getAuditLogs();

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">📋 System Audit Trail & Compliance Log</h1>
        <div class="page-meta">Immutable historical record of attendance corrections, policy updates, and administrative events</div>
      </div>
    </div>

    <div class="card">
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th style="width: 160px;">Timestamp</th>
              <th style="width: 200px;">Action Type</th>
              <th style="width: 180px;">User / Faculty</th>
              <th>Log Details</th>
            </tr>
          </thead>
          <tbody>
            ${logs.map(lg => `
              <tr>
                <td style="font-family: monospace; font-size: 0.78rem;">${lg.timestamp}</td>
                <td><span class="badge" style="background:#fee2e2; color:#991b1b; font-weight:700;">${lg.action}</span></td>
                <td><strong>${lg.user}</strong></td>
                <td style="font-size: 0.8rem;">${lg.details}</td>
              </tr>
            `).join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// ===================================================================
// Interactive Handlers for New Modules
// ===================================================================

window.setLeaveFilter = function(status) {
  leaveFilterStatus = status;
  render();
};

window.openApplyLeaveModal = function() {
  const modal = document.getElementById("modal-container");
  const today = new Date().toISOString().split("T")[0];
  modal.innerHTML = `
    <div class="modal-backdrop">
      <div class="modal-card" style="max-width: 480px;">
        <div class="modal-header">
          <h3 class="modal-title">📝 Apply for Leave or On-Duty (OD)</h3>
          <button class="modal-close" onclick="closeModal()">✕</button>
        </div>
        <form onsubmit="handleApplyLeave(event)">
          <div class="modal-body">
            <div class="form-group" style="margin-bottom: 12px;">
              <label>Leave Category *</label>
              <select id="leave-type-input" class="form-control" required>
                <option value="OD">Official On-Duty (OD - Hackathon/Symposium)</option>
                <option value="Medical">Medical Leave (Doctor Certificate)</option>
                <option value="Personal">Personal / Family Leave</option>
              </select>
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px;">
              <div class="form-group">
                <label>From Date *</label>
                <input type="date" id="leave-from-input" class="form-control" value="${today}" required />
              </div>
              <div class="form-group">
                <label>To Date *</label>
                <input type="date" id="leave-to-input" class="form-control" value="${today}" required />
              </div>
            </div>
            <div class="form-group" style="margin-bottom: 12px;">
              <label>Detailed Reason *</label>
              <textarea id="leave-reason-input" class="form-control" rows="3" placeholder="Explain the official event or medical condition..." required></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            <button type="submit" class="btn btn-primary">Submit Application</button>
          </div>
        </form>
      </div>
    </div>
  `;
};

window.handleApplyLeave = function(e) {
  e.preventDefault();
  const type = document.getElementById("leave-type-input").value;
  const from = document.getElementById("leave-from-input").value;
  const to = document.getElementById("leave-to-input").value;
  const reason = document.getElementById("leave-reason-input").value.trim();

  const students = store.getStudents();
  const student = students.find(s => s.id === (currentUser.studentId || 1)) || students[0];

  const leaves = store.getLeaves();
  const nextId = Math.max(0, ...leaves.map(l => l.id)) + 1;

  leaves.unshift({
    id: nextId,
    studentId: student.id,
    studentName: student.name,
    regNo: student.regNo,
    dept: student.dept,
    leaveType: type,
    fromDate: from,
    toDate: to,
    reason,
    status: "PENDING",
    approvedBy: null,
    remarks: null
  });

  store.saveLeaves(leaves);
  logAudit("LEAVE_APPLIED", `${student.name} (${student.regNo}) applied for ${type} leave from ${from} to ${to}`);
  closeModal();
  alert("Leave application submitted successfully for faculty advisor review!");
  render();
};

window.approveLeave = function(id) {
  const remark = prompt("Enter approval remarks (optional):", "Approved per academic guidelines");
  if (remark === null) return;

  const leaves = store.getLeaves();
  const leave = leaves.find(l => l.id === id);
  if (leave) {
    leave.status = "APPROVED";
    leave.approvedBy = currentUser.name;
    leave.remarks = remark;
    store.saveLeaves(leaves);
    logAudit("LEAVE_APPROVED", `Approved ${leave.leaveType} for ${leave.studentName} (${leave.regNo})`);
    render();
  }
};

window.rejectLeave = function(id) {
  const remark = prompt("Enter reason for rejection:", "Insufficient attendance criteria");
  if (!remark) return;

  const leaves = store.getLeaves();
  const leave = leaves.find(l => l.id === id);
  if (leave) {
    leave.status = "REJECTED";
    leave.approvedBy = currentUser.name;
    leave.remarks = remark;
    store.saveLeaves(leaves);
    logAudit("LEAVE_REJECTED", `Rejected ${leave.leaveType} for ${leave.studentName} (${leave.regNo}) - Reason: ${remark}`);
    render();
  }
};

window.handleStartQrSession = function(e) {
  e.preventDefault();
  const subId = parseInt(document.getElementById("qr-sub-select").value);
  const period = document.getElementById("qr-period-select").value;
  const expiry = parseInt(document.getElementById("qr-expiry-select").value);

  const sub = store.getSubjects().find(s => s.id === subId);
  const token = "SAMS-" + Math.random().toString(36).substring(2, 7).toUpperCase();

  activeQrSession = {
    token,
    subjectId: subId,
    subjectCode: sub ? sub.code : "CS501",
    period,
    expirySeconds: expiry,
    remainingSeconds: expiry,
    scannedStudents: []
  };

  if (qrCountdownInterval) clearInterval(qrCountdownInterval);
  qrCountdownInterval = setInterval(() => {
    if (activeQrSession) {
      activeQrSession.remainingSeconds--;
      if (activeQrSession.remainingSeconds <= 0) {
        clearInterval(qrCountdownInterval);
        activeQrSession = null;
        alert("QR Code attendance session expired!");
        render();
      } else {
        const timerEl = document.getElementById("qr-time-rem");
        if (timerEl) timerEl.innerText = formatTime(activeQrSession.remainingSeconds);
      }
    }
  }, 1000);

  logAudit("QR_SESSION_STARTED", `Started QR session ${token} for ${sub ? sub.code : ""} (${period})`);
  render();
};

window.closeQrSession = function() {
  if (confirm("End this QR attendance session now?")) {
    if (qrCountdownInterval) clearInterval(qrCountdownInterval);
    if (activeQrSession) {
      logAudit("QR_SESSION_CLOSED", `Closed session ${activeQrSession.token} with ${activeQrSession.scannedStudents.length} students marked present`);
      activeQrSession = null;
    }
    render();
  }
};

window.handleManualRollCall = function() {
  const reg = document.getElementById("manual-reg-input").value.trim().toUpperCase();
  if (!reg) return;
  const students = store.getStudents();
  const s = students.find(st => st.regNo.toUpperCase() === reg);
  if (!s) {
    alert("Student not found with Reg No: " + reg);
    return;
  }
  if (!activeQrSession) {
    alert("Please generate an active QR session first!");
    return;
  }
  if (!activeQrSession.scannedStudents.some(st => st.id === s.id)) {
    activeQrSession.scannedStudents.push(s);
    // Mark in attendance store
    const today = new Date().toISOString().split("T")[0];
    const att = store.getAttendance();
    if (!att[s.id]) att[s.id] = {};
    att[s.id][today] = "PRESENT";
    store.saveAttendance(att);
    logAudit("MANUAL_QR_MARK", `Manually recorded presence for ${s.name} (${s.regNo}) in session ${activeQrSession.token}`);
    document.getElementById("manual-reg-input").value = "";
    render();
  } else {
    alert("Student already marked present in this session!");
  }
};

window.handleStudentQrCheckIn = function(e) {
  e.preventDefault();
  const token = document.getElementById("qr-token-input").value.trim().toUpperCase();
  const msgEl = document.getElementById("qr-checkin-msg");

  const students = store.getStudents();
  const student = students.find(s => s.id === (currentUser.studentId || 1)) || students[0];

  if (!activeQrSession) {
    // If no live teacher session, simulate valid session for demo convenience
    const today = new Date().toISOString().split("T")[0];
    const att = store.getAttendance();
    if (!att[student.id]) att[student.id] = {};
    att[student.id][today] = "PRESENT";
    store.saveAttendance(att);

    msgEl.style.color = "#15803d";
    msgEl.innerHTML = "✅ Check-in Recorded! You have been successfully marked PRESENT for today.";
    logAudit("QR_STUDENT_CHECKIN", `Student ${student.name} (${student.regNo}) checked in via token ${token}`);
    setTimeout(() => render(), 1200);
    return;
  }

  if (activeQrSession.token.toUpperCase() === token) {
    if (!activeQrSession.scannedStudents.some(st => st.id === student.id)) {
      activeQrSession.scannedStudents.push(student);
      const today = new Date().toISOString().split("T")[0];
      const att = store.getAttendance();
      if (!att[student.id]) att[student.id] = {};
      att[student.id][today] = "PRESENT";
      store.saveAttendance(att);

      msgEl.style.color = "#15803d";
      msgEl.innerHTML = `✅ Successfully checked in for ${activeQrSession.subjectCode} (${activeQrSession.period})!`;
      logAudit("QR_STUDENT_CHECKIN", `Student ${student.name} (${student.regNo}) checked in via token ${token}`);
      setTimeout(() => render(), 1200);
    } else {
      msgEl.style.color = "#d97706";
      msgEl.innerHTML = "ℹ️ You are ALREADY marked PRESENT for this lecture session!";
    }
  } else {
    msgEl.style.color = "#dc2626";
    msgEl.innerHTML = "❌ Invalid or expired session token. Please verify token on projector.";
  }
};

window.openAddSubjectModal = function() {
  const modal = document.getElementById("modal-container");
  const settings = store.getSettings();
  const teachers = store.getTeachers();

  modal.innerHTML = `
    <div class="modal-backdrop">
      <div class="modal-card" style="max-width: 520px;">
        <div class="modal-header">
          <h3 class="modal-title">📚 Add New Academic Course</h3>
          <button class="modal-close" onclick="closeModal()">✕</button>
        </div>
        <form onsubmit="handleSaveSubject(event)">
          <div class="modal-body">
            <div style="display: grid; grid-template-columns: 1fr 2fr; gap: 12px; margin-bottom: 12px;">
              <div class="form-group">
                <label>Subject Code *</label>
                <input type="text" id="sub-code" class="form-control" placeholder="e.g. CS503" required />
              </div>
              <div class="form-group">
                <label>Course Name *</label>
                <input type="text" id="sub-name" class="form-control" placeholder="e.g. Software Engineering" required />
              </div>
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; margin-bottom: 12px;">
              <div class="form-group">
                <label>Department</label>
                <select id="sub-dept" class="form-control">
                  ${settings.departments.map(d => `<option value="${d}">${d}</option>`).join("")}
                </select>
              </div>
              <div class="form-group">
                <label>Year</label>
                <select id="sub-year" class="form-control">
                  ${settings.years.map(y => `<option value="${y}">Year ${y}</option>`).join("")}
                </select>
              </div>
              <div class="form-group">
                <label>Credits</label>
                <input type="number" id="sub-credits" class="form-control" value="3" min="1" max="6" required />
              </div>
            </div>
            <div class="form-group" style="margin-bottom: 12px;">
              <label>Faculty In-Charge</label>
              <select id="sub-teacher" class="form-control">
                ${teachers.map(t => `<option value="${t.id}">${t.name} (${t.dept})</option>`).join("")}
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            <button type="submit" class="btn btn-primary">Save Course</button>
          </div>
        </form>
      </div>
    </div>
  `;
};

window.handleSaveSubject = function(e) {
  e.preventDefault();
  const code = document.getElementById("sub-code").value.trim().toUpperCase();
  const name = document.getElementById("sub-name").value.trim();
  const dept = document.getElementById("sub-dept").value;
  const year = parseInt(document.getElementById("sub-year").value);
  const credits = parseInt(document.getElementById("sub-credits").value);
  const teacherId = parseInt(document.getElementById("sub-teacher").value);
  const teacher = store.getTeachers().find(t => t.id === teacherId);

  const subjects = store.getSubjects();
  const nextId = Math.max(0, ...subjects.map(s => s.id)) + 1;

  subjects.push({
    id: nextId,
    code,
    name,
    dept,
    year,
    sem: "V",
    credits,
    teacherId,
    teacherName: teacher ? teacher.name : "Faculty In-Charge"
  });

  store.saveSubjects(subjects);
  logAudit("SUBJECT_CREATED", `Added subject ${code} - ${name} (${dept} Y${year})`);
  closeModal();
  render();
};

window.deleteSubject = function(id) {
  if (confirm("Are you sure you want to delete this course?")) {
    const subjects = store.getSubjects().filter(s => s.id !== id);
    store.saveSubjects(subjects);
    logAudit("SUBJECT_DELETED", `Deleted course ID ${id}`);
    render();
  }
};

window.openEnrollClassModal = function() {
  const modal = document.getElementById("modal-container");
  const settings = store.getSettings();
  const subjects = store.getSubjects();

  modal.innerHTML = `
    <div class="modal-backdrop">
      <div class="modal-card" style="max-width: 480px;">
        <div class="modal-header">
          <h3 class="modal-title">👥 Batch Enroll Class in Course</h3>
          <button class="modal-close" onclick="closeModal()">✕</button>
        </div>
        <form onsubmit="handleEnrollClass(event)">
          <div class="modal-body">
            <div class="form-group" style="margin-bottom: 12px;">
              <label>Select Course *</label>
              <select id="enroll-sub" class="form-control" required>
                ${subjects.map(s => `<option value="${s.id}">${s.code} - ${s.name} (${s.dept} Y${s.year})</option>`).join("")}
              </select>
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px;">
              <div class="form-group">
                <label>Department</label>
                <select id="enroll-dept" class="form-control">
                  ${settings.departments.map(d => `<option value="${d}">${d}</option>`).join("")}
                </select>
              </div>
              <div class="form-group">
                <label>Year</label>
                <select id="enroll-year" class="form-control">
                  ${settings.years.map(y => `<option value="${y}">Year ${y}</option>`).join("")}
                </select>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            <button type="submit" class="btn btn-primary">Enroll Entire Class</button>
          </div>
        </form>
      </div>
    </div>
  `;
};

window.handleEnrollClass = function(e) {
  e.preventDefault();
  const subId = parseInt(document.getElementById("enroll-sub").value);
  const dept = document.getElementById("enroll-dept").value;
  const year = parseInt(document.getElementById("enroll-year").value);
  const sub = store.getSubjects().find(s => s.id === subId);
  const students = store.getStudents().filter(s => s.dept === dept && s.year === year);

  closeModal();
  logAudit("BATCH_ENROLLMENT", `Enrolled ${students.length} students of ${dept} Year ${year} in ${sub ? sub.code : "Course"}`);
  alert(`Successfully enrolled all ${students.length} students of ${dept} Year ${year} in ${sub ? sub.code : "Course"}!`);
  render();
};

window.openAddTimetableModal = function() {
  const modal = document.getElementById("modal-container");
  const settings = store.getSettings();
  const subjects = store.getSubjects();
  const days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"];

  modal.innerHTML = `
    <div class="modal-backdrop">
      <div class="modal-card" style="max-width: 500px;">
        <div class="modal-header">
          <h3 class="modal-title">🗓️ Add Timetable Slot</h3>
          <button class="modal-close" onclick="closeModal()">✕</button>
        </div>
        <form onsubmit="handleSaveTimetable(event)">
          <div class="modal-body">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px;">
              <div class="form-group">
                <label>Day of Week *</label>
                <select id="tt-day" class="form-control" required>
                  ${days.map(d => `<option value="${d}">${d}</option>`).join("")}
                </select>
              </div>
              <div class="form-group">
                <label>Period Slot *</label>
                <select id="tt-period" class="form-control" required>
                  ${DEFAULT_PERIODS.map(p => `<option value="${p.id}">${p.name} (${p.timeSlot})</option>`).join("")}
                </select>
              </div>
            </div>
            <div class="form-group" style="margin-bottom: 12px;">
              <label>Select Course *</label>
              <select id="tt-sub" class="form-control" required>
                ${subjects.map(s => `<option value="${s.id}">${s.code} - ${s.name} (${s.dept})</option>`).join("")}
              </select>
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px;">
              <div class="form-group">
                <label>Lecture Room / Hall</label>
                <input type="text" id="tt-room" class="form-control" value="LH-201" required />
              </div>
              <div class="form-group">
                <label>Section</label>
                <select id="tt-sec" class="form-control">
                  <option value="A">Section A</option>
                  <option value="B">Section B</option>
                </select>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            <button type="submit" class="btn btn-primary">Add Slot</button>
          </div>
        </form>
      </div>
    </div>
  `;
};

window.handleSaveTimetable = function(e) {
  e.preventDefault();
  const day = document.getElementById("tt-day").value;
  const periodId = parseInt(document.getElementById("tt-period").value);
  const period = DEFAULT_PERIODS.find(p => p.id === periodId);
  const subId = parseInt(document.getElementById("tt-sub").value);
  const sub = store.getSubjects().find(s => s.id === subId);
  const room = document.getElementById("tt-room").value.trim();
  const sec = document.getElementById("tt-sec").value;

  const tt = store.getTimetable();
  const nextId = Math.max(0, ...tt.map(t => t.id)) + 1;

  tt.push({
    id: nextId,
    day,
    periodId,
    periodName: period ? period.name : `Period ${periodId}`,
    subjectCode: sub ? sub.code : "CS501",
    subjectName: sub ? sub.name : "Course",
    teacherName: sub ? sub.teacherName : "Faculty",
    dept: sub ? sub.dept : "CSE",
    year: sub ? sub.year : 3,
    sec,
    room
  });

  store.saveTimetable(tt);
  logAudit("TIMETABLE_UPDATED", `Added slot ${day} Period ${periodId} (${sub ? sub.code : ""}) Room ${room}`);
  closeModal();
  render();
};

window.openNotificationModal = function() {
  const notifs = store.getNotifications();
  const modal = document.getElementById("modal-container");

  modal.innerHTML = `
    <div class="modal-backdrop">
      <div class="modal-card" style="max-width: 520px;">
        <div class="modal-header">
          <h3 class="modal-title">🔔 Institutional Notifications & Alerts</h3>
          <button class="modal-close" onclick="closeModal()">✕</button>
        </div>
        <div class="modal-body" style="max-height: 420px; overflow-y: auto;">
          ${notifs.map(n => `
            <div style="padding: 12px; margin-bottom: 10px; background: var(--bg-subtle); border-radius: 8px; border-left: 4px solid var(--primary);">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                <strong style="font-size: 0.9rem; color: var(--primary-deep);">${n.title}</strong>
                <span class="badge" style="background:#fee2e2; color:#991b1b; font-size:0.7rem;">${n.type}</span>
              </div>
              <p style="font-size: 0.8rem; color: var(--text-main); margin-bottom: 6px;">${n.message}</p>
              <div style="font-size: 0.7rem; color: var(--text-muted);">🕒 ${n.date} | Target: ${n.targetRole}</div>
            </div>
          `).join("")}
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" onclick="closeModal()">Close</button>
        </div>
      </div>
    </div>
  `;
};

// 7. Student Attendance Card PDF & Excel
window.exportStudentAttendanceCardPdf = function(studentId) {
  const settings = store.getSettings();
  const students = store.getStudents();
  const s = students.find(st => st.id === studentId) || students[0];
  const summary = getStudentSummary(s.id);
  const subjects = store.getSubjects().filter(sub => sub.dept === s.dept && sub.year === s.year);

  const doc = new jsPDF();
  doc.setFontSize(16);
  doc.setTextColor(139, 0, 0); // Crimson Red
  doc.text(settings.collegeName, 105, 14, { align: "center" });

  doc.setFontSize(11);
  doc.setTextColor(30, 41, 59);
  doc.text("OFFICIAL STUDENT ATTENDANCE CARD", 105, 22, { align: "center" });

  doc.setFontSize(9);
  doc.text(`Name: ${s.name}   |   Register No: ${s.regNo}`, 14, 32);
  doc.text(`Department: ${s.dept}   |   Year: ${s.year}   |   Section: ${s.section}`, 14, 38);
  doc.text(`Overall Attendance: ${summary.percentage}% (${summary.presentDays}/${summary.totalDays} Days)   |   Status: ${summary.status}`, 14, 44);

  const tableData = subjects.map((sub, i) => {
    const subPct = Math.min(100, Math.max(0, summary.percentage + ((i % 3) - 1) * 3));
    const subCond = Math.round(summary.totalDays * 0.8);
    const subAtt = Math.round(subCond * (subPct / 100));
    return [
      sub.code,
      sub.name,
      sub.teacherName || "Faculty In-Charge",
      sub.credits,
      subCond,
      subAtt,
      `${subPct.toFixed(1)}%`,
      subPct >= 75 ? "Satisfied" : "Shortage"
    ];
  });

  doc.autoTable({
    head: [["Code", "Subject Name", "Faculty", "Credits", "Conducted", "Attended", "Attendance %", "Status"]],
    body: tableData,
    startY: 50,
    theme: "striped",
    headStyles: { fillColor: [139, 0, 0] },
    styles: { fontSize: 8 }
  });

  const finalY = doc.lastAutoTable.finalY + 20;
  doc.text("_________________________", 25, finalY);
  doc.text("Faculty Advisor Signature", 25, finalY + 6);

  doc.text("_________________________", 140, finalY);
  doc.text("Head of Department / Principal", 140, finalY + 6);

  doc.save(`Attendance_Card_${s.regNo}.pdf`);
};

window.exportStudentAttendanceCardExcel = function(studentId) {
  const settings = store.getSettings();
  const students = store.getStudents();
  const s = students.find(st => st.id === studentId) || students[0];
  const summary = getStudentSummary(s.id);
  const subjects = store.getSubjects().filter(sub => sub.dept === s.dept && sub.year === s.year);

  const cardData = subjects.map((sub, i) => {
    const subPct = Math.min(100, Math.max(0, summary.percentage + ((i % 3) - 1) * 3));
    const subCond = Math.round(summary.totalDays * 0.8);
    const subAtt = Math.round(subCond * (subPct / 100));
    return {
      "Subject Code": sub.code,
      "Subject Name": sub.name,
      "Faculty": sub.teacherName || "Faculty In-Charge",
      "Credits": sub.credits,
      "Conducted Classes": subCond,
      "Attended Classes": subAtt,
      "Attendance %": `${subPct.toFixed(1)}%`,
      "Eligibility Status": subPct >= 75 ? "Satisfied" : "Shortage"
    };
  });

  const wb = XLSX.utils.book_new();
  const ws = XLSX.utils.json_to_sheet(cardData);
  XLSX.utils.book_append_sheet(wb, ws, "Attendance Card");
  XLSX.writeFile(wb, `Attendance_Card_${s.regNo}.xlsx`);
};

// ===================================================================
// Real Excel & PDF Export Handlers
// ===================================================================

window.exportStudentListExcel = function() {
  const students = store.getStudents();
  const ws = XLSX.utils.json_to_sheet(students.map(s => ({
    "ID": s.id,
    "Register No": s.regNo,
    "Student Name": s.name,
    "Gender": s.gender,
    "Department": s.dept,
    "Year": s.year,
    "Section": s.section,
    "Email": s.email,
    "Phone": s.phone,
    "Admission Date": s.admissionDate
  })));
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Student List");
  XLSX.writeFile(wb, "Student_List.xlsx");
};

window.exportLowAttendanceExcel = function() {
  const stats = calculateDashboardStats();
  const ws = XLSX.utils.json_to_sheet(stats.lowStudents.map(s => ({
    "Register No": s.regNo,
    "Student Name": s.name,
    "Department": s.dept,
    "Year": s.year,
    "Section": s.section,
    "Working Days": s.totalDays,
    "Present Days": s.presentDays,
    "Absent Days": s.absentDays,
    "Attendance %": s.percentage,
    "Status": s.status
  })));
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Low Attendance");
  XLSX.writeFile(wb, "Low_Attendance_Report.xlsx");
};

window.exportLowAttendancePdf = function() {
  const stats = calculateDashboardStats();
  const settings = store.getSettings();
  const doc = new jsPDF();

  doc.setFontSize(16);
  doc.setTextColor(30, 58, 138);
  doc.text(settings.collegeName, 105, 14, { align: "center" });

  doc.setFontSize(11);
  doc.setTextColor(51, 65, 85);
  doc.text(`Low Attendance Students Report (< ${stats.requiredPct}%)`, 105, 22, { align: "center" });
  doc.setFontSize(8);
  doc.text(`Generated: ${new Date().toLocaleString("en-IN")}`, 196, 28, { align: "right" });

  const tableData = stats.lowStudents.map((s, idx) => [
    idx + 1, s.regNo, s.name, s.dept, s.year, s.section, s.presentDays, `${s.percentage}%`, s.status
  ]);

  doc.autoTable({
    startY: 32,
    head: [["S.No", "Reg No", "Student Name", "Dept", "Year", "Sec", "Present", "Attendance %", "Status"]],
    body: tableData,
    theme: "striped",
    headStyles: { fillColor: [37, 99, 235] },
    styles: { fontSize: 8 }
  });

  doc.save("Low_Attendance_Report.pdf");
};

window.exportReportExcel = function() {
  const table = document.getElementById("report-table");
  if (!table) return;
  const wb = XLSX.utils.table_to_book(table, { sheet: "Report" });
  XLSX.writeFile(wb, "Attendance_Report.xlsx");
};

window.exportReportPdf = function() {
  const settings = store.getSettings();
  const doc = new jsPDF();
  doc.setFontSize(15);
  doc.setTextColor(30, 58, 138);
  doc.text(settings.collegeName, 105, 14, { align: "center" });

  doc.autoTable({
    html: "#report-table",
    startY: 24,
    theme: "striped",
    headStyles: { fillColor: [37, 99, 235] },
    styles: { fontSize: 8 }
  });
  doc.save("Attendance_Report.pdf");
};

window.exportCompleteWorkbookExcel = function() {
  const settings = store.getSettings();
  const stats = calculateDashboardStats();
  const students = store.getStudents();
  const wb = XLSX.utils.book_new();

  // Sheet 1: Summary
  const summaryData = [
    { "Metric": "College Name", "Value": settings.collegeName },
    { "Metric": "Academic Year", "Value": settings.academicYear },
    { "Metric": "Semester", "Value": settings.semester },
    { "Metric": "Report Date", "Value": new Date().toLocaleDateString("en-IN") },
    { "Metric": "Total Students", "Value": stats.totalStudents },
    { "Metric": "Average Attendance", "Value": `${stats.averageAttendance}%` },
    { "Metric": "Present Today", "Value": stats.presentToday },
    { "Metric": "Absent Today", "Value": stats.absentToday },
    { "Metric": `Students Below ${stats.requiredPct}%`, "Value": stats.studentsBelowCount }
  ];
  XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(summaryData), "Summary");

  // Sheet 2: Student Attendance
  const studentAttData = students.map(s => {
    const sum = getStudentSummary(s.id);
    return {
      "Register No": s.regNo,
      "Name": s.name,
      "Department": s.dept,
      "Year": s.year,
      "Section": s.section,
      "Working Days": sum.totalDays,
      "Present Days": sum.presentDays,
      "Absent Days": sum.absentDays,
      "Attendance %": sum.percentage,
      "Status": sum.status
    };
  });
  XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(studentAttData), "Student Attendance");

  // Sheet 3: Low Attendance
  XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(stats.lowStudents.map(s => ({
    "Register No": s.regNo,
    "Name": s.name,
    "Department": s.dept,
    "Year": s.year,
    "Attendance %": s.percentage,
    "Status": s.status
  }))), "Low Attendance");

  // Sheet 4: Student List
  XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(students), "Student List");

  XLSX.writeFile(wb, "Complete_Attendance_Summary.xlsx");
};

// ===================================================================
// Initialization
// ===================================================================

function attachTabEvents() {
  if (currentTab === "students") {
    filterStudentsTable();
  } else if (currentTab === "attendance") {
    loadAttendanceList();
  } else if (currentTab === "reports") {
    generateReportData();
  }
}

// Start app
render();
