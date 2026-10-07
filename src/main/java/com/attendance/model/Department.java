package com.attendance.model;

/**
 * Model representing an Academic Department.
 */
public class Department {
    private int deptId;
    private String deptCode;
    private String deptName;

    public Department() {}

    public Department(int deptId, String deptCode, String deptName) {
        this.deptId = deptId;
        this.deptCode = deptCode;
        this.deptName = deptName;
    }

    public int getDeptId() { return deptId; }
    public void setDeptId(int deptId) { this.deptId = deptId; }

    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    @Override
    public String toString() {
        return deptCode;
    }
}
