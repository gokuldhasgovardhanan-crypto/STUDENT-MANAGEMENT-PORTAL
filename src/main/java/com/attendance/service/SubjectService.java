package com.attendance.service;

import com.attendance.dao.SubjectDAO;
import com.attendance.model.Subject;

import java.sql.SQLException;
import java.util.List;

/**
 * Service for Subject Management and Enrollments.
 */
public class SubjectService {

    private final SubjectDAO subjectDAO = new SubjectDAO();

    public boolean createSubject(Subject s) throws SQLException {
        if (s.getSubjectCode() == null || s.getSubjectCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject code cannot be empty.");
        }
        if (s.getSubjectName() == null || s.getSubjectName().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject name cannot be empty.");
        }
        return subjectDAO.create(s);
    }

    public boolean updateSubject(Subject s) throws SQLException {
        return subjectDAO.update(s);
    }

    public boolean deleteSubject(int subjectId) throws SQLException {
        return subjectDAO.delete(subjectId);
    }

    public Subject getSubjectById(int id) {
        try {
            return subjectDAO.findById(id);
        } catch (SQLException e) {
            return null;
        }
    }

    public List<Subject> getAllSubjects() {
        try {
            return subjectDAO.findAll();
        } catch (SQLException e) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Subject> getSubjectsByDepartmentAndYear(String dept, int year) {
        try {
            return subjectDAO.findByDepartmentAndYear(dept, year);
        } catch (SQLException e) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Subject> getSubjectsByTeacher(int teacherId) throws SQLException {
        return subjectDAO.findByTeacherId(teacherId);
    }

    public List<Subject> getEnrolledSubjects(int studentId) throws SQLException {
        return subjectDAO.getEnrolledSubjects(studentId);
    }

    public int assignSubjectToStudents(int subjectId, List<Integer> studentIds) throws SQLException {
        return subjectDAO.assignSubjectToStudents(subjectId, studentIds);
    }

    public int assignSubjectByClass(int subjectId, String dept, int year, String section) throws SQLException {
        return subjectDAO.assignSubjectByClass(subjectId, dept, year, section);
    }

    public boolean removeSubjectFromStudent(int studentId, int subjectId) throws SQLException {
        return subjectDAO.removeSubjectFromStudent(studentId, subjectId);
    }

    public boolean saveSubject(Subject s) throws SQLException {
        return s.getId() > 0 ? updateSubject(s) : createSubject(s);
    }

    public List<Subject> getSubjectsByDeptAndYear(String dept, int year) {
        return getSubjectsByDepartmentAndYear(dept, year);
    }

    public int enrollClass(String dept, int year, int subjectId) throws SQLException {
        return assignSubjectByClass(subjectId, dept, year, null);
    }
}
