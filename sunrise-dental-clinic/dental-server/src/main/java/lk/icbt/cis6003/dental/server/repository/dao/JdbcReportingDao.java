package lk.icbt.cis6003.dental.server.repository.dao;

import lk.icbt.cis6003.dental.common.dto.report.DailyScheduleRow;
import lk.icbt.cis6003.dental.common.dto.report.DentistWorkloadRow;
import lk.icbt.cis6003.dental.common.dto.report.OutstandingInvoiceRow;
import lk.icbt.cis6003.dental.common.dto.report.RevenueRow;
import lk.icbt.cis6003.dental.common.dto.report.TreatmentPopularityRow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * JDBC implementation of {@link ReportingDao}.
 *
 * <p>Every statement here reads a database <em>view</em> or calls a
 * <em>stored function</em> rather than embedding the aggregation logic in
 * Java. The benefit is not stylistic: the ageing-bucket rule and the invoice
 * total formula then exist once, in the database, and apply equally to this
 * application, to an ad-hoc SQL query run by the accountant, and to any future
 * reporting tool pointed at the same schema.</p>
 *
 * <p>All parameters are bound, never concatenated - the SQL-injection defence
 * required by the Ethical strand of the assessment criteria.</p>
 */
@Repository
public class JdbcReportingDao implements ReportingDao {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcReportingDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /* ------------------------------------------------------------------ */
    /* Daily schedule                                                      */
    /* ------------------------------------------------------------------ */

    private static final String SQL_DAILY_SCHEDULE = """
            SELECT appointment_time, appointment_number, patient_name, contact_number,
                   dentist_name, treatment_name, status
            FROM v_daily_schedule
            WHERE appointment_date = :onDate
              AND (:dentistCode IS NULL OR dentist_code = :dentistCode)
            ORDER BY appointment_time ASC, appointment_number ASC
            """;

