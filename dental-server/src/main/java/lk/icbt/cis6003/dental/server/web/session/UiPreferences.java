package lk.icbt.cis6003.dental.server.web.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Reads and writes the {@code clinic-prefs} <b>cookie</b>.
 *
 * <p><b>Why a cookie rather than the session or the database.</b> A
 * receptionist who prefers a compact table wants that setting to survive
 * signing out and coming back tomorrow, so the session is too short-lived. It
 * is also a property of the <em>workstation</em>, not of the person - the front
 * desk machine has a small screen whoever is sitting at it - so storing it
 * against the user account would be wrong as well as heavier. A cookie is
 * exactly the right lifetime and the right scope.</p>
 *
 * <p><b>Security properties, chosen deliberately:</b></p>
 * <ul>
 *   <li>{@code HttpOnly} - unlike the session cookie, this one carries no
 *       authentication, but marking it anyway costs nothing and keeps the
 *       policy uniform;</li>
 *   <li>{@code SameSite=Lax} - it is never sent on a cross-site request;</li>
 *   <li>the value is validated against a fixed set on read, so a hand-edited
 *       cookie cannot inject anything into a template. Cookies are user input
 *       and are treated as such.</li>
 * </ul>
 */
@Component
public class UiPreferences {

    public static final String COOKIE_NAME = "clinic-prefs";

    public static final String DENSITY_COMFORTABLE = "comfortable";
    public static final String DENSITY_COMPACT = "compact";

    /** One year: a display preference does not need re-stating every week. */
    private static final int COOKIE_MAX_AGE_SECONDS = 365 * 24 * 60 * 60;

    /**
     * @return the stored table density, or {@code comfortable} when the cookie
     *         is absent or holds anything unrecognised
     */
    public String readDensity(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return DENSITY_COMFORTABLE;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return sanitise(cookie.getValue());
            }
        }
        return DENSITY_COMFORTABLE;
    }

    /** Writes the preference, rejecting any value that is not one of the two. */
    public void writeDensity(HttpServletResponse response, String density) {
        Cookie cookie = new Cookie(COOKIE_NAME, sanitise(density));
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /** Removes the preference, returning the workstation to the default. */
    public void clear(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Whitelist, not blacklist. Anything unrecognised becomes the default, so a
     * tampered cookie can never reach a template.
     */
    private String sanitise(String value) {
        if (value == null) {
            return DENSITY_COMFORTABLE;
        }
        String normalised = value.trim().toLowerCase(Locale.ROOT);
        return DENSITY_COMPACT.equals(normalised) ? DENSITY_COMPACT : DENSITY_COMFORTABLE;
    }
}
