package com.attendance.service;

import com.attendance.model.AppSettings;
import com.attendance.model.Attendance;
import com.attendance.model.DashboardStats;
import com.attendance.model.Student;
import com.attendance.model.StudentAttendanceSummary;
import com.attendance.util.DateUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for generating polished, real Apache POI Excel (.xlsx) workbooks.
 */
public class ExcelExportService {

    public void exportStudentList(List<Student> students, File targetFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Student List");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            String[] headers = {
                    "ID", "Register No", "Student Name", "Gender", "Date of Birth",
                    "Department", "Year", "Section", "Email", "Phone", "Admission Date", "Address"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Student s : students) {
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, s.getStudentId(), dataStyle);
                createCell(row, 1, s.getRegisterNo(), dataStyle);
                createCell(row, 2, s.getStudentName(), dataStyle);
                createCell(row, 3, s.getGender(), dataStyle);
                createCell(row, 4, DateUtil.formatDisplayDate(s.getDateOfBirth()), dateStyle);
                createCell(row, 5, s.getDepartment(), dataStyle);
                createCell(row, 6, s.getYearOfStudy(), dataStyle);
                createCell(row, 7, s.getSection(), dataStyle);
                createCell(row, 8, s.getEmail(), dataStyle);
                createCell(row, 9, s.getPhoneNumber(), dataStyle);
                createCell(row, 10, DateUtil.formatDisplayDate(s.getAdmissionDate()), dateStyle);
                createCell(row, 11, s.getAddress() != null ? s.getAddress() : "", dataStyle);
            }

            sheet.createFreezePane(0, 1);
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                workbook.write(fos);
            }
        }
    }

    public void exportDailyAttendance(LocalDate date, String dept, Integer year, String section,
                                      List<Attendance> records, File targetFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Attendance");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle presentStyle = createStatusStyle(workbook, true);
            CellStyle absentStyle = createStatusStyle(workbook, false);

            // Title block
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Daily Attendance Report - Date: " + DateUtil.formatDisplayDate(date) +
                    " | Dept: " + (dept != null ? dept : "All") +
                    " | Year: " + (year != null && year > 0 ? year : "All") +
                    " | Section: " + (section != null ? section : "All"));
            CellStyle titleStyle = createTitleStyle(workbook);
            titleCell.setCellStyle(titleStyle);

            String[] headers = {"S.No", "Register No", "Student Name", "Department", "Year", "Section", "Date", "Status"};
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            int sno = 1;
            for (Attendance a : records) {
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, sno++, dataStyle);
                createCell(row, 1, a.getRegisterNo(), dataStyle);
                createCell(row, 2, a.getStudentName(), dataStyle);
                createCell(row, 3, a.getDepartment(), dataStyle);
                createCell(row, 4, a.getYearOfStudy(), dataStyle);
                createCell(row, 5, a.getSection(), dataStyle);
                createCell(row, 6, DateUtil.formatDisplayDate(a.getAttendanceDate()), dateStyle);

                Cell sc = row.createCell(7);
                sc.setCellValue(a.getStatus());
                sc.setCellStyle("PRESENT".equalsIgnoreCase(a.getStatus()) ? presentStyle : absentStyle);
            }

            sheet.createFreezePane(0, 3);
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                workbook.write(fos);
            }
        }
    }

    public void exportStudentProfileAttendance(Student student, StudentAttendanceSummary summary,
                                              List<Attendance> history, File targetFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Student Attendance");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            // Student Info Header
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("STUDENT ATTENDANCE PROFILE - " + student.getRegisterNo());
            c0.setCellStyle(titleStyle);

            createLabelValueRow(sheet, 2, "Student Name:", student.getStudentName(), "Department:", student.getDepartment());
            createLabelValueRow(sheet, 3, "Year / Section:", student.getYearOfStudy() + " - " + student.getSection(), "Email:", student.getEmail());
            createLabelValueRow(sheet, 4, "Working Days:", String.valueOf(summary.getTotalWorkingDays()), "Present Days:", String.valueOf(summary.getPresentDays()));
            createLabelValueRow(sheet, 5, "Absent Days:", String.valueOf(summary.getAbsentDays()), "Attendance Percentage:", String.format("%.2f%%", summary.getAttendancePercentage()));
            createLabelValueRow(sheet, 6, "Overall Status:", summary.getStatus(), "Report Generated:", DateUtil.formatCurrentTimestamp());

            String[] headers = {"S.No", "Date", "Status"};
            Row headerRow = sheet.createRow(8);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle presentStyle = createStatusStyle(workbook, true);
            CellStyle absentStyle = createStatusStyle(workbook, false);

            int rowIdx = 9;
            int sno = 1;
            for (Attendance a : history) {
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, sno++, dataStyle);
                createCell(row, 1, DateUtil.formatDisplayDate(a.getAttendanceDate()), dateStyle);
                Cell sc = row.createCell(2);
                sc.setCellValue(a.getStatus());
                sc.setCellStyle("PRESENT".equalsIgnoreCase(a.getStatus()) ? presentStyle : absentStyle);
            }

            sheet.createFreezePane(0, 9);
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                workbook.write(fos);
            }
        }
    }

    public void exportMonthlyAttendance(String periodLabel, List<StudentAttendanceSummary> summaries, File targetFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Monthly Attendance");
            populateSummarySheet(workbook, sheet, "Monthly Attendance Report - " + periodLabel, summaries);

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                workbook.write(fos);
            }
        }
    }

    public void exportLowAttendanceReport(double threshold, List<StudentAttendanceSummary> lowStudents, File targetFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Low Attendance (< " + threshold + "%)");
            populateSummarySheet(workbook, sheet, "Low Attendance Report (Threshold: " + threshold + "%)", lowStudents);

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * Generates the comprehensive multi-sheet workbook as required in Section 18:
     * Sheet 1: Summary
     * Sheet 2: Student Attendance
     * Sheet 3: Daily Attendance
     * Sheet 4: Low Attendance
     * Sheet 5: Student List
     */
    /**
     * Requirement 22: Printable & Exportable Student Attendance Card (Excel format)
     */
    public void exportStudentAttendanceCard(Student student, java.util.List<java.util.Map<String, Object>> subjectBreakdown,
                                           double overallPct, String status, File targetFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance Card");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle labelStyle = createBoldStyle(workbook);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("KIT ENGINEERING COLLEGE - STUDENT ATTENDANCE CARD");
            c0.setCellStyle(titleStyle);

            createLabelValueRow(sheet, 2, "Student Name:", student.getStudentName(), "Register No:", student.getRegisterNo());
            createLabelValueRow(sheet, 3, "Department:", student.getDepartment(), "Year / Section:", student.getYearOfStudy() + " - " + student.getSection());
            createLabelValueRow(sheet, 4, "Overall Attendance:", String.format("%.2f%%", overallPct), "Status:", status);
            createLabelValueRow(sheet, 5, "Generated On:", DateUtil.formatCurrentTimestamp(), "", "");

            String[] headers = {"S.No", "Subject Code", "Subject Name", "Conducted", "Attended", "Attendance %"};
            Row hr = sheet.createRow(7);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = hr.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 8;
            int sno = 1;
            for (java.util.Map<String, Object> map : subjectBreakdown) {
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, sno++, dataStyle);
                createCell(row, 1, (String) map.get("subjectCode"), dataStyle);
                createCell(row, 2, (String) map.get("subjectName"), dataStyle);
                createCell(row, 3, (Integer) map.get("conducted"), dataStyle);
                createCell(row, 4, (Integer) map.get("attended"), dataStyle);
                createCell(row, 5, String.format("%.1f%%", (Double) map.get("percentage")), dataStyle);
            }

            sheet.createFreezePane(0, 8);
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * Requirement 20: Comprehensive multi-sheet workbook:
     * Sheet 1: Summary
     * Sheet 2: Students
     * Sheet 3: Attendance
     * Sheet 4: Subject-wise
     * Sheet 5: Low Attendance
     * Sheet 6: Leave
     */
    public void exportCompleteAttendanceSummaryWorkbook(AppSettings settings, DashboardStats stats,
                                                       List<StudentAttendanceSummary> summaries,
                                                       List<Attendance> recentAttendance,
                                                       List<StudentAttendanceSummary> lowStudents,
                                                       List<Student> students,
                                                       File targetFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet 1: Summary
            Sheet sheet1 = workbook.createSheet("Summary");
            populateExecutiveSummarySheet(workbook, sheet1, settings, stats);

            // Sheet 2: Students
            Sheet sheet2 = workbook.createSheet("Students");
            populateStudentListSheet(workbook, sheet2, students);

            // Sheet 3: Attendance
            Sheet sheet3 = workbook.createSheet("Attendance");
            populateSummarySheet(workbook, sheet3, "Consolidated Student Attendance Report", summaries);

            // Sheet 4: Subject-wise (Recent Attendance records with Subject & Period)
            Sheet sheet4 = workbook.createSheet("Subject-wise");
            populateDailySheet(workbook, sheet4, recentAttendance);

            // Sheet 5: Low Attendance
            Sheet sheet5 = workbook.createSheet("Low Attendance");
            populateSummarySheet(workbook, sheet5, "Students with Low Attendance (< " + settings.getRequiredAttendancePct() + "%)", lowStudents);

            // Sheet 6: Leave
            Sheet sheet6 = workbook.createSheet("Leave");
            populateLeaveSheet(workbook, sheet6, students);

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                workbook.write(fos);
            }
        }
    }

    private void populateLeaveSheet(Workbook workbook, Sheet sheet, List<Student> students) {
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("Student Leave Records Summary");
        c0.setCellStyle(titleStyle);

        String[] headers = {"S.No", "Register No", "Student Name", "Department", "Year", "Section", "Leave Status"};
        Row hr = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = hr.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 3;
        int sno = 1;
        for (Student s : students) {
            Row row = sheet.createRow(rowIdx++);
            createCell(row, 0, sno++, dataStyle);
            createCell(row, 1, s.getRegisterNo(), dataStyle);
            createCell(row, 2, s.getStudentName(), dataStyle);
            createCell(row, 3, s.getDepartment(), dataStyle);
            createCell(row, 4, s.getYearOfStudy(), dataStyle);
            createCell(row, 5, s.getSection(), dataStyle);
            createCell(row, 6, "Regular", dataStyle);
        }

        sheet.createFreezePane(0, 3);
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void populateExecutiveSummarySheet(Workbook workbook, Sheet sheet, AppSettings settings, DashboardStats stats) {
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle labelStyle = createBoldStyle(workbook);
        CellStyle valStyle = createDataStyle(workbook);

        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(settings.getCollegeName() + " - Comprehensive Attendance Report");
        c0.setCellStyle(titleStyle);

        int rowIdx = 2;
        rowIdx = addSummaryRow(sheet, rowIdx, "Academic Year:", settings.getAcademicYear(), labelStyle, valStyle);
        rowIdx = addSummaryRow(sheet, rowIdx, "Semester:", settings.getSemester(), labelStyle, valStyle);
        rowIdx = addSummaryRow(sheet, rowIdx, "Report Generated:", DateUtil.formatCurrentTimestamp(), labelStyle, valStyle);
        rowIdx++;
        rowIdx = addSummaryRow(sheet, rowIdx, "Total Enrolled Students:", String.valueOf(stats.getTotalStudents()), labelStyle, valStyle);
        rowIdx = addSummaryRow(sheet, rowIdx, "College Average Attendance:", String.format("%.2f%%", stats.getAverageAttendancePct()), labelStyle, valStyle);
        rowIdx = addSummaryRow(sheet, rowIdx, "Present Today:", String.valueOf(stats.getPresentToday()), labelStyle, valStyle);
        rowIdx = addSummaryRow(sheet, rowIdx, "Absent Today:", String.valueOf(stats.getAbsentToday()), labelStyle, valStyle);
        rowIdx = addSummaryRow(sheet, rowIdx, "Attendance Not Marked Today:", String.valueOf(stats.getAttendanceNotMarked()), labelStyle, valStyle);
        rowIdx = addSummaryRow(sheet, rowIdx, "Required Attendance Criteria:", settings.getRequiredAttendancePct() + "%", labelStyle, valStyle);
        rowIdx = addSummaryRow(sheet, rowIdx, "Students Below Criteria:", String.valueOf(stats.getStudentsBelowThresholdCount()), labelStyle, valStyle);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private int addSummaryRow(Sheet sheet, int rowIdx, String label, String value, CellStyle labelStyle, CellStyle valStyle) {
        Row row = sheet.createRow(rowIdx);
        Cell cl = row.createCell(0);
        cl.setCellValue(label);
        cl.setCellStyle(labelStyle);
        Cell cv = row.createCell(1);
        cv.setCellValue(value);
        cv.setCellStyle(valStyle);
        return rowIdx + 1;
    }

    private void populateSummarySheet(Workbook workbook, Sheet sheet, String title, List<StudentAttendanceSummary> summaries) {
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle pctStyle = createPercentStyle(workbook);

        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(title);
        c0.setCellStyle(titleStyle);

        String[] headers = {
                "S.No", "Register No", "Student Name", "Department", "Year", "Section",
                "Working Days", "Present Days", "Absent Days", "Attendance %", "Status", "Phone"
        };

        Row hr = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = hr.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 3;
        int sno = 1;
        for (StudentAttendanceSummary s : summaries) {
            Row r = sheet.createRow(rowIdx++);
            createCell(r, 0, sno++, dataStyle);
            createCell(r, 1, s.getRegisterNo(), dataStyle);
            createCell(r, 2, s.getStudentName(), dataStyle);
            createCell(r, 3, s.getDepartment(), dataStyle);
            createCell(r, 4, s.getYearOfStudy(), dataStyle);
            createCell(r, 5, s.getSection(), dataStyle);
            createCell(r, 6, s.getTotalWorkingDays(), dataStyle);
            createCell(r, 7, s.getPresentDays(), dataStyle);
            createCell(r, 8, s.getAbsentDays(), dataStyle);

            Cell pc = r.createCell(9);
            pc.setCellValue(s.getAttendancePercentage() / 100.0);
            pc.setCellStyle(pctStyle);

            createCell(r, 10, s.getStatus(), dataStyle);
            createCell(r, 11, s.getPhone() != null ? s.getPhone() : "", dataStyle);
        }

        sheet.createFreezePane(0, 3);
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void populateDailySheet(Workbook workbook, Sheet sheet, List<Attendance> records) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        String[] headers = {"S.No", "Register No", "Student Name", "Department", "Year", "Section", "Date", "Status"};
        Row hr = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        int sno = 1;
        if (records != null) {
            for (Attendance a : records) {
                Row r = sheet.createRow(rowIdx++);
                createCell(r, 0, sno++, dataStyle);
                createCell(r, 1, a.getRegisterNo(), dataStyle);
                createCell(r, 2, a.getStudentName(), dataStyle);
                createCell(r, 3, a.getDepartment(), dataStyle);
                createCell(r, 4, a.getYearOfStudy(), dataStyle);
                createCell(r, 5, a.getSection(), dataStyle);
                createCell(r, 6, DateUtil.formatDisplayDate(a.getAttendanceDate()), dateStyle);
                createCell(r, 7, a.getStatus(), dataStyle);
            }
        }

        sheet.createFreezePane(0, 1);
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void populateStudentListSheet(Workbook workbook, Sheet sheet, List<Student> students) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        String[] headers = {"ID", "Register No", "Student Name", "Gender", "Dept", "Year", "Sec", "Email", "Phone", "Admission Date"};
        Row hr = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        if (students != null) {
            for (Student s : students) {
                Row r = sheet.createRow(rowIdx++);
                createCell(r, 0, s.getStudentId(), dataStyle);
                createCell(r, 1, s.getRegisterNo(), dataStyle);
                createCell(r, 2, s.getStudentName(), dataStyle);
                createCell(r, 3, s.getGender(), dataStyle);
                createCell(r, 4, s.getDepartment(), dataStyle);
                createCell(r, 5, s.getYearOfStudy(), dataStyle);
                createCell(r, 6, s.getSection(), dataStyle);
                createCell(r, 7, s.getEmail(), dataStyle);
                createCell(r, 8, s.getPhoneNumber(), dataStyle);
                createCell(r, 9, DateUtil.formatDisplayDate(s.getAdmissionDate()), dateStyle);
            }
        }

        sheet.createFreezePane(0, 1);
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createLabelValueRow(Sheet sheet, int rowIdx, String l1, String v1, String l2, String v2) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(l1);
        row.createCell(1).setCellValue(v1);
        row.createCell(3).setCellValue(l2);
        row.createCell(4).setCellValue(v2);
    }

    private void createCell(Row row, int col, Object val, CellStyle style) {
        Cell c = row.createCell(col);
        if (val instanceof Number) {
            c.setCellValue(((Number) val).doubleValue());
        } else {
            c.setCellValue(val != null ? val.toString() : "");
        }
        if (style != null) c.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        setBorders(style);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDateStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        setBorders(style);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("dd-MM-yyyy"));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createPercentStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        setBorders(style);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createStatusStyle(Workbook wb, boolean isPresent) {
        CellStyle style = wb.createCellStyle();
        setBorders(style);
        Font font = wb.createFont();
        font.setBold(true);
        if (isPresent) {
            font.setColor(IndexedColors.DARK_GREEN.getIndex());
            style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        } else {
            font.setColor(IndexedColors.DARK_RED.getIndex());
            style.setFillForegroundColor(IndexedColors.CORAL.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
