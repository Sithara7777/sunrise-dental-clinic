package lk.icbt.cis6003.dental.server.service.report;

import lk.icbt.cis6003.dental.common.dto.report.ReportDto;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>Template Method pattern</b> - the skeleton every report follows.
 *
 * <p><b>The problem it solves.</b> All five reports do the same six things:
 * check the date range, stamp a header, fetch rows, derive percentages, compute
 * totals and hand back an envelope. Written as five independent services, that
 * boilerplate is copied five times - and when the date validation is fixed in
 * one, the other four keep the bug.</p>
 *
 * <p><b>How this is better.</b> The invariant sequence lives once, here, in a
 * {@code final} method. A concrete report supplies only what genuinely differs:
 * its identity, its columns, its query and its totals. The revenue report is
 * about eighty lines, of which none is plumbing.</p>
 *
 * <p><b>Relationship to the Strategy pattern used in billing.</b> Both make
 * behaviour vary, but the constraint differs. Billing rules vary
 * <em>completely</em> and are chosen at run time from a database value, so
 * composition (Strategy) is right. Reports vary only in fixed, well-known
 * slots within an order that must not change, so inheritance (Template Method)
 * is right. Using one where the other belongs is a common way to get an
 * over-engineered or an under-constrained design.</p>
 *
 * @param <R> the row type this report produces
 */
public abstract class AbstractReportGenerator<R> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** No report may span more than a year - it would time out and help nobody. */
    private static final long MAX_RANGE_DAYS = 366;

    /**
     * The fixed algorithm. {@code final} so a subclass cannot reorder it and
     * accidentally summarise rows it has not filtered yet.
     */
    public final ReportDto<R> generate(ReportRequest request) {
        long start = System.currentTimeMillis();

        validateRange(request);

        ReportDto<R> report = new ReportDto<>();
        report.setReportCode(getCode());
        report.setTitle(getTitle());
        report.setDescription(getDescription());
        report.setFromDate(request.effectiveFrom());
        report.setToDate(request.effectiveTo());
        report.setGeneratedAt(LocalDateTime.now());
        report.setGeneratedBy(request.generatedBy());
        report.setColumnHeaders(getColumnHeaders());

        List<R> rows = fetchRows(request);
        rows = postProcess(rows, request);
        report.setRows(rows);

        // Render each row to display strings once, here, so that neither user
        // interface has to know this report's row type.
        List<List<String>> cells = new ArrayList<>(rows.size());
        for (R row : rows) {
            cells.add(toCells(row));
        }
        report.setCells(cells);

        summarise(report, rows, request);

        log.debug("Report {} produced {} rows in {} ms",
                  getCode(), rows.size(), System.currentTimeMillis() - start);
        return report;
    }

    /**
     * Shared guard: dates must make sense and the window must be bounded.
     * Being here rather than in each report is the point of the pattern.
     */
    private void validateRange(ReportRequest request) {
        if (request.effectiveFrom().isAfter(request.effectiveTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "The 'from' date (" + request.effectiveFrom() + ") is after the 'to' date ("
                            + request.effectiveTo() + ").");
        }
        if (request.dayCount() > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "A report can cover at most " + MAX_RANGE_DAYS
                            + " days. Please narrow the date range.");
        }
    }

    /* ------------------------------------------------------------------ */
    /* Steps a concrete report must supply                                 */
    /* ------------------------------------------------------------------ */

    /** Stable identifier, e.g. {@code DAILY_SCHEDULE}. */
    public abstract String getCode();

    /** Heading printed on the report. */
    public abstract String getTitle();

    /** One sentence stating which decision this report supports. */
    public abstract String getDescription();

    /** Column headings, in display order. */
    public abstract List<String> getColumnHeaders();

    /** Reads the rows, normally through the reporting DAO. */
    protected abstract List<R> fetchRows(ReportRequest request);

    /**
     * Renders one row as display strings, in the same order as
     * {@link #getColumnHeaders()}.
     *
     * <p>This is what makes a single generic table able to display every
     * report. Formatting decisions - how a date reads, how many decimal places
     * a percentage carries - belong to the report that owns the data, not to
     * whichever user interface happens to be showing it.</p>
     */
    protected abstract List<String> toCells(R row);

    /** Fills in the named totals shown above the table. */
    protected abstract void summarise(ReportDto<R> report, List<R> rows, ReportRequest request);

    /* ------------------------------------------------------------------ */
    /* Optional step                                                       */
    /* ------------------------------------------------------------------ */

    /**
     * Hook for derived values that need the whole result set - a percentage of
     * a total, for instance, which cannot be computed row by row.
     *
     * <p>Defaults to returning the rows untouched, so reports that need nothing
     * extra do not have to say so.</p>
     */
    protected List<R> postProcess(List<R> rows, ReportRequest request) {
        return rows;
    }

    /* ------------------------------------------------------------------ */
    /* Formatting helpers shared by every report's toCells                 */
    /* ------------------------------------------------------------------ */

    private static final DateTimeFormatter CELL_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter CELL_TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Empty rather than the string "null" - a blank cell reads better. */
    protected String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** Thousands-separated, two decimal places, so money columns line up. */
    protected String money(BigDecimal value) {
        return lk.icbt.cis6003.dental.server.util.MoneyUtils.format(value);
    }

    protected String percent(BigDecimal value) {
        return value == null ? "0.0%" : value.stripTrailingZeros().toPlainString() + "%";
    }

    protected String date(java.time.LocalDate value) {
        return value == null ? "" : value.format(CELL_DATE);
    }

    protected String time(java.time.LocalTime value) {
        return value == null ? "" : value.format(CELL_TIME);
    }
}
