package lk.icbt.cis6003.dental.server.web;

import lk.icbt.cis6003.dental.server.repository.AuditLogRepository;
import lk.icbt.cis6003.dental.server.repository.NotificationLogRepository;
import lk.icbt.cis6003.dental.server.repository.UserRepository;
import lk.icbt.cis6003.dental.server.service.ReminderScheduler;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEventPublisher;
import lk.icbt.cis6003.dental.server.service.pricing.PricingStrategyFactory;
import lk.icbt.cis6003.dental.server.service.report.ReportGeneratorFactory;
import lk.icbt.cis6003.dental.server.service.validation.BookingValidationChain;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Administrator screens: the audit trail, the notification delivery log, staff
 * accounts and a diagnostics page.
 *
 * <p>The diagnostics page is more than decoration. Every one of the four
 * patterns whose registration is automatic - pricing strategies, booking rules,
 * event observers and report generators - reports what it actually loaded. That
 * is the mitigation for the main criticism of registry-based patterns: no
 * single source file lists the members, so the running application is asked
 * instead.</p>
 *
 * <p>Restricted to {@code ROLE_ADMIN} by {@code SecurityConfig}, because the
 * audit trail shows who did what and is precisely what a member of staff
 * covering their tracks would want to reach.</p>
 */
@Controller
public class AdminWebController {

    private final AuditLogRepository auditLogRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final UserRepository userRepository;
    private final ReminderScheduler reminderScheduler;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final BookingValidationChain validationChain;
    private final AppointmentEventPublisher eventPublisher;
    private final ReportGeneratorFactory reportGeneratorFactory;

    public AdminWebController(AuditLogRepository auditLogRepository,
                              NotificationLogRepository notificationLogRepository,
                              UserRepository userRepository,
                              ReminderScheduler reminderScheduler,
                              PricingStrategyFactory pricingStrategyFactory,
                              BookingValidationChain validationChain,
                              AppointmentEventPublisher eventPublisher,
                              ReportGeneratorFactory reportGeneratorFactory) {
        this.auditLogRepository = auditLogRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.userRepository = userRepository;
        this.reminderScheduler = reminderScheduler;
        this.pricingStrategyFactory = pricingStrategyFactory;
        this.validationChain = validationChain;
        this.eventPublisher = eventPublisher;
        this.reportGeneratorFactory = reportGeneratorFactory;
    }

    @GetMapping("/admin/audit")
    public String audit(@RequestParam(required = false) String username,
                        @RequestParam(required = false) String action,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        model.addAttribute("entries",
                auditLogRepository.search(username, action, PageRequest.of(Math.max(page, 0), 30)));
        model.addAttribute("username", username);
        model.addAttribute("action", action);
        model.addAttribute("pageTitle", "Audit Trail");
        return "admin/audit";
    }

    @GetMapping("/admin/notifications")
    public String notifications(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("entries", notificationLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), 30)));
        model.addAttribute("pageTitle", "Notification History");
        return "admin/notifications";
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAllByOrderByFullNameAsc());
        model.addAttribute("pageTitle", "Staff Accounts");
        return "admin/users";
    }

    @GetMapping("/admin/system")
    public String system(Model model) {
        model.addAttribute("pricingStrategies", pricingStrategyFactory.getAll().values());
        model.addAttribute("bookingRules", validationChain.describeRules());
        model.addAttribute("observers", eventPublisher.describeObservers());
        model.addAttribute("reports", reportGeneratorFactory.listAvailable());
        model.addAttribute("pageTitle", "System Diagnostics");
        return "admin/system";
    }

    /**
     * Runs the reminder job on demand.
     *
     * <p>The scheduled run is at 18:00; this lets the feature be demonstrated
     * without waiting, and lets an administrator re-run it after fixing an SMS
     * outage.</p>
     */
    @PostMapping("/admin/notifications/run-reminders")
    public String runReminders(RedirectAttributes redirect) {
        int sent = reminderScheduler.runNow();
        redirect.addFlashAttribute("successMessage",
                sent + " reminder(s) published. See the notification history below.");
        return "redirect:/admin/notifications";
    }
}
