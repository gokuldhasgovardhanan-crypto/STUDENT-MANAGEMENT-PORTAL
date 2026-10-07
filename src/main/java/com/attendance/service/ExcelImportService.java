package com.attendance.service;

import com.attendance.dao.StudentDAO;
import com.attendance.model.Student;
import com.attendance.util.ValidationUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for parsing, validating, and importing students from Excel (.xlsx) files.
 */
public class ExcelImportService {

    private final StudentDAO studentDAO = new StudentDAO();

    public static class ImportAnalysisResult {
        public int totalRows = 0;
        public int validCount = 0;
        public int duplicateCount = 0;
        public int invalidCount = 0;
        public List<Student> validStudents = new ArrayList<>();
        public List<String> issues = new ArrayList<>();
    }

    /**
     * Parses and validates an Excel file row-by-row before committing to database.
     */
    public ImportAnalysisResult analyzeExcelFile(File excelFile) throws Exception {
        ImportAnalysisResult result = new ImportAnalysisResult();

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                result.issues.add("Excel workbook has no sheets.");
                return result;
            }

            int rowIdx = 0;
            for (Row row : sheet) {
                rowIdx++;
                if (rowIdx == 1) continue; // Skip header row

                String regNo = getCellString(row.getCell(0));
                String name = getCellString(row.getCell(1));
                if (regNo.isEmpty() && name.isEmpty()) continue; // Skip blank line

                result.totalRows++;

                String gender = getCellString(row.getCell(2));
                if (gender.isEmpty()) gender = "Male";
                String dept = getCellString(row.getCell(3));
                String yearStr = getCellString(row.getCell(4));
                String section = getCellString(row.getCell(5));
                String email = getCellString(row.getCell(6));
                String phone = getCellString(row.getCell(7));
                String address = getCellString(row.getCell(8));
                String admDateStr = getCellString(row.getCell(9));

                int year = 1;
                try {
                    year = (int) Double.parseDouble(yearStr.replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {}

                // Validation
                if (!ValidationUtil.isValidRegisterNo(regNo)) {
                    result.invalidCount++;
                    result.issues.add(String.format("Row %d: Invalid Register No '%s'", rowIdx, regNo));
                    continue;
                }
                if (!ValidationUtil.isValidName(name)) {
                    result.invalidCount++;
                    result.issues.add(String.format("Row %d: Invalid Name '%s'", rowIdx, name));
                    continue;
                }
                if (!ValidationUtil.isValidEmail(email)) {
                    result.invalidCount++;
                    result.issues.add(String.format("Row %d: Invalid Email '%s'", rowIdx, email));
                    continue;
                }

                // Duplicate Check
                if (studentDAO.isRegisterNoTaken(regNo, 0)) {
                    result.duplicateCount++;
                    result.issues.add(String.format("Row %d: Duplicate Register No '%s' (already exists in database)", rowIdx, regNo));
                    continue;
                }

                LocalDate dob = LocalDate.of(2004, 1, 1);
                LocalDate admDate = LocalDate.now();
                if (!admDateStr.isEmpty()) {
                    try {
                        admDate = LocalDate.parse(admDateStr);
                    } catch (Exception ignored) {}
                }

                Student student = new Student(
                        0, regNo, name, gender, dob, dept.isEmpty() ? "CSE" : dept,
                        year > 0 ? year : 1, section.isEmpty() ? "A" : section,
                        email, phone, address, admDate
                );

                result.validStudents.add(student);
                result.validCount++;
            }
        }
        return result;
    }

    /**
     * Inserts the verified valid students into the database.
     */
    public int commitValidStudents(List<Student> students) throws Exception {
        int inserted = 0;
        for (Student s : students) {
            if (studentDAO.insert(s)) {
                inserted++;
            }
        }
        return inserted;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}
