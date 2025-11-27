package com.taskmgmt.util;

/**
 * Utility class for validation.
 * Contains helper methods for common validation tasks.
 */
public class ValidationUtil {
    
    /**
     * Validates email format.
     * Simple validation - not comprehensive.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.contains("@") && email.contains(".");
    }
    
    /**
     * Validates username format.
     * Username must be alphanumeric and 3-20 characters.
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.length() < 3 || username.length() > 20) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]+$");
    }
    
    /**
     * Sanitizes user input to prevent XSS.
     * Basic implementation - production would use a proper library.
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        // TODO: Use proper HTML escaping library
        return input.replace("<", "&lt;").replace(">", "&gt;");
    }
}

