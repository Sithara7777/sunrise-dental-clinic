package lk.icbt.cis6003.dental.client.api;

import lk.icbt.cis6003.dental.common.dto.ApiResponse;

/**
 * A call to the clinic server that did not succeed.
 *
 * <p>Carries the server's own {@code errorCode} so the client can branch on a
 * stable identifier - {@code SLOT_UNAVAILABLE}, {@code NOT_BILLABLE} - rather
 * than on English message text that the server is free to reword.</p>
 */
public class ApiException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int httpStatus;
    private final String errorCode;

    public ApiException(String message) {
        super(message);
        this.httpStatus = 0;
        this.errorCode = null;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 0;
        this.errorCode = null;
    }

    public ApiException(int httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public ApiException(int httpStatus, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    /** Builds the exception from a failed response envelope, field errors included. */
    public static ApiException from(int httpStatus, ApiResponse<?> response) {
        return new ApiException(httpStatus,
                                response.getErrorCode(),
                                response.describeErrors());
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /** True when the session has expired and the user must sign in again. */
    public boolean isAuthenticationFailure() {
        return httpStatus == 401 || "AUTHENTICATION_FAILED".equals(errorCode);
    }

    /** True when the server is unreachable rather than refusing the request. */
    public boolean isConnectionFailure() {
        return httpStatus == 0;
    }
}
