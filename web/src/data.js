// ===================================================================
// Default Demo Seed Data for Student Attendance Management System Web
// ===================================================================

export const DEFAULT_SETTINGS = {
  collegeName: "KIT ENGINEERING COLLEGE",
  academicYear: "2026-27",
  semester: "V",
  requiredAttendancePct: 75,
  workingDaysTarget: 60,
  departments: ["CSE", "IT", "AI&DS", "ECE", "EEE", "MECH"],
  years: [1, 2, 3, 4],
  sections: ["A", "B"]
};

export const DEFAULT_TEACHERS = [
  { id: 1, empId: "EMP101", name: "Dr. R. Ramanathan", dept: "CSE", email: "ramanathan.r@kit.edu.in", phone: "9840123451" },
  { id: 2, empId: "EMP102", name: "Dr. S. Gayathri", dept: "IT", email: "gayathri.s@kit.edu.in", phone: "9840123452" },
  { id: 3, empId: "EMP103", name: "Prof. K. Venkatesh", dept: "AI&DS", email: "venkatesh.k@kit.edu.in", phone: "9840123453" },
  { id: 4, empId: "EMP104", name: "Dr. P. Meenakshi", dept: "ECE", email: "meenakshi.p@kit.edu.in", phone: "9840123454" },
  { id: 5, empId: "EMP105", name: "Prof. M. Suresh", dept: "EEE", email: "suresh.m@kit.edu.in", phone: "9840123455" },
  { id: 6, empId: "EMP106", name: "Dr. T. Revathi", dept: "MECH", email: "revathi.t@kit.edu.in", phone: "9840123456" }
];

export function generateInitialStudents() {
  const maleNames = [
    "Aarav", "Arjun", "Aditya", "Rohan", "Karthik", "Siddharth", "Varun", "Rahul",
    "Kavin", "Gowtham", "Vikram", "Pranav", "Harish", "Sai", "Manoj", "Dinesh",
    "Surya", "Naveen", "Ashwin", "Vishal", "Vijay", "Anand", "Deepak", "Ganesh",
    "Madhav", "Nikhil", "Praveen", "Rishi", "Sanjay", "Tarun", "Ajay", "Alok",
    "Amit", "Bala", "Bhuvnesh", "Charan", "Dev", "Dhruv", "Girish", "Hemant",
    "Jitendra", "Kishore", "Lokesh", "Manish", "Mayank", "Mohit", "Mukesh", "Nitesh",
    "Pradeep", "Raghav"
  ];

  const femaleNames = [
    "Ananya", "Priya", "Sneha", "Divya", "Pooja", "Meera", "Swetha", "Nithya",
    "Deepa", "Kavya", "Keerthana", "Shreya", "Rhea", "Sandhya", "Shruthi", "Lakshmi",
    "Harini", "Pavithra", "Aishwarya", "Janani", "Ishwarya", "Bhavana", "Lavanya", "Malini",
    "Preethi", "Roshni", "Swathi", "Vaishnavi", "Vidhya", "Yamini", "Anitha", "Aparna",
    "Archana", "Charulatha", "Geetha", "Hema", "Indira", "Kalyani", "Madhumitha", "Nandhini",
    "Padma", "Radhika", "Rekha", "Revathi", "Sangeetha", "Saranya", "Shobana", "Sowmya",
    "Sujatha", "Vandana"
  ];

  const lastNames = [
    "Kumar", "Raj", "Iyer", "Nair", "Patel", "Sharma", "Verma", "Reddy",
    "Sundaram", "Krishnan", "Deshmukh", "Subramanian", "Nambiar", "Praneeth", "Balan",
    "Ranganathan", "Varadarajan", "Murugan", "Ramachandran", "Balaji", "Natarajan", "Gopal",
    "Srinivasan", "Sankaran", "Chakravarthy", "Menon", "Pillai", "Chettiar", "Muthusamy", "Venkatesan",
    "Bose", "Choudhury", "Das", "Dutta", "Ghosh", "Gupta", "Jadhav", "Joshi", "Kapoor", "Kulkarni",
    "Mahajan", "Mehta", "Mishra", "Pandey", "Rao", "Sen", "Seth", "Shah", "Singh", "Tiwari"
  ];

  const cities = ["Chennai", "Coimbatore", "Madurai", "Salem", "Trichy", "Tirunelveli", "Erode", "Vellore"];

  const depts = [
    { code: "CSE", count: 20 },
    { code: "IT", count: 20 },
    { code: "AI&DS", count: 15 },
    { code: "ECE", count: 20 },
    { code: "EEE", count: 13 },
    { code: "MECH", count: 12 }
  ];

  const students = [];
  let globalIdx = 0;

  for (const d of depts) {
    const prefix = d.code.replace("&", "");
    for (let j = 1; j <= d.count; j++) {
      const idx = globalIdx;
      const isMale = (idx % 2 === 0);
      const first = isMale ? maleNames[Math.floor(idx / 2)] : femaleNames[Math.floor(idx / 2)];
      const last = lastNames[idx % lastNames.length];
      const name = `${first} ${last}`;
      const year = 1 + ((j - 1) % 4);
      const section = ((j - 1) % 2 === 0) ? "A" : "B";
      const regNo = `24${prefix}${String(j).padStart(3, "0")}`;
      const email = `${first.toLowerCase()}.${last.toLowerCase()}@kit.edu.in`;
      const phone = `98${String(40 + (idx % 50)).padStart(2, "0")}${String(100000 + idx * 73).substring(0, 6)}`;
      const city = cities[idx % cities.length];

      // Target rate for realistic attendance
      let targetRate = 0.85;
      if (idx < 30) targetRate = 0.92 + (idx % 7) * 0.01;
      else if (idx < 65) targetRate = 0.81 + (idx % 8) * 0.01;
      else if (idx < 80) targetRate = 0.75 + (idx % 5) * 0.01;
      else if (idx < 92) targetRate = 0.66 + (idx % 8) * 0.01;
      else targetRate = 0.45 + (idx % 6) * 0.03;

      students.push({
        id: idx + 1,
        regNo,
        name,
        gender: isMale ? "Male" : "Female",
        dob: `200${3 + (4 - year)}-${String(1 + (idx % 12)).padStart(2, "0")}-${String(1 + (idx % 28)).padStart(2, "0")}`,
        dept: d.code,
        year,
        section,
        email,
        phone,
        address: `${12 + idx}, Gandhi Street, ${city}, Tamil Nadu - 6000${10 + (idx % 50)}`,
        admissionDate: `${2023 + (year === 4 ? 0 : 4 - year)}-08-01`,
        targetRate
      });

      globalIdx++;
    }
  }

  return students;
}

