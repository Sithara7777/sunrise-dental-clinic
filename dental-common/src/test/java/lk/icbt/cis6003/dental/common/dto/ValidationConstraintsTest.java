package lk.icbt.cis6003.dental.common.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the validation that "restricts invalid entries to the system"
 * (Task B).
 *
 * <p>These run a real Bean Validation engine over the real DTOs, so what is
 * verified is exactly what the server will enforce - not a re-implementation of
 * the same regular expressions inside the test, which would only prove the test
 * agrees with itself.</p>
 *
 * <p>The parameterised cases are chosen as boundaries and near-misses rather
 * than as obviously-wrong rubbish: {@code 077123456} (one digit short) matters
 * far more than {@code "hello"}, because a receptionist will actually type the
 * former.</p>
 */
@DisplayName("Input validation constraints")
class ValidationConstraintsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void startValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    /* ================================================================== */
    @Nested
    @DisplayName("Contact number")
    class ContactNumber {

        @ParameterizedTest(name = "accepts {0}")
        @ValueSource(strings = { "0771234567", "0112573100", "+94771234567", "0701234567" })
        @DisplayName("accepts a valid Sri Lankan number in local or international form")
        void acceptsValidNumbers(String number) {
            assertThat(violationsOn(requestWith(r -> r.setContactNumber(number)), "contactNumber"))
                    .isEmpty();
        }

        @ParameterizedTest(name = "rejects {0}")
        @ValueSource(strings = {
            "077123456",      // one digit short
            "07712345678",    // one digit too many
            "1771234567",     // does not start 0 or +94
            "077-1234567",    // punctuation is normalised before storage, not accepted here
            "abcdefghij"      // not a number
        })
        @DisplayName("rejects a malformed number, including the near-misses staff actually type")
        void rejectsInvalidNumbers(String number) {
            assertThat(violationsOn(requestWith(r -> r.setContactNumber(number)), "contactNumber"))
                    .isNotEmpty();
        }

        @Test
        @DisplayName("is mandatory - the clinic cannot contact a patient without it")
        void isMandatory() {
            assertThat(violationsOn(requestWith(r -> r.setContactNumber(null)), "contactNumber"))
                    .isNotEmpty();
        }
    }

    /* ================================================================== */
    @Nested
    @DisplayName("NIC")
    class Nic {

        @ParameterizedTest(name = "accepts {0}")
        @ValueSource(strings = { "901234567V", "901234567X", "901234567v", "199012345678" })
        @DisplayName("accepts both the old and the new NIC formats")
        void acceptsBothFormats(String nic) {
            assertThat(violationsOn(requestWith(r -> r.setNic(nic)), "nic")).isEmpty();
        }

        @ParameterizedTest(name = "rejects {0}")
        @ValueSource(strings = { "90123456V", "9012345678", "901234567A", "19901234567" })
        @DisplayName("rejects a malformed NIC")
        void rejectsMalformed(String nic) {
            assertThat(violationsOn(requestWith(r -> r.setNic(nic)), "nic")).isNotEmpty();
        }

        @Test
        @DisplayName("is optional - many patients do not carry it to a dental appointment")
        void isOptional() {
            assertThat(violationsOn(requestWith(r -> r.setNic(null)), "nic")).isEmpty();
            assertThat(violationsOn(requestWith(r -> r.setNic("")), "nic")).isEmpty();
        }
    }

    /* ================================================================== */
    @Nested
    @DisplayName("Patient name")
    class PatientName {

        @ParameterizedTest(name = "accepts {0}")
        @ValueSource(strings = { "Kamala Perera", "D'Silva", "Anne-Marie Fernando", "Dr. R. Silva" })
        @DisplayName("accepts the punctuation that appears in real Sri Lankan names")
        void acceptsRealNames(String name) {
            assertThat(violationsOn(requestWith(r -> r.setPatientName(name)), "patientName"))
                    .isEmpty();
        }

        @ParameterizedTest(name = "rejects {0}")
        @ValueSource(strings = { "123456", "Robert<script>", "Name@Home", "1Kamala" })
        @DisplayName("rejects digits and markup, which are never part of a name")
        void rejectsSuspiciousInput(String name) {
            assertThat(violationsOn(requestWith(r -> r.setPatientName(name)), "patientName"))
                    .isNotEmpty();
        }

        @Test
        @DisplayName("is mandatory")
        void isMandatory() {
            assertThat(violationsOn(requestWith(r -> r.setPatientName("  ")), "patientName"))
                    .isNotEmpty();
        }
    }

    /* ================================================================== */
    @Nested
    @DisplayName("Appointment date and time")
    class DateAndTime {

        @Test
        @DisplayName("a date in the past is rejected")
        void pastDateIsRejected() {
            assertThat(violationsOn(
                    requestWith(r -> r.setAppointmentDate(LocalDate.now().minusDays(1))),
                    "appointmentDate")).isNotEmpty();
        }

        @Test
        @DisplayName("a future date is accepted")
        void futureDateIsAccepted() {
            assertThat(violationsOn(
                    requestWith(r -> r.setAppointmentDate(LocalDate.now().plusDays(1))),
                    "appointmentDate")).isEmpty();
        }

        @Test
        @DisplayName("both date and time are mandatory")
        void bothAreMandatory() {
            assertThat(violationsOn(requestWith(r -> r.setAppointmentDate(null)), "appointmentDate"))
                    .isNotEmpty();
            assertThat(violationsOn(requestWith(r -> r.setAppointmentTime(null)), "appointmentTime"))
                    .isNotEmpty();
        }

        @Test
        @DisplayName("a dentist and a treatment must both be chosen")
        void dentistAndTreatmentAreMandatory() {
            assertThat(violationsOn(requestWith(r -> r.setDentistCode(null)), "dentistCode"))
                    .isNotEmpty();
            assertThat(violationsOn(requestWith(r -> r.setTreatmentCode("")), "treatmentCode"))
                    .isNotEmpty();
        }
    }

    /* ================================================================== */
    @Nested
    @DisplayName("Billing")
    class Billing {

        @Test
        @DisplayName("a discount above 50% is rejected before it reaches the server logic")
        void excessiveDiscountIsRejected() {
            BillingRequest request = new BillingRequest("APT-2026-000001");
            request.setDiscountPercentage(new BigDecimal("75"));

            assertThat(names(validator.validate(request))).contains("discountPercentage");
        }

        @Test
        @DisplayName("a negative discount is rejected")
        void negativeDiscountIsRejected() {
            BillingRequest request = new BillingRequest("APT-2026-000001");
            request.setDiscountPercentage(new BigDecimal("-5"));

            assertThat(names(validator.validate(request))).contains("discountPercentage");
        }

        @Test
        @DisplayName("a discount of exactly 50% is accepted - the boundary is inclusive")
        void fiftyPercentIsTheBoundary() {
            BillingRequest request = new BillingRequest("APT-2026-000001");
            request.setDiscountPercentage(new BigDecimal("50"));

            assertThat(validator.validate(request)).isEmpty();
        }

        @Test
        @DisplayName("a payment of zero or less is rejected")
        void zeroPaymentIsRejected() {
            PaymentRequest request = new PaymentRequest(
                    BigDecimal.ZERO, lk.icbt.cis6003.dental.common.enums.PaymentMethod.CASH);

            assertThat(names(validator.validate(request))).contains("amount");
        }

        @Test
        @DisplayName("a payment must name a method")
        void paymentMethodIsMandatory() {
            PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"), null);

            assertThat(names(validator.validate(request))).contains("paymentMethod");
        }
    }

    /* ================================================================== */
    @Nested
    @DisplayName("Credentials")
    class Credentials {

        @Test
        @DisplayName("username and password are both mandatory")
        void bothAreMandatory() {
            assertThat(names(validator.validate(new LoginRequest("", ""))))
                    .contains("username", "password");
        }

        @Test
        @DisplayName("a password shorter than six characters is rejected")
        void shortPasswordIsRejected() {
            assertThat(names(validator.validate(new LoginRequest("reception", "abc"))))
                    .contains("password");
        }

        @Test
        @DisplayName("toString() masks the password so it can never reach a log file")
        void toStringMasksThePassword() {
            String rendered = new LoginRequest("reception", "Reception@123").toString();

            assertThat(rendered)
                    .contains("reception")
                    .doesNotContain("Reception@123")
                    .contains("********");
        }
    }

    /* ================================================================== */
    @Test
    @DisplayName("a fully valid booking request produces no violations at all")
    void aValidRequestPasses() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    private AppointmentRequest validRequest() {
        return AppointmentRequest.builder()
                .patientName("Kamala Perera")
                .address("No. 45, Galle Road, Colombo 03")
                .contactNumber("0771234567")
                .email("kamala.perera@example.lk")
                .nic("901234567V")
                .dentistCode("DEN-001")
                .treatmentCode("SCALING")
                .appointmentDate(LocalDate.now().plusDays(7))
                .appointmentTime(LocalTime.of(10, 0))
                .build();
    }

    private AppointmentRequest requestWith(java.util.function.Consumer<AppointmentRequest> change) {
        AppointmentRequest request = validRequest();
        change.accept(request);
        return request;
    }

    private Set<String> violationsOn(AppointmentRequest request, String property) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .filter(property::equals)
                .collect(Collectors.toSet());
    }

    private Set<String> names(Set<? extends jakarta.validation.ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
