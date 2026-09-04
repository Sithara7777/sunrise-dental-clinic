package lk.icbt.cis6003.dental.server.web;

import jakarta.validation.Valid;
import lk.icbt.cis6003.dental.common.dto.PatientDto;
import lk.icbt.cis6003.dental.common.enums.Gender;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.service.AppointmentService;
import lk.icbt.cis6003.dental.server.service.PatientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Browser screens for the patient master file. */
@Controller
public class PatientWebController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public PatientWebController(PatientService patientService,
                                AppointmentService appointmentService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/patients")
    public String list(@RequestParam(required = false) String term,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("results", patientService.search(term, page, 15));
        model.addAttribute("term", term);
        model.addAttribute("pageTitle", "Patients");
        return "patients/list";
    }

    @GetMapping("/patients/new")
    public String newForm(Model model) {
        model.addAttribute("patient", new PatientDto());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("editing", false);
        model.addAttribute("pageTitle", "Register Patient");
        return "patients/form";
    }

    @PostMapping("/patients")
    public String create(@Valid @ModelAttribute("patient") PatientDto form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("genders", Gender.values());
            model.addAttribute("editing", false);
            model.addAttribute("pageTitle", "Register Patient");
            return "patients/form";
        }
        try {
            PatientDto created = patientService.register(form);
            redirect.addFlashAttribute("successMessage",
                    "Patient registered as " + created.getPatientCode() + ".");
            return "redirect:/patients/" + created.getPatientCode();
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("genders", Gender.values());
            model.addAttribute("editing", false);
            model.addAttribute("pageTitle", "Register Patient");
            return "patients/form";
        }
    }

    @GetMapping("/patients/{patientCode}")
    public String view(@PathVariable String patientCode, Model model) {
        model.addAttribute("patient", patientService.findByCode(patientCode));
        // The complete visit history in one query - the record the paper
        // system could never assemble.
        model.addAttribute("history", appointmentService.historyForPatient(patientCode));
        model.addAttribute("pageTitle", "Patient " + patientCode);
        return "patients/view";
    }

    @GetMapping("/patients/{patientCode}/edit")
    public String editForm(@PathVariable String patientCode, Model model) {
        model.addAttribute("patient", patientService.findByCode(patientCode));
        model.addAttribute("genders", Gender.values());
        model.addAttribute("editing", true);
        model.addAttribute("pageTitle", "Edit Patient " + patientCode);
        return "patients/form";
    }

    @PostMapping("/patients/{patientCode}")
    public String update(@PathVariable String patientCode,
                         @Valid @ModelAttribute("patient") PatientDto form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("genders", Gender.values());
            model.addAttribute("editing", true);
            model.addAttribute("pageTitle", "Edit Patient " + patientCode);
            return "patients/form";
        }
        try {
            patientService.update(patientCode, form);
            redirect.addFlashAttribute("successMessage", "Patient record updated.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/patients/" + patientCode;
    }
}
