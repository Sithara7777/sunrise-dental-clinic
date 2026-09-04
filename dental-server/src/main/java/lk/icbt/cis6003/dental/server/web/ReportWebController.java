package lk.icbt.cis6003.dental.server.web;

import lk.icbt.cis6003.dental.common.dto.report.ReportDto;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.service.DentistService;
import lk.icbt.cis6003.dental.server.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Browser screens for the five management reports.
 *
 * <p>There is one view template for every report. It works because all five
 * return the same {@code ReportDto} envelope - column headings, rows and named
 * totals - so the template renders headings and cells generically. A sixth
 * report appears in this UI with no change to this controller or that
 * template.</p>
 */
@Controller
public class ReportWebController {

    private final ReportService reportService;
    private final DentistService dentistService;

    public ReportWebController(ReportService reportService, DentistService dentistService) {
        this.reportService = reportService;
        this.dentistService = dentistService;
    }

    @GetMapping("/reports")
    public String index(Model model) {
        model.addAttribute("reports", reportService.listAvailableReports());
        model.addAttribute("pageTitle", "Reports");
        return "reports/index";
    }

    @GetMapping("/reports/{reportCode}")
    public String run(@PathVariable String reportCode,
                      @RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                      @RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                      @RequestParam(required = false) String dentistCode,
                      Model model) {

        model.addAttribute("reports", reportService.listAvailableReports());
        model.addAttribute("dentists", dentistService.listActive());
        model.addAttribute("reportCode", reportCode);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("dentistCode", dentistCode);

        try {
            ReportDto<?> report = reportService.run(reportCode, fromDate, toDate, dentistCode);
            model.addAttribute("report", report);
            model.addAttribute("pageTitle", report.getTitle());
        } catch (BusinessException ex) {
            // A date range the user typed wrongly is not a server error.
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Reports");
        }

        return "reports/view";
    }
}
