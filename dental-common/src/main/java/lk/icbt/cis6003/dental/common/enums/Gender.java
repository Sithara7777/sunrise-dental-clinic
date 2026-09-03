package lk.icbt.cis6003.dental.common.enums;

/**
 * Patient gender. {@link #UNSPECIFIED} exists so the field is never a forced
 * choice - the scenario does not require it and refusing to record it must not
 * block registration.
 */
public enum Gender {

    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other"),
    UNSPECIFIED("Prefer not to say");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
