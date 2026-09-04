package lk.icbt.cis6003.dental.server.exception;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The direct answer to the scenario's "double bookings" problem: raised when a
 * requested dentist / date / time is already occupied.
 */
public class SlotUnavailableException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public SlotUnavailableException(String dentistName, LocalDate date, LocalTime time) {
        super(ErrorCode.SLOT_UNAVAILABLE,
              "Dr " + dentistName + " is already booked on " + date + " at " + time
                      + ". Please choose another time slot.");
    }

    public SlotUnavailableException(String message) {
        super(ErrorCode.SLOT_UNAVAILABLE, message);
    }
}
