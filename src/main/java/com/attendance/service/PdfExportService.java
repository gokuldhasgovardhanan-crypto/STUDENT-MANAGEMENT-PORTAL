package com.attendance.service;

import com.attendance.model.Attendance;
import com.attendance.model.Student;
import com.attendance.model.StudentAttendanceSummary;
import com.attendance.util.DateUtil;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for generating polished PDF reports using OpenPDF.
 */
public class PdfExportService {

    private static final Color PRIMARY_COLOR = new Color(168, 28, 28); // KIT Crimson Red
    private static final Color HEADER_BG = new Color(153, 27, 27); // Rich Collegiate Red
    private static final Color ALT_ROW_BG = new Color(254, 250, 250); // Clean warm white
    private static final Color BORDER_COLOR = new Color(229, 215, 215);
    private static final Color PRESENT_COLOR = new Color(34, 139, 34);
    private static final Color ABSENT_COLOR = new Color(220, 38, 38);

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARY_COLOR);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(50, 50, 50));
    private static final Font META_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
    private static final Font TH_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font TD_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font TD_BOLD = new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK);

    private static class PageFooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();
            String footerText = "KIT Engineering College — Student Attendance Portal | Page " + writer.getPageNumber();
            cb.beginText();
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                cb.setFontAndSize(bf, 8);
                cb.setColorFill(Color.GRAY);
                cb.showTextAligned(Element.ALIGN_CENTER, footerText, (document.right() + document.left()) / 2, document.bottom() - 15, 0);
            } catch (Exception ignored) {}
            cb.endText();
            cb.restoreState();
        }
    }

    public void exportDailyAttendancePdf(String collegeName, LocalDate date, String dept, Integer year, String section,
                                         List<Attendance> records, File targetFile) throws IOException {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(targetFile));
            writer.setPageEvent(new PageFooterEvent());
            document.open();

            addHeader(document, collegeName, "Daily Attendance Report",
                    "Date: " + DateUtil.formatDisplayDate(date) +
                    " | Department: " + (dept != null ? dept : "All") +
                    " | Year: " + (year != null && year > 0 ? year : "All") +
                    " | Section: " + (section != null ? section : "All"));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.5f, 4.0f, 1.8f, 1.2f, 1.2f, 2.0f});

            addTableHeader(table, new String[]{"S.No", "Register No", "Student Name", "Dept", "Year", "Sec", "Status"});

            int sno = 1;
            boolean alt = false;
            int presentCount = 0;
            int absentCount = 0;

            for (Attendance a : records) {
                Color bg = alt ? ALT_ROW_BG : Color.WHITE;
                addCell(table, String.valueOf(sno++), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, a.getRegisterNo(), TD_BOLD, Element.ALIGN_LEFT, bg);
                addCell(table, a.getStudentName(), TD_FONT, Element.ALIGN_LEFT, bg);
                addCell(table, a.getDepartment(), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, String.valueOf(a.getYearOfStudy()), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, a.getSection(), TD_FONT, Element.ALIGN_CENTER, bg);

                boolean isPres = "PRESENT".equalsIgnoreCase(a.getStatus());
                if (isPres) presentCount++; else absentCount++;
                Font stFont = new Font(Font.HELVETICA, 8, Font.BOLD, isPres ? PRESENT_COLOR : ABSENT_COLOR);
                addCell(table, a.getStatus(), stFont, Element.ALIGN_CENTER, bg);

                alt = !alt;
            }

            document.add(table);

            // Summary stats footer
            document.add(new Paragraph(" "));
            Paragraph summary = new Paragraph(
                    String.format("Total Students: %d   |   Present: %d   |   Absent: %d   |   Daily Attendance Rate: %.1f%%",
                            records.size(), presentCount, absentCount,
                            records.isEmpty() ? 0.0 : (double) presentCount / records.size() * 100.0),
                    SUBTITLE_FONT
            );
            summary.setAlignment(Element.ALIGN_RIGHT);
            document.add(summary);

        } catch (DocumentException e) {
            throw new IOException("Failed to generate Daily Attendance PDF", e);
        } finally {
            document.close();
        }
    }

    public void exportStudentProfilePdf(String collegeName, Student student, StudentAttendanceSummary summary,
                                       List<Attendance> history, File targetFile) throws IOException {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(targetFile));
            writer.setPageEvent(new PageFooterEvent());
            document.open();

            addHeader(document, collegeName, "Individual Student Attendance Profile",
                    "Register No: " + student.getRegisterNo() + " | Student Name: " + student.getStudentName());

            // Details Box
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15);
            infoTable.setWidths(new float[]{2.5f, 3.5f, 2.5f, 3.5f});

            addInfoRow(infoTable, "Department:", student.getDepartment(), "Year / Section:", student.getYearOfStudy() + " / " + student.getSection());
            addInfoRow(infoTable, "Email Address:", student.getEmail(), "Phone Number:", student.getPhoneNumber());
            addInfoRow(infoTable, "Total Working Days:", String.valueOf(summary.getTotalWorkingDays()), "Present Days:", String.valueOf(summary.getPresentDays()));
            addInfoRow(infoTable, "Absent Days:", String.valueOf(summary.getAbsentDays()), "Attendance Percentage:", String.format("%.2f%%", summary.getAttendancePercentage()));
            addInfoRow(infoTable, "Current Standing:", summary.getStatus(), "Admission Date:", DateUtil.formatDisplayDate(student.getAdmissionDate()));
            document.add(infoTable);

            // History Table
            Paragraph histHeading = new Paragraph("Detailed Attendance Log", SUBTITLE_FONT);
            histHeading.setSpacingAfter(8);
            document.add(histHeading);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 4.0f, 3.0f});
            addTableHeader(table, new String[]{"S.No", "Date", "Status"});

            int sno = 1;
            boolean alt = false;
            for (Attendance a : history) {
                Color bg = alt ? ALT_ROW_BG : Color.WHITE;
                addCell(table, String.valueOf(sno++), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, DateUtil.formatDisplayDate(a.getAttendanceDate()), TD_FONT, Element.ALIGN_CENTER, bg);
                boolean isPres = "PRESENT".equalsIgnoreCase(a.getStatus());
                Font stFont = new Font(Font.HELVETICA, 8, Font.BOLD, isPres ? PRESENT_COLOR : ABSENT_COLOR);
                addCell(table, a.getStatus(), stFont, Element.ALIGN_CENTER, bg);
                alt = !alt;
            }
            document.add(table);

        } catch (DocumentException e) {
            throw new IOException("Failed to generate Student Profile PDF", e);
        } finally {
            document.close();
        }
    }

    public void exportAttendanceSummaryPdf(String collegeName, String reportTitle, String subtitle,
                                          List<StudentAttendanceSummary> summaries, File targetFile) throws IOException {
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 36, 36); // Landscape for wide tables
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(targetFile));
            writer.setPageEvent(new PageFooterEvent());
            document.open();

            addHeader(document, collegeName, reportTitle, subtitle);

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.5f, 4.5f, 1.8f, 1.0f, 1.0f, 1.5f, 1.5f, 2.2f});

            addTableHeader(table, new String[]{
                    "S.No", "Register No", "Student Name", "Dept", "Year", "Sec", "Working Days", "Present", "Attendance %"
            });

            int sno = 1;
            boolean alt = false;
            for (StudentAttendanceSummary s : summaries) {
                Color bg = alt ? ALT_ROW_BG : Color.WHITE;
                addCell(table, String.valueOf(sno++), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, s.getRegisterNo(), TD_BOLD, Element.ALIGN_LEFT, bg);
                addCell(table, s.getStudentName(), TD_FONT, Element.ALIGN_LEFT, bg);
                addCell(table, s.getDepartment(), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, String.valueOf(s.getYearOfStudy()), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, s.getSection(), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, String.valueOf(s.getTotalWorkingDays()), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, String.valueOf(s.getPresentDays()), TD_FONT, Element.ALIGN_CENTER, bg);

                Color pctCol = s.getAttendancePercentage() >= 75.0 ? PRESENT_COLOR : ABSENT_COLOR;
                Font pctFont = new Font(Font.HELVETICA, 8, Font.BOLD, pctCol);
                addCell(table, String.format("%.2f%% (%s)", s.getAttendancePercentage(), s.getStatus()), pctFont, Element.ALIGN_CENTER, bg);

                alt = !alt;
            }

            document.add(table);

            document.add(new Paragraph(" "));
            Paragraph totalFooter = new Paragraph("Total Records Evaluated: " + summaries.size(), SUBTITLE_FONT);
            totalFooter.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalFooter);

        } catch (DocumentException e) {
            throw new IOException("Failed to generate Attendance Summary PDF", e);
        } finally {
            document.close();
        }
    }

    public void exportStudentAttendanceCardPdf(String collegeName, Student student,
                                               List<java.util.Map<String, Object>> subjectBreakdown,
                                               double overallPct, String status, File targetFile) throws IOException {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(targetFile));
            writer.setPageEvent(new PageFooterEvent());
            document.open();

            addHeader(document, collegeName != null ? collegeName : "KIT ENGINEERING COLLEGE",
                    "OFFICIAL STUDENT ATTENDANCE CARD",
                    "Student: " + student.getStudentName() + " (" + student.getRegisterNo() + ")");

            // Info Table
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(12);
            infoTable.setWidths(new float[]{2.5f, 3.5f, 2.5f, 3.5f});

            addInfoRow(infoTable, "Student Name:", student.getStudentName(), "Register Number:", student.getRegisterNo());
            addInfoRow(infoTable, "Department:", student.getDepartment(), "Year / Section:", student.getYearOfStudy() + " / " + student.getSection());
            addInfoRow(infoTable, "Overall Attendance:", String.format("%.2f%%", overallPct), "Academic Standing:", status);
            addInfoRow(infoTable, "Email Address:", student.getEmail() != null ? student.getEmail() : "N/A", "Phone Number:", student.getPhoneNumber() != null ? student.getPhoneNumber() : "N/A");
            document.add(infoTable);

            // Subject breakdown
            Paragraph subHead = new Paragraph("Subject-Wise Attendance Breakdown", SUBTITLE_FONT);
            subHead.setSpacingBefore(5);
            subHead.setSpacingAfter(8);
            document.add(subHead);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.5f, 5.0f, 2.0f, 2.0f, 2.5f});
            addTableHeader(table, new String[]{"S.No", "Subject Code", "Subject Name", "Conducted", "Attended", "Attendance %"});

            int sno = 1;
            boolean alt = false;
            for (java.util.Map<String, Object> map : subjectBreakdown) {
                Color bg = alt ? ALT_ROW_BG : Color.WHITE;
                addCell(table, String.valueOf(sno++), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, (String) map.get("subjectCode"), TD_BOLD, Element.ALIGN_LEFT, bg);
                addCell(table, (String) map.get("subjectName"), TD_FONT, Element.ALIGN_LEFT, bg);
                addCell(table, String.valueOf(map.get("conducted")), TD_FONT, Element.ALIGN_CENTER, bg);
                addCell(table, String.valueOf(map.get("attended")), TD_FONT, Element.ALIGN_CENTER, bg);

                double pct = (Double) map.get("percentage");
                Color pctCol = pct >= 75.0 ? PRESENT_COLOR : ABSENT_COLOR;
                Font pctFont = new Font(Font.HELVETICA, 8, Font.BOLD, pctCol);
                addCell(table, String.format("%.1f%%", pct), pctFont, Element.ALIGN_CENTER, bg);

                alt = !alt;
            }
            document.add(table);

            // Signatures block
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            PdfPTable sigTable = new PdfPTable(3);
            sigTable.setWidthPercentage(100);
            sigTable.setWidths(new float[]{3.3f, 3.3f, 3.3f});

            addSignatureCell(sigTable, "Class Advisor / Mentor");
            addSignatureCell(sigTable, "Head of Department (HOD)");
            addSignatureCell(sigTable, "Principal / Dean Academic");
            document.add(sigTable);

        } catch (DocumentException e) {
            throw new IOException("Failed to generate Student Attendance Card PDF", e);
        } finally {
            document.close();
        }
    }

    private void addSignatureCell(PdfPTable table, String title) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(25);

        Paragraph line = new Paragraph("___________________________", META_FONT);
        line.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(line);

        Paragraph text = new Paragraph(title, TD_BOLD);
        text.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(text);

        table.addCell(cell);
    }

    private void addHeader(Document doc, String collegeName, String title, String subtitle) throws DocumentException {
        Paragraph col = new Paragraph(collegeName, TITLE_FONT);
        col.setAlignment(Element.ALIGN_CENTER);
        doc.add(col);

        Paragraph tit = new Paragraph(title, SUBTITLE_FONT);
        tit.setAlignment(Element.ALIGN_CENTER);
        tit.setSpacingBefore(3);
        doc.add(tit);

        if (subtitle != null && !subtitle.isEmpty()) {
            Paragraph sub = new Paragraph(subtitle, META_FONT);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingBefore(2);
            doc.add(sub);
        }

        Paragraph gen = new Paragraph("Report Generated: " + DateUtil.formatCurrentTimestamp(), META_FONT);
        gen.setAlignment(Element.ALIGN_RIGHT);
        gen.setSpacingBefore(4);
        gen.setSpacingAfter(12);
        doc.add(gen);
    }

    private void addTableHeader(PdfPTable table, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TH_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
    }

    private void addCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addInfoRow(PdfPTable table, String l1, String v1, String l2, String v2) {
        addInfoCell(table, l1, true);
        addInfoCell(table, v1, false);
        addInfoCell(table, l2, true);
        addInfoCell(table, v2, false);
    }

    private void addInfoCell(PdfPTable table, String text, boolean isLabel) {
        PdfPCell cell = new PdfPCell(new Phrase(text, isLabel ? TD_BOLD : TD_FONT));
        cell.setBackgroundColor(isLabel ? ALT_ROW_BG : Color.WHITE);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }
}
