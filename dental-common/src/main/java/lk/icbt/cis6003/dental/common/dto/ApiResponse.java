package lk.icbt.cis6003.dental.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Uniform envelope returned by every REST endpoint.
 *
 * <p>A consistent shape means the remote Swing client needs exactly one piece
 * of response-handling code: check {@code success}, then either read
 * {@code data} or show {@code message} plus {@code fieldErrors}. Without the
 * envelope, each screen would have to interpret raw HTTP status codes.</p>
 *
 * @param <T> the payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private List<FieldError> fieldErrors;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ApiResponse() {
        // required by Jackson
    }

    private ApiResponse(boolean success, String message, T data, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }

    /* ---------------- factory methods (keeps call sites readable) -------- */

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> fail(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode);
    }

    public static <T> ApiResponse<T> validationFailure(String message, List<FieldError> errors) {
        ApiResponse<T> response = new ApiResponse<>(false, message, null, "VALIDATION_ERROR");
        response.setFieldErrors(errors);
        return response;
    }

    /* ---------------- accessors ----------------------------------------- */

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors == null ? null : new ArrayList<>(fieldErrors);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Flattens all field errors into one human readable block, which is what
     * the Swing client drops straight into a {@code JOptionPane}.
     */
    public String describeErrors() {
        StringBuilder sb = new StringBuilder(message == null ? "Request failed" : message);
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            for (FieldError fe : fieldErrors) {
                sb.append(System.lineSeparator())
                  .append("  • ").append(fe.getField()).append(": ").append(fe.getMessage());
            }
        }
        return sb.toString();
    }

    /** One rejected input field. */
    public static class FieldError {

        private String field;
        private String message;
        private Object rejectedValue;

        public FieldError() {
            // required by Jackson
        }

        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }

        @Override
        public String toString() {
            return field + ": " + message;
        }
    }
}
