package com.attendance.util;

import org.mindrot.jbcrypt.BCrypt;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Utility for secure password hashing and verification using BCrypt with legacy SHA-256 fallback.
 */
public class PasswordUtil {

    /**
     * Hashes a plain text password using BCrypt.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    /**
     * Checks if a plain password matches the stored hash.
     * Supports BCrypt hashes as well as legacy SHA-256 for smooth operation.
     */
    public static boolean checkPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }

        // Standard BCrypt hash (starts with $2a$, $2b$, or $2y$)
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            try {
                return BCrypt.checkpw(plainPassword, storedHash);
            } catch (Exception e) {
                return false;
            }
        }

        // Fallback: SHA-256 check
        if (storedHash.length() == 64) {
            String sha256 = hashSha256(plainPassword);
            return sha256.equalsIgnoreCase(storedHash);
        }

        // Direct comparison fallback for plain development seeds if needed
        return plainPassword.equals(storedHash);
    }

    private static String hashSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
