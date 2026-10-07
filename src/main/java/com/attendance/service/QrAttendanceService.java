package com.attendance.service;

import com.attendance.dao.AttendanceDAO;
import com.attendance.dao.AttendanceSessionDAO;
import com.attendance.model.AttendanceSession;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for QR Code Attendance generation and real-time scanning validation.
 */
public class QrAttendanceService {

    private final AttendanceSessionDAO sessionDAO = new AttendanceSessionDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    /**
     * Generates a new secure attendance session with temporary token and QR image.
     */
    public AttendanceSession createSession(int subjectId, int teacherId, LocalDate date, String period,
                                          String dept, int year, String section, int expiryMinutes) throws SQLException {
        String token = "SAMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(expiryMinutes > 0 ? expiryMinutes : 5);

        AttendanceSession session = new AttendanceSession(
                0, token, subjectId, teacherId, date, period, dept, year, section, now, expiresAt, "ACTIVE"
        );

        boolean created = sessionDAO.create(session);
        if (!created) {
            throw new SQLException("Failed to persist attendance session in database.");
        }
        return session;
    }

    /**
     * Renders a BufferedImage QR code encoding the session token.
     */
    public BufferedImage generateQrImage(String token, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(token, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Student side: validates token and marks PRESENT.
     * Returns "SUCCESS" or specific error message.
     */
    public String scanAndMarkAttendance(String token, int studentId) {
        if (token == null || token.trim().isEmpty()) {
            return "Please provide a valid session token.";
        }
        return attendanceDAO.recordQrAttendance(token.trim(), studentId);
    }

    public AttendanceSession getSessionByToken(String token) throws SQLException {
        return sessionDAO.findByToken(token);
    }

    public List<AttendanceSession> getActiveSessions() throws SQLException {
        return sessionDAO.getActiveSessions();
    }

    public boolean expireSession(int sessionId) throws SQLException {
        return sessionDAO.expireSession(sessionId);
    }

    public BufferedImage generateQrCodeImage(String text, int width, int height) throws Exception {
        return generateQrImage(text, width, height);
    }

    public String validateAndRecordAttendance(String token, int studentId) {
        return scanAndMarkAttendance(token, studentId);
    }

    public int getSessionPresentCount(int subjectId, LocalDate date, String periodName) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE subject_id = ? AND attendance_date = ? AND period = ? AND status = 'PRESENT'";
        try (java.sql.Connection conn = com.attendance.config.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setString(3, periodName);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public boolean closeSession(String token) {
        String sql = "UPDATE attendance_session SET status = 'CLOSED' WHERE token = ?";
        try (java.sql.Connection conn = com.attendance.config.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            return ps.executeUpdate() > 0;
        } catch (Exception ignored) {}
        return false;
    }
}
