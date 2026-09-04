package lk.icbt.cis6003.dental.server.service.validation;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Rule 3 - the start time must sit on the clinic's 30-minute diary grid.
 *
 * <p>This rule is what makes the anti-double-booking design work. The unique
 * constraint on {@code (dentist_id, slot_lock)} compares exact times, so
 * 10:00 and 10:15 are different keys and would both be accepted - two patients
 * in one chair. Forcing every booking onto :00 and :30 means "the same slot" is
 * a value comparison the database can enforce, instead of an interval-overlap
 * problem it cannot.</p>
 *
 * <p>Seconds are rejected for the same reason: 10:00:00 and 10:00:30 must not
 * be able to coexist.</p>
 */
@Component
public class SlotAlignmentHandler extends BookingValidationHandler {

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public String getRuleName() {
        return "SLOT_ALIGNMENT";
    }

    @Override
    protected ValidationOutcome check(BookingValidationRequest request) {
        LocalTime time = request.appointmentTime();

        if (time.getSecond() != 0 || time.getNano() != 0) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "Appointment times are recorded to the minute. Please enter a time such as 10:00 or 10:30.");
        }

        if (time.getMinute() % ClinicConstants.SLOT_DURATION_MINUTES != 0) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "Appointments start on the hour or the half hour. "
                            + time + " is not a valid slot - please choose "
                            + time.withMinute(0) + " or " + time.withMinute(30) + ".");
        }

        return ValidationOutcome.valid();
    }
}
