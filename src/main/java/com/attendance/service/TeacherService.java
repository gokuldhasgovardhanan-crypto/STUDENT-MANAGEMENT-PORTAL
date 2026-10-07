package com.attendance.service;

import com.attendance.dao.TeacherDAO;
import com.attendance.model.Teacher;
import com.attendance.util.ValidationUtil;

import java.util.List;

/**
 * Service managing teacher operations and business rules.
 */
public class TeacherService {

    private final TeacherDAO teacherDAO;

    public TeacherService() {
        this.teacherDAO = new TeacherDAO();
    }

    public List<Teacher> getAllTeachers() {
        return teacherDAO.getAllTeachers();
    }

    public Teacher getTeacherById(int id) {
        return teacherDAO.getTeacherById(id);
    }

    public void addTeacher(Teacher teacher) throws Exception {
        validateTeacher(teacher, true);
        boolean success = teacherDAO.addTeacher(teacher);
        if (!success) {
            throw new Exception("Failed to insert teacher record into database.");
        }
    }

    public void updateTeacher(Teacher teacher) throws Exception {
        validateTeacher(teacher, false);
        boolean success = teacherDAO.updateTeacher(teacher);
        if (!success) {
            throw new Exception("Failed to update teacher record in database.");
        }
    }

    public void deleteTeacher(int teacherId) throws Exception {
        boolean success = teacherDAO.deleteTeacher(teacherId);
        if (!success) {
            throw new Exception("Failed to delete teacher record.");
        }
    }

    public List<Teacher> searchTeachers(String keyword, String department) {
        return teacherDAO.searchTeachers(keyword, department);
    }

    private void validateTeacher(Teacher t, boolean isNew) throws Exception {
        if (t == null) {
            throw new IllegalArgumentException("Teacher details cannot be empty.");
        }
        if (ValidationUtil.isEmpty(t.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID is required.");
        }
        if (ValidationUtil.isEmpty(t.getTeacherName())) {
            throw new IllegalArgumentException("Teacher Name is required.");
        }
        if (!ValidationUtil.isValidName(t.getTeacherName())) {
            throw new IllegalArgumentException("Teacher Name contains invalid characters.");
        }
        if (ValidationUtil.isEmpty(t.getDepartment())) {
            throw new IllegalArgumentException("Department is required.");
        }
        if (ValidationUtil.isEmpty(t.getEmail()) || !ValidationUtil.isValidEmail(t.getEmail())) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (ValidationUtil.isEmpty(t.getPhone()) || !ValidationUtil.isValidPhone(t.getPhone())) {
            throw new IllegalArgumentException("Please enter a valid phone number.");
        }

        Integer excludeId = isNew ? null : t.getTeacherId();
        if (teacherDAO.isEmployeeIdExists(t.getEmployeeId().trim(), excludeId)) {
            throw new IllegalArgumentException("Employee ID '" + t.getEmployeeId() + "' already exists.");
        }
    }
}
