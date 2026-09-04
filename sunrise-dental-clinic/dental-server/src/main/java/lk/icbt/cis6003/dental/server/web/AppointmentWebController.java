package lk.icbt.cis6003.dental.server.web;

import jakarta.validation.Valid;
import lk.icbt.cis6003.dental.common.dto.AppointmentDto;
import lk.icbt.cis6003.dental.common.dto.AppointmentRequest;
import lk.icbt.cis6003.dental.common.dto.StatusUpdateRequest;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.common.enums.Gender;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.service.AppointmentService;
import lk.icbt.cis6003.dental.server.service.BillingService;
import lk.icbt.cis6003.dental.server.service.DentistService;
import lk.icbt.cis6003.dental.server.service.TreatmentService;
import lk.icbt.cis6003.dental.server.web.session.RecentlyViewedTracker;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Browser screens for requirements 2 and 3.
 *
 * <p>Calls exactly the same {@code AppointmentService} methods as the REST
 * controller. Every booking rule, the appointment-number allocation and the
 * double-booking guard therefore behave identically whether a receptionist uses
 * the browser or the desktop client - which is the practical payoff of keeping
 * the business tier free of any web concern.</p>
 */
@Controller
public class AppointmentWebController {

    private final AppointmentService appointmentService;
    private final DentistService dentistService;
    private final TreatmentService treatmentService;
    private final BillingService billingService;
    private final RecentlyViewedTracker recentlyViewedTracker;

    public AppointmentWebController(AppointmentService appointmentService,
                                    DentistService dentistService,
                                    TreatmentService treatmentService,
                                    BillingService billingService,
                                    RecentlyViewedTracker recentlyViewedTracker) {
        this.appointmentService = appointmentService;
        this.dentistService = dentistService;
        this.treatmentService = treatmentService;
        this.billingService = billingService;
        this.recentlyViewedTracker = recentlyViewedTracker;
    }

    /* ------------------------------------------------------------------ */
    /* List and search                                                     */
    /* ------------------------------------------------------------------ */

    @GetMapping("/appointments")
    public String list(@RequestParam(required = false) String term,
                       @RequestParam(required = false) AppointmentStatus status,
                       @RequestParam(required = false) String dentistCode,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        model.addAttribute("results",
                appointmentService.search(term, status, dentistCode, fromDate, toDate, page, 15));
        model.addAttribute("term", term);
        model.addAttribute("status", status);
        model.addAttribute("dentistCode", dentistCode);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("dentists", dentistService.listActive());
        model.addAttribute("pageTitle", "Appointments");
        return "appointments/list";
    }

    /* ------------------------------------------------------------------ */
    /* Register - requirement 2                                            */
    /* ------------------------------------------------------------------ */

    @GetMapping("/appointments/new")
    public String newForm(@RequestParam(required = false) String patientCode, Model model) {
        AppointmentRequest form = new AppointmentRequest();
        form.setAppointmentDate(LocalDate.now().plusDays(1));
        form.setPatientCode(patientCode);
        model.addAttribute("appointmentRequest", form);
        addFormReferenceData(model);
        model.addAttribute("pageTitle", "Register New Appointment");
        return "appointments/form";
    }

    @PostMapping("/appointments")
    public String register(@Valid @ModelAttribute("appointmentRequest") AppointmentRequest form,
                           BindingResult binding,
                           Model model,
                           RedirectAttributes redirect) {

        if (binding.hasErrors()) {
            addFormReferenceData(model);
            model.addAttribute("pageTitle", "Register New Appointment");
            return "appointments/form";
        }

        try {
            AppointmentDto created = appointmentService.register(form);
            redirect.addFlashAttribute("successMessage",
                    "Appointment " + created.getAppointmentNumber() + " registered for "
                            + created.getPatientName() + ". Give the patient this number.");
            return "redirect:/appointments/" + created.getAppointmentNumber();

        } catch (BusinessException ex) {
            // A rejected booking rule is not a server error - re-show the form
            // with the reason and everything the user already typed.
            model.addAttribute("errorMessage", ex.getMessage());
            addFormReferenceData(model);
            model.addAttribute("pageTitle", "Register New Appointment");
            return "appointments/form";
        }
    }

