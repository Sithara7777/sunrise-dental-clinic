package lk.icbt.cis6003.dental.server.exception;

/**
 * Base type for every rule the business tier can refuse.
 *
 * <p>Deliberately unchecked. These conditions - a taken slot, an already
 * invoiced appointment - are not recoverable at the call site; the correct
 * behaviour is always to abandon the transaction and tell the user, which the
 * single global exception handler does in one place.</p>
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
