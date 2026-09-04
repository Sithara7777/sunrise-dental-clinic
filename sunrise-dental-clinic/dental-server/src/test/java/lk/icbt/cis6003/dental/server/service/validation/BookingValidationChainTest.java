package lk.icbt.cis6003.dental.server.service.validation;

import lk.icbt.cis6003.dental.server.domain.Dentist;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import lk.icbt.cis6003.dental.server.repository.AppointmentRepository;
import lk.icbt.cis6003.dental.server.testsupport.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the booking validation chain - the Chain of Responsibility.
 *
 * <p>Two things are under test, and they are different:</p>
 * <ol>
 *   <li><b>Each rule in isolation</b> - does the clinic-hours rule reject
 *       21:00? Each of these needs only the one field it is about, which is
 *       exactly what the pattern buys.</li>
 *   <li><b>The chain itself</b> - does it stop at the first failure, and does
 *       it stop <em>before</em> the rules that cost a database query? The
 *       {@code verify(..., never())} assertions below are how the
 *       short-circuiting is actually proved rather than assumed.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Booking validation chain (Chain of Responsibility)")
class BookingValidationChainTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private BookingValidationChain chain;

    @BeforeEach
    void setUp() {
        chain = new BookingValidationChain(List.of(
                new BookingWindowHandler(),
                new ClinicHoursHandler(),
                new SlotAlignmentHandler(),
                new DentistAvailabilityHandler(),
                new DentistDoubleBookingHandler(appointmentRepository),
                new PatientDoubleBookingHandler(appointmentRepository)));
        chain.assembleChain();

        // By default nothing clashes; individual tests override this.
        when(appointmentRepository.isSlotTaken(anyString(), any(), any(), any(), any()))
                .thenReturn(false);
        when(appointmentRepository.patientAlreadyBooked(anyString(), any(), any(), any()))
                .thenReturn(false);
    }

    /* ================================================================== */
    /* The happy path                                                      */
    /* ================================================================== */

    @Test
    @DisplayName("a well-formed booking passes every rule")
    void validBookingPasses() {
        ValidationOutcome outcome = chain.validate(request(
                LocalDate.now().plusDays(7), LocalTime.of(10, 0)));

        assertThat(outcome.isValid()).isTrue();
    }

    /* ================================================================== */
    /* Rule 1 - the booking window                                         */
    /* ================================================================== */

    @Test
    @DisplayName("a booking in the past is refused")
    void pastDateIsRefused() {
        ValidationOutcome outcome = chain.validate(request(
                LocalDate.now().minusDays(1), LocalTime.of(10, 0)));

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getFailedRule()).isEqualTo("BOOKING_WINDOW");
        assertThat(outcome.getMessage()).contains("past");
    }

    @Test
    @DisplayName("a booking more than 180 days ahead is refused - almost always a mistyped year")
    void tooFarAheadIsRefused() {
        ValidationOutcome outcome = chain.validate(request(
                LocalDate.now().plusDays(400), LocalTime.of(10, 0)));

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getFailedRule()).isEqualTo("BOOKING_WINDOW");
        assertThat(outcome.getMessage()).contains("180");
    }

    @Test
    @DisplayName("an invalid date short-circuits BEFORE any database query is made")
    void invalidDateSkipsTheDatabaseRules() {
        chain.validate(request(LocalDate.now().minusDays(5), LocalTime.of(10, 0)));

        verify(appointmentRepository, never()).isSlotTaken(anyString(), any(), any(), any(), any());
        verify(appointmentRepository, never()).patientAlreadyBooked(anyString(), any(), any(), any());
    }

    /* ================================================================== */
    /* Rule 2 - clinic hours                                               */
    /* ================================================================== */

    @Test
    @DisplayName("a booking before the clinic opens is refused")
    void beforeOpeningIsRefused() {
        ValidationOutcome outcome = chain.validate(request(
                LocalDate.now().plusDays(7), LocalTime.of(7, 0)));

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getFailedRule()).isEqualTo("CLINIC_HOURS");
    }

    @Test
    @DisplayName("a treatment that would FINISH after closing is refused, not just one that starts late")
    void treatmentOverrunningClosingIsRefused() {
        // A 90-minute surgical extraction starting at 19:00 ends at 20:30,
        // half an hour after the clinic closes.
        BookingValidationRequest request = new BookingValidationRequest(
                dentistOpenAllDay(), TestDataFactory.surgicalTreatment(), "PAT-000001",
                LocalDate.now().plusDays(7), LocalTime.of(19, 0), null);

        ValidationOutcome outcome = chain.validate(request);

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getFailedRule()).isEqualTo("CLINIC_HOURS");
        assertThat(outcome.getMessage()).contains("20:30");
    }

    /* ================================================================== */
    /* Rule 3 - slot alignment                                             */
    /* ================================================================== */

    @Test
    @DisplayName("a time off the 30-minute grid is refused - it would defeat the slot uniqueness rule")
    void unalignedTimeIsRefused() {
        ValidationOutcome outcome = chain.validate(request(
                LocalDate.now().plusDays(7), LocalTime.of(10, 15)));

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getFailedRule()).isEqualTo("SLOT_ALIGNMENT");
    }

    @Test
    @DisplayName("both :00 and :30 are accepted")
    void bothHalvesOfTheHourAreAccepted() {
        assertThat(chain.validate(request(LocalDate.now().plusDays(7), LocalTime.of(11, 0)))
                .isValid()).isTrue();
        assertThat(chain.validate(request(LocalDate.now().plusDays(7), LocalTime.of(11, 30)))
                .isValid()).isTrue();
    }

    @Test
    @DisplayName("a time carrying seconds is refused")
    void secondsAreRefused() {
        ValidationOutcome outcome = chain.validate(request(
                LocalDate.now().plusDays(7), LocalTime.of(10, 0, 30)));

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getFailedRule()).isEqualTo("SLOT_ALIGNMENT");
    }

    /* ================================================================== */
    /* Rule 4 - dentist availability                                       */
    /* ================================================================== */

    @Test
    @DisplayName("a booking outside the dentist's own shift is refused")
    void outsideTheDentistShiftIsRefused() {
        // Dr Jayawardena finishes at 14:00; 15:00 is inside clinic hours but
        // not inside hers.
        BookingValidationRequest request = new BookingValidationRequest(
                TestDataFactory.partTimeDentist(), TestDataFactory.standardTreatment(),
                "PAT-000001", LocalDate.now().plusDays(7), LocalTime.of(15, 0), null);

        ValidationOutcome outcome = chain.validate(request);

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getFailedRule()).isEqualTo("DENTIST_AVAILABILITY");
        assertThat(outcome.getMessage()).contains("14:00");
    }

    @Test
    @DisplayName("a booking with a retired dentist is refused")
    void retiredDentistIsRefused() {
        BookingValidationRequest request = new BookingValidationRequest(
                TestDataFactory.retiredDentist(), TestDataFactory.standardTreatment(),
                "PAT-000001", LocalDate.now().plusDays(7), LocalTime.of(10, 0), null);

        ValidationOutcome outcome = chain.validate(request);

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getFailedRule()).isEqualTo("DENTIST_AVAILABILITY");
        assertThat(outcome.getMessage()).contains("no longer practising");
    }

    /* ================================================================== */
    /* Rule 5 - the double-booking rule                                    */
    /* ================================================================== */

    @Test
    @DisplayName("a taken dentist slot is refused with SLOT_UNAVAILABLE")
    void takenSlotIsRefused() {
        when(appointmentRepository.isSlotTaken(anyString(), any(), any(), any(), any()))
                .thenReturn(true);

        ValidationOutcome outcome = chain.validate(request(
                LocalDate.now().plusDays(7), LocalTime.of(10, 0)));

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getFailedRule()).isEqualTo("DENTIST_DOUBLE_BOOKING");
        assertThat(outcome.getErrorCode()).isEqualTo(ErrorCode.SLOT_UNAVAILABLE);
        assertThat(outcome.getMessage()).contains("already has an appointment");
    }

    @Test
    @DisplayName("only live statuses count as occupying the chair")
    void onlyLiveStatusesOccupyTheChair() {
        assertThat(DentistDoubleBookingHandler.occupyingStatuses())
                .containsExactlyInAnyOrder(
                        lk.icbt.cis6003.dental.common.enums.AppointmentStatus.SCHEDULED,
                        lk.icbt.cis6003.dental.common.enums.AppointmentStatus.CONFIRMED,
                        lk.icbt.cis6003.dental.common.enums.AppointmentStatus.COMPLETED);
    }

    /* ================================================================== */
    /* Rule 6 - patient double booking                                     */
    /* ================================================================== */

    @Test
    @DisplayName("the same patient booked twice at one time is refused")
    void patientDoubleBookingIsRefused() {
        when(appointmentRepository.patientAlreadyBooked(anyString(), any(), any(), any()))
                .thenReturn(true);

        ValidationOutcome outcome = chain.validate(request(
                LocalDate.now().plusDays(7), LocalTime.of(10, 0)));

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getFailedRule()).isEqualTo("PATIENT_DOUBLE_BOOKING");
    }

    @Test
    @DisplayName("a brand new patient skips the patient clash query - they cannot have one")
    void newPatientSkipsTheClashQuery() {
        BookingValidationRequest request = new BookingValidationRequest(
                TestDataFactory.dentist(), TestDataFactory.standardTreatment(),
                null,                                    // no patient code yet
                LocalDate.now().plusDays(7), LocalTime.of(10, 0), null);

        assertThat(chain.validate(request).isValid()).isTrue();
        verify(appointmentRepository, never()).patientAlreadyBooked(anyString(), any(), any(), any());
    }

    /* ================================================================== */
    /* The chain itself                                                    */
    /* ================================================================== */

    @Test
    @DisplayName("the chain runs the rules in ascending order")
    void rulesRunInOrder() {
        assertThat(chain.describeRules()).containsExactly(
                "BOOKING_WINDOW",
                "CLINIC_HOURS",
                "SLOT_ALIGNMENT",
                "DENTIST_AVAILABILITY",
                "DENTIST_DOUBLE_BOOKING",
                "PATIENT_DOUBLE_BOOKING");
    }

    @Test
    @DisplayName("the chain reports the FIRST failure, not the last")
    void reportsTheFirstFailure() {
        // This request breaks three rules at once: it is in the past, it is
        // outside clinic hours, and it is off the slot grid. The user must be
        // told about the date, which is the most fundamental problem.
        ValidationOutcome outcome = chain.validate(request(
                LocalDate.now().minusDays(3), LocalTime.of(23, 17)));

        assertThat(outcome.getFailedRule()).isEqualTo("BOOKING_WINDOW");
    }

    @Test
    @DisplayName("validateOrThrow raises a BusinessException carrying the rule's error code")
    void validateOrThrowCarriesTheErrorCode() {
        when(appointmentRepository.isSlotTaken(anyString(), any(), any(), any(), any()))
                .thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                chain.validateOrThrow(request(LocalDate.now().plusDays(7), LocalTime.of(10, 0))))
                .isInstanceOf(lk.icbt.cis6003.dental.server.exception.BusinessException.class)
                .extracting(e ->
                        ((lk.icbt.cis6003.dental.server.exception.BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SLOT_UNAVAILABLE);
    }

    /* ------------------------------------------------------------------ */

    private BookingValidationRequest request(LocalDate date, LocalTime time) {
        return new BookingValidationRequest(
                TestDataFactory.dentist(), TestDataFactory.standardTreatment(),
                "PAT-000001", date, time, null);
    }

    /** A dentist rostered for the clinic's whole trading day. */
    private Dentist dentistOpenAllDay() {
        Dentist dentist = TestDataFactory.dentist();
        dentist.setWorkStartTime(LocalTime.of(8, 0));
        dentist.setWorkEndTime(LocalTime.of(20, 0));
        return dentist;
    }
}
