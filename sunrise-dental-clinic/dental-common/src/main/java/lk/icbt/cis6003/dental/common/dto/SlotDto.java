package lk.icbt.cis6003.dental.common.dto;

import java.time.LocalTime;

/**
 * One 30-minute slot in a dentist's diary.
 *
 * <p>Exposing availability up-front is the primary defence against the
 * "double bookings" problem in the scenario: the receptionist picks from slots
 * the server has already declared free, and the unique database constraint
 * then catches the rare race between two receptionists booking at once.</p>
 */
public class SlotDto {

    private LocalTime startTime;
    private LocalTime endTime;
    private boolean available;
    private String occupiedBy;

    public SlotDto() {
        // required by Jackson
    }

    public SlotDto(LocalTime startTime, LocalTime endTime, boolean available, String occupiedBy) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
        this.occupiedBy = occupiedBy;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    /** Appointment number occupying this slot, or {@code null} when free. */
    public String getOccupiedBy() {
        return occupiedBy;
    }

    public void setOccupiedBy(String occupiedBy) {
        this.occupiedBy = occupiedBy;
    }

    @Override
    public String toString() {
        return startTime + " - " + endTime + (available ? " (free)" : " (booked)");
    }
}
