package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.common.dto.report.DashboardStatsDto;
import lk.icbt.cis6003.dental.common.dto.report.ReportDto;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.server.security.SecurityUtils;
import lk.icbt.cis6003.dental.server.service.report.AbstractReportGenerator;
import lk.icbt.cis6003.dental.server.service.report.ReportGeneratorFactory;
import lk.icbt.cis6003.dental.server.service.report.ReportRequest;
import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The single entry point to the reporting subsystem.
 *
 * <p>Because the factory resolves any report code and every report returns the
 * same {@code ReportDto} envelope, the REST layer, the web UI and the desktop
 * client each need exactly one report-handling path. A sixth report becomes
 * available in all three interfaces the moment its {@code @Component} exists -
 * no controller, template or window is touched.</p>
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("EEE dd");

    private final ReportGeneratorFactory reportGeneratorFactory;
    private final AppointmentService appointmentService;
    private final BillingService billingService;
    private final PatientService patientService;
    private final DentistService dentistService;
    private final TreatmentService treatmentService;

    public ReportService(ReportGeneratorFactory reportGeneratorFactory,
                         AppointmentService appointmentService,
                         BillingService billingService,
                         PatientService patientService,
                         DentistService dentistService,
                         TreatmentService treatmentService) {
        this.reportGeneratorFactory = reportGeneratorFactory;
        this.appointmentService = appointmentService;
        this.billingService = billingService;
        this.patientService = patientService;
        this.dentistService = dentistService;
        this.treatmentService = treatmentService;
    }

    /**
     * Runs any report by code.
     *
     * @param reportCode  e.g. {@code REVENUE}
     * @param from        start of the period, may be {@code null}
     * @param to          end of the period, may be {@code null}
     * @param dentistCode optional dentist filter
     */
    public ReportDto<?> run(String reportCode, LocalDate from, LocalDate to, String dentistCode) {
        AbstractReportGenerator<?> generator = reportGeneratorFactory.resolve(reportCode);
        ReportRequest request = new ReportRequest(reportCode, from, to, dentistCode,
                                                  SecurityUtils.getCurrentUsernameOrSystem());
        return generator.generate(request);
    }

    /** Every report, for the reports menu. */
    public List<ReportGeneratorFactory.ReportDescriptor> listAvailableReports() {
        return reportGeneratorFactory.listAvailable();
    }

    /**
     * The dashboard figures.
     *
     * <p>Assembled from counters rather than by running the five reports: a
     * landing page must load quickly, and none of these numbers needs a full
     * report's detail.</p>
     */
    public DashboardStatsDto dashboard() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        DashboardStatsDto stats = new DashboardStatsDto();

        stats.setTodayAppointments(appointmentService.countOn(today));
        stats.setTodayCompleted(appointmentService.countOnWithStatus(today, AppointmentStatus.COMPLETED));
        stats.setTodayCancelled(appointmentService.countOnWithStatus(today, AppointmentStatus.CANCELLED));
        stats.setTodayPending(
                appointmentService.countOnWithStatus(today, AppointmentStatus.SCHEDULED)
                        + appointmentService.countOnWithStatus(today, AppointmentStatus.CONFIRMED));
        stats.setUpcomingSevenDays(
                appointmentService.countBetween(today.plusDays(1), today.plusDays(7)));

        stats.setTotalPatients(patientService.countAll());
        stats.setNewPatientsThisMonth(
                patientService.countRegisteredSince(LocalDateTime.of(monthStart, java.time.LocalTime.MIN)));
        stats.setActiveDentists(dentistService.countActive());
        stats.setActiveTreatments(treatmentService.countActive());

        stats.setTodayRevenue(billingService.revenueOn(today));
        stats.setMonthRevenue(billingService.revenueBetween(monthStart, today));
        stats.setOutstandingBalance(billingService.totalOutstanding());
        stats.setOutstandingInvoiceCount(billingService.countOutstanding());

        stats.setNoShowRate(noShowRate());
        stats.setChairUtilisationToday(chairUtilisationToday());
        stats.setWeeklyTrend(weeklyTrend(today));

        return stats;
    }

    /**
     * Missed appointments as a percentage of all appointments ever booked.
     *
     * <p>A leading indicator worth a place on the dashboard: every no-show is a
     * slot the clinic could have sold, and a rising rate is the signal to start
     * sending reminders earlier.</p>
     */
    private BigDecimal noShowRate() {
        long noShows = appointmentService.countWithStatus(AppointmentStatus.NO_SHOW);
        long completed = appointmentService.countWithStatus(AppointmentStatus.COMPLETED);
        long cancelled = appointmentService.countWithStatus(AppointmentStatus.CANCELLED);
        long total = noShows + completed + cancelled;

        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(noShows)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    /** Booked chair minutes today against the clinic's total available minutes. */
    private BigDecimal chairUtilisationToday() {
        long bookedMinutes = appointmentService.bookedMinutesOn(LocalDate.now());
        long dentists = dentistService.countActive();

        long clinicMinutes = java.time.Duration.between(
                lk.icbt.cis6003.dental.common.ClinicConstants.CLINIC_OPENING_TIME,
                lk.icbt.cis6003.dental.common.ClinicConstants.CLINIC_CLOSING_TIME).toMinutes();
        long capacity = clinicMinutes * Math.max(dentists, 1);

        if (capacity == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(bookedMinutes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(capacity), 1, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
    }

    /** The last seven days of appointment volume, for the dashboard chart. */
    private List<DashboardStatsDto.TrendPoint> weeklyTrend(LocalDate today) {
        List<DashboardStatsDto.TrendPoint> points = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            points.add(new DashboardStatsDto.TrendPoint(
                    day.format(DAY_LABEL), appointmentService.countOn(day)));
        }
        return points;
    }

    /** Convenience used by the dashboard tiles. */
    public String formattedOutstanding() {
        return MoneyUtils.formatWithCurrency(billingService.totalOutstanding());
    }
}
