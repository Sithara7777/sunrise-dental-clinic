package lk.icbt.cis6003.dental.server.api;

import jakarta.servlet.http.HttpServletRequest;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.ApiResponse;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns every exception the API can raise into the standard response envelope.
 *
 * <p>Without this, a client would face three different failure shapes - Spring
 * Boot's default JSON error, a stack trace, and whatever a controller happened
 * to return - and would need three ways of reading them. Handling it once here
 * is what lets the desktop client have a single response-handling path.</p>
 *
 * <p><b>Information disclosure.</b> Business failures return their own message,
 * because that message was written for the user. Unexpected failures return a
 * generic sentence and log the detail server-side: an SQL fragment or a class
 * name in an error response tells an attacker about the internals and tells the
 * receptionist nothing useful.</p>
 *
 * <p>Scoped to the API packages so the browser controllers keep their own
 * HTML error pages.</p>
 */
@RestControllerAdvice(basePackages = "lk.icbt.cis6003.dental.server.api")
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    /** Bean Validation failures - reported field by field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldError> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(new ApiResponse.FieldError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue()));
        }
        for (var globalError : ex.getBindingResult().getGlobalErrors()) {
            errors.add(new ApiResponse.FieldError(
                    globalError.getObjectName(), globalError.getDefaultMessage(), null));
        }

        log.debug("Request rejected by validation: {}", errors);
        return ResponseEntity.badRequest().body(
                ApiResponse.validationFailure("Please correct the highlighted fields.", errors));
    }

    /** Every rule the business tier can refuse, mapped to its proper status. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        HttpStatus status = statusFor(ex.getErrorCode());

        if (status.is5xxServerError()) {
            log.error("Business failure [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        } else {
            log.info("Business rule refused the request [{}]: {}", ex.getErrorCode(), ex.getMessage());
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.fail(ex.getMessage(), ex.getErrorCode().name()));
    }

    /**
     * A constraint violation that reached this far is almost always the
     * appointment slot uniqueness rule firing on a genuine race.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database rejected the request: {}", ex.getMostSpecificCause().getMessage());

        String message = describeConstraint(ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(message, ErrorCode.DUPLICATE.name()));
    }

    /** Two users edited the same record; the second save is refused. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.info("Concurrent update detected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(
                "Somebody else changed this record while you were working on it. "
                        + "Please reload it and try again.",
                ErrorCode.CONCURRENT_UPDATE.name()));
    }

    /**
     * The connection pool (or the transaction manager generally) could not
     * service this request in time.
     *
     * <p>Distinguished from an ordinary unexpected failure because it is not
     * one: it means the system is momentarily busier than it is provisioned
     * for, and the honest, actionable response is "try again shortly," not
     * "an unexpected error occurred." A burst of truly simultaneous requests -
     * several receptionists saving at once, or a stress test - is the
     * realistic trigger; it was found by firing a dozen simultaneous booking
     * requests at the same slot deliberately, to verify the double-booking
     * guarantee under a genuine race.</p>
     */
    @ExceptionHandler(org.springframework.transaction.TransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionException(
            org.springframework.transaction.TransactionException ex) {
        log.warn("Transaction could not be started or completed - "
                 + "the connection pool may be saturated: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.fail(
                "The system is handling a lot of requests right now. Please wait a few seconds "
                        + "and try again.",
                ErrorCode.SERVICE_BUSY.name()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(
                "Your role does not permit this action.", ErrorCode.ACCESS_DENIED.name()));
    }

    /** A path or query parameter that could not be converted. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String expected = ex.getRequiredType() == null ? "the expected type"
                : ex.getRequiredType().getSimpleName();
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                "'" + ex.getValue() + "' is not a valid value for " + ex.getName()
                        + " - expected " + expected + ".",
                ErrorCode.VALIDATION_ERROR.name()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                "The required parameter '" + ex.getParameterName() + "' was not supplied.",
                ErrorCode.VALIDATION_ERROR.name()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                "The request body could not be read. Check that it is valid JSON and that dates "
                        + "are in yyyy-MM-dd format.",
                ErrorCode.VALIDATION_ERROR.name()));
    }

    /**
     * Anything not anticipated.
     *
     * <p>The detail is logged with a stack trace; the caller gets a generic
     * sentence. Never echo {@code ex.getMessage()} here - it routinely contains
     * SQL, file paths and class names.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (!request.getRequestURI().startsWith(ApiPaths.API_ROOT)) {
            // Let the browser chain render its own error page.
            throw new RuntimeException(ex);
        }

        log.error("Unhandled exception serving {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(
                "An unexpected error occurred. The problem has been logged - please contact "
                        + "your system administrator if it continues.",
                ErrorCode.INTERNAL_ERROR.name()));
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    private HttpStatus statusFor(ErrorCode code) {
        switch (code) {
            case NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case VALIDATION_ERROR:
                return HttpStatus.BAD_REQUEST;
            case DUPLICATE:
            case SLOT_UNAVAILABLE:
            case ALREADY_INVOICED:
            case CONCURRENT_UPDATE:
                return HttpStatus.CONFLICT;
            case INVALID_STATE:
            case NOT_BILLABLE:
            case PAYMENT_EXCEEDS_BALANCE:
                return HttpStatus.UNPROCESSABLE_ENTITY;
            case ACCESS_DENIED:
                return HttpStatus.FORBIDDEN;
            case AUTHENTICATION_FAILED:
            case ACCOUNT_LOCKED:
                return HttpStatus.UNAUTHORIZED;
            case INTERNAL_ERROR:
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /** Translates the constraint name into something a receptionist understands. */
    private String describeConstraint(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        String lower = cause == null ? "" : cause.toLowerCase();

        if (lower.contains("uk_appointment_slot")) {
            return "That time slot has just been taken by another user. "
                    + "Please check availability again and choose a different slot.";
        }
        if (lower.contains("uk_appointment_number")) {
            return "That appointment number is already in use. Please try again.";
        }
        if (lower.contains("uk_invoice_appointment")) {
            return "A bill has already been issued for that appointment.";
        }
        if (lower.contains("uk_patient_code")) {
            return "That patient number is already in use. Please try again.";
        }
        if (lower.contains("chk_invoice_amount_paid")) {
            return "The payment would exceed the total on the bill.";
        }
        return "The information supplied conflicts with a record that already exists.";
    }
}
