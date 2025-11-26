package com.fintech.recon.repository;

import com.fintech.recon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email (case-insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Find user by username (case-insensitive)
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Find user by email or username
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identifier) OR LOWER(u.username) = LOWER(:identifier)")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);

    /**
     * Check if email exists
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Find user by refresh token
     */
    Optional<User> findByRefreshToken(String refreshToken);

    /**
     * Update last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin, u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("lastLogin") LocalDateTime lastLogin);

    /**
     * Update refresh token
     */
    @Modifying
    @Query("UPDATE User u SET u.refreshToken = :refreshToken, u.refreshTokenExpiry = :expiry WHERE u.id = :userId")
    void updateRefreshToken(@Param("userId") UUID userId, 
                           @Param("refreshToken") String refreshToken, 
                           @Param("expiry") LocalDateTime expiry);

    /**
     * Invalidate refresh token (logout)
     */
    @Modifying
    @Query("UPDATE User u SET u.refreshToken = null, u.refreshTokenExpiry = null WHERE u.id = :userId")
    void invalidateRefreshToken(@Param("userId") UUID userId);

    /**
     * Record failed login attempt
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1, " +
           "u.lockedUntil = CASE WHEN u.failedLoginAttempts >= 4 THEN :lockUntil ELSE u.lockedUntil END " +
           "WHERE u.id = :userId")
    void recordFailedLogin(@Param("userId") UUID userId, @Param("lockUntil") LocalDateTime lockUntil);

    /**
     * Unlock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null, u.accountNonLocked = true WHERE u.id = :userId")
    void unlockAccount(@Param("userId") UUID userId);

    /**
     * Find locked accounts that should be unlocked
     */
    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil < :now")
    java.util.List<User> findLockedAccountsToUnlock(@Param("now") LocalDateTime now);
}
