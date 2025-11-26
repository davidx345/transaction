package com.fintech.recon.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Security configuration properties
 */
@Component
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityProperties {

    /**
     * JWT configuration
     */
    private Jwt jwt = new Jwt();

    /**
     * Rate limiting configuration
     */
    private RateLimiting rateLimiting = new RateLimiting();

    /**
     * CORS configuration
     */
    private Cors cors = new Cors();

    /**
     * Password policy configuration
     */
    private PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Data
    public static class Jwt {
        /**
         * Secret key for signing JWTs (should be at least 256 bits for HS256)
         */
        private String secret = "default-secret-key-change-in-production-must-be-at-least-256-bits";

        /**
         * JWT expiration time in milliseconds (default: 24 hours)
         */
        private long expirationMs = 86400000;

        /**
         * Refresh token expiration time in milliseconds (default: 7 days)
         */
        private long refreshExpirationMs = 604800000;

        /**
         * JWT issuer
         */
        private String issuer = "fintech-recon-engine";

        /**
         * JWT audience
         */
        private String audience = "fintech-recon-api";
    }

    @Data
    public static class RateLimiting {
        /**
         * Enable/disable rate limiting
         */
        private boolean enabled = true;

        /**
         * Requests per minute for general API
         */
        private int requestsPerMinute = 100;

        /**
         * Requests per minute for authentication endpoints
         */
        private int authRequestsPerMinute = 10;

        /**
         * Requests per minute for file upload endpoints
         */
        private int uploadRequestsPerMinute = 20;

        /**
         * Requests per minute for report generation
         */
        private int reportRequestsPerMinute = 30;
    }

    @Data
    public static class Cors {
        /**
         * Allowed origins
         */
        private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "https://fintech-recon.vercel.app"
        ));

        /**
         * Allowed HTTP methods
         */
        private List<String> allowedMethods = new ArrayList<>(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        /**
         * Allowed headers
         */
        private List<String> allowedHeaders = new ArrayList<>(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "X-CSRF-Token",
            "X-Request-Id"
        ));

        /**
         * Max age of CORS preflight cache in seconds
         */
        private long maxAge = 3600;

        /**
         * Allow credentials (cookies, authorization headers)
         */
        private boolean allowCredentials = true;
    }

    @Data
    public static class PasswordPolicy {
        /**
         * Minimum password length
         */
        private int minLength = 8;

        /**
         * Require uppercase letter
         */
        private boolean requireUppercase = true;

        /**
         * Require lowercase letter
         */
        private boolean requireLowercase = true;

        /**
         * Require digit
         */
        private boolean requireDigit = true;

        /**
         * Require special character
         */
        private boolean requireSpecial = true;

        /**
         * Maximum consecutive identical characters
         */
        private int maxConsecutiveChars = 3;

        /**
         * BCrypt strength (4-31, higher = more secure but slower)
         */
        private int bcryptStrength = 12;
    }
}
