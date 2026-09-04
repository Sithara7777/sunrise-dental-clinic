package lk.icbt.cis6003.dental.common;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Clinic-wide constants shared by the server and the remote desktop client.
 *
 * <p>Values that the scenario did not specify are documented assumptions:</p>
 * <ul>
 *   <li>The clinic trades 08:00-20:00, seven days a week.</li>
 *   <li>Appointment slots are 30 minutes, so a dentist's diary is a fixed grid
 *       and "double booking" reduces to a uniqueness rule on
 *       (dentist, date, start-time).</li>
 *   <li>Sri Lankan VAT of 18% applies to dental services, and the clinic bills
 *       in LKR.</li>
 *   <li>Appointments may be booked at most 180 days ahead.</li>
 * </ul>
 *
 * <p>The class is {@code final} with a private constructor: it is a pure
 * constant holder and must never be instantiated or subclassed.</p>
 */
public final class ClinicConstants {

    private ClinicConstants() {
        throw new AssertionError("ClinicConstants is a constant holder and must not be instantiated");
    }

    /* ------------------------------------------------------------------ */
    /* Clinic identity (printed on every bill / receipt)                   */
    /* ------------------------------------------------------------------ */

    public static final String CLINIC_NAME = "Sunrise Dental Clinic";
    public static final String CLINIC_ADDRESS_LINE_1 = "No. 172, Galle Road";
    public static final String CLINIC_ADDRESS_LINE_2 = "Colombo 03, Sri Lanka";
    public static final String CLINIC_PHONE = "+94 11 2 573 100";
    public static final String CLINIC_EMAIL = "info@sunrisedental.lk";
    public static final String CLINIC_REGISTRATION_NO = "SLMC/DC/2011/0842";

    /* ------------------------------------------------------------------ */
    /* Money                                                               */
    /* ------------------------------------------------------------------ */

    public static final String CURRENCY_CODE = "LKR";
    public static final String CURRENCY_SYMBOL = "Rs.";

    /** Sri Lankan VAT applied to dental services. */
    public static final BigDecimal VAT_RATE = new BigDecimal("0.18");

    /** Scale used for every monetary amount stored or displayed. */
    public static final int MONEY_SCALE = 2;

    /* ------------------------------------------------------------------ */
    /* Diary rules                                                         */
    /* ------------------------------------------------------------------ */

    public static final LocalTime CLINIC_OPENING_TIME = LocalTime.of(8, 0);
    public static final LocalTime CLINIC_CLOSING_TIME = LocalTime.of(20, 0);
    public static final int SLOT_DURATION_MINUTES = 30;
    public static final int MAX_ADVANCE_BOOKING_DAYS = 180;

    /* ------------------------------------------------------------------ */
    /* Identifier formats                                                  */
    /* ------------------------------------------------------------------ */

    /** e.g. {@code APT-2026-000137} - the unique appointment number. */
    public static final String APPOINTMENT_NUMBER_PREFIX = "APT";
    public static final String APPOINTMENT_NUMBER_PATTERN = "^APT-\\d{4}-\\d{6}$";

    /** e.g. {@code INV-2026-000137}. */
    public static final String INVOICE_NUMBER_PREFIX = "INV";
    public static final String INVOICE_NUMBER_PATTERN = "^INV-\\d{4}-\\d{6}$";

    /** e.g. {@code PAT-000042}. */
    public static final String PATIENT_CODE_PREFIX = "PAT";

    /** e.g. {@code DEN-007}. */
    public static final String DENTIST_CODE_PREFIX = "DEN";

    /* ------------------------------------------------------------------ */
    /* Validation patterns                                                 */
    /* ------------------------------------------------------------------ */

    /**
     * Sri Lankan mobile / land line in either local or international form:
     * {@code 0771234567}, {@code 011 2573100}, {@code +94771234567}.
     */
    public static final String CONTACT_NUMBER_PATTERN = "^(?:\\+94|0)(?:\\d{9})$";

    /** National Identity Card: old 9 digits + V/X, or new 12 digits. */
    public static final String NIC_PATTERN = "^(?:\\d{9}[VvXx]|\\d{12})$";

    public static final String EMAIL_PATTERN =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    /** Letters, spaces, apostrophes, dots and hyphens only. */
    public static final String PERSON_NAME_PATTERN = "^[A-Za-z][A-Za-z .'\\-]{1,99}$";

    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9._-]{4,30}$";

    /* ------------------------------------------------------------------ */
    /* Formatting                                                          */
    /* ------------------------------------------------------------------ */

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter DISPLAY_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
}
