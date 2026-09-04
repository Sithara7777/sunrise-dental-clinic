package lk.icbt.cis6003.dental.server.service.report;

import java.time.LocalDate;

/**
 * Parameters for one report run.
 *
 * <p>A single request type serves all five reports even though not every
 * report reads every field - the daily schedule ignores {@code toDate}, the
 * debtor report ignores both dates. That is a deliberate simplification: it
 * lets the report factory, the REST endpoint, the web controller and the Swing
 * report window each handle "a report" generically instead of carrying five
 * near-identical shapes.
 *
 * @param reportCode  which report, e.g. {@code DAILY_SCHEDULE}
 * @param fromDate    start of the period, inclusive
 * @param toDate      end of the period, inclusive
 * @param dentistCode optional dentist filter; {@code null} for all
 * @param generatedBy the username stamped on the report header
 */
public record ReportRequest(String reportCode,
                            LocalDate fromDate,
                            LocalDate toDate,
                            String dentistCode,
                            String generatedBy) {

    /** A single day, used by the daily schedule. */
    public static ReportRequest forDay(String reportCode, LocalDate day,
                                       String dentistCode, String generatedBy) {
        return new ReportRequest(reportCode, day, day, dentistCode, generatedBy);
    }

    /** A date range, used by the revenue, workload and popularity reports. */
    public static ReportRequest forRange(String reportCode, LocalDate from, LocalDate to,
                                         String generatedBy) {
        return new ReportRequest(reportCode, from, to, null, generatedBy);
    }

    /** Effective start date, defaulting to the first of the current month. */
    public LocalDate effectiveFrom() {
        return fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
    }

    /** Effective end date, defaulting to today. */
    public LocalDate effectiveTo() {
        return toDate != null ? toDate : LocalDate.now();
    }

    /** Whole days covered, inclusive of both ends. Never less than one. */
    public long dayCount() {
        long days = java.time.temporal.ChronoUnit.DAYS.between(effectiveFrom(), effectiveTo()) + 1;
        return Math.max(days, 1);
    }
}
