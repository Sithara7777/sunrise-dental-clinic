package lk.icbt.cis6003.dental.server.service.validation;

import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import lk.icbt.cis6003.dental.server.repository.AppointmentRepository;
import org.springframework.stereotype.Component;

/**
 * Rule 6 - the patient must not already be booked elsewhere at that time.
 *
 * <p>The database constraint prevents one <em>dentist</em> being double
 * booked, but nothing stops the same patient being entered with two different
 * dentists at 10:00. In practice that happens when a patient telephones and
 * the call is handled twice, and it wastes a slot the clinic could have
 * sold.</p>
 *
 * <p>Runs last: it is the second database query, and it only matters once
 * everything else about the booking is already valid. It is skipped for a
 * brand-new patient, who by definition has no other appointments.</p>
 */
@Component
public class PatientDoubleBookingHandler extends BookingValidationHandler {

    private final AppointmentRepository appointmentRepository;

    public PatientDoubleBookingHandler(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public int getOrder() {
        return 60;
    }

    @Override
    public String getRuleName() {
        return "PATIENT_DOUBLE_BOOKING";
    }

    @Override
    protected ValidationOutcome check(BookingValidationRequest request) {
        String patientCode = request.patientCode();
        if (patientCode == null || patientCode.isBlank()) {
            // A new patient cannot already be booked.
            return ValidationOutcome.valid();
        }

        boolean alreadyBooked = appointmentRepository.patientAlreadyBooked(
                patientCode,
                request.appointmentDate(),
                request.appointmentTime(),
                DentistDoubleBookingHandler.occupyingStatuses());

        if (alreadyBooked) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.SLOT_UNAVAILABLE,
                    "This patient already has an appointment on " + request.appointmentDate()
                            + " at " + request.appointmentTime()
                            + ". Please check the patient's appointment history before booking again.");
        }

        return ValidationOutcome.valid();
    }
}
