package lk.icbt.cis6003.dental.server.exception;

/** Raised when a uniqueness rule (patient NIC, treatment code, username) is broken. */
public class DuplicateResourceException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public DuplicateResourceException(String resourceType, String key) {
        super(ErrorCode.DUPLICATE, resourceType + " '" + key + "' already exists");
    }

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE, message);
    }
}
