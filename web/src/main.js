import { 
  DEFAULT_SETTINGS, 
  DEFAULT_TEACHERS, 
  generateInitialStudents, 
  generate40WorkingDays, 
  generateInitialAttendance 
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

  app.innerHTML = `
    <div id="app-container">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <div class="sidebar-brand-badge">🏛️ KIT COLLEGE</div>
          <div class="sidebar-title">KIT College</div>
          <div class="sidebar-subtitle">Student Attendance Portal</div>
        </div>
        <div class="nav-section-title">Navigation</div>
        <ul class="nav-list">
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
            <button class="${currentTab === "reports" ? "active" : ""}" onclick="switchTab('reports')">
              📑 Reports
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
          <p>Student Attendance Management Portal</p>
        </div>
        <form id="login-form" onsubmit="handleLogin(event)">
          <div class="form-group" style="margin-bottom: 16px;">
            <label>Username / ID</label>
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
              <button type="button" class="btn btn-secondary" onclick="setDemo('admin', 'admin123')">Admin Mode</button>
              <button type="button" class="btn btn-secondary" onclick="setDemo('teacher', 'teacher123')">Faculty Mode</button>
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
    currentUser = { username: "teacher", name: "Prof. Rajesh Sharma", role: "TEACHER" };
    currentTab = "dashboard";
    render();
  } else {
    alert("Invalid credentials! Try admin/admin123 or teacher/teacher123");
  }
};

// ===================================================================
// Tab Content Renderers
// ===================================================================

function renderTabContent() {
  switch (currentTab) {
    case "dashboard": return renderDashboard();
    case "students": return renderStudents();
    case "attendance": return renderAttendance();
    case "reports": return renderReports();
    case "teachers": return renderTeachers();
    case "settings": return renderSettings();
    default: return renderDashboard();
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
  closeModal();
  render();
};

window.deleteTeacher = function(id) {
  if (confirm("Are you sure you want to delete this faculty member?")) {
    const teachers = store.getTeachers().filter(t => t.id !== id);
    store.saveTeachers(teachers);
    render();
  }
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
