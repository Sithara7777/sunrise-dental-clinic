package lk.icbt.cis6003.dental.server.exception;

/**
 * Raised when a lookup by business key finds nothing - most often a search on
 * an appointment number that does not exist.
 */
public class ResourceNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String resourceType, String key) {
        super(ErrorCode.NOT_FOUND, resourceType + " '" + key + "' was not found");
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
