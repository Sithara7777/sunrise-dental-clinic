package lk.icbt.cis6003.dental.common;

/**
 * The published REST contract.
 *
 * <p>Holding the paths in the shared module means the server's
 * {@code @RequestMapping} values and the desktop client's URI builder are
 * driven by the same literals. A typo therefore becomes a compile-time
 * problem in one place rather than a 404 at demo time.</p>
 */
public final class ApiPaths {

    private ApiPaths() {
        throw new AssertionError("ApiPaths is a constant holder and must not be instantiated");
    }

    public static final String API_ROOT = "/api/v1";

    public static final String AUTH = API_ROOT + "/auth";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_ME = AUTH + "/me";
    public static final String AUTH_LOGOUT = AUTH + "/logout";

    public static final String APPOINTMENTS = API_ROOT + "/appointments";
    public static final String APPOINTMENT_BY_NUMBER = APPOINTMENTS + "/{appointmentNumber}";
    public static final String APPOINTMENT_STATUS = APPOINTMENT_BY_NUMBER + "/status";
    public static final String APPOINTMENT_AVAILABLE_SLOTS = APPOINTMENTS + "/available-slots";

    public static final String PATIENTS = API_ROOT + "/patients";
    public static final String PATIENT_BY_CODE = PATIENTS + "/{patientCode}";
    public static final String PATIENT_SEARCH = PATIENTS + "/search";

    public static final String DENTISTS = API_ROOT + "/dentists";
    public static final String DENTIST_BY_CODE = DENTISTS + "/{dentistCode}";

    public static final String TREATMENTS = API_ROOT + "/treatments";
    public static final String TREATMENT_BY_CODE = TREATMENTS + "/{treatmentCode}";

    public static final String INVOICES = API_ROOT + "/invoices";
    public static final String INVOICE_BY_NUMBER = INVOICES + "/{invoiceNumber}";
    public static final String INVOICE_FOR_APPOINTMENT = INVOICES + "/appointment/{appointmentNumber}";
    public static final String INVOICE_PREVIEW = INVOICES + "/preview/{appointmentNumber}";
    public static final String INVOICE_PAY = INVOICE_BY_NUMBER + "/payment";
    public static final String INVOICE_RECEIPT_TEXT = INVOICE_BY_NUMBER + "/receipt";

    public static final String REPORTS = API_ROOT + "/reports";
    public static final String REPORT_DAILY_SCHEDULE = REPORTS + "/daily-schedule";
    public static final String REPORT_REVENUE = REPORTS + "/revenue";
    public static final String REPORT_DENTIST_WORKLOAD = REPORTS + "/dentist-workload";
    public static final String REPORT_TREATMENT_POPULARITY = REPORTS + "/treatment-popularity";
    public static final String REPORT_OUTSTANDING = REPORTS + "/outstanding-invoices";
    public static final String REPORT_DASHBOARD = REPORTS + "/dashboard";

    public static final String HELP = API_ROOT + "/help";
}
