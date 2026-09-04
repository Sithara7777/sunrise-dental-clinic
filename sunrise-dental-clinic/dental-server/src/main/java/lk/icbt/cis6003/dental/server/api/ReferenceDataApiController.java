package lk.icbt.cis6003.dental.server.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.ApiResponse;
import lk.icbt.cis6003.dental.common.dto.DentistDto;
import lk.icbt.cis6003.dental.common.dto.PageResponse;
import lk.icbt.cis6003.dental.common.dto.PatientDto;
import lk.icbt.cis6003.dental.common.dto.TreatmentDto;
import lk.icbt.cis6003.dental.server.service.ClinicFacade;
import lk.icbt.cis6003.dental.server.service.DentistService;
import lk.icbt.cis6003.dental.server.service.PatientService;
import lk.icbt.cis6003.dental.server.service.TreatmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Web services for the master data the booking screen depends on: patients,
 * dentists and the treatment catalogue.
 *
 * <p>The three are grouped in one controller because the desktop client treats
 * them as one concern - "the lists I need to fill in a form". Splitting them
 * would produce three near-identical classes with no reader benefit.</p>
 *
 * <p>{@link #bookingFormData} is the Facade call: it returns the dentists, the
 * treatments and the chosen dentist's free slots in a single response, so
 * opening the booking window costs one network round trip instead of three.</p>
 */
@RestController
@Tag(name = "4. Reference data", description = "Patients, dentists and the treatment catalogue")
public class ReferenceDataApiController {

    private final PatientService patientService;
    private final DentistService dentistService;
    private final TreatmentService treatmentService;
    private final ClinicFacade clinicFacade;

    public ReferenceDataApiController(PatientService patientService,
                                      DentistService dentistService,
                                      TreatmentService treatmentService,
                                      ClinicFacade clinicFacade) {
        this.patientService = patientService;
        this.dentistService = dentistService;
        this.treatmentService = treatmentService;
        this.clinicFacade = clinicFacade;
    }

    /* ---------------------------- patients ----------------------------- */

    @GetMapping(ApiPaths.PATIENT_SEARCH)
    @Operation(summary = "Search patients",
               description = "Matches name, patient number, telephone number or NIC. Always "
                       + "search before registering somebody as new - one patient, one record.")
    public ResponseEntity<ApiResponse<PageResponse<PatientDto>>> searchPatients(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(patientService.search(term, page, size)));
    }

    @GetMapping(ApiPaths.PATIENT_BY_CODE)
    @Operation(summary = "Retrieve a patient")
    public ResponseEntity<ApiResponse<PatientDto>> findPatient(@PathVariable String patientCode) {
        return ResponseEntity.ok(ApiResponse.ok(patientService.findByCode(patientCode)));
    }

    @PostMapping(ApiPaths.PATIENTS)
    @Operation(summary = "Register a patient",
               description = "Registers a patient without booking an appointment at the same time.")
    public ResponseEntity<ApiResponse<PatientDto>> registerPatient(@Valid @RequestBody PatientDto dto) {
        PatientDto created = patientService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created,
                "Patient registered as " + created.getPatientCode() + "."));
    }

    @PutMapping(ApiPaths.PATIENT_BY_CODE)
    @Operation(summary = "Update a patient's details")
    public ResponseEntity<ApiResponse<PatientDto>> updatePatient(@PathVariable String patientCode,
                                                                 @Valid @RequestBody PatientDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(patientService.update(patientCode, dto),
                "Patient record updated."));
    }

    /* ---------------------------- dentists ----------------------------- */

    @GetMapping(ApiPaths.DENTISTS)
    @Operation(summary = "List dentists",
               description = "Practising dentists only by default; pass includeInactive=true for "
                       + "the full roster.")
    public ResponseEntity<ApiResponse<List<DentistDto>>> listDentists(
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        return ResponseEntity.ok(ApiResponse.ok(
                includeInactive ? dentistService.listAll() : dentistService.listActive()));
    }

    @GetMapping(ApiPaths.DENTIST_BY_CODE)
    @Operation(summary = "Retrieve a dentist")
    public ResponseEntity<ApiResponse<DentistDto>> findDentist(@PathVariable String dentistCode) {
        return ResponseEntity.ok(ApiResponse.ok(dentistService.findByCode(dentistCode)));
    }

    @PostMapping(ApiPaths.DENTISTS)
    @Operation(summary = "Add a dentist (administrators only)")
    public ResponseEntity<ApiResponse<DentistDto>> createDentist(@Valid @RequestBody DentistDto dto) {
        DentistDto created = dentistService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created,
                "Dentist added as " + created.getDentistCode() + "."));
    }

    @PutMapping(ApiPaths.DENTIST_BY_CODE)
    @Operation(summary = "Update a dentist (administrators only)")
    public ResponseEntity<ApiResponse<DentistDto>> updateDentist(@PathVariable String dentistCode,
                                                                 @Valid @RequestBody DentistDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(dentistService.update(dentistCode, dto),
                "Dentist record updated."));
    }

    /* --------------------------- treatments ---------------------------- */

    @GetMapping(ApiPaths.TREATMENTS)
    @Operation(summary = "List treatments",
               description = "The scenario's 'treatment type' catalogue. Bookable treatments only "
                       + "by default.")
    public ResponseEntity<ApiResponse<List<TreatmentDto>>> listTreatments(
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        return ResponseEntity.ok(ApiResponse.ok(
                includeInactive ? treatmentService.listAll() : treatmentService.listActive()));
    }

    @GetMapping(ApiPaths.TREATMENT_BY_CODE)
    @Operation(summary = "Retrieve a treatment")
    public ResponseEntity<ApiResponse<TreatmentDto>> findTreatment(@PathVariable String treatmentCode) {
        return ResponseEntity.ok(ApiResponse.ok(treatmentService.findByCode(treatmentCode)));
    }

    @PostMapping(ApiPaths.TREATMENTS)
    @Operation(summary = "Add a treatment (administrators only)",
               description = "The pricing rule must be one the system actually implements - an "
                       + "unrecognised key is rejected rather than silently producing wrong bills.")
    public ResponseEntity<ApiResponse<TreatmentDto>> createTreatment(@Valid @RequestBody TreatmentDto dto) {
        TreatmentDto created = treatmentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created,
                "Treatment " + created.getCode() + " added to the catalogue."));
    }

    @PutMapping(ApiPaths.TREATMENT_BY_CODE)
    @Operation(summary = "Update a treatment (administrators only)")
    public ResponseEntity<ApiResponse<TreatmentDto>> updateTreatment(@PathVariable String treatmentCode,
                                                                     @Valid @RequestBody TreatmentDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(treatmentService.update(treatmentCode, dto),
                "Treatment updated."));
    }

    /* ------------------------- facade operation ------------------------ */

    @GetMapping(ApiPaths.API_ROOT + "/booking-form-data")
    @Operation(summary = "Everything the booking screen needs, in one call",
               description = "Facade operation. Returns the dentist list, the treatment list and - "
                       + "when a dentist and date are supplied - that dentist's slots for the day. "
                       + "Three round trips collapse into one.")
    public ResponseEntity<ApiResponse<ClinicFacade.BookingFormData>> bookingFormData(
            @RequestParam(required = false) String dentistCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(ApiResponse.ok(clinicFacade.bookingFormData(dentistCode, date)));
    }
}
