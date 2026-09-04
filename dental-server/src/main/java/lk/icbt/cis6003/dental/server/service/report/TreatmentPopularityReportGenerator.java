package lk.icbt.cis6003.dental.server.service.report;

import lk.icbt.cis6003.dental.common.dto.report.ReportDto;
import lk.icbt.cis6003.dental.common.dto.report.TreatmentPopularityRow;
import lk.icbt.cis6003.dental.server.repository.dao.ReportingDao;
import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * REPORT 4 - Treatment Popularity and Yield.
 *
 * <p><b>Decision it supports:</b> which treatments to promote, and which to
 * retire. Volume alone is misleading - a treatment can be the most frequently
 * performed while contributing a small share of income, in which case it is
 * consuming chair time a higher-yield treatment could use. The report therefore
 * shows count, total revenue, average value <em>and</em> share of income
 * together.</p>
 */
@Component
public class TreatmentPopularityReportGenerator extends AbstractReportGenerator<TreatmentPopularityRow> {

    public static final String CODE = "TREATMENT_POPULARITY";

    private final ReportingDao reportingDao;

    public TreatmentPopularityReportGenerator(ReportingDao reportingDao) {
        this.reportingDao = reportingDao;
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getTitle() {
        return "Treatment Popularity and Yield";
    }

    @Override
    public String getDescription() {
        return "How often each treatment was performed, what it earned, and its share of total "
                + "income - so that volume and value can be compared rather than confused.";
    }

    @Override
    public List<String> getColumnHeaders() {
        return List.of("Code", "Treatment", "Category", "Times Performed",
                       "Total Revenue", "Average Value", "Share of Income %");
    }

    @Override
    protected List<TreatmentPopularityRow> fetchRows(ReportRequest request) {
        return reportingDao.findTreatmentPopularity(request.effectiveFrom(), request.effectiveTo());
    }

    /**
     * Average value and revenue share both need the grand total, so they are
     * derived here rather than per row.
     */
    @Override
    protected List<TreatmentPopularityRow> postProcess(List<TreatmentPopularityRow> rows,
                                                       ReportRequest request) {
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (TreatmentPopularityRow row : rows) {
            grandTotal = grandTotal.add(MoneyUtils.nullSafe(row.getTotalRevenue()));
        }

        for (TreatmentPopularityRow row : rows) {
            if (row.getTimesPerformed() > 0) {
                row.setAverageRevenue(MoneyUtils.nullSafe(row.getTotalRevenue())
                        .divide(BigDecimal.valueOf(row.getTimesPerformed()), 2, RoundingMode.HALF_UP));
            } else {
                row.setAverageRevenue(BigDecimal.ZERO);
            }

            if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                row.setRevenueSharePercentage(MoneyUtils.nullSafe(row.getTotalRevenue())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(grandTotal, 1, RoundingMode.HALF_UP));
            } else {
                row.setRevenueSharePercentage(BigDecimal.ZERO);
            }
        }
        return rows;
    }

    @Override
    protected List<String> toCells(TreatmentPopularityRow row) {
        return List.of(text(row.getTreatmentCode()),
                       text(row.getTreatmentName()),
                       text(row.getCategory()),
                       String.valueOf(row.getTimesPerformed()),
                       money(row.getTotalRevenue()),
                       money(row.getAverageRevenue()),
                       percent(row.getRevenueSharePercentage()));
    }

    @Override
    protected void summarise(ReportDto<TreatmentPopularityRow> report,
                             List<TreatmentPopularityRow> rows,
                             ReportRequest request) {

        long performed = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        TreatmentPopularityRow mostPerformed = null;
        TreatmentPopularityRow highestEarning = null;
        int neverPerformed = 0;

        for (TreatmentPopularityRow row : rows) {
            performed += row.getTimesPerformed();
            revenue = revenue.add(MoneyUtils.nullSafe(row.getTotalRevenue()));

            if (row.getTimesPerformed() == 0) {
                neverPerformed++;
                continue;
            }
            if (mostPerformed == null || row.getTimesPerformed() > mostPerformed.getTimesPerformed()) {
                mostPerformed = row;
            }
            if (highestEarning == null
                    || row.getTotalRevenue().compareTo(highestEarning.getTotalRevenue()) > 0) {
                highestEarning = row;
            }
        }

        report.addSummary("Treatments in catalogue", rows.size());
        report.addSummary("Treatments performed", performed);
        report.addSummary("Total revenue", MoneyUtils.formatWithCurrency(revenue));
        report.addSummary("Most frequently performed",
                mostPerformed == null ? "none"
                        : mostPerformed.getTreatmentName() + " (" + mostPerformed.getTimesPerformed() + ")");
        report.addSummary("Highest earning",
                highestEarning == null ? "none"
                        : highestEarning.getTreatmentName() + " ("
                          + MoneyUtils.formatWithCurrency(highestEarning.getTotalRevenue()) + ")");
        report.addSummary("Not performed at all in this period", neverPerformed);
    }
}
