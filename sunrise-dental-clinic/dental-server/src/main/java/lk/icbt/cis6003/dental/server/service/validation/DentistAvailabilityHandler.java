package lk.icbt.cis6003.dental.server.service.validation;

import lk.icbt.cis6003.dental.server.domain.Dentist;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Rule 4 - the dentist must be practising, and on shift.
 *
 * <p>The clinic trades 08:00-20:00 but no single dentist works all twelve
 * hours. Booking Dr Jayawardena (08:00-14:00) at 15:00 passes the clinic-hours
 * rule and would then produce a patient sitting in reception waiting for
 * somebody who went home an hour earlier. This is the rule that prevents the
 * "long waiting times" complaint from being reproduced by the new system.</p>
 */
@Component
public class DentistAvailabilityHandler extends BookingValidationHandler {

    @Override
    public int getOrder() {
        return 40;
    }

    @Override
    public String getRuleName() {
        return "DENTIST_AVAILABILITY";
    }

    @Override
    protected ValidationOutcome check(BookingValidationRequest request) {
        Dentist dentist = request.dentist();

        if (dentist == null) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "A dentist must be selected.");
        }

        if (!dentist.isActive()) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "Dr " + dentist.getFullName() + " is no longer practising at the clinic. "
                            + "Please select another dentist.");
        }

        if (!dentist.isWithinWorkingHours(request.appointmentTime(), request.endTime())) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "Dr " + dentist.getFullName() + " works " + dentist.getWorkStartTime()
                            + " to " + dentist.getWorkEndTime() + ". The requested slot ("
                            + request.appointmentTime() + " to " + request.endTime()
                            + ") falls outside those hours.");
        }

        return ValidationOutcome.valid();
    }
}
