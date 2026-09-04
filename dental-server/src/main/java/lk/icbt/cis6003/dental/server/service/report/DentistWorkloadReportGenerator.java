package lk.icbt.cis6003.dental.server.service.report;

import lk.icbt.cis6003.dental.common.dto.report.DentistWorkloadRow;
import lk.icbt.cis6003.dental.common.dto.report.ReportDto;
import lk.icbt.cis6003.dental.server.domain.Dentist;
import lk.icbt.cis6003.dental.server.repository.DentistRepository;
import lk.icbt.cis6003.dental.server.repository.dao.ReportingDao;
import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REPORT 3 - Dentist Workload and Utilisation.
 *
 * <p><b>Decision it supports:</b> whether to recruit, and how to rebalance the
 * diary. A dentist consistently above 85% utilisation is a bottleneck the
 * clinic is turning patients away from; one below 40% is capacity being paid
 * for and not sold.</p>
 *
 * <p>Utilisation is deliberately computed <em>here</em> rather than in the SQL
 * view, because the denominator is each dentist's own shift length multiplied
 * by the number of days in the period. Putting per-dentist working hours into
 * an aggregate view would have made it far harder to read than the arithmetic
 * below.</p>
 */
@Component
public class DentistWorkloadReportGenerator extends AbstractReportGenerator<DentistWorkloadRow> {

    public static final String CODE = "DENTIST_WORKLOAD";

    /** Above this, the dentist is a bottleneck. */
    private static final BigDecimal HIGH_UTILISATION = new BigDecimal("85");

    /** Below this, capacity is being paid for but not sold. */
    private static final BigDecimal LOW_UTILISATION = new BigDecimal("40");

    private final ReportingDao reportingDao;
    private final DentistRepository dentistRepository;

    public DentistWorkloadReportGenerator(ReportingDao reportingDao,
                                          DentistRepository dentistRepository) {
        this.reportingDao = reportingDao;
        this.dentistRepository = dentistRepository;
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getTitle() {
        return "Dentist Workload and Utilisation";
    }

    @Override
    public String getDescription() {
        return "Appointments, outcomes, booked chair time and revenue per dentist, with "
                + "utilisation measured against each dentist's own working hours.";
    }

    @Override
    public List<String> getColumnHeaders() {
        return List.of("Code", "Dentist", "Specialization", "Total", "Completed",
                       "Cancelled", "No Show", "Booked (min)", "Utilisation %", "Revenue");
    }

    @Override
    protected List<DentistWorkloadRow> fetchRows(ReportRequest request) {
        return reportingDao.findDentistWorkload(request.effectiveFrom(), request.effectiveTo());
    }

    /**
     * Derives utilisation once the whole result set is known - the step the
     * Template Method's {@code postProcess} hook exists for.
     */
    @Override
    protected List<DentistWorkloadRow> postProcess(List<DentistWorkloadRow> rows, ReportRequest request) {
        Map<String, Dentist> byCode = new HashMap<>();
        for (Dentist dentist : dentistRepository.findAll()) {
            byCode.put(dentist.getDentistCode(), dentist);
        }

        long days = request.dayCount();

        for (DentistWorkloadRow row : rows) {
            Dentist dentist = byCode.get(row.getDentistCode());
            long capacityMinutes = dentist == null ? 0 : dentist.getDailyCapacityMinutes() * days;

            if (capacityMinutes > 0) {
                BigDecimal utilisation = BigDecimal.valueOf(row.getBookedMinutes())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(capacityMinutes), 1, RoundingMode.HALF_UP);
                row.setUtilisationPercentage(utilisation);
            } else {
                row.setUtilisationPercentage(BigDecimal.ZERO);
            }
        }
        return rows;
    }

    @Override
    protected List<String> toCells(DentistWorkloadRow row) {
        return List.of(text(row.getDentistCode()),
                       "Dr " + text(row.getDentistName()),
                       text(row.getSpecialization()),
                       String.valueOf(row.getTotalAppointments()),
                       String.valueOf(row.getCompletedAppointments()),
                       String.valueOf(row.getCancelledAppointments()),
                       String.valueOf(row.getNoShowAppointments()),
                       String.valueOf(row.getBookedMinutes()),
                       percent(row.getUtilisationPercentage()),
                       money(row.getRevenueGenerated()));
    }

    @Override
    protected void summarise(ReportDto<DentistWorkloadRow> report,
                             List<DentistWorkloadRow> rows,
                             ReportRequest request) {

        long total = 0;
        long completed = 0;
        long cancelled = 0;
        long noShow = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal utilisationSum = BigDecimal.ZERO;

        StringBuilder bottlenecks = new StringBuilder();
        StringBuilder underused = new StringBuilder();

        for (DentistWorkloadRow row : rows) {
            total += row.getTotalAppointments();
            completed += row.getCompletedAppointments();
            cancelled += row.getCancelledAppointments();
            noShow += row.getNoShowAppointments();
            revenue = revenue.add(MoneyUtils.nullSafe(row.getRevenueGenerated()));
            utilisationSum = utilisationSum.add(MoneyUtils.nullSafe(row.getUtilisationPercentage()));

            if (row.getUtilisationPercentage().compareTo(HIGH_UTILISATION) >= 0) {
                append(bottlenecks, row.getDentistName());
            } else if (row.getTotalAppointments() > 0
                    && row.getUtilisationPercentage().compareTo(LOW_UTILISATION) < 0) {
                append(underused, row.getDentistName());
            }
        }

        report.addSummary("Dentists reported", rows.size());
        report.addSummary("Total appointments", total);
        report.addSummary("Completed", completed);
        report.addSummary("Cancelled", cancelled);
        report.addSummary("No shows", noShow);
        report.addSummary("No-show rate", rate(noShow, total) + "%");
        report.addSummary("Revenue generated", MoneyUtils.formatWithCurrency(revenue));

        if (!rows.isEmpty()) {
            report.addSummary("Average utilisation",
                    utilisationSum.divide(BigDecimal.valueOf(rows.size()), 1, RoundingMode.HALF_UP) + "%");
        }

        report.addSummary("At capacity (>= " + HIGH_UTILISATION + "%)",
                bottlenecks.length() == 0 ? "none" : bottlenecks.toString());
        report.addSummary("Under-used (< " + LOW_UTILISATION + "%)",
                underused.length() == 0 ? "none" : underused.toString());
    }

    private void append(StringBuilder sb, String name) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(name);
    }

    private BigDecimal rate(long part, long whole) {
        if (whole <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP);
    }
}
