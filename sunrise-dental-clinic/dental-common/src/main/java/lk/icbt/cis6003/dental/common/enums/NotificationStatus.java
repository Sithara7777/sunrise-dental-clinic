package lk.icbt.cis6003.dental.common.enums;

/**
 * Outcome of an attempted notification. Every attempt - successful or not - is
 * persisted, which gives the clinic an auditable trail of what the system told
 * a patient and when.
 */
public enum NotificationStatus {

    SENT("Sent"),
    FAILED("Failed"),
    SUPPRESSED("Suppressed");

    private final String displayName;

    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