    @Override
    @Transactional(readOnly = true)
    public List<DailyScheduleRow> findDailySchedule(LocalDate date, String dentistCode) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("onDate", date)
                .addValue("dentistCode", blankToNull(dentistCode));
        return jdbc.query(SQL_DAILY_SCHEDULE, params, DAILY_SCHEDULE_MAPPER);
    }

    private static final RowMapper<DailyScheduleRow> DAILY_SCHEDULE_MAPPER = (rs, rowNum) -> {
        DailyScheduleRow row = new DailyScheduleRow();
        row.setAppointmentTime(rs.getTime("appointment_time").toLocalTime());
        row.setAppointmentNumber(rs.getString("appointment_number"));
        row.setPatientName(rs.getString("patient_name"));
        row.setContactNumber(rs.getString("contact_number"));
        row.setDentistName(rs.getString("dentist_name"));
        row.setTreatmentName(rs.getString("treatment_name"));
        row.setStatus(rs.getString("status"));
        return row;
    };

    /* ------------------------------------------------------------------ */
    /* Revenue                                                             */
    /* ------------------------------------------------------------------ */

    private static final String SQL_REVENUE = """
            SELECT issued_date, invoice_count, gross_amount, discount_amount,
                   tax_amount, net_amount, collected_amount, outstanding_amount
            FROM v_revenue_daily
            WHERE issued_date BETWEEN :fromDate AND :toDate
            ORDER BY issued_date ASC
            """;

    @Override
    @Transactional(readOnly = true)
    public List<RevenueRow> findRevenueBetween(LocalDate from, LocalDate to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromDate", from)
                .addValue("toDate", to);
        return jdbc.query(SQL_REVENUE, params, REVENUE_MAPPER);
    }

    private static final RowMapper<RevenueRow> REVENUE_MAPPER = (rs, rowNum) -> {
        RevenueRow row = new RevenueRow();
        row.setDate(rs.getDate("issued_date").toLocalDate());
        row.setInvoiceCount(rs.getLong("invoice_count"));
        row.setGrossAmount(money(rs, "gross_amount"));
        row.setDiscountAmount(money(rs, "discount_amount"));
        row.setTaxAmount(money(rs, "tax_amount"));
        row.setNetAmount(money(rs, "net_amount"));
        row.setCollectedAmount(money(rs, "collected_amount"));
        row.setOutstandingAmount(money(rs, "outstanding_amount"));
        return row;
    };

    /* ------------------------------------------------------------------ */
    /* Dentist workload                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Aggregates the view over the requested window.
     *
     * <p>The date filter is inside the {@code SUM(CASE ...)} rather than in a
     * {@code WHERE} clause on purpose: a dentist who saw nobody in the period
     * is precisely the row a manager needs to see, and a {@code WHERE} would
     * silently drop them.</p>
     */
    private static final String SQL_WORKLOAD = """
            SELECT dentist_code,
                   MIN(dentist_name)   AS dentist_name,
                   MIN(specialization) AS specialization,
                   SUM(CASE WHEN work_date BETWEEN :fromDate AND :toDate
                            THEN total_appointments     ELSE 0 END) AS total_appointments,
                   SUM(CASE WHEN work_date BETWEEN :fromDate AND :toDate
                            THEN completed_appointments ELSE 0 END) AS completed_appointments,
                   SUM(CASE WHEN work_date BETWEEN :fromDate AND :toDate
                            THEN cancelled_appointments ELSE 0 END) AS cancelled_appointments,
                   SUM(CASE WHEN work_date BETWEEN :fromDate AND :toDate
                            THEN no_show_appointments   ELSE 0 END) AS no_show_appointments,
                   SUM(CASE WHEN work_date BETWEEN :fromDate AND :toDate
                            THEN booked_minutes         ELSE 0 END) AS booked_minutes,
                   SUM(CASE WHEN work_date BETWEEN :fromDate AND :toDate
                            THEN revenue_generated      ELSE 0 END) AS revenue_generated
            FROM v_dentist_workload
            GROUP BY dentist_code
            ORDER BY total_appointments DESC, dentist_code ASC
            """;

    @Override
    @Transactional(readOnly = true)
    public List<DentistWorkloadRow> findDentistWorkload(LocalDate from, LocalDate to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromDate", from)
                .addValue("toDate", to);
        return jdbc.query(SQL_WORKLOAD, params, WORKLOAD_MAPPER);
    }

    private static final RowMapper<DentistWorkloadRow> WORKLOAD_MAPPER = (rs, rowNum) -> {
        DentistWorkloadRow row = new DentistWorkloadRow();
        row.setDentistCode(rs.getString("dentist_code"));
        row.setDentistName(rs.getString("dentist_name"));
        row.setSpecialization(rs.getString("specialization"));
        row.setTotalAppointments(rs.getLong("total_appointments"));
        row.setCompletedAppointments(rs.getLong("completed_appointments"));
        row.setCancelledAppointments(rs.getLong("cancelled_appointments"));
        row.setNoShowAppointments(rs.getLong("no_show_appointments"));
        row.setBookedMinutes(rs.getLong("booked_minutes"));
        row.setRevenueGenerated(money(rs, "revenue_generated"));
        return row;
    };

    /* ------------------------------------------------------------------ */
    /* Treatment popularity                                                */
    /* ------------------------------------------------------------------ */

    private static final String SQL_POPULARITY = """
            SELECT treatment_code,
                   MIN(treatment_name) AS treatment_name,
                   MIN(category)       AS category,
                   SUM(CASE WHEN performed_date BETWEEN :fromDate AND :toDate
                            THEN times_performed ELSE 0 END) AS times_performed,
                   SUM(CASE WHEN performed_date BETWEEN :fromDate AND :toDate
                            THEN total_revenue   ELSE 0 END) AS total_revenue
            FROM v_treatment_popularity
            GROUP BY treatment_code
            ORDER BY times_performed DESC, treatment_code ASC
            """;

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentPopularityRow> findTreatmentPopularity(LocalDate from, LocalDate to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromDate", from)
                .addValue("toDate", to);
        return jdbc.query(SQL_POPULARITY, params, POPULARITY_MAPPER);
    }

    private static final RowMapper<TreatmentPopularityRow> POPULARITY_MAPPER = (rs, rowNum) -> {
        TreatmentPopularityRow row = new TreatmentPopularityRow();
        row.setTreatmentCode(rs.getString("treatment_code"));
        row.setTreatmentName(rs.getString("treatment_name"));
        row.setCategory(rs.getString("category"));
        row.setTimesPerformed(rs.getLong("times_performed"));
        row.setTotalRevenue(money(rs, "total_revenue"));
        return row;
    };

    /* ------------------------------------------------------------------ */
    /* Outstanding invoices                                                */
    /* ------------------------------------------------------------------ */

    private static final String SQL_OUTSTANDING = """
            SELECT invoice_number, appointment_number, patient_name, patient_contact,
                   issued_date, days_outstanding, ageing_bucket,
                   total_amount, amount_paid, balance_due, payment_status
            FROM v_outstanding_invoice
            ORDER BY days_outstanding DESC, balance_due DESC
            """;

    @Override
    @Transactional(readOnly = true)
    public List<OutstandingInvoiceRow> findOutstandingInvoices() {
        return jdbc.query(SQL_OUTSTANDING, new MapSqlParameterSource(), OUTSTANDING_MAPPER);
    }

    private static final RowMapper<OutstandingInvoiceRow> OUTSTANDING_MAPPER = (rs, rowNum) -> {
        OutstandingInvoiceRow row = new OutstandingInvoiceRow();
        row.setInvoiceNumber(rs.getString("invoice_number"));
        row.setAppointmentNumber(rs.getString("appointment_number"));
        row.setPatientName(rs.getString("patient_name"));
        row.setContactNumber(rs.getString("patient_contact"));
        row.setIssuedDate(rs.getDate("issued_date").toLocalDate());
        row.setDaysOutstanding(rs.getLong("days_outstanding"));
        row.setAgeingBucket(rs.getString("ageing_bucket"));
        row.setTotalAmount(money(rs, "total_amount"));
        row.setAmountPaid(money(rs, "amount_paid"));
        row.setBalanceDue(money(rs, "balance_due"));
        row.setPaymentStatus(rs.getString("payment_status"));
        return row;
    };

    /* ------------------------------------------------------------------ */
    /* Stored function calls                                               */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateInvoiceTotalInDatabase(BigDecimal consultationFee,
                                                      BigDecimal treatmentCost,
                                                      BigDecimal surcharge,
                                                      BigDecimal discountPercentage,
                                                      BigDecimal taxRate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("consultationFee", consultationFee)
                .addValue("treatmentCost", treatmentCost)
                .addValue("surcharge", surcharge)
                .addValue("discountPct", discountPercentage)
                .addValue("taxRate", taxRate);
        return jdbc.queryForObject(
                "SELECT FN_INVOICE_TOTAL(:consultationFee, :treatmentCost, :surcharge, :discountPct, :taxRate)",
                params, BigDecimal.class);
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveAgeingBucket(long daysOutstanding) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("days", daysOutstanding);
        return jdbc.queryForObject("SELECT FN_AGEING_BUCKET(:days)", params, String.class);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    /** SUM() over no rows yields SQL NULL; a report must show 0.00, not blank. */
    private static BigDecimal money(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
