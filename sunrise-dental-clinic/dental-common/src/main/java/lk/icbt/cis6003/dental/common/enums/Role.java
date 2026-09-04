package lk.icbt.cis6003.dental.common.enums;

/**
 * Staff roles recognised by the clinic system.
 *
 * <p>The scenario only states that "only authorized staff can use the system".
 * We assume three distinct staff roles, because a receptionist booking an
 * appointment and a dentist reviewing their own day's list have genuinely
 * different needs, and billing should not be editable by everyone.</p>
 *
 * <p>The {@code authority} string is the value stored against the Spring
 * Security principal; the {@code ROLE_} prefix is the framework convention.</p>
 */
public enum Role {

    /** Full access: staff management, treatment catalogue, all reports. */
    ADMIN("ROLE_ADMIN", "Administrator"),

    /** Front desk: register patients, book appointments, issue and settle bills. */
    RECEPTIONIST("ROLE_RECEPTIONIST", "Receptionist"),

    /** Clinical staff: view their own schedule and patient history, complete visits. */
    DENTIST("ROLE_DENTIST", "Dentist");

    private final String authority;
    private final String displayName;

    Role(String authority, String displayName) {
        this.authority = authority;
        this.displayName = displayName;
    }

    public String getAuthority() {
        return authority;
    }

    public String getDisplayName() {
        return displayName;
    }
}
