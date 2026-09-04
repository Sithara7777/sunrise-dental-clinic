package lk.icbt.cis6003.dental.server.service.validation;

import lk.icbt.cis6003.dental.server.exception.ErrorCode;

/**
 * The verdict of one link in the booking validation chain.
 *
 * <p>Carries the name of the rule that rejected the request as well as the
 * message. When a receptionist reports "it will not let me book", the log line
 * names the exact rule rather than leaving support to guess which of six
 * checks fired.</p>
 */
public final class ValidationOutcome {

    private static final ValidationOutcome VALID = new ValidationOutcome(true, null, null, null);

    private final boolean valid;
    private final String message;
    private final ErrorCode errorCode;
    private final String failedRule;

    private ValidationOutcome(boolean valid, String message, ErrorCode errorCode, String failedRule) {
        this.valid = valid;
        this.message = message;
        this.errorCode = errorCode;
        this.failedRule = failedRule;
    }

    public static ValidationOutcome valid() {
        return VALID;
    }

    public static ValidationOutcome invalid(String rule, ErrorCode errorCode, String message) {
        return new ValidationOutcome(false, message, errorCode, rule);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getFailedRule() {
        return failedRule;
    }

    @Override
    public String toString() {
        return valid ? "VALID" : ("INVALID[" + failedRule + "] " + message);
    }
}
