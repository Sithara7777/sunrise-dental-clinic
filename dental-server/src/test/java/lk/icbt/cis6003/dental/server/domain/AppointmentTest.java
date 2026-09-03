package lk.icbt.cis6003.dental.server.domain;

import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.server.exception.InvalidStateTransitionException;
import lk.icbt.cis6003.dental.server.testsupport.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the {@link Appointment} entity, and in particular for
 * {@code slotLock} - the single field the whole anti-double-booking design
 * rests on.
 *
 * <p>The rule is: {@code slotLock} holds {@code "date|time"} while the
 * appointment occupies the dentist's chair, and {@code NULL} once it does not.
 * The unique index on {@code (dentist_id, slot_lock)} then refuses a second
 * live booking while permitting any number of cancelled ones, because SQL never
 * treats two NULLs as equal. If these tests ever fail, the clinic's original
 * problem is back.</p>
 */
@DisplayName("Appointment entity")
class AppointmentTest {

    @Test
    @DisplayName("a new appointment holds a slot lock derived from its date and time")
    void newAppointmentHoldsSlotLock() {
        Appointment appointment = Appointment.builder()
                .appointmentNumber("APT-2026-000001")
                .patient(TestDataFactory.adultPatient())
                .dentist(TestDataFactory.dentist())
                .treatment(TestDataFactory.standardTreatment())
                .appointmentDate(LocalDate.of(2026, 9, 15))
                .appointmentTime(LocalTime.of(10, 30))
                .status(AppointmentStatus.SCHEDULED)
                .createdBy("reception")
                .build();

        assertThat(appointment.getSlotLock()).isEqualTo("2026-09-15|10:30");
    }

    @Test
    @DisplayName("cancelling releases the slot lock, so the slot can be resold")
    void cancellingReleasesTheSlot() {
        Appointment appointment = TestDataFactory.appointment();
        assertThat(appointment.getSlotLock()).isNotNull();

        appointment.changeStatus(AppointmentStatus.CANCELLED, "Patient telephoned", "reception");

        assertThat(appointment.getSlotLock())
                .as("a cancelled appointment must not keep holding the dentist's chair")
                .isNull();
    }

    @Test
    @DisplayName("a no-show also releases the slot lock")
    void noShowReleasesTheSlot() {
        Appointment appointment = TestDataFactory.appointment();

        appointment.changeStatus(AppointmentStatus.NO_SHOW, null, "reception");

        assertThat(appointment.getSlotLock()).isNull();
    }

    @Test
    @DisplayName("completing KEEPS the slot lock - the chair really was occupied")
    void completingKeepsTheSlot() {
        Appointment appointment = TestDataFactory.appointment();

        appointment.changeStatus(AppointmentStatus.COMPLETED, null, "reception");

        assertThat(appointment.getSlotLock()).isNotNull();
    }

    @Test
    @DisplayName("rescheduling moves the slot lock to the new date and time")
    void reschedulingMovesTheSlotLock() {
        Appointment appointment = TestDataFactory.appointment();

        appointment.reschedule(LocalDate.of(2026, 10, 1), LocalTime.of(14, 0), "reception");

        assertThat(appointment.getSlotLock()).isEqualTo("2026-10-01|14:00");
    }

    @Test
    @DisplayName("changing the date alone still re-derives the slot lock")
    void changingDateReDerivesTheSlotLock() {
        Appointment appointment = TestDataFactory.appointment();

        appointment.setAppointmentDate(LocalDate.of(2027, 1, 5));

        assertThat(appointment.getSlotLock()).startsWith("2027-01-05|");
    }

    @Test
    @DisplayName("an illegal status change is refused with a message naming both states")
    void illegalTransitionIsRefused() {
        Appointment appointment = TestDataFactory.appointment();
        appointment.changeStatus(AppointmentStatus.CANCELLED, "Patient telephoned", "reception");

        assertThatThrownBy(() ->
                appointment.changeStatus(AppointmentStatus.COMPLETED, null, "reception"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cancelled")
                .hasMessageContaining("Completed");
    }

    @Test
    @DisplayName("changing to the status it already has is a harmless no-op")
    void repeatingTheCurrentStatusIsHarmless() {
        Appointment appointment = TestDataFactory.appointment();

        appointment.changeStatus(AppointmentStatus.SCHEDULED, null, "reception");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("a null target status is rejected rather than silently ignored")
    void nullTargetIsRejected() {
        Appointment appointment = TestDataFactory.appointment();

        assertThatThrownBy(() -> appointment.changeStatus(null, null, "reception"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("cancelling records the reason for the audit trail")
    void cancellingRecordsTheReason() {
        Appointment appointment = TestDataFactory.appointment();

        appointment.changeStatus(AppointmentStatus.CANCELLED, "Patient unwell", "reception");

        assertThat(appointment.getCancellationReason()).isEqualTo("Patient unwell");
        assertThat(appointment.getUpdatedBy()).isEqualTo("reception");
    }

    @Test
    @DisplayName("the end time is derived from the treatment's duration")
    void endTimeFollowsTheTreatmentDuration() {
        Appointment appointment = TestDataFactory.appointment();   // 45-minute scaling at 10:00

        assertThat(appointment.getDurationMinutes()).isEqualTo(45);
        assertThat(appointment.getEndTime()).isEqualTo(LocalTime.of(10, 45));
    }

    @Test
    @DisplayName("only a completed appointment reports itself as billable")
    void onlyCompletedIsBillable() {
        Appointment appointment = TestDataFactory.appointment();
        assertThat(appointment.isBillable()).isFalse();

        appointment.changeStatus(AppointmentStatus.COMPLETED, null, "reception");
        assertThat(appointment.isBillable()).isTrue();
    }

    @Test
    @DisplayName("a future appointment reports itself as upcoming")
    void futureAppointmentIsUpcoming() {
        assertThat(TestDataFactory.appointment().isUpcoming()).isTrue();
    }
}
