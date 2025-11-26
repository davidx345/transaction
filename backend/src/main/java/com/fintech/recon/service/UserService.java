package com.fintech.recon.service;

import com.fintech.recon.entity.User;
import com.fintech.recon.repository.UserRepository;
import com.fintech.recon.security.JwtTokenProvider;
import com.fintech.recon.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service for user authentication and management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailOrUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Authenticate user and return tokens
     */
    @Transactional
    public AuthResult authenticate(String identifier, String password) {
        log.info("Authentication attempt for: {}", identifier);

        User user = userRepository.findByEmailOrUsername(identifier)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check if account is locked
        if (!user.isAccountNonLocked()) {
            log.warn("Login attempt for locked account: {}", identifier);
            throw new LockedException("Account is locked. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            user.recordFailedLogin();
            userRepository.save(user);
            log.warn("Failed login attempt for: {}", identifier);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Check if account is enabled
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        // Record successful login
        user.recordSuccessfulLogin();

        // Generate tokens
        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Store refresh token
        LocalDateTime refreshExpiry = LocalDateTime.now()
                .plusSeconds(securityProperties.getJwt().getRefreshTokenExpiry() / 1000);
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(refreshExpiry);

        userRepository.save(user);

        log.info("Successful authentication for: {}", identifier);

        return new AuthResult(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles(),
                accessToken,
                refreshToken,
                securityProperties.getJwt().getAccessTokenExpiry()
        );
    }

    /**
     * Register new user
     */
    @Transactional
    public AuthResult register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.email());

        // Check if email exists
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Check if username exists
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }

        // Create user
        User user = User.builder()
                .username(request.username())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .companyName(request.companyName())
                .roles(Set.of("USER"))
                .enabled(true)
                .emailVerified(false)
                .passwordChangedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        log.info("User registered successfully: {}", user.getEmail());

        // Generate tokens
        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Store refresh token
        LocalDateTime refreshExpiry = LocalDateTime.now()
                .plusSeconds(securityProperties.getJwt().getRefreshTokenExpiry() / 1000);
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(refreshExpiry);
        userRepository.save(user);

        return new AuthResult(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles(),
                accessToken,
                refreshToken,
                securityProperties.getJwt().getAccessTokenExpiry()
        );
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public AuthResult refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        // Find user by refresh token
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Refresh token not found"));

        // Check if refresh token is expired in database
        if (user.getRefreshTokenExpiry() != null && 
            LocalDateTime.now().isAfter(user.getRefreshTokenExpiry())) {
            throw new BadCredentialsException("Refresh token expired");
        }

        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateToken(user);

        log.info("Token refreshed for user: {}", user.getEmail());

        return new AuthResult(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles(),
                newAccessToken,
                refreshToken,  // Return same refresh token
                securityProperties.getJwt().getAccessTokenExpiry()
        );
    }

    /**
     * Logout user (invalidate refresh token)
     */
    @Transactional
    public void logout(UUID userId) {
        userRepository.invalidateRefreshToken(userId);
        log.info("User logged out: {}", userId);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(UUID userId) {
        return userRepository.findById(userId);
    }

    /**
     * Get user profile
     */
    @Transactional(readOnly = true)
    public UserProfile getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new UserProfile(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getCompanyName(),
                user.getRoles(),
                user.isEmailVerified(),
                user.getLastLogin(),
                user.getCreatedAt()
        );
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserProfile updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.companyName() != null) {
            user.setCompanyName(request.companyName());
        }

        userRepository.save(user);

        return getUserProfile(userId);
    }

    /**
     * Change password
     */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        // Invalidate refresh token to force re-login
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);

        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    // ========== DTOs ==========

    public record AuthResult(
            UUID userId,
            String username,
            String email,
            String fullName,
            Set<String> roles,
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {}

    public record RegisterRequest(
            String username,
            String email,
            String password,
            String fullName,
            String companyName
    ) {}

    public record UserProfile(
            UUID id,
            String username,
            String email,
            String fullName,
            String companyName,
            Set<String> roles,
            boolean emailVerified,
            LocalDateTime lastLogin,
            LocalDateTime createdAt
    ) {}

    public record UpdateProfileRequest(
            String fullName,
            String companyName
    ) {}
}
