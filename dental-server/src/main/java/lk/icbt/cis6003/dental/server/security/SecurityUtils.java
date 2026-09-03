package lk.icbt.cis6003.dental.server.security;

import lk.icbt.cis6003.dental.common.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Answers "who is doing this?" from anywhere in the business tier.
 *
 * <p>Every appointment, bill and audit entry records the member of staff
 * responsible. Threading a username parameter through every service method
 * would add a parameter to about forty signatures and one more thing for a
 * caller to get wrong; reading it from the {@code SecurityContext} keeps the
 * signatures about the clinic's domain.</p>
 *
 * <p>{@code SecurityContextHolder} is thread-bound, so concurrent requests
 * never see each other's user.</p>
 *
 * <p>The class is {@code final} with a private constructor - it is a static
 * utility and must never be instantiated or subclassed.</p>
 */
public final class SecurityUtils {

    /** Used when work is done by a scheduled job rather than by a person. */
    public static final String SYSTEM_USER = "system";

    private SecurityUtils() {
        throw new AssertionError("SecurityUtils is a utility class and must not be instantiated");
    }

    /** @return the signed-in username, or empty when there is no authenticated user */
    public static Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    /**
     * @return the signed-in username, or {@link #SYSTEM_USER} - so an audit
     *         entry is never written with a null actor
     */
    public static String getCurrentUsernameOrSystem() {
        return getCurrentUsername().orElse(SYSTEM_USER);
    }

    /** @return the full principal, when the caller needs the role or linked dentist */
    public static Optional<ClinicUserDetails> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ClinicUserDetails details)) {
            return Optional.empty();
        }
        return Optional.of(details);
    }

    public static boolean hasRole(Role role) {
        return getCurrentUser().map(u -> u.getRole() == role).orElse(false);
    }

    public static boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
}
