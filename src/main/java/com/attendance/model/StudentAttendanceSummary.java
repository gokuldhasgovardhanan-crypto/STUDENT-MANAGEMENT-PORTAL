package com.attendance.model;

/**
 * Model representing dynamic calculated attendance performance for a student.
 */
public class StudentAttendanceSummary {
    private int studentId;
    private String registerNo;
    private String studentName;
    private String department;
    private int yearOfStudy;
    private String section;
    private String email;
    private String phone;
    private int totalWorkingDays;
    private int presentDays;
    private int absentDays;
    private double attendancePercentage;
    private String status; // "Excellent", "Good", "Warning", "Critical"

    public StudentAttendanceSummary() {}

    public StudentAttendanceSummary(int studentId, String registerNo, String studentName,
                                    String department, int yearOfStudy, String section,
                                    int totalWorkingDays, int presentDays, int absentDays,
                                    double attendancePercentage, String status) {
        this.studentId = studentId;
        this.registerNo = registerNo;
        this.studentName = studentName;
        this.department = department;
        this.yearOfStudy = yearOfStudy;
        this.section = section;
        this.totalWorkingDays = totalWorkingDays;
        this.presentDays = presentDays;
        this.absentDays = absentDays;
        this.attendancePercentage = attendancePercentage;
        this.status = status;
    }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getRegisterNo() { return registerNo; }
    public void setRegisterNo(String registerNo) { this.registerNo = registerNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getTotalWorkingDays() { return totalWorkingDays; }
    public void setTotalWorkingDays(int totalWorkingDays) { this.totalWorkingDays = totalWorkingDays; }

    public int getPresentDays() { return presentDays; }
    public void setPresentDays(int presentDays) { this.presentDays = presentDays; }

    public int getAbsentDays() { return absentDays; }
    public void setAbsentDays(int absentDays) { this.absentDays = absentDays; }

    public double getAttendancePercentage() { return attendancePercentage; }
    public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static String calculateStatus(double percentage) {
        if (percentage >= 85.0) return "Excellent";
        if (percentage >= 75.0) return "Good";
        if (percentage >= 65.0) return "Warning";
        return "Critical";
    }
}
