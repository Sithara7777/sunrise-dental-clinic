package lk.icbt.cis6003.dental.server.exception;

/**
 * Stable, machine-readable error identifiers.
 *
 * <p>The desktop client switches on these rather than on English message text,
 * so wording can be improved without breaking a released client - the whole
 * point of publishing a versioned web service.</p>
 */
public enum ErrorCode {

    VALIDATION_ERROR("The information supplied is not valid"),
    NOT_FOUND("The requested record could not be found"),
    DUPLICATE("A record with those details already exists"),
    SLOT_UNAVAILABLE("That appointment slot is already taken"),
    INVALID_STATE("That operation is not allowed in the current state"),
    ALREADY_INVOICED("A bill has already been issued for this appointment"),
    NOT_BILLABLE("Only completed appointments can be billed"),
    PAYMENT_EXCEEDS_BALANCE("The payment is larger than the outstanding balance"),
    ACCESS_DENIED("You do not have permission to perform this action"),
    AUTHENTICATION_FAILED("Invalid username or password"),
    ACCOUNT_LOCKED("The account is temporarily locked"),
    CONCURRENT_UPDATE("Somebody else changed this record while you were editing it"),
    SERVICE_BUSY("The system is handling an unusually high number of requests right now"),
    INTERNAL_ERROR("An unexpected error occurred");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
