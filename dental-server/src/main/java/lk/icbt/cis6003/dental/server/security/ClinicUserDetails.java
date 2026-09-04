package lk.icbt.cis6003.dental.server.security;

import lk.icbt.cis6003.dental.common.enums.Role;
import lk.icbt.cis6003.dental.server.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * <b>Adapter</b> from the clinic's {@link User} entity onto Spring Security's
 * {@link UserDetails} contract.
 *
 * <p>Spring Security must be told whether an account is enabled, expired or
 * locked; the clinic models the same ideas as {@code active} and
 * {@code lockedUntil}. Rather than contorting the entity to satisfy a framework
 * interface, this class translates between the two - the same reasoning applied
 * to the messaging gateways.</p>
 *
 * <p>It also carries the clinic-specific extras the framework has no place
 * for - the display name shown in the header, and the dentist code that lets a
 * dentist see their own diary - so controllers can read them straight from the
 * principal.</p>
 *
 * <p>The password hash is held because the authentication provider needs it to
 * verify a login, and is erased by Spring Security immediately afterwards.</p>
 */
public class ClinicUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String email;
    private final Role role;
    private final boolean active;
    private final boolean locked;
    private final String linkedDentistCode;

    public ClinicUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.active = user.isActive();
        this.locked = user.isLocked();
        this.linkedDentistCode = user.getLinkedDentistCode();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Reflects the temporary lock-out applied after repeated failed logins. */
    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** A member of staff who has left is deactivated, never deleted - their
     *  name must remain readable on the appointments they created. */
    @Override
    public boolean isEnabled() {
        return active;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    /** Set when this account belongs to a practising dentist. */
    public String getLinkedDentistCode() {
        return linkedDentistCode;
    }

    /** Convenience for the Thymeleaf header. */
    public String getRoleDisplayName() {
        return role == null ? "" : role.getDisplayName();
    }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
