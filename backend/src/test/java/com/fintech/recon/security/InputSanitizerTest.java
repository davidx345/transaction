package com.fintech.recon.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InputSanitizer
 */
class InputSanitizerTest {

    private InputSanitizer inputSanitizer;

    @BeforeEach
    void setUp() {
        inputSanitizer = new InputSanitizer();
    }

    // ========== String Sanitization Tests ==========

    @Test
    @DisplayName("Should sanitize normal string without modification")
    void shouldSanitizeNormalString() {
        String input = "Hello World";
        
        String result = inputSanitizer.sanitizeString(input);
        
        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        String result = inputSanitizer.sanitizeString(null);
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should trim whitespace")
    void shouldTrimWhitespace() {
        String input = "  Hello World  ";
        
        String result = inputSanitizer.sanitizeString(input);
        
        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("Should remove script tags")
    void shouldRemoveScriptTags() {
        String input = "Hello<script>alert('xss')</script>World";
        
        String result = inputSanitizer.sanitizeString(input);
        
        assertThat(result).doesNotContain("<script>");
        assertThat(result).doesNotContain("</script>");
    }

    // ========== SQL Injection Tests ==========

    @ParameterizedTest
    @DisplayName("Should detect SQL injection patterns")
    @ValueSource(strings = {
        "1' OR '1'='1",
        "1; DROP TABLE users;--",
        "1 UNION SELECT * FROM users",
        "admin'--",
        "1' AND 1=1--",
        "'; INSERT INTO users VALUES('hacker');--",
        "1' ORDER BY 1--"
    })
    void shouldDetectSqlInjection(String input) {
        boolean containsSql = inputSanitizer.containsSqlInjection(input);
        
        assertThat(containsSql).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Should not flag normal inputs as SQL injection")
    @ValueSource(strings = {
        "John Doe",
        "user@example.com",
        "Transaction-12345",
        "Order placed successfully",
        "100.50",
        "2024-01-15"
    })
    void shouldNotFlagNormalInputAsSqlInjection(String input) {
        boolean containsSql = inputSanitizer.containsSqlInjection(input);
        
        assertThat(containsSql).isFalse();
    }

    // ========== XSS Tests ==========

    @ParameterizedTest
    @DisplayName("Should detect XSS patterns")
    @ValueSource(strings = {
        "<script>alert('xss')</script>",
        "<img src='x' onerror='alert(1)'>",
        "<body onload='alert(1)'>",
        "<a href='javascript:alert(1)'>click</a>",
        "<div onclick='alert(1)'>",
        "data:text/html,<script>alert('XSS')</script>",
        "<svg/onload=alert('XSS')>"
    })
    void shouldDetectXss(String input) {
        boolean containsXss = inputSanitizer.containsXss(input);
        
        assertThat(containsXss).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Should not flag normal HTML content as XSS")
    @ValueSource(strings = {
        "Hello World",
        "user@example.com",
        "The price is $100",
        "Less than < greater than >",
        "Tom & Jerry"
    })
    void shouldNotFlagNormalContentAsXss(String input) {
        boolean containsXss = inputSanitizer.containsXss(input);
        
        assertThat(containsXss).isFalse();
    }

    // ========== Email Validation Tests ==========

    @ParameterizedTest
    @DisplayName("Should sanitize valid email addresses")
    @CsvSource({
        "user@example.com, user@example.com",
        "  USER@EXAMPLE.COM  , user@example.com",
        "test.user@domain.co.uk, test.user@domain.co.uk"
    })
    void shouldSanitizeValidEmails(String input, String expected) {
        String result = inputSanitizer.sanitizeEmail(input);
        
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("Should throw exception for invalid emails")
    @ValueSource(strings = {
        "invalid-email",
        "@nodomain.com",
        "no@domain",
        "user@.com"
    })
    void shouldThrowExceptionForInvalidEmails(String input) {
        assertThatThrownBy(() -> inputSanitizer.sanitizeEmail(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email format");
    }

    // ========== Transaction Reference Tests ==========

    @ParameterizedTest
    @DisplayName("Should validate correct transaction references")
    @ValueSource(strings = {
        "TXN-12345",
        "REF_ABC_123",
        "PAY-2024-001",
        "ABCD1234"
    })
    void shouldValidateCorrectTransactionReferences(String input) {
        boolean isValid = inputSanitizer.isValidTransactionReference(input);
        
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Should reject invalid transaction references")
    @ValueSource(strings = {
        "txn@123",
        "ref#456",
        "pay$789",
        "<script>",
        "'; DROP TABLE--"
    })
    void shouldRejectInvalidTransactionReferences(String input) {
        boolean isValid = inputSanitizer.isValidTransactionReference(input);
        
        assertThat(isValid).isFalse();
    }

    // ========== Amount Validation Tests ==========

    @ParameterizedTest
    @DisplayName("Should validate correct amounts")
    @CsvSource({
        "100.00, true",
        "0.01, true",
        "999999.99, true",
        "1000, true"
    })
    void shouldValidateCorrectAmounts(String amount, boolean expected) {
        boolean isValid = inputSanitizer.isValidAmount(new java.math.BigDecimal(amount));
        
        assertThat(isValid).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should reject negative amounts")
    void shouldRejectNegativeAmounts() {
        boolean isValid = inputSanitizer.isValidAmount(new java.math.BigDecimal("-100.00"));
        
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject null amounts")
    void shouldRejectNullAmounts() {
        boolean isValid = inputSanitizer.isValidAmount(null);
        
        assertThat(isValid).isFalse();
    }

    // ========== Bank Name Validation Tests ==========

    @ParameterizedTest
    @DisplayName("Should validate correct bank names")
    @ValueSource(strings = {
        "GTBank",
        "Access Bank",
        "First Bank of Nigeria",
        "Zenith Bank Plc"
    })
    void shouldValidateCorrectBankNames(String input) {
        boolean isValid = inputSanitizer.isValidBankName(input);
        
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Should reject invalid bank names")
    @ValueSource(strings = {
        "Bank<script>",
        "Bank'; DROP TABLE",
        "Bank123!@#",
        ""
    })
    void shouldRejectInvalidBankNames(String input) {
        boolean isValid = inputSanitizer.isValidBankName(input);
        
        assertThat(isValid).isFalse();
    }

    // ========== Reference Sanitization Tests ==========

    @Test
    @DisplayName("Should sanitize reference by removing special characters")
    void shouldSanitizeReference() {
        String input = "TXN-123_ABC";
        
        String result = inputSanitizer.sanitizeReference(input);
        
        assertThat(result).isEqualTo("TXN-123_ABC");
    }

    @Test
    @DisplayName("Should remove dangerous characters from reference")
    void shouldRemoveDangerousCharactersFromReference() {
        String input = "TXN<script>123";
        
        String result = inputSanitizer.sanitizeReference(input);
        
        assertThat(result).doesNotContain("<");
        assertThat(result).doesNotContain(">");
    }
}
