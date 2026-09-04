package lk.icbt.cis6003.dental.server.exception;

/**
 * Raised when an appointment or invoice is asked to make a move its lifecycle
 * forbids, e.g. completing an appointment that was already cancelled.
 */
public class InvalidStateTransitionException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidStateTransitionException(String message) {
        super(ErrorCode.INVALID_STATE, message);
    }
}
