package lk.icbt.cis6003.dental.server.service.validation;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rule 1 - the appointment must fall inside the clinic's booking window.
 *
 * <p>Runs first because it is the cheapest check and the most fundamental: if
 * the date itself is nonsense, telling the user that the dentist is busy would
 * be actively misleading.</p>
 *
 * <p>Rejects a booking in the past, and one more than
 * {@value ClinicConstants#MAX_ADVANCE_BOOKING_DAYS} days ahead. The upper bound
 * exists because a booking made a year out is almost always a mistyped year,
 * and it would silently hold a dentist's slot until someone noticed.</p>
 */
@Component
public class BookingWindowHandler extends BookingValidationHandler {

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public String getRuleName() {
        return "BOOKING_WINDOW";
    }

    @Override
    protected ValidationOutcome check(BookingValidationRequest request) {
        LocalDate date = request.appointmentDate();
        if (date == null) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "An appointment date is required.");
        }
        if (request.appointmentTime() == null) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "An appointment time is required.");
        }

        LocalDateTime startsAt = LocalDateTime.of(date, request.appointmentTime());
        if (startsAt.isBefore(LocalDateTime.now())) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "Appointments cannot be booked in the past. "
                            + "Please choose a date and time from now onwards.");
        }

        LocalDate latest = LocalDate.now().plusDays(ClinicConstants.MAX_ADVANCE_BOOKING_DAYS);
        if (date.isAfter(latest)) {
            return ValidationOutcome.invalid(getRuleName(), ErrorCode.VALIDATION_ERROR,
                    "Appointments can only be booked up to "
                            + ClinicConstants.MAX_ADVANCE_BOOKING_DAYS + " days ahead (on or before "
                            + latest + "). Please check the year you entered.");
        }

        return ValidationOutcome.valid();
    }
}
