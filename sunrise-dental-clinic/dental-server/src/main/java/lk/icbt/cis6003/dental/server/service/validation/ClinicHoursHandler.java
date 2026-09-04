package lk.icbt.cis6003.dental.server.service.validation;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Rule 2 - the whole appointment must fit inside the clinic's opening hours.
 *
 * <p>Checks the <em>end</em> of the appointment as well as the start. A
 * 90-minute root canal starting at 19:00 begins while the clinic is open and
 * ends half an hour after it closes; checking only the start time would let
 * that through and strand a patient mid-treatment at closing.</p>
 */
@Component
public class ClinicHoursHandler extends BookingValidationHandler {

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public String getRuleName() {
        return "CLINIC_HOURS";
    }

    @Override
    protected ValidationOutcome check(BookingValidationRequest request) {
        LocalTime start = request.appointmentTime();
        LocalTime end = request.endTime();

        if (start.isBefore(ClinicConstants.CLINIC_OPENING_TIME)) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "The clinic opens at " + ClinicConstants.CLINIC_OPENING_TIME
                            + ". Please choose a later time.");
        }

        if (end != null && end.isAfter(ClinicConstants.CLINIC_CLOSING_TIME)) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "This treatment takes " + request.treatment().getDurationMinutes()
                            + " minutes and would finish at " + end + ", after the clinic closes at "
                            + ClinicConstants.CLINIC_CLOSING_TIME + ". Please choose an earlier time.");
        }

        return ValidationOutcome.valid();
    }
}
