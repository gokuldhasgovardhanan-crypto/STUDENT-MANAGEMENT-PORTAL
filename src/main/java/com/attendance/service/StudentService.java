package com.attendance.service;

import com.attendance.dao.StudentDAO;
import com.attendance.model.Student;
import com.attendance.util.ValidationUtil;

import java.util.List;

/**
 * Service managing student business logic and validations.
 */
public class StudentService {

    private final StudentDAO studentDAO;

    public StudentService() {
        this.studentDAO = new StudentDAO();
    }

    public List<Student> getAllStudents() {
        return studentDAO.getAllStudents();
    }

    public Student getStudentById(int studentId) {
        return studentDAO.getStudentById(studentId);
    }

    public Student getStudentByRegisterNo(String registerNo) {
        return studentDAO.getStudentByRegisterNo(registerNo);
    }

    public void addStudent(Student student) throws Exception {
        validateStudent(student, true);
        boolean success = studentDAO.addStudent(student);
        if (!success) {
            throw new Exception("Failed to insert student record into database.");
        }
    }

    public void updateStudent(Student student) throws Exception {
        validateStudent(student, false);
        boolean success = studentDAO.updateStudent(student);
        if (!success) {
            throw new Exception("Failed to update student record in database.");
        }
    }

    public void deleteStudent(int studentId) throws Exception {
        boolean success = studentDAO.deleteStudent(studentId);
        if (!success) {
            throw new Exception("Failed to delete student. Please verify student exists.");
        }
    }

    public List<Student> searchStudents(String keyword, String department, Integer year, String section) {
        return studentDAO.searchStudents(keyword, department, year, section);
    }

    public List<Student> getStudentsByClass(String department, int year, String section) {
        return studentDAO.getStudentsByFilter(department, year, section);
    }

    public int getTotalStudentCount() {
        return studentDAO.getTotalStudentCount();
    }

    private void validateStudent(Student s, boolean isNew) throws Exception {
        if (s == null) {
            throw new IllegalArgumentException("Student details cannot be empty.");
        }
        if (ValidationUtil.isEmpty(s.getRegisterNo())) {
            throw new IllegalArgumentException("Register Number is required.");
        }
        if (ValidationUtil.isEmpty(s.getStudentName())) {
            throw new IllegalArgumentException("Student Name is required.");
        }
        if (!ValidationUtil.isValidName(s.getStudentName())) {
            throw new IllegalArgumentException("Student Name contains invalid characters.");
        }
        if (ValidationUtil.isEmpty(s.getDepartment())) {
            throw new IllegalArgumentException("Department is required.");
        }
        if (!ValidationUtil.isValidYear(s.getYearOfStudy())) {
            throw new IllegalArgumentException("Year of Study must be between 1 and 4.");
        }
        if (ValidationUtil.isEmpty(s.getSection())) {
            throw new IllegalArgumentException("Section is required.");
        }
        if (ValidationUtil.isEmpty(s.getEmail()) || !ValidationUtil.isValidEmail(s.getEmail())) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (ValidationUtil.isEmpty(s.getPhoneNumber()) || !ValidationUtil.isValidPhone(s.getPhoneNumber())) {
            throw new IllegalArgumentException("Please enter a valid phone number (10 to 15 digits).");
        }
        if (s.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of Birth is required.");
        }
        if (s.getAdmissionDate() == null) {
            throw new IllegalArgumentException("Admission Date is required.");
        }

        // Uniqueness check for Register Number
        Integer excludeId = isNew ? null : s.getStudentId();
        if (studentDAO.isRegisterNoExists(s.getRegisterNo().trim(), excludeId)) {
            throw new IllegalArgumentException("Register Number '" + s.getRegisterNo() + "' already exists.");
        }
    }
}