    /* ------------------------------------------------------------------ */
    /* Display details - requirement 3                                     */
    /* ------------------------------------------------------------------ */

    @GetMapping("/appointments/{appointmentNumber}")
    public String view(@PathVariable String appointmentNumber, Model model) {
        AppointmentDto appointment = appointmentService.findByNumber(appointmentNumber);

        // Session state: build this user's own trail of recent records.
        recentlyViewedTracker.record(appointment);

        model.addAttribute("appointment", appointment);
        model.addAttribute("allowedTransitions", appointment.getStatus().allowedTransitions());
        model.addAttribute("history",
                appointmentService.historyForPatient(appointment.getPatientCode()));

        if (appointment.isInvoiced()) {
            model.addAttribute("invoice",
                    billingService.findByAppointmentNumber(appointment.getAppointmentNumber()));
        }

        model.addAttribute("pageTitle", "Appointment " + appointment.getAppointmentNumber());
        return "appointments/view";
    }

    /* ------------------------------------------------------------------ */
    /* Availability                                                        */
    /* ------------------------------------------------------------------ */

    /**
     * The slot picker. Offering only free slots is what stops a receptionist
     * ever requesting a taken one.
     */
    @GetMapping("/appointments/availability")
    public String availability(@RequestParam String dentistCode,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false) String treatmentCode,
                               @RequestParam(required = false) String patientCode,
                               Model model) {

        model.addAttribute("slots", appointmentService.availableSlots(dentistCode, date));
        model.addAttribute("selectedDentist", dentistService.findByCode(dentistCode));
        model.addAttribute("selectedDate", date);

        AppointmentRequest form = new AppointmentRequest();
        form.setDentistCode(dentistCode);
        form.setTreatmentCode(treatmentCode);
        form.setPatientCode(patientCode);
        form.setAppointmentDate(date);
        model.addAttribute("appointmentRequest", form);

        addFormReferenceData(model);
        model.addAttribute("pageTitle", "Available Slots");
        return "appointments/availability";
    }

    /* ------------------------------------------------------------------ */
    /* Lifecycle                                                           */
    /* ------------------------------------------------------------------ */

    @PostMapping("/appointments/{appointmentNumber}/status")
    public String changeStatus(@PathVariable String appointmentNumber,
                               @RequestParam AppointmentStatus status,
                               @RequestParam(required = false) String reason,
                               RedirectAttributes redirect) {
        try {
            StatusUpdateRequest request = new StatusUpdateRequest(status);
            request.setReason(reason);
            AppointmentDto updated = appointmentService.updateStatus(appointmentNumber, request);
            redirect.addFlashAttribute("successMessage",
                    "Appointment " + appointmentNumber + " is now "
                            + updated.getStatus().getDisplayName() + ".");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/appointments/" + appointmentNumber;
    }

    @PostMapping("/appointments/{appointmentNumber}/reschedule")
    public String reschedule(@PathVariable String appointmentNumber,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newTime,
                             RedirectAttributes redirect) {
        try {
            appointmentService.reschedule(appointmentNumber, newDate, newTime);
            redirect.addFlashAttribute("successMessage",
                    "Appointment moved to " + newDate + " at " + newTime + ".");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/appointments/" + appointmentNumber;
    }

    /* ------------------------------------------------------------------ */
    /* Today's schedule                                                    */
    /* ------------------------------------------------------------------ */

    @GetMapping("/appointments/day")
    public String day(@RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                      Model model) {
        LocalDate target = date == null ? LocalDate.now() : date;
        model.addAttribute("date", target);
        model.addAttribute("appointments", appointmentService.listForDate(target));
        model.addAttribute("pageTitle", "Schedule for " + target);
        return "appointments/day";
    }

    private void addFormReferenceData(Model model) {
        model.addAttribute("dentists", dentistService.listActive());
        model.addAttribute("treatments", treatmentService.listActive());
        model.addAttribute("genders", Gender.values());
    }
}
