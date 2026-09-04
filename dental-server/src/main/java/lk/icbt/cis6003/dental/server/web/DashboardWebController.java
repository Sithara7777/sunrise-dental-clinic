package lk.icbt.cis6003.dental.server.web;

import lk.icbt.cis6003.dental.common.dto.report.DashboardStatsDto;
import lk.icbt.cis6003.dental.server.service.AppointmentService;
import lk.icbt.cis6003.dental.server.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

/**
 * The landing page: today's figures and today's diary in one screen.
 *
 * <p>What is on it was chosen by asking what a practice manager can act on
 * before lunch - today's load, the money still owed, the no-show rate and how
 * much chair time is unsold. Counts that cannot change a decision were left
 * off.</p>
 */
@Controller
public class DashboardWebController {

    private final ReportService reportService;
    private final AppointmentService appointmentService;

    public DashboardWebController(ReportService reportService,
                                  AppointmentService appointmentService) {
        this.reportService = reportService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStatsDto stats = reportService.dashboard();
        model.addAttribute("stats", stats);
        model.addAttribute("todaySchedule", appointmentService.listForDate(LocalDate.now()));

        // Scales the sparkline bars against the busiest day in the window, so a
        // quiet week is still readable rather than five invisible stubs.
        long peak = stats.getWeeklyTrend().stream()
                .mapToLong(DashboardStatsDto.TrendPoint::getValue)
                .max().orElse(1L);
        model.addAttribute("trendPeak", Math.max(peak, 1L));

        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard";
    }
}