export function generate40WorkingDays() {
  const days = [];
  let d = new Date(2026, 7, 3); // Aug 3, 2026
  while (days.length < 40) {
    const dayOfWeek = d.getDay();
    if (dayOfWeek !== 0 && dayOfWeek !== 6) { // Not Sat or Sun
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, "0");
      const dd = String(d.getDate()).padStart(2, "0");
      days.push(`${yyyy}-${mm}-${dd}`);
    }
    d.setDate(d.getDate() + 1);
  }
  return days;
}

export function generateInitialAttendance(students, workingDays) {
  const attendance = {};
  for (const s of students) {
    attendance[s.id] = {};
    for (const day of workingDays) {
      // Deterministic pseudo-random based on student id and date
      const hash = (s.id * 9301 + day.length * 49297 + day.charCodeAt(8)) % 233280;
      const rnd = hash / 233280;
      attendance[s.id][day] = rnd < s.targetRate ? "PRESENT" : "ABSENT";
    }
  }
  return attendance;
}

export const DEFAULT_PERIODS = [
  { id: 1, name: "Period 1", timeSlot: "09:00 AM - 10:00 AM" },
  { id: 2, name: "Period 2", timeSlot: "10:00 AM - 11:00 AM" },
  { id: 3, name: "Period 3", timeSlot: "11:15 AM - 12:15 PM" },
  { id: 4, name: "Period 4", timeSlot: "01:00 PM - 02:00 PM" },
  { id: 5, name: "Period 5", timeSlot: "02:00 PM - 03:00 PM" },
  { id: 6, name: "Period 6", timeSlot: "03:15 PM - 04:15 PM" }
];

export const DEFAULT_SUBJECTS = [
  { id: 1, code: "CS501", name: "Design & Analysis of Algorithms", dept: "CSE", year: 3, sem: "V", credits: 4, teacherId: 1, teacherName: "Dr. R. Ramanathan" },
  { id: 2, code: "CS502", name: "Database Management Systems", dept: "CSE", year: 3, sem: "V", credits: 4, teacherId: 1, teacherName: "Dr. R. Ramanathan" },
  { id: 3, code: "IT501", name: "Full Stack Web Development", dept: "IT", year: 3, sem: "V", credits: 3, teacherId: 2, teacherName: "Dr. S. Gayathri" },
  { id: 4, code: "IT502", name: "Cloud Computing & DevOps", dept: "IT", year: 3, sem: "V", credits: 3, teacherId: 2, teacherName: "Dr. S. Gayathri" },
  { id: 5, code: "AI501", name: "Deep Learning Foundations", dept: "AI&DS", year: 3, sem: "V", credits: 4, teacherId: 3, teacherName: "Prof. K. Venkatesh" },
  { id: 6, code: "EC501", name: "Digital Signal Processing", dept: "ECE", year: 3, sem: "V", credits: 4, teacherId: 4, teacherName: "Dr. P. Meenakshi" },
  { id: 7, code: "EE501", name: "Power Electronics & Drives", dept: "EEE", year: 3, sem: "V", credits: 3, teacherId: 5, teacherName: "Prof. M. Suresh" },
  { id: 8, code: "ME501", name: "Design of Transmission Systems", dept: "MECH", year: 3, sem: "V", credits: 4, teacherId: 6, teacherName: "Dr. T. Revathi" }
];

