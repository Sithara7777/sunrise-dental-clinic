package lk.icbt.cis6003.dental.server.web;

import jakarta.servlet.http.HttpServletRequest;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import lk.icbt.cis6003.dental.server.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Renders an HTML error page when a browser request fails.
 *
 * <p>Separate from {@code RestExceptionHandler} because the two audiences need
 * completely different things: the desktop client needs a JSON envelope it can
 * parse, a receptionist needs a page that says what went wrong and offers a way
 * back. One handler trying to serve both would give the wrong answer to
 * whichever it was not written for.</p>
 *
 * <p>Business messages are shown as written - they were composed for the user.
 * Unexpected failures show a generic sentence and log the detail, so an SQL
 * fragment never appears on a screen at the front desk.</p>
 */
@ControllerAdvice(basePackages = "lk.icbt.cis6003.dental.server.web")
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("pageTitle", "Not Found");
        model.addAttribute("errorTitle", "We could not find that record");
        model.addAttribute("errorDetail", ex.getMessage());
        model.addAttribute("errorHint",
                "Check the number you typed. Appointment numbers look like APT-2026-000137 and "
                        + "bill numbers like INV-2026-000137.");
        return "error/message";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, Model model) {
        log.info("Business rule refused a browser request [{}]: {}",
                 ex.getErrorCode(), ex.getMessage());

        model.addAttribute("pageTitle", "Not Allowed");
        model.addAttribute("errorTitle", titleFor(ex.getErrorCode()));
        model.addAttribute("errorDetail", ex.getMessage());
        model.addAttribute("errorHint",
                "Nothing has been changed. Go back, adjust what you entered and try again.");
        return "error/message";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled exception serving {} {}", request.getMethod(), request.getRequestURI(), ex);

        model.addAttribute("pageTitle", "Something went wrong");
        model.addAttribute("errorTitle", "Something went wrong");
        // Deliberately generic: never echo ex.getMessage() to a browser.
        model.addAttribute("errorDetail",
                "An unexpected error occurred and has been recorded in the system log.");
        model.addAttribute("errorHint",
                "Please try again. If the problem continues, tell your administrator what you "
                        + "were doing at the time.");
        return "error/message";
    }

    private String titleFor(ErrorCode code) {
        switch (code) {
            case SLOT_UNAVAILABLE:
                return "That slot is not available";
            case ALREADY_INVOICED:
                return "This appointment has already been billed";
            case NOT_BILLABLE:
                return "This appointment cannot be billed yet";
            case INVALID_STATE:
                return "That change is not allowed";
            case DUPLICATE:
                return "That record already exists";
            case CONCURRENT_UPDATE:
                return "Somebody else changed this record";
            default:
                return "We could not complete that action";
        }
    }
}
