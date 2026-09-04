package lk.icbt.cis6003.dental.server.service.validation;

import lk.icbt.cis6003.dental.server.domain.Dentist;
import lk.icbt.cis6003.dental.server.domain.Treatment;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The subject a booking validation chain inspects.
 *
 * <p>Holds the already-resolved {@link Dentist} and {@link Treatment} entities
 * rather than their codes, so no handler has to hit the database to answer a
 * question the caller has already answered. Handlers stay cheap, which is what
 * makes a six-link chain acceptable on the request path.</p>
 *
 * @param dentist               the chosen dentist, already loaded
 * @param treatment             the chosen treatment, already loaded
 * @param patientCode           the patient, or {@code null} for a brand new one
 * @param appointmentDate       the requested date
 * @param appointmentTime       the requested start time
 * @param excludeAppointmentId  when rescheduling, the appointment being moved -
 *                              so it does not report itself as a clash
 */
public record BookingValidationRequest(Dentist dentist,
                                       Treatment treatment,
                                       String patientCode,
                                       LocalDate appointmentDate,
                                       LocalTime appointmentTime,
                                       Long excludeAppointmentId) {

    /** Convenience factory for a brand-new booking. */
    public static BookingValidationRequest forNewBooking(Dentist dentist, Treatment treatment,
                                                         String patientCode,
                                                         LocalDate date, LocalTime time) {
        return new BookingValidationRequest(dentist, treatment, patientCode, date, time, null);
    }

    /** End of the proposed appointment, derived from the treatment duration. */
    public LocalTime endTime() {
        if (appointmentTime == null || treatment == null || treatment.getDurationMinutes() == null) {
            return appointmentTime;
        }
        return appointmentTime.plusMinutes(treatment.getDurationMinutes());
    }

    public boolean isReschedule() {
        return excludeAppointmentId != null;
    }
}
