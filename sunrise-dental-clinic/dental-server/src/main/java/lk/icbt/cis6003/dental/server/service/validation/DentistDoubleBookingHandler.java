package lk.icbt.cis6003.dental.server.service.validation;

import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import lk.icbt.cis6003.dental.server.repository.AppointmentRepository;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Rule 5 - the dentist must not already be booked at that time.
 *
 * <p>This is the rule the scenario is really about: "double bookings" is the
 * first problem the clinic listed. It is placed fifth because it is the first
 * rule that costs a database query, so the four cheap checks eliminate the
 * obviously invalid requests first.</p>
 *
 * <p><b>This check is not the guarantee.</b> Between this query and the
 * {@code INSERT}, a second receptionist can book the same slot. The real
 * guarantee is the unique constraint {@code uk_appointment_slot} on
 * {@code (dentist_id, slot_lock)}, which the database enforces atomically.
 * This handler exists so that the ordinary case produces a clear, actionable
 * sentence instead of a constraint-violation stack trace - defence in depth,
 * with the database as the authority and this as the courtesy.</p>
 */
@Component
public class DentistDoubleBookingHandler extends BookingValidationHandler {

    /** Statuses that actually hold the chair; cancelled and no-show release it. */
    private static final Set<AppointmentStatus> OCCUPYING = EnumSet.of(
            AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED);

    private final AppointmentRepository appointmentRepository;

    public DentistDoubleBookingHandler(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public String getRuleName() {
        return "DENTIST_DOUBLE_BOOKING";
    }

    @Override
    protected ValidationOutcome check(BookingValidationRequest request) {
        boolean taken = appointmentRepository.isSlotTaken(
                request.dentist().getDentistCode(),
                request.appointmentDate(),
                request.appointmentTime(),
                OCCUPYING,
                request.excludeAppointmentId());

        if (taken) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.SLOT_UNAVAILABLE,
                    "Dr " + request.dentist().getFullName() + " already has an appointment on "
                            + request.appointmentDate() + " at " + request.appointmentTime()
                            + ". Use 'Check availability' to see the free slots for that day.");
        }

        return ValidationOutcome.valid();
    }

    /** Exposed so the availability endpoint uses exactly the same definition. */
    public static Set<AppointmentStatus> occupyingStatuses() {
        return OCCUPYING;
    }
}
