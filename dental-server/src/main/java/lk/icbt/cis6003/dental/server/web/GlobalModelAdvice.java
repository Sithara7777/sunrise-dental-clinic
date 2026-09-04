package lk.icbt.cis6003.dental.server.web;

import jakarta.servlet.http.HttpServletRequest;
import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.server.security.ClinicUserDetails;
import lk.icbt.cis6003.dental.server.security.SecurityUtils;
import lk.icbt.cis6003.dental.server.web.session.RecentlyViewedTracker;
import lk.icbt.cis6003.dental.server.web.session.UiPreferences;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;

/**
 * Supplies the model attributes every page needs.
 *
 * <p>The clinic name, the signed-in user, the density preference read from the
 * cookie and the session's recently-viewed trail appear in the header of every
 * screen. Adding them here means no controller has to remember to put them in
 * its model - and a new page cannot accidentally render with a broken
 * header.</p>
 *
 * <p>Scoped to the {@code web} package so it never touches the REST
 * controllers, whose responses must contain only the data they declare.</p>
 */
@ControllerAdvice(basePackages = "lk.icbt.cis6003.dental.server.web")
public class GlobalModelAdvice {

    private final UiPreferences uiPreferences;
    private final RecentlyViewedTracker recentlyViewedTracker;

    public GlobalModelAdvice(UiPreferences uiPreferences,
                             RecentlyViewedTracker recentlyViewedTracker) {
        this.uiPreferences = uiPreferences;
        this.recentlyViewedTracker = recentlyViewedTracker;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model, HttpServletRequest request) {
        model.addAttribute("clinicName", ClinicConstants.CLINIC_NAME);
        model.addAttribute("clinicPhone", ClinicConstants.CLINIC_PHONE);
        model.addAttribute("currencySymbol", ClinicConstants.CURRENCY_SYMBOL);
        model.addAttribute("today", LocalDate.now());

        // Read from the cookie on every request - the workstation preference.
        model.addAttribute("density", uiPreferences.readDensity(request));

        // Read from the session - this user's own trail.
        model.addAttribute("recentlyViewed", recentlyViewedTracker.list());

        /*
         * The header is rendered from these model attributes rather than from
         * sec:authentication="principal.fullName".
         *
         * Reading a property straight off the principal assumes the principal
         * is always a ClinicUserDetails. It usually is - but a remember-me
         * token, a future single-sign-on provider or a test double need not be,
         * and when it is not, Thymeleaf raises NotReadablePropertyException and
         * the WHOLE PAGE returns 500 because of the header. Resolving the name
         * here, with a fallback to the plain username, means an unexpected
         * principal type degrades to a less friendly greeting instead of taking
         * the page down.
         */
        String displayName = SecurityUtils.getCurrentUser()
                .map(ClinicUserDetails::getFullName)
                .orElseGet(() -> SecurityUtils.getCurrentUsername().orElse(null));

        String displayRole = SecurityUtils.getCurrentUser()
                .map(ClinicUserDetails::getRoleDisplayName)
                .orElse("");

        if (displayName != null) {
            model.addAttribute("currentUserName", displayName);
            model.addAttribute("currentUserRole", displayRole);
            SecurityUtils.getCurrentUser().ifPresent(user -> model.addAttribute("currentUser", user));
        }

        model.addAttribute("requestUri", request.getRequestURI());
    }

    /**
     * The raw principal, for the few templates that need more than the name.
     *
     * <p>May legitimately be {@code null} when the authenticated principal is
     * not one of ours, so templates must guard before dereferencing it.</p>
     */
    @ModelAttribute("principal")
    public ClinicUserDetails principal() {
        return SecurityUtils.getCurrentUser().orElse(null);
    }
}
