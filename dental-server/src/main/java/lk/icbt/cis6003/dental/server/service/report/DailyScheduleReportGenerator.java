package lk.icbt.cis6003.dental.server.service.report;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.dto.report.DailyScheduleRow;
import lk.icbt.cis6003.dental.common.dto.report.ReportDto;
import lk.icbt.cis6003.dental.server.repository.dao.ReportingDao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * REPORT 1 - Daily Appointment Schedule.
 *
 * <p><b>Decision it supports:</b> printed at 07:45, it tells the practice
 * manager who is expected today, with which dentist, and - through the
 * {@code Slots still free} figure - how much capacity is left to sell to
 * walk-in and emergency patients.</p>
 */
@Component
public class DailyScheduleReportGenerator extends AbstractReportGenerator<DailyScheduleRow> {

    public static final String CODE = "DAILY_SCHEDULE";

    private final ReportingDao reportingDao;

    public DailyScheduleReportGenerator(ReportingDao reportingDao) {
        this.reportingDao = reportingDao;
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getTitle() {
        return "Daily Appointment Schedule";
    }

    @Override
    public String getDescription() {
        return "Every appointment for the selected day, in time order, with the remaining "
                + "capacity available for walk-in and emergency patients.";
    }

    @Override
    public List<String> getColumnHeaders() {
        return List.of("Time", "Appointment No", "Patient", "Contact", "Dentist", "Treatment", "Status");
    }

    @Override
    protected List<DailyScheduleRow> fetchRows(ReportRequest request) {
        return reportingDao.findDailySchedule(request.effectiveFrom(), request.dentistCode());
    }

    @Override
    protected List<String> toCells(DailyScheduleRow row) {
        return List.of(time(row.getAppointmentTime()),
                       text(row.getAppointmentNumber()),
                       text(row.getPatientName()),
                       text(row.getContactNumber()),
                       "Dr " + text(row.getDentistName()),
                       text(row.getTreatmentName()),
                       text(row.getStatus()));
    }

    @Override
    protected void summarise(ReportDto<DailyScheduleRow> report,
                             List<DailyScheduleRow> rows,
                             ReportRequest request) {

        long scheduled = countStatus(rows, "SCHEDULED");
        long confirmed = countStatus(rows, "CONFIRMED");
        long completed = countStatus(rows, "COMPLETED");
        long cancelled = countStatus(rows, "CANCELLED");
        long noShow = countStatus(rows, "NO_SHOW");
        long occupying = scheduled + confirmed + completed;

        report.addSummary("Date", request.effectiveFrom().format(ClinicConstants.DISPLAY_DATE_FORMAT));
        report.addSummary("Total appointments", rows.size());
        report.addSummary("Scheduled", scheduled);
        report.addSummary("Confirmed", confirmed);
        report.addSummary("Completed", completed);
        report.addSummary("Cancelled", cancelled);
        report.addSummary("No shows", noShow);

        // Capacity is expressed in 30-minute slots between opening and closing.
        long slotsPerDay = java.time.Duration
                .between(ClinicConstants.CLINIC_OPENING_TIME, ClinicConstants.CLINIC_CLOSING_TIME)
                .toMinutes() / ClinicConstants.SLOT_DURATION_MINUTES;

        report.addSummary("Slots still free", Math.max(slotsPerDay - occupying, 0));
        report.addSummary("Chair utilisation",
                percentage(occupying, slotsPerDay) + "%");
    }

    private long countStatus(List<DailyScheduleRow> rows, String status) {
        return rows.stream().filter(r -> status.equalsIgnoreCase(r.getStatus())).count();
    }

    private BigDecimal percentage(long part, long whole) {
        if (whole <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP);
    }
}
