package com.attendance.model;

/**
 * Model representing an Academic Subject/Course.
 */
public class Subject {
    private int subjectId;
    private String subjectCode;
    private String subjectName;
    private String department;
    private int yearOfStudy;
    private String semester;
    private int credits = 3;
    private Integer teacherId;

    // Associated Teacher name for display
    private String teacherName;

    public Subject() {}

    public Subject(int subjectId, String subjectCode, String subjectName, String department, int yearOfStudy, String semester, int credits, Integer teacherId) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.department = department;
        this.yearOfStudy = yearOfStudy;
        this.semester = semester;
        this.credits = credits;
        this.teacherId = teacherId;
    }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public int getId() { return subjectId; }
    public void setId(int id) { this.subjectId = id; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    @Override
    public String toString() {
        return subjectCode + " - " + subjectName;
    }
}
