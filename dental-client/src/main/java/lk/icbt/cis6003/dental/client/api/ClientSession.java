package lk.icbt.cis6003.dental.client.api;

import lk.icbt.cis6003.dental.common.dto.UserDto;
import lk.icbt.cis6003.dental.common.enums.Role;

/**
 * <b>Singleton pattern</b> - the one place that knows who is signed in.
 *
 * <p><b>Why a Singleton is the right tool here, when it usually is not.</b>
 * A desktop application has exactly one user at the keyboard for the lifetime
 * of the process. That is not an assumption the design imposes; it is a fact
 * about the deployment. Every window - the menu bar, the booking dialog, the
 * billing dialog - needs the signed-in user's name and role, and threading a
 * {@code UserDto} parameter through every constructor would add noise to
 * fifteen classes to express something genuinely global.</p>
 *
 * <p><b>Why it would be wrong on the server.</b> The server handles many users
 * at once, so the equivalent state there lives in the HTTP session and is read
 * through {@code SecurityUtils}, which is thread-bound. The same idea, scoped
 * correctly for each side.</p>
 *
 * <p>Implemented with the initialisation-on-demand holder idiom: the JVM
 * guarantees the holder class is loaded lazily and exactly once, so the
 * instance is thread-safe with no synchronisation on every access.</p>
 */
public final class ClientSession {

    private UserDto currentUser;
    private String serverBaseUrl;

    private ClientSession() {
        // instantiated only by the holder below
    }

    /** Loaded lazily and exactly once by the JVM's class-initialisation lock. */
    private static final class Holder {
        private static final ClientSession INSTANCE = new ClientSession();
    }

    public static ClientSession getInstance() {
        return Holder.INSTANCE;
    }

    public UserDto getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserDto currentUser) {
        this.currentUser = currentUser;
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
    }

    public boolean isSignedIn() {
        return currentUser != null;
    }

    public String getDisplayName() {
        return currentUser == null ? "Not signed in" : currentUser.getFullName();
    }

    public String getUsername() {
        return currentUser == null ? "" : currentUser.getUsername();
    }

    public Role getRole() {
        return currentUser == null ? null : currentUser.getRole();
    }

    public boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    /** Billing is restricted to administrators and receptionists, as on the server. */
    public boolean canHandleBilling() {
        return hasRole(Role.ADMIN) || hasRole(Role.RECEPTIONIST);
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    /** Clears the signed-in user on sign-out. */
    public void clear() {
        this.currentUser = null;
    }
}
