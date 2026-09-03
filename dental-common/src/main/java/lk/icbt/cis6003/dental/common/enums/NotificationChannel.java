package lk.icbt.cis6003.dental.common.enums;

/**
 * Delivery channels for appointment alerts.
 *
 * <p>Each channel is implemented behind a common gateway interface (Adapter
 * pattern) so that swapping the mock SMS gateway for a real one - or adding
 * WhatsApp later - requires no change to the business tier.</p>
 */
public enum NotificationChannel {

    EMAIL("E-mail"),
    SMS("SMS"),
    SYSTEM("In-system alert");

    private final String displayName;

    NotificationChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
