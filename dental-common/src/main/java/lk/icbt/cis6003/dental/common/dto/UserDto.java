package lk.icbt.cis6003.dental.common.dto;

import lk.icbt.cis6003.dental.common.enums.Role;

import java.time.LocalDateTime;

/**
 * The authenticated staff member, as seen by any client.
 *
 * <p>Note what is <em>absent</em>: no password, no password hash, no salt. The
 * DTO exists precisely so that persistence-layer secrets cannot accidentally
 * be serialised onto the network.</p>
 */
public class UserDto {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Role role;
    private boolean active;
    private LocalDateTime lastLoginAt;
    private String sessionId;

    public UserDto() {
        // required by Jackson
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    /** Server-issued session identifier, echoed back so the client can hold it. */
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return fullName + " (" + username + " / " + (role == null ? "?" : role.getDisplayName()) + ")";
    }
}
