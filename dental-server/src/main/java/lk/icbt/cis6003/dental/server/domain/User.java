package lk.icbt.cis6003.dental.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lk.icbt.cis6003.dental.common.enums.Role;

import java.time.LocalDateTime;

/**
 * A member of clinic staff who may log in - requirement 1, "User
 * Authentication".
 *
 * <p>The table is called {@code app_user} because {@code USER} is a reserved
 * word in both H2 and MySQL.</p>
 *
 * <p>Only the BCrypt hash is stored, never the password itself, and
 * {@code failedLoginAttempts}/{@code lockedUntil} implement lock-out after
 * repeated failures so the login form cannot be brute-forced.</p>
 */
@Entity
@Table(name = "app_user",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_app_user_username", columnNames = "username")
       })
public class User extends BaseEntity {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCKOUT_MINUTES = 15;

    @Column(name = "username", nullable = false, length = 30)
    private String username;

    /** BCrypt hash. Never the plain password. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    /** Set when the user account belongs to a practising dentist. */
    @Column(name = "linked_dentist_code", length = 20)
    private String linkedDentistCode;

    public User() {
        // required by JPA
    }

    public User(String username, String passwordHash, String fullName, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    /* ------------------------- behaviour ------------------------------- */

    /** True while the account is temporarily locked after failed logins. */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Records a failed sign-in and locks the account once the threshold is
     * reached. Keeping this rule on the entity means every caller - web form,
     * REST endpoint, future mobile app - gets the same protection.
     */
    public void registerFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
        }
    }

    /** Clears the failure counter and stamps the sign-in time. */
    public void registerSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean hasRole(Role candidate) {
        return this.role == candidate;
    }

    /* ------------------------- accessors ------------------------------- */

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public String getLinkedDentistCode() {
        return linkedDentistCode;
    }

    public void setLinkedDentistCode(String linkedDentistCode) {
        this.linkedDentistCode = linkedDentistCode;
    }

    @Override
    public String toString() {
        return "User{" + username + ", role=" + role + ", active=" + active + "}";
    }
}
