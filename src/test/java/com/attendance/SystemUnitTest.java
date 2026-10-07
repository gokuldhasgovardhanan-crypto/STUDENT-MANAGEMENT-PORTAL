package com.attendance;

import com.attendance.model.*;
import com.attendance.service.DatabaseInitService;
import com.attendance.service.ExcelExportService;
import com.attendance.service.PdfExportService;
import com.attendance.util.DateUtil;
import com.attendance.util.PasswordUtil;
import com.attendance.util.ValidationUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SystemUnitTest {

    @Test
    public void testPasswordHashingAndVerification() {
        String adminHash = PasswordUtil.hashPassword("admin123");
        assertNotNull(adminHash);
        assertTrue(adminHash.startsWith("$2a$") || adminHash.startsWith("$2b$"));

        assertTrue(PasswordUtil.checkPassword("admin123", adminHash));
        assertFalse(PasswordUtil.checkPassword("wrongpassword", adminHash));

        String teacherHash = PasswordUtil.hashPassword("teacher123");
        assertTrue(PasswordUtil.checkPassword("teacher123", teacherHash));
        assertFalse(PasswordUtil.checkPassword("admin123", teacherHash));
    }

    @Test
    public void testValidationRules() {
        // Name validation
        assertTrue(ValidationUtil.isValidName("Aarav Kumar"));
        assertTrue(ValidationUtil.isValidName("Dr. R. Ramanathan"));
        assertFalse(ValidationUtil.isValidName("123456"));
        assertFalse(ValidationUtil.isValidName(""));

        // Email validation
        assertTrue(ValidationUtil.isValidEmail("admin@kit.edu.in"));
        assertTrue(ValidationUtil.isValidEmail("student.24cse001@example.com"));
        assertFalse(ValidationUtil.isValidEmail("invalid-email"));
        assertFalse(ValidationUtil.isValidEmail(""));

        // Phone validation
        assertTrue(ValidationUtil.isValidPhone("9840123456"));
        assertTrue(ValidationUtil.isValidPhone("+91 9840123456"));
        assertFalse(ValidationUtil.isValidPhone("123")); // too short
        assertFalse(ValidationUtil.isValidPhone(""));

        // Year validation
        assertTrue(ValidationUtil.isValidYear(1));
        assertTrue(ValidationUtil.isValidYear(4));
        assertFalse(ValidationUtil.isValidYear(0));
        assertFalse(ValidationUtil.isValidYear(5));

        // Attendance status validation
        assertTrue(ValidationUtil.isValidAttendanceStatus("PRESENT"));
        assertTrue(ValidationUtil.isValidAttendanceStatus("ABSENT"));
        assertFalse(ValidationUtil.isValidAttendanceStatus("LATE"));
        assertFalse(ValidationUtil.isValidAttendanceStatus(""));
    }

    @Test
    public void testDateUtilAndWorkingDays() {
        LocalDate d = LocalDate.of(2026, 10, 7);
        assertEquals("07-10-2026", DateUtil.formatDisplayDate(d));
        assertEquals("2026-10-07", DateUtil.formatDbDate(d));

        LocalDate parsed = DateUtil.parseDisplayDate("07-10-2026");
        assertEquals(d, parsed);

        // Check working days calculation (40 days weekdays only)
        List<LocalDate> workingDays = DatabaseInitService.generate40WorkingDays();
        assertEquals(40, workingDays.size());

        for (LocalDate day : workingDays) {
            DayOfWeek dow = day.getDayOfWeek();
            assertNotEquals(DayOfWeek.SATURDAY, dow, "Saturday must not be a working day");
            assertNotEquals(DayOfWeek.SUNDAY, dow, "Sunday must not be a working day");
        }
    }

    @Test
    public void testAttendancePercentageAndStatusCalculation() {
        // Example: Working Days = 50, Present = 45 -> 90% (Excellent)
        double pct1 = (45.0 / 50.0) * 100.0;
        assertEquals(90.0, pct1, 0.001);
        assertEquals("Excellent", StudentAttendanceSummary.calculateStatus(pct1));

        // 80% -> Good
        double pct2 = (40.0 / 50.0) * 100.0;
        assertEquals("Good", StudentAttendanceSummary.calculateStatus(pct2));

        // 70% -> Warning
        double pct3 = (35.0 / 50.0) * 100.0;
        assertEquals("Warning", StudentAttendanceSummary.calculateStatus(pct3));

        // 50% -> Critical
        double pct4 = (25.0 / 50.0) * 100.0;
        assertEquals("Critical", StudentAttendanceSummary.calculateStatus(pct4));
    }

    @Test
    public void test100DemoStudentsGeneration() {
        List<DatabaseInitService.DemoStudentSeed> students = DatabaseInitService.generate100StudentDefinitions();
        assertEquals(100, students.size(), "Must generate exactly 100 demo students");

        Set<String> regNos = new HashSet<>();
        Set<String> names = new HashSet<>();

        for (DatabaseInitService.DemoStudentSeed s : students) {
            assertNotNull(s.regNo);
            assertNotNull(s.name);
            assertNotNull(s.email);
            assertNotNull(s.phone);
            assertNotNull(s.dept);
            assertTrue(s.year >= 1 && s.year <= 4);
            assertTrue("A".equals(s.section) || "B".equals(s.section));

            assertFalse(s.name.contains("Student 1"), "Must not use placeholder names");
            assertTrue(regNos.add(s.regNo), "Register number must be unique: " + s.regNo);
            names.add(s.name);
        }

        assertEquals(100, names.size(), "All 100 demo students must have unique full names");
    }

    @Test
    public void testRealExcelExport(@TempDir Path tempDir) throws Exception {
        ExcelExportService exportService = new ExcelExportService();

        // 1. Export Student List
        List<Student> students = new ArrayList<>();
        Student s1 = new Student(1, "24CSE001", "Aarav Kumar", "Male",
                LocalDate.of(2005, 5, 10), "CSE", 2, "A",
                "aarav@abc.edu.in", "9840123456", "Chennai", LocalDate.of(2024, 8, 1));
        students.add(s1);

        File studentListFile = tempDir.resolve("Student_List.xlsx").toFile();
        exportService.exportStudentList(students, studentListFile);
        assertTrue(studentListFile.exists());
        assertTrue(studentListFile.length() > 0);

        // Verify with POI
        try (FileInputStream fis = new FileInputStream(studentListFile);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Student List");
            assertNotNull(sheet);
            assertEquals("ID", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("24CSE001", sheet.getRow(1).getCell(1).getStringCellValue());
        }

        // 2. Export Multi-Sheet Comprehensive Attendance Summary
        AppSettings settings = new AppSettings();
        DashboardStats stats = new DashboardStats();
        stats.setTotalStudents(100);
        stats.setPresentToday(85);
        stats.setAbsentToday(15);
        stats.setAverageAttendancePct(82.5);

        List<StudentAttendanceSummary> summaries = new ArrayList<>();
        summaries.add(new StudentAttendanceSummary(1, "24CSE001", "Aarav Kumar", "CSE", 2, "A",
                40, 36, 4, 90.0, "Excellent"));

        List<Attendance> recentAtt = new ArrayList<>();
        recentAtt.add(new Attendance(1, 1, LocalDate.of(2026, 10, 7), "PRESENT"));

        File summaryWbFile = tempDir.resolve("Attendance_Report.xlsx").toFile();
        exportService.exportCompleteAttendanceSummaryWorkbook(
                settings, stats, summaries, recentAtt, summaries, students, summaryWbFile
        );

        assertTrue(summaryWbFile.exists());
        try (FileInputStream fis = new FileInputStream(summaryWbFile);
             Workbook wb = new XSSFWorkbook(fis)) {
            assertNotNull(wb.getSheet("Summary"), "Must have Sheet: Summary");
            assertNotNull(wb.getSheet("Students") != null ? wb.getSheet("Students") : wb.getSheet("Student List"), "Must have Students sheet");
            assertNotNull(wb.getSheet("Attendance") != null ? wb.getSheet("Attendance") : wb.getSheet("Student Attendance"), "Must have Attendance sheet");
            assertNotNull(wb.getSheet("Low Attendance"), "Must have Sheet: Low Attendance");
            assertTrue(wb.getNumberOfSheets() >= 5, "Must have at least 5 sheets in comprehensive workbook");
        }
    }

    @Test
    public void testRealPdfExport(@TempDir Path tempDir) throws Exception {
        PdfExportService pdfService = new PdfExportService();

        // 1. Export Daily Attendance PDF
        List<Attendance> records = new ArrayList<>();
        Attendance a1 = new Attendance(1, 1, LocalDate.of(2026, 10, 7), "PRESENT");
        a1.setRegisterNo("24CSE001");
        a1.setStudentName("Aarav Kumar");
        a1.setDepartment("CSE");
        a1.setYearOfStudy(2);
        a1.setSection("A");
        records.add(a1);

        File dailyPdf = tempDir.resolve("Daily_Attendance_Report.pdf").toFile();
        pdfService.exportDailyAttendancePdf("KIT ENGINEERING COLLEGE", LocalDate.of(2026, 10, 7), "CSE", 2, "A", records, dailyPdf);

        assertTrue(dailyPdf.exists());
        assertTrue(dailyPdf.length() > 500);

        byte[] header = Files.readAllBytes(dailyPdf.toPath());
        String headerStr = new String(header, 0, Math.min(10, header.length));
        assertTrue(headerStr.startsWith("%PDF-"), "Generated file must be a valid PDF");

        // 2. Export Student Profile PDF
        Student s = new Student(1, "24CSE001", "Aarav Kumar", "Male", LocalDate.of(2005, 5, 10),
                "CSE", 2, "A", "aarav@kit.edu.in", "9840123456", "Chennai", LocalDate.of(2024, 8, 1));
        StudentAttendanceSummary sum = new StudentAttendanceSummary(1, "24CSE001", "Aarav Kumar",
                "CSE", 2, "A", 40, 36, 4, 90.0, "Excellent");

        File profilePdf = tempDir.resolve("Student_Profile.pdf").toFile();
        pdfService.exportStudentProfilePdf("KIT ENGINEERING COLLEGE", s, sum, records, profilePdf);
        assertTrue(profilePdf.exists());
        assertTrue(profilePdf.length() > 500);
    }
}
