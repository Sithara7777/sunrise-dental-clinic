package lk.icbt.cis6003.dental.server.service.report;

import lk.icbt.cis6003.dental.common.dto.report.OutstandingInvoiceRow;
import lk.icbt.cis6003.dental.common.dto.report.ReportDto;
import lk.icbt.cis6003.dental.server.repository.dao.ReportingDao;
import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REPORT 5 - Outstanding Payments (debtor ageing).
 *
 * <p><b>Decision it supports:</b> who the front desk telephones this morning.
 * The ageing bands come from the database function {@code FN_AGEING_BUCKET},
 * so this report, an accountant's own SQL query and the dashboard all bucket
 * the same debt identically. Money in the 90+ band is the money least likely to
 * be recovered, so it is called out separately.</p>
 */
@Component
public class OutstandingInvoiceReportGenerator extends AbstractReportGenerator<OutstandingInvoiceRow> {

    public static final String CODE = "OUTSTANDING_INVOICES";

    private static final List<String> BUCKETS = List.of("0-30", "31-60", "61-90", "90+");

    private final ReportingDao reportingDao;

    public OutstandingInvoiceReportGenerator(ReportingDao reportingDao) {
        this.reportingDao = reportingDao;
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getTitle() {
        return "Outstanding Payments (Debtor Ageing)";
    }

    @Override
    public String getDescription() {
        return "Every unpaid and part-paid bill, oldest first, grouped into ageing bands so "
                + "collection effort goes where the money is hardest to recover.";
    }

    @Override
    public List<String> getColumnHeaders() {
        return List.of("Invoice No", "Appointment No", "Patient", "Contact", "Issued",
                       "Days", "Ageing", "Total", "Paid", "Balance Due", "Status");
    }

    /**
     * Ignores the date range: an unpaid bill from four months ago is precisely
     * the row this report exists to surface, and filtering it out by period
     * would hide the worst debt.
     */
    @Override
    protected List<OutstandingInvoiceRow> fetchRows(ReportRequest request) {
        return reportingDao.findOutstandingInvoices();
    }

    @Override
    protected List<String> toCells(OutstandingInvoiceRow row) {
        return List.of(text(row.getInvoiceNumber()),
                       text(row.getAppointmentNumber()),
                       text(row.getPatientName()),
                       text(row.getContactNumber()),
                       date(row.getIssuedDate()),
                       String.valueOf(row.getDaysOutstanding()),
                       text(row.getAgeingBucket()),
                       money(row.getTotalAmount()),
                       money(row.getAmountPaid()),
                       money(row.getBalanceDue()),
                       text(row.getPaymentStatus()));
    }

    @Override
    protected void summarise(ReportDto<OutstandingInvoiceRow> report,
                             List<OutstandingInvoiceRow> rows,
                             ReportRequest request) {

        Map<String, BigDecimal> byBucket = new LinkedHashMap<>();
        Map<String, Integer> countByBucket = new LinkedHashMap<>();
        for (String bucket : BUCKETS) {
            byBucket.put(bucket, BigDecimal.ZERO);
            countByBucket.put(bucket, 0);
        }

        BigDecimal totalDue = BigDecimal.ZERO;
        OutstandingInvoiceRow oldest = null;
        OutstandingInvoiceRow largest = null;

        for (OutstandingInvoiceRow row : rows) {
            BigDecimal balance = MoneyUtils.nullSafe(row.getBalanceDue());
            totalDue = totalDue.add(balance);

            String bucket = row.getAgeingBucket() == null ? "0-30" : row.getAgeingBucket();
            byBucket.merge(bucket, balance, BigDecimal::add);
            countByBucket.merge(bucket, 1, Integer::sum);

            if (oldest == null || row.getDaysOutstanding() > oldest.getDaysOutstanding()) {
                oldest = row;
            }
            if (largest == null || balance.compareTo(MoneyUtils.nullSafe(largest.getBalanceDue())) > 0) {
                largest = row;
            }
        }

        report.addSummary("Unpaid bills", rows.size());
        report.addSummary("Total outstanding", MoneyUtils.formatWithCurrency(totalDue));

        for (String bucket : BUCKETS) {
            report.addSummary(bucket + " days",
                    MoneyUtils.formatWithCurrency(byBucket.get(bucket))
                            + " (" + countByBucket.get(bucket) + " bills)");
        }

        BigDecimal atRisk = byBucket.get("90+");
        report.addSummary("At serious risk (90+ days)", MoneyUtils.formatWithCurrency(atRisk));

        report.addSummary("Oldest debt",
                oldest == null ? "none"
                        : oldest.getPatientName() + " - " + oldest.getDaysOutstanding() + " days ("
                          + oldest.getInvoiceNumber() + ")");
        report.addSummary("Largest single balance",
                largest == null ? "none"
                        : largest.getPatientName() + " - "
                          + MoneyUtils.formatWithCurrency(largest.getBalanceDue()));
    }
}
