package lk.icbt.cis6003.dental.server.web;

import jakarta.validation.Valid;
import lk.icbt.cis6003.dental.common.dto.DentistDto;
import lk.icbt.cis6003.dental.common.dto.TreatmentDto;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.service.DentistService;
import lk.icbt.cis6003.dental.server.service.TreatmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Browser screens for the two reference lists - the dentist roster and the
 * treatment catalogue.
 *
 * <p>Viewing is open to all staff, because a receptionist needs to quote a
 * price. Editing is restricted to administrators by {@code SecurityConfig},
 * since changing a treatment price changes every bill issued from that moment
 * on.</p>
 */
@Controller
public class ReferenceWebController {

    private final DentistService dentistService;
    private final TreatmentService treatmentService;

    public ReferenceWebController(DentistService dentistService,
                                  TreatmentService treatmentService) {
        this.dentistService = dentistService;
        this.treatmentService = treatmentService;
    }

    /* ---------------------------- dentists ----------------------------- */

    @GetMapping("/dentists")
    public String dentists(Model model) {
        model.addAttribute("dentists", dentistService.listAll());
        model.addAttribute("pageTitle", "Dentists");
        return "reference/dentists";
    }

    @GetMapping("/dentists/new")
    public String newDentist(Model model) {
        model.addAttribute("dentist", new DentistDto());
        model.addAttribute("editing", false);
        model.addAttribute("pageTitle", "Add Dentist");
        return "reference/dentist-form";
    }

    @GetMapping("/dentists/{dentistCode}/edit")
    public String editDentist(@PathVariable String dentistCode, Model model) {
        model.addAttribute("dentist", dentistService.findByCode(dentistCode));
        model.addAttribute("editing", true);
        model.addAttribute("pageTitle", "Edit Dentist " + dentistCode);
        return "reference/dentist-form";
    }

    @PostMapping("/dentists")
    public String saveDentist(@Valid @ModelAttribute("dentist") DentistDto form,
                              BindingResult binding,
                              Model model,
                              RedirectAttributes redirect) {
        boolean editing = form.getDentistCode() != null && !form.getDentistCode().isBlank();

        if (binding.hasErrors()) {
            model.addAttribute("editing", editing);
            model.addAttribute("pageTitle", editing ? "Edit Dentist" : "Add Dentist");
            return "reference/dentist-form";
        }
        try {
            if (editing) {
                dentistService.update(form.getDentistCode(), form);
                redirect.addFlashAttribute("successMessage", "Dentist record updated.");
            } else {
                DentistDto created = dentistService.create(form);
                redirect.addFlashAttribute("successMessage",
                        "Dentist added as " + created.getDentistCode() + ".");
            }
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dentists";
    }

    @PostMapping("/dentists/{dentistCode}/toggle")
    public String toggleDentist(@PathVariable String dentistCode,
                                RedirectAttributes redirect) {
        DentistDto dentist = dentistService.findByCode(dentistCode);
        if (dentist.isActive()) {
            dentistService.deactivate(dentistCode);
            redirect.addFlashAttribute("successMessage",
                    "Dr " + dentist.getFullName() + " marked as no longer practising. "
                            + "Their historic appointments are unaffected.");
        } else {
            dentistService.reactivate(dentistCode);
            redirect.addFlashAttribute("successMessage",
                    "Dr " + dentist.getFullName() + " is practising again.");
        }
        return "redirect:/dentists";
    }

    /* --------------------------- treatments ---------------------------- */

    @GetMapping("/treatments")
    public String treatments(Model model) {
        model.addAttribute("treatments", treatmentService.listAll());
        model.addAttribute("pricingStrategies", treatmentService.availablePricingStrategies());
        model.addAttribute("pageTitle", "Treatment Catalogue");
        return "reference/treatments";
    }

    @GetMapping("/treatments/new")
    public String newTreatment(Model model) {
        model.addAttribute("treatment", new TreatmentDto());
        model.addAttribute("pricingStrategies", treatmentService.availablePricingStrategies());
        model.addAttribute("editing", false);
        model.addAttribute("pageTitle", "Add Treatment");
        return "reference/treatment-form";
    }

    @GetMapping("/treatments/{code}/edit")
    public String editTreatment(@PathVariable String code, Model model) {
        model.addAttribute("treatment", treatmentService.findByCode(code));
        model.addAttribute("pricingStrategies", treatmentService.availablePricingStrategies());
        model.addAttribute("editing", true);
        model.addAttribute("pageTitle", "Edit Treatment " + code);
        return "reference/treatment-form";
    }

    @PostMapping("/treatments")
    public String saveTreatment(@Valid @ModelAttribute("treatment") TreatmentDto form,
                                BindingResult binding,
                                @ModelAttribute("editingFlag") String editingFlag,
                                Model model,
                                RedirectAttributes redirect) {
        boolean editing = "true".equals(editingFlag);

        if (binding.hasErrors()) {
            model.addAttribute("pricingStrategies", treatmentService.availablePricingStrategies());
            model.addAttribute("editing", editing);
            model.addAttribute("pageTitle", editing ? "Edit Treatment" : "Add Treatment");
            return "reference/treatment-form";
        }
        try {
            if (editing) {
                treatmentService.update(form.getCode(), form);
                redirect.addFlashAttribute("successMessage", "Treatment updated.");
            } else {
                treatmentService.create(form);
                redirect.addFlashAttribute("successMessage",
                        "Treatment " + form.getCode() + " added to the catalogue.");
            }
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/treatments";
    }

    @PostMapping("/treatments/{code}/toggle")
    public String toggleTreatment(@PathVariable String code, RedirectAttributes redirect) {
        TreatmentDto treatment = treatmentService.findByCode(code);
        if (treatment.isActive()) {
            treatmentService.deactivate(code);
            redirect.addFlashAttribute("successMessage",
                    treatment.getName() + " withdrawn from the catalogue. "
                            + "Historic bills that reference it are unaffected.");
        } else {
            treatmentService.reactivate(code);
            redirect.addFlashAttribute("successMessage", treatment.getName() + " is bookable again.");
        }
        return "redirect:/treatments";
    }
}
