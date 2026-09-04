package lk.icbt.cis6003.dental.server.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.ApiResponse;
import lk.icbt.cis6003.dental.common.dto.HelpTopicDto;
import lk.icbt.cis6003.dental.common.dto.report.DashboardStatsDto;
import lk.icbt.cis6003.dental.common.dto.report.ReportDto;
import lk.icbt.cis6003.dental.server.service.HelpService;
import lk.icbt.cis6003.dental.server.service.ReminderScheduler;
import lk.icbt.cis6003.dental.server.service.ReportService;
import lk.icbt.cis6003.dental.server.service.report.ReportGeneratorFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Web services for the management reports, the dashboard and the Help section
 * (requirement 5).
 *
 * <p>One endpoint serves all five reports. Because the report factory resolves
 * any code and every report returns the same envelope, adding a sixth report
 * makes it available through this endpoint - and therefore in the desktop
 * client - without touching this class.</p>
 */
@RestController
@Tag(name = "5. Reports and help", description = "Management reports, dashboard figures and the Help section")
public class ReportApiController {

    private final ReportService reportService;
    private final HelpService helpService;
    private final ReminderScheduler reminderScheduler;

    public ReportApiController(ReportService reportService,
                               HelpService helpService,
                               ReminderScheduler reminderScheduler) {
        this.reportService = reportService;
        this.helpService = helpService;
        this.reminderScheduler = reminderScheduler;
    }

    @GetMapping(ApiPaths.REPORTS)
    @Operation(summary = "List the available reports",
               description = "Drives the reports menu in both user interfaces.")
    public ResponseEntity<ApiResponse<List<ReportGeneratorFactory.ReportDescriptor>>> listReports() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.listAvailableReports()));
    }

    @GetMapping(ApiPaths.REPORTS + "/{reportCode}")
    @Operation(summary = "Run a report",
               description = """
                       Runs any report by code:

                       * `DAILY_SCHEDULE` - who is expected today and how much capacity is left
                       * `REVENUE` - invoiced against collected income, with the collection rate
                       * `DENTIST_WORKLOAD` - utilisation per dentist, against their own hours
                       * `TREATMENT_POPULARITY` - volume against yield per treatment
                       * `OUTSTANDING_INVOICES` - debtor ageing, oldest money first

                       Dates default to the current month. The maximum range is one year.
                       """)
    public ResponseEntity<ApiResponse<ReportDto<?>>> runReport(
            @PathVariable String reportCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String dentistCode) {

        return ResponseEntity.ok(ApiResponse.ok(
                reportService.run(reportCode, fromDate, toDate, dentistCode)));
    }

    @GetMapping(ApiPaths.REPORT_DASHBOARD)
    @Operation(summary = "Dashboard figures",
               description = "Today's load, month-to-date income, outstanding balance, no-show "
                       + "rate and chair utilisation - the numbers a practice manager can act on "
                       + "the same morning.")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.dashboard()));
    }

    @GetMapping(ApiPaths.HELP)
    @Operation(summary = "Help contents",
               description = "Requirement 5. Step-by-step instructions for new staff, served from "
                       + "the API so the web application and the desktop client always agree.")
    public ResponseEntity<ApiResponse<List<HelpTopicDto>>> help() {
        return ResponseEntity.ok(ApiResponse.ok(helpService.listTopics()));
    }

    @GetMapping(ApiPaths.HELP + "/{topicId}")
    @Operation(summary = "One help topic")
    public ResponseEntity<ApiResponse<HelpTopicDto>> helpTopic(@PathVariable String topicId) {
        return ResponseEntity.ok(ApiResponse.ok(helpService.findTopic(topicId)));
    }

    @PostMapping(ApiPaths.API_ROOT + "/notifications/run-reminders")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Send tomorrow's reminders now (administrators only)",
               description = "The reminder job normally runs at 18:00. This triggers it on demand, "
                       + "so the feature can be demonstrated without waiting, and so it can be "
                       + "re-run after an SMS outage.")
    public ResponseEntity<ApiResponse<Integer>> runReminders() {
        int sent = reminderScheduler.runNow();
        return ResponseEntity.ok(ApiResponse.ok(sent, sent + " reminder(s) published."));
    }
}
