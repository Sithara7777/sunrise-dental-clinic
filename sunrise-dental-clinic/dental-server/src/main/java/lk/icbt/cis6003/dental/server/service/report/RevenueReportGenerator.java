package lk.icbt.cis6003.dental.server.service.report;

import lk.icbt.cis6003.dental.common.dto.report.ReportDto;
import lk.icbt.cis6003.dental.common.dto.report.RevenueRow;
import lk.icbt.cis6003.dental.server.repository.dao.ReportingDao;
import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * REPORT 2 - Revenue Analysis.
 *
 * <p><b>Decision it supports:</b> the gap between {@code Total invoiced} and
 * {@code Total collected} is the clinic's working-capital problem in one
 * number. A collection rate that drifts below about 90% says the front desk is
 * letting patients leave without settling, which is a process fix rather than a
 * pricing one.</p>
 */
@Component
public class RevenueReportGenerator extends AbstractReportGenerator<RevenueRow> {

    public static final String CODE = "REVENUE";

    private final ReportingDao reportingDao;

    public RevenueReportGenerator(ReportingDao reportingDao) {
        this.reportingDao = reportingDao;
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getTitle() {
        return "Revenue Analysis";
    }

    @Override
    public String getDescription() {
        return "Invoiced versus collected income per day, with discounts and VAT separated, "
                + "so that pricing decisions and collection performance can be judged apart.";
    }

    @Override
    public List<String> getColumnHeaders() {
        return List.of("Date", "Invoices", "Gross", "Discount", "VAT", "Net Invoiced",
                       "Collected", "Outstanding");
    }

    @Override
    protected List<RevenueRow> fetchRows(ReportRequest request) {
        return reportingDao.findRevenueBetween(request.effectiveFrom(), request.effectiveTo());
    }

    @Override
    protected List<String> toCells(RevenueRow row) {
        return List.of(date(row.getDate()),
                       String.valueOf(row.getInvoiceCount()),
                       money(row.getGrossAmount()),
                       money(row.getDiscountAmount()),
                       money(row.getTaxAmount()),
                       money(row.getNetAmount()),
                       money(row.getCollectedAmount()),
                       money(row.getOutstandingAmount()));
    }

    @Override
    protected void summarise(ReportDto<RevenueRow> report, List<RevenueRow> rows, ReportRequest request) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        long invoices = 0;

        for (RevenueRow row : rows) {
            gross = gross.add(MoneyUtils.nullSafe(row.getGrossAmount()));
            discount = discount.add(MoneyUtils.nullSafe(row.getDiscountAmount()));
            tax = tax.add(MoneyUtils.nullSafe(row.getTaxAmount()));
            net = net.add(MoneyUtils.nullSafe(row.getNetAmount()));
            collected = collected.add(MoneyUtils.nullSafe(row.getCollectedAmount()));
            outstanding = outstanding.add(MoneyUtils.nullSafe(row.getOutstandingAmount()));
            invoices += row.getInvoiceCount();
        }

        report.addSummary("Trading days with income", rows.size());
        report.addSummary("Invoices issued", invoices);
        report.addSummary("Gross charges", MoneyUtils.formatWithCurrency(gross));
        report.addSummary("Discounts given", MoneyUtils.formatWithCurrency(discount));
        report.addSummary("VAT collected", MoneyUtils.formatWithCurrency(tax));
        report.addSummary("Total invoiced", MoneyUtils.formatWithCurrency(net));
        report.addSummary("Total collected", MoneyUtils.formatWithCurrency(collected));
        report.addSummary("Still outstanding", MoneyUtils.formatWithCurrency(outstanding));

        // The headline operational figure.
        report.addSummary("Collection rate", ratio(collected, net) + "%");

        // Average value per bill - the number to watch when treatment mix changes.
        if (invoices > 0) {
            report.addSummary("Average bill",
                    MoneyUtils.formatWithCurrency(
                            net.divide(BigDecimal.valueOf(invoices), 2, RoundingMode.HALF_UP)));
        }
        report.addSummary("Average per day",
                MoneyUtils.formatWithCurrency(
                        net.divide(BigDecimal.valueOf(request.dayCount()), 2, RoundingMode.HALF_UP)));
    }

    private BigDecimal ratio(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100))
                   .divide(whole, 1, RoundingMode.HALF_UP);
    }
}
