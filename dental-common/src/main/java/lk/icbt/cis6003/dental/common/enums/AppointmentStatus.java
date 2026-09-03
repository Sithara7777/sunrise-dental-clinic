package lk.icbt.cis6003.dental.common.enums;

import java.util.Set;

/**
 * Lifecycle of a single dental appointment.
 *
 * <p>The enum is not a plain marker: it owns the legal state transitions, so
 * the rule "a cancelled appointment can never be billed" lives in exactly one
 * place instead of being re-checked in every service and controller.</p>
 */
public enum AppointmentStatus {

    /** Created by the front desk, patient has not confirmed yet. */
    SCHEDULED("Scheduled", "badge-scheduled"),

    /** Patient confirmed (reply to the reminder e-mail / SMS). */
    CONFIRMED("Confirmed", "badge-confirmed"),

    /** Patient attended and the treatment was carried out. Billable. */
    COMPLETED("Completed", "badge-completed"),

    /** Called off by clinic or patient. Frees the dentist's time slot. */
    CANCELLED("Cancelled", "badge-cancelled"),

    /** Patient never turned up. Frees the slot but is kept for reporting. */
    NO_SHOW("No Show", "badge-noshow");

    private final String displayName;
    private final String cssClass;

    AppointmentStatus(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }

    /**
     * @return the statuses this status is allowed to move to.
     */
    public Set<AppointmentStatus> allowedTransitions() {
        switch (this) {
            case SCHEDULED:
                return Set.of(CONFIRMED, COMPLETED, CANCELLED, NO_SHOW);
            case CONFIRMED:
                return Set.of(COMPLETED, CANCELLED, NO_SHOW);
            case COMPLETED:
            case CANCELLED:
            case NO_SHOW:
            default:
                return Set.of();
        }
    }

    public boolean canTransitionTo(AppointmentStatus target) {
        return target != null && allowedTransitions().contains(target);
    }

    /** Only a completed visit may be invoiced. */
    public boolean isBillable() {
        return this == COMPLETED;
    }

    /** Cancelled / no-show slots are released back into the dentist's diary. */
    public boolean occupiesSlot() {
        return this == SCHEDULED || this == CONFIRMED || this == COMPLETED;
    }

    /** A terminal status can no longer be edited. */
    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
