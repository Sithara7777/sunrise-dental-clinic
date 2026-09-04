package lk.icbt.cis6003.dental.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;

/**
 * Moves an appointment along its lifecycle. The legality of the move itself is
 * decided by {@link AppointmentStatus#canTransitionTo}, not by this DTO.
 */
public class StatusUpdateRequest {

    @NotNull(message = "New status is required")
    private AppointmentStatus status;

    @Size(max = 300, message = "Reason must not exceed 300 characters")
    private String reason;

    public StatusUpdateRequest() {
        // required by Jackson
    }

    public StatusUpdateRequest(AppointmentStatus status) {
        this.status = status;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
