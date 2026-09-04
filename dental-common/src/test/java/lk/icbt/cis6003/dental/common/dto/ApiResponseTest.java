package lk.icbt.cis6003.dental.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the response envelope that every REST endpoint returns.
 *
 * <p>Worth testing because the desktop client's entire response-handling path
 * depends on this shape being consistent. If {@code ok()} ever produced
 * {@code success=false}, every screen in the client would report a failure on
 * a successful call.</p>
 */
@DisplayName("API response envelope")
class ApiResponseTest {

    @Test
    @DisplayName("ok() marks the response successful and carries the payload")
    void okCarriesData() {
        ApiResponse<String> response = ApiResponse.ok("APT-2026-000001");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("APT-2026-000001");
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("fail() marks the response unsuccessful and carries no payload")
    void failCarriesNoData() {
        ApiResponse<String> response = ApiResponse.fail("That slot is taken", "SLOT_UNAVAILABLE");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getErrorCode()).isEqualTo("SLOT_UNAVAILABLE");
        assertThat(response.getMessage()).isEqualTo("That slot is taken");
    }

    @Test
    @DisplayName("validation failures list every rejected field")
    void validationFailureListsFields() {
        ApiResponse<Void> response = ApiResponse.validationFailure(
                "Please correct the highlighted fields.",
                List.of(new ApiResponse.FieldError("contactNumber", "not a valid number", "12345"),
                        new ApiResponse.FieldError("patientName", "is required", null)));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getFieldErrors()).hasSize(2);
    }

    @Test
    @DisplayName("describeErrors() flattens everything into one readable block for the client")
    void describeErrorsIsReadable() {
        ApiResponse<Void> response = ApiResponse.validationFailure(
                "Please correct the highlighted fields.",
                List.of(new ApiResponse.FieldError("contactNumber", "not a valid number", "12345")));

        String description = response.describeErrors();

        assertThat(description)
                .contains("Please correct the highlighted fields.")
                .contains("contactNumber")
                .contains("not a valid number");
    }

    @Test
    @DisplayName("describeErrors() copes with no field errors at all")
    void describeErrorsWithoutFieldErrors() {
        assertThat(ApiResponse.fail("Server unavailable", "INTERNAL_ERROR").describeErrors())
                .isEqualTo("Server unavailable");
    }

    @Test
    @DisplayName("the field error list is defensively copied, so callers cannot mutate it")
    void fieldErrorsAreDefensivelyCopied() {
        List<ApiResponse.FieldError> source = new java.util.ArrayList<>();
        source.add(new ApiResponse.FieldError("a", "b", null));

        ApiResponse<Void> response = new ApiResponse<>();
        response.setFieldErrors(source);
        source.clear();

        assertThat(response.getFieldErrors()).hasSize(1);
    }
}
