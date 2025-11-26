package com.fintech.recon.api;

import com.fintech.recon.entity.User;
import com.fintech.recon.security.InputSanitizer;
import com.fintech.recon.security.JwtTokenProvider;
import com.fintech.recon.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Authentication controller for login, logout, registration and token refresh
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final InputSanitizer inputSanitizer;

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            String email = inputSanitizer.sanitizeEmail(request.getEmail());
            
            UserService.AuthResult result = userService.authenticate(email, request.getPassword());
            
            log.info("User {} logged in successfully", email);
            
            return ResponseEntity.ok(AuthResponse.builder()
                    .accessToken(result.accessToken())
                    .refreshToken(result.refreshToken())
                    .tokenType("Bearer")
                    .expiresIn(result.expiresIn())
                    .user(UserInfo.builder()
                            .id(result.userId().toString())
                            .username(result.username())
                            .email(result.email())
                            .fullName(result.fullName())
                            .roles(List.copyOf(result.roles()))
                            .build())
                    .build());
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.builder()
                            .error("INVALID_CREDENTIALS")
                            .message("Invalid email or password")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (LockedException e) {
            log.warn("Login attempt for locked account: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.builder()
                            .error("ACCOUNT_LOCKED")
                            .message("Account is locked due to too many failed attempts. Please try again later.")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Register new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String email = inputSanitizer.sanitizeEmail(request.getEmail());
            String username = inputSanitizer.sanitizeString(request.getUsername());
            String fullName = request.getFullName() != null ? 
                    inputSanitizer.sanitizeString(request.getFullName()) : null;
            String companyName = request.getCompanyName() != null ? 
                    inputSanitizer.sanitizeString(request.getCompanyName()) : null;

            UserService.RegisterRequest registerRequest = new UserService.RegisterRequest(
                    username,
                    email,
                    request.getPassword(),
                    fullName,
                    companyName
            );

            UserService.AuthResult result = userService.register(registerRequest);

            log.info("User {} registered successfully", email);

            return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.builder()
                    .accessToken(result.accessToken())
                    .refreshToken(result.refreshToken())
                    .tokenType("Bearer")
                    .expiresIn(result.expiresIn())
                    .user(UserInfo.builder()
                            .id(result.userId().toString())
                            .username(result.username())
                            .email(result.email())
                            .fullName(result.fullName())
                            .roles(List.copyOf(result.roles()))
                            .build())
                    .build());
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.builder()
                            .error("REGISTRATION_FAILED")
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            UserService.AuthResult result = userService.refreshToken(request.getRefreshToken());

            log.info("Token refreshed for user: {}", result.email());

            return ResponseEntity.ok(AuthResponse.builder()
                    .accessToken(result.accessToken())
                    .refreshToken(result.refreshToken())
                    .tokenType("Bearer")
                    .expiresIn(result.expiresIn())
                    .user(UserInfo.builder()
                            .id(result.userId().toString())
                            .username(result.username())
                            .email(result.email())
                            .fullName(result.fullName())
                            .roles(List.copyOf(result.roles()))
                            .build())
                    .build());
        } catch (BadCredentialsException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.builder()
                            .error("INVALID_TOKEN")
                            .message("Invalid or expired refresh token")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.builder()
                            .error("UNAUTHORIZED")
                            .message("Not authenticated")
                            .timestamp(LocalDateTime.now())
                            .build());
        }

        try {
            User user = (User) authentication.getPrincipal();
            UserService.UserProfile profile = userService.getUserProfile(user.getId());

            return ResponseEntity.ok(UserProfileResponse.builder()
                    .id(profile.id().toString())
                    .username(profile.username())
                    .email(profile.email())
                    .fullName(profile.fullName())
                    .companyName(profile.companyName())
                    .roles(List.copyOf(profile.roles()))
                    .emailVerified(profile.emailVerified())
                    .lastLogin(profile.lastLogin())
                    .createdAt(profile.createdAt())
                    .build());
        } catch (Exception e) {
            log.error("Error getting current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_ERROR")
                            .message("Unable to retrieve user profile")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Logout endpoint (invalidates refresh token)
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            userService.logout(user.getId());
            log.info("User {} logged out", user.getEmail());
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(LogoutResponse.builder()
                .message("Logged out successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Validate token endpoint
     */
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@Valid @RequestBody TokenValidationRequest request) {
        boolean valid = jwtTokenProvider.validateToken(request.getToken());
        String userId = null;

        if (valid) {
            try {
                userId = jwtTokenProvider.getUserIdFromToken(request.getToken());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(TokenValidationResponse.builder()
                .valid(valid)
                .userId(userId)
                .build());
    }

    /**
     * Change password endpoint
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.builder()
                            .error("UNAUTHORIZED")
                            .message("Not authenticated")
                            .timestamp(LocalDateTime.now())
                            .build());
        }

        try {
            User user = (User) authentication.getPrincipal();
            userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());

            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Password changed successfully. Please log in again.")
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .error("INVALID_PASSWORD")
                            .message("Current password is incorrect")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    // ========== Request/Response DTOs ==========

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
                 message = "Password must contain at least one uppercase letter, one lowercase letter, and one number")
        private String password;

        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        private String fullName;

        @Size(max = 100, message = "Company name cannot exceed 100 characters")
        private String companyName;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    public static class TokenValidationRequest {
        @NotBlank(message = "Token is required")
        private String token;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                 message = "Password must contain at least one uppercase letter, one lowercase letter, and one number")
        private String newPassword;
    }

    @Data
    @lombok.Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;
        private UserInfo user;
    }

    @Data
    @lombok.Builder
    public static class UserInfo {
        private String id;
        private String username;
        private String email;
        private String fullName;
        private List<String> roles;
    }

    @Data
    @lombok.Builder
    public static class UserProfileResponse {
        private String id;
        private String username;
        private String email;
        private String fullName;
        private String companyName;
        private List<String> roles;
        private boolean emailVerified;
        private LocalDateTime lastLogin;
        private LocalDateTime createdAt;
    }

    @Data
    @lombok.Builder
    public static class LogoutResponse {
        private String message;
        private LocalDateTime timestamp;
    }

    @Data
    @lombok.Builder
    public static class TokenValidationResponse {
        private boolean valid;
        private String userId;
    }

    @Data
    @lombok.Builder
    public static class ErrorResponse {
        private String error;
        private String message;
        private LocalDateTime timestamp;
    }

    @Data
    @lombok.Builder
    public static class MessageResponse {
        private String message;
        private LocalDateTime timestamp;
    }
}
