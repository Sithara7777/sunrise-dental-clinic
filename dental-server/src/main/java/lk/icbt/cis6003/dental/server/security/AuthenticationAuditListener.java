package lk.icbt.cis6003.dental.server.security;

import jakarta.servlet.http.HttpServletRequest;
import lk.icbt.cis6003.dental.server.domain.AuditLog;
import lk.icbt.cis6003.dental.server.domain.User;
import lk.icbt.cis6003.dental.server.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Records every sign-in attempt and enforces lock-out after repeated failures.
 *
 * <p>Implemented as an event listener rather than as code inside a login
 * controller, because the clinic has two front doors - the browser form and the
 * REST endpoint the desktop client uses. Spring Security publishes the same
 * events for both, so one listener secures both, and a third entry point added
 * later is covered automatically.</p>
 *
 * <p>Lock-out policy: {@value User#MAX_FAILED_ATTEMPTS} consecutive failures
 * lock the account for {@value User#LOCKOUT_MINUTES} minutes. A time-boxed lock
 * rather than a permanent one is deliberate - it defeats password guessing
 * without giving an attacker a way to lock legitimate staff out of the clinic
 * permanently by deliberately failing their logins.</p>
 *
 * <p><b>Concurrent sign-ins from the same account are expected, not
 * exceptional.</b> A shared front-desk account authenticating from two tabs, or
 * any client that re-authenticates with HTTP Basic on every call (Swagger UI's
 * "Authorize" button does exactly this), can fire two
 * {@code AuthenticationSuccessEvent}s for the same user within milliseconds of
 * each other. Both try to update the same versioned {@code User} row; the
 * second to commit loses the optimistic-locking race. That update is delegated
 * to {@link UserLoginBookkeepingService} specifically so its
 * {@code REQUIRES_NEW} commit happens synchronously inside the call below,
 * where this class's own try/catch can actually see the failure - see that
 * class's Javadoc for why a try/catch in the old, single-method version of
 * this listener was not reliably in the right place to catch it. Losing the
 * bookkeeping update on that rare collision is harmless: the audit entry below
 * is written unconditionally regardless, and the request that triggered the
 * sign-in must never fail because of this listener.</p>
 */
@Component
public class AuthenticationAuditListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationAuditListener.class);

    private final UserLoginBookkeepingService loginBookkeepingService;
    private final AuditLogRepository auditLogRepository;

    public AuthenticationAuditListener(UserLoginBookkeepingService loginBookkeepingService,
                                       AuditLogRepository auditLogRepository) {
        this.loginBookkeepingService = loginBookkeepingService;
        this.auditLogRepository = auditLogRepository;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();

        try {
            loginBookkeepingService.recordSuccessfulLogin(username);
        } catch (OptimisticLockingFailureException ex) {
            log.debug("Login bookkeeping for '{}' lost a concurrent update - "
                      + "the sign-in itself is unaffected", username, ex);
        }

        record(username, "LOGIN_SUCCESS", "Signed in successfully");
        log.info("User '{}' signed in", username);
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = String.valueOf(event.getAuthentication().getName());
        String detail;

        try {
            Optional<User> maybeUser = loginBookkeepingService.recordFailedLogin(username);

            if (maybeUser.isPresent()) {
                User user = maybeUser.get();
                if (user.isLocked()) {
                    detail = "Incorrect password - account locked until " + user.getLockedUntil();
                    log.warn("Account '{}' locked after {} failed attempts",
                             username, user.getFailedLoginAttempts());
                } else {
                    int remaining = User.MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts();
                    detail = "Incorrect password - " + remaining + " attempt(s) remaining";
                }
            } else {
                // No such user. Recorded, because a burst of these is itself a signal.
                detail = "Sign-in attempted with an unrecognised username";
            }
        } catch (OptimisticLockingFailureException ex) {
            // Two failed attempts on the same account landed in the same
            // instant. The count could not be safely updated this time; the
            // account is not left any more lenient than before, and the next
            // failure (almost certainly seconds away) records normally.
            log.debug("Login-failure bookkeeping for '{}' lost a concurrent update", username, ex);
            detail = "Incorrect password";
        }

        record(username, "LOGIN_FAILURE", detail);
    }

    private void record(String username, String action, String detail) {
        AuditLog entry = new AuditLog(username, action, "USER", username, detail);
        entry.setIpAddress(currentIpAddress());
        auditLogRepository.save(entry);
    }

    /**
     * @return the caller's IP address, or {@code null} when the event did not
     *         originate from an HTTP request (for example, a test)
     */
    private String currentIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            return (forwarded != null && !forwarded.isBlank())
                    ? forwarded.split(",")[0].trim()
                    : request.getRemoteAddr();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