export const DEFAULT_TIMETABLE = [
  { id: 1, day: "Monday", periodId: 1, periodName: "Period 1", subjectCode: "CS501", subjectName: "Design & Analysis of Algorithms", teacherName: "Dr. R. Ramanathan", dept: "CSE", year: 3, sec: "A", room: "LH-201" },
  { id: 2, day: "Monday", periodId: 2, periodName: "Period 2", subjectCode: "CS502", subjectName: "Database Management Systems", teacherName: "Dr. R. Ramanathan", dept: "CSE", year: 3, sec: "A", room: "LH-201" },
  { id: 3, day: "Tuesday", periodId: 1, periodName: "Period 1", subjectCode: "CS502", subjectName: "Database Management Systems", teacherName: "Dr. R. Ramanathan", dept: "CSE", year: 3, sec: "A", room: "Lab-3" },
  { id: 4, day: "Wednesday", periodId: 3, periodName: "Period 3", subjectCode: "CS501", subjectName: "Design & Analysis of Algorithms", teacherName: "Dr. R. Ramanathan", dept: "CSE", year: 3, sec: "A", room: "LH-201" },
  { id: 5, day: "Thursday", periodId: 2, periodName: "Period 2", subjectCode: "CS501", subjectName: "Design & Analysis of Algorithms", teacherName: "Dr. R. Ramanathan", dept: "CSE", year: 3, sec: "A", room: "LH-201" },
  { id: 6, day: "Friday", periodId: 4, periodName: "Period 4", subjectCode: "CS502", subjectName: "Database Management Systems", teacherName: "Dr. R. Ramanathan", dept: "CSE", year: 3, sec: "A", room: "LH-201" }
];

export const DEFAULT_LEAVES = [
  { id: 1, studentId: 1, studentName: "Aarav Kumar", regNo: "24CSE001", dept: "CSE", leaveType: "OD", fromDate: "2026-08-15", toDate: "2026-08-16", reason: "Participating in Smart India Hackathon Regional Finals", status: "APPROVED", approvedBy: "Dr. R. Ramanathan", remarks: "Approved for National Competition" },
  { id: 2, studentId: 15, studentName: "Manoj Murugan", regNo: "24CSE015", dept: "CSE", leaveType: "Medical", fromDate: "2026-09-02", toDate: "2026-09-04", reason: "Viral fever with medical doctor prescription", status: "PENDING", approvedBy: null, remarks: null },
  { id: 3, studentId: 21, studentName: "Vijay Bose", regNo: "24IT001", dept: "IT", leaveType: "Personal", fromDate: "2026-09-10", toDate: "2026-09-11", reason: "Family wedding event in Madurai", status: "REJECTED", approvedBy: "Dr. S. Gayathri", remarks: "Attendance below 75% cutoff threshold" }
];

export const DEFAULT_NOTIFICATIONS = [
  { id: 1, title: "Academic Attendance Cutoff", message: "Mandatory 75% attendance cutoff for Semester V Anna University Examinations.", type: "ALERT", targetRole: "ALL", date: "2026-10-01 09:00" },
  { id: 2, title: "QR Code Attendance Enabled", message: "Students can now scan lecture session QR codes directly from the student portal.", type: "INFO", targetRole: "STUDENT", date: "2026-10-03 10:30" },
  { id: 3, title: "Leave Approvals Pending", message: "You have 1 pending student OD/Medical leave application awaiting review.", type: "REMINDER", targetRole: "TEACHER", date: "2026-10-06 14:15" }
];

export const DEFAULT_AUDIT_LOGS = [
  { id: 1, action: "ATTENDANCE_CORRECTION", user: "Dr. R. Ramanathan", details: "Corrected 24CSE001 on 2026-08-12 from ABSENT to PRESENT (Reason: Student attended authorized seminar)", timestamp: "2026-10-02 11:24" },
  { id: 2, action: "SETTINGS_UPDATE", user: "admin", details: "Updated college name to KIT ENGINEERING COLLEGE and enabled Leave Counting Policy", timestamp: "2026-10-04 15:40" },
  { id: 3, action: "SUBJECT_ENROLLMENT", user: "admin", details: "Enrolled 20 students of Dept CSE Year 3 to CS501 Algorithms", timestamp: "2026-10-05 10:15" }
];
