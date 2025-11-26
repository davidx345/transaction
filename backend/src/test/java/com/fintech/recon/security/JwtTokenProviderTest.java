package com.fintech.recon.security;

import com.fintech.recon.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private SecurityProperties securityProperties;
    private User testUser;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        SecurityProperties.JwtConfig jwtConfig = new SecurityProperties.JwtConfig();
        jwtConfig.setSecretKey("test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm");
        jwtConfig.setAccessTokenExpiry(3600000); // 1 hour
        jwtConfig.setRefreshTokenExpiry(86400000); // 24 hours
        jwtConfig.setIssuer("test-issuer");
        securityProperties.setJwt(jwtConfig);

        jwtTokenProvider = new JwtTokenProvider(securityProperties);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("hashedpassword")
                .fullName("Test User")
                .roles(Set.of("USER", "ADMIN"))
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should generate valid access token for user")
    void shouldGenerateValidAccessToken() {
        String token = jwtTokenProvider.generateToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should generate valid refresh token for user")
    void shouldGenerateValidRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {
        String token = jwtTokenProvider.generateToken(testUser);

        String username = jwtTokenProvider.getUsernameFromToken(token);

        assertThat(username).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        String token = jwtTokenProvider.generateToken(testUser);

        String userId = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(testUser.getId().toString());
    }

    @Test
    @DisplayName("Should extract claims from token")
    void shouldExtractClaimsFromToken() {
        String token = jwtTokenProvider.generateToken(testUser);

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
        assertThat(claims.get("userId")).isEqualTo(testUser.getId().toString());
    }

    @Test
    @DisplayName("Should validate token against user details")
    void shouldValidateTokenAgainstUserDetails() {
        String token = jwtTokenProvider.generateToken(testUser);

        boolean isValid = jwtTokenProvider.validateToken(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject tampered token")
    void shouldRejectTamperedToken() {
        String token = jwtTokenProvider.generateToken(testUser);
        // Tamper with the token
        String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify refresh token")
    void shouldCorrectlyIdentifyRefreshToken() {
        String accessToken = jwtTokenProvider.generateToken(testUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        assertThat(jwtTokenProvider.isRefreshToken(accessToken)).isFalse();
        assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Create provider with very short expiry
        SecurityProperties shortExpiry = new SecurityProperties();
        SecurityProperties.JwtConfig jwtConfig = new SecurityProperties.JwtConfig();
        jwtConfig.setSecretKey("test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm");
        jwtConfig.setAccessTokenExpiry(1); // 1 millisecond
        jwtConfig.setRefreshTokenExpiry(1);
        jwtConfig.setIssuer("test-issuer");
        shortExpiry.setJwt(jwtConfig);

        JwtTokenProvider shortExpiryProvider = new JwtTokenProvider(shortExpiry);
        String token = shortExpiryProvider.generateToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(shortExpiryProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Should include roles in token claims")
    void shouldIncludeRolesInTokenClaims() {
        String token = jwtTokenProvider.generateToken(testUser);

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        @SuppressWarnings("unchecked")
        var roles = (java.util.List<String>) claims.get("roles");
        assertThat(roles).containsExactlyInAnyOrder("USER", "ADMIN");
    }
}
