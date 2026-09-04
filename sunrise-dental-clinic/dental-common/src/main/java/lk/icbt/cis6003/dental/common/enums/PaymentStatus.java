package lk.icbt.cis6003.dental.common.enums;

/**
 * Settlement state of an invoice. Drives the "outstanding payments" report,
 * which is one of the decision-support reports proposed for clinic management.
 */
public enum PaymentStatus {

    PENDING("Pending", "badge-pending"),
    PARTIALLY_PAID("Partially Paid", "badge-partial"),
    PAID("Paid", "badge-paid"),
    CANCELLED("Cancelled", "badge-cancelled");

    private final String displayName;
    private final String cssClass;

    PaymentStatus(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }

    /** Money still owed to the clinic. */
    public boolean isOutstanding() {
        return this == PENDING || this == PARTIALLY_PAID;
    }
}
