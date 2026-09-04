package lk.icbt.cis6003.dental.server.web;

import jakarta.servlet.http.HttpServletResponse;
import lk.icbt.cis6003.dental.server.security.SecurityUtils;
import lk.icbt.cis6003.dental.server.web.session.UiPreferences;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The browser's sign-in, sign-out and access-denied pages - requirement 1.
 *
 * <p>The controller does not verify the password itself. Spring Security's
 * filter chain does that before this class is reached; the controller only
 * renders the form and turns the query-string flags Spring Security appends
 * ({@code ?error}, {@code ?logout}, {@code ?expired}) into a sentence.</p>
 */
@Controller
public class AuthWebController {

    private final UiPreferences uiPreferences;

    public AuthWebController(UiPreferences uiPreferences) {
        this.uiPreferences = uiPreferences;
    }

    /** Sends an already-authenticated visitor straight to their work. */
    @GetMapping("/")
    public String root() {
        return SecurityUtils.getCurrentUsername().isPresent()
                ? "redirect:/dashboard"
                : "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String expired,
                        Model model) {

        if (SecurityUtils.getCurrentUsername().isPresent()) {
            return "redirect:/dashboard";
        }

        if (error != null) {
            // Identical wording whichever field was wrong - the login form must
            // not become a tool for discovering valid usernames.
            model.addAttribute("errorMessage",
                    "Invalid username or password. After five failed attempts the account is "
                            + "locked for 15 minutes.");
        }
        if (logout != null) {
            model.addAttribute("infoMessage", "You have been signed out successfully.");
        }
        if (expired != null) {
            model.addAttribute("infoMessage",
                    "Your session ended because it was idle for 30 minutes, or you signed in "
                            + "elsewhere. Please sign in again.");
        }
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("errorMessage",
                "Your role does not permit that action. If you believe you should have access, "
                        + "please ask an administrator.");
        return "access-denied";
    }

    /* ------------------------------------------------------------------ */
    /* Workstation preference - a worked example of cookie use             */
    /* ------------------------------------------------------------------ */

    /**
     * Stores the table-density preference in the {@code clinic-prefs} cookie
     * and returns the user to the page they were on.
     *
     * <p>POST rather than GET because it changes state; the {@code Referer}
     * header is used only to return the user to their page, and is checked to
     * be a local path so it cannot be used to bounce them off-site.</p>
     */
    @PostMapping("/preferences/density")
    public String setDensity(@RequestParam String value,
                             @RequestHeader(value = "Referer", required = false) String referer,
                             HttpServletResponse response) {

        uiPreferences.writeDensity(response, value);

        if (referer != null && referer.startsWith("/")) {
            return "redirect:" + referer;
        }
        return "redirect:/dashboard";
    }
}
