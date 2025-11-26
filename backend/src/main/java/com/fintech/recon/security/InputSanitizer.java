package com.fintech.recon.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Input sanitization utilities to prevent injection attacks
 */
@Component
@Slf4j
public class InputSanitizer {

    // Patterns for validation
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{1,100}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
    private static final Pattern BANK_CODE_PATTERN = Pattern.compile("^[A-Z]{2,20}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    
    // SQL injection patterns to detect
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|EXEC|EXECUTE|CREATE|TRUNCATE)\\b)|" +
        "(--|;|/\\*|\\*/|'|\"|\\\\'|\\\\\")"
    );
    
    // XSS patterns to detect
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|javascript:|on\\w+=|<iframe|<object|<embed|<form|<input|<button)"
    );

    /**
     * Sanitize transaction reference
     */
    public String sanitizeReference(String reference) {
        if (!StringUtils.hasText(reference)) {
            return null;
        }
        
        // Remove any whitespace
        String cleaned = reference.trim().toUpperCase();
        
        // Validate pattern
        if (!REFERENCE_PATTERN.matcher(cleaned).matches()) {
            log.warn("Invalid reference format detected: {}", maskSensitive(reference));
            throw new IllegalArgumentException("Invalid reference format");
        }
        
        // Check for injection attempts
        if (containsSqlInjection(cleaned) || containsXss(cleaned)) {
            log.error("Injection attempt detected in reference: {}", maskSensitive(reference));
            throw new SecurityException("Invalid input detected");
        }
        
        return cleaned;
    }

    /**
     * Sanitize email address
     */
    public String sanitizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        
        String cleaned = email.trim().toLowerCase();
        
        if (!EMAIL_PATTERN.matcher(cleaned).matches()) {
            log.warn("Invalid email format: {}", maskEmail(email));
            throw new IllegalArgumentException("Invalid email format");
        }
        
        if (containsSqlInjection(cleaned) || containsXss(cleaned)) {
            log.error("Injection attempt detected in email: {}", maskEmail(email));
            throw new SecurityException("Invalid input detected");
        }
        
        return cleaned;
    }

    /**
     * Sanitize phone number
     */
    public String sanitizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        
        // Remove spaces, dashes, parentheses
        String cleaned = phone.replaceAll("[\\s\\-()]", "");
        
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            log.warn("Invalid phone format: {}", maskPhone(phone));
            throw new IllegalArgumentException("Invalid phone format");
        }
        
        return cleaned;
    }

    /**
     * Sanitize bank code
     */
    public String sanitizeBankCode(String bankCode) {
        if (!StringUtils.hasText(bankCode)) {
            return null;
        }
        
        String cleaned = bankCode.trim().toUpperCase();
        
        if (!BANK_CODE_PATTERN.matcher(cleaned).matches()) {
            log.warn("Invalid bank code format: {}", bankCode);
            throw new IllegalArgumentException("Invalid bank code format");
        }
        
        return cleaned;
    }

    /**
     * Sanitize UUID
     */
    public String sanitizeUuid(String uuid) {
        if (!StringUtils.hasText(uuid)) {
            return null;
        }
        
        String cleaned = uuid.trim().toLowerCase();
        
        if (!UUID_PATTERN.matcher(cleaned).matches()) {
            log.warn("Invalid UUID format: {}", uuid);
            throw new IllegalArgumentException("Invalid UUID format");
        }
        
        return cleaned;
    }

    /**
     * Sanitize general text input
     */
    public String sanitizeText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        
        // Trim and limit length
        String cleaned = text.trim();
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        
        // Check for injection attempts
        if (containsSqlInjection(cleaned) || containsXss(cleaned)) {
            log.error("Injection attempt detected in text input");
            throw new SecurityException("Invalid input detected");
        }
        
        // Escape HTML entities
        return escapeHtml(cleaned);
    }

    /**
     * Sanitize filename for file uploads
     */
    public String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        
        // Remove path traversal characters
        String cleaned = filename.replaceAll("[/\\\\]", "");
        
        // Remove null bytes
        cleaned = cleaned.replaceAll("\0", "");
        
        // Only allow safe characters
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Limit length
        if (cleaned.length() > 255) {
            cleaned = cleaned.substring(0, 255);
        }
        
        return cleaned;
    }

    /**
     * Check for SQL injection patterns
     */
    public boolean containsSqlInjection(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Check for XSS patterns
     */
    public boolean containsXss(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * Escape HTML entities
     */
    public String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    // Masking methods for logging
    
    private String maskSensitive(String value) {
        if (value == null || value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.***";
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];
        
        String maskedUsername = username.length() > 2 
            ? username.substring(0, 2) + "***" 
            : "***";
        
        return maskedUsername + "@" + domain;
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}
