package com.attendance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model representing a Student.
 */
public class Student {
    private int studentId;
    private String registerNo;
    private String studentName;
    private String gender;
    private LocalDate dateOfBirth;
    private String department;
    private int yearOfStudy;
    private String section;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate admissionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Student() {}

    public Student(int studentId, String registerNo, String studentName, String gender,
                   LocalDate dateOfBirth, String department, int yearOfStudy, String section,
                   String email, String phoneNumber, String address, LocalDate admissionDate) {
        this.studentId = studentId;
        this.registerNo = registerNo;
        this.studentName = studentName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.department = department;
        this.yearOfStudy = yearOfStudy;
        this.section = section;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.admissionDate = admissionDate;
    }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getRegisterNo() { return registerNo; }
    public void setRegisterNo(String registerNo) { this.registerNo = registerNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return registerNo + " - " + studentName;
    }
}
