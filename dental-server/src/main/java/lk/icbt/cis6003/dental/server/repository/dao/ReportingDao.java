package lk.icbt.cis6003.dental.server.repository.dao;

import lk.icbt.cis6003.dental.common.dto.report.DailyScheduleRow;
import lk.icbt.cis6003.dental.common.dto.report.DentistWorkloadRow;
import lk.icbt.cis6003.dental.common.dto.report.OutstandingInvoiceRow;
import lk.icbt.cis6003.dental.common.dto.report.RevenueRow;
import lk.icbt.cis6003.dental.common.dto.report.TreatmentPopularityRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object for the reporting reads.
 *
 * <p><b>Why a hand-written DAO next to Spring Data repositories?</b> The two
 * solve different problems and the split is intentional:</p>
 *
 * <ul>
 *   <li>The JPA repositories own the <em>transactional</em> model - load an
 *       appointment, change it, save it. Object identity and dirty checking
 *       earn their cost there.</li>
 *   <li>Reporting reads never load an object graph. They aggregate thousands
 *       of rows into a handful of totals, which is what SQL is good at and
 *       what JPA is worst at. Pushing that work into database <em>views</em>
 *       and <em>stored functions</em>, then reading flat rows through
 *       {@code JdbcTemplate}, keeps the aggregation next to the data.</li>
 * </ul>
 *
 * <p>The interface exists so the business tier depends on the abstraction; the
 * report generators can be unit tested against a stub DAO with no database at
 * all.</p>
 */
public interface ReportingDao {

    /**
     * Reads {@code v_daily_schedule}.
     *
     * @param date        the clinic day
     * @param dentistCode optional filter; {@code null} for all dentists
     */
    List<DailyScheduleRow> findDailySchedule(LocalDate date, String dentistCode);

    /** Reads {@code v_revenue_daily} for an inclusive date range. */
    List<RevenueRow> findRevenueBetween(LocalDate from, LocalDate to);

    /**
     * Reads {@code v_dentist_workload}. Utilisation is deliberately left to the
     * generator, which knows each dentist's shift length.
     */
    List<DentistWorkloadRow> findDentistWorkload(LocalDate from, LocalDate to);

    /** Reads {@code v_treatment_popularity}. Revenue share is computed by the generator. */
    List<TreatmentPopularityRow> findTreatmentPopularity(LocalDate from, LocalDate to);

    /**
     * Reads {@code v_outstanding_invoice}, whose ageing bucket comes from the
     * stored function {@code FN_AGEING_BUCKET}.
     */
    List<OutstandingInvoiceRow> findOutstandingInvoices();

    /**
     * Invokes the database stored function {@code FN_INVOICE_TOTAL}.
     *
     * <p>Used by the billing reconciliation check: the Java pricing tier and
     * the database function compute the total independently, and a mismatch is
     * reported rather than silently trusted. That is the "billing errors"
     * requirement taken seriously.</p>
     */
    BigDecimal calculateInvoiceTotalInDatabase(BigDecimal consultationFee,
                                               BigDecimal treatmentCost,
                                               BigDecimal surcharge,
                                               BigDecimal discountPercentage,
                                               BigDecimal taxRate);

    /** Invokes the stored function {@code FN_AGEING_BUCKET}. */
    String resolveAgeingBucket(long daysOutstanding);
}
