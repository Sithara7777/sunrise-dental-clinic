package lk.icbt.cis6003.dental.server.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.ApiResponse;
import lk.icbt.cis6003.dental.common.dto.AppointmentDto;
import lk.icbt.cis6003.dental.common.dto.AppointmentRequest;
import lk.icbt.cis6003.dental.common.dto.PageResponse;
import lk.icbt.cis6003.dental.common.dto.SlotDto;
import lk.icbt.cis6003.dental.common.dto.StatusUpdateRequest;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.server.service.AppointmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Web service for requirements 2 and 3 - registering an appointment and
 * displaying its details.
 *
 * <p>The controller is intentionally thin. It binds and validates the request,
 * calls one service method and wraps the result in the standard envelope. Every
 * rule - the six booking checks, the appointment number, the double-booking
 * guard - lives in the business tier, which is what allows the browser
 * controllers to enforce exactly the same rules without duplicating a line.</p>
 */
@RestController
@Tag(name = "2. Appointments", description = "Register, search, reschedule and update appointments")
public class AppointmentApiController {

    private final AppointmentService appointmentService;

    public AppointmentApiController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping(ApiPaths.APPOINTMENTS)
    @Operation(summary = "Register a new appointment",
               description = """
                       Requirement 2. Registers the patient if they are new, applies the six
                       booking rules, allocates a unique appointment number and returns the
                       complete record.

                       Do not send an appointment number - the server issues it. Leave
                       patientCode empty to register a new patient from the name, address and
                       contact number supplied.
                       """)
    public ResponseEntity<ApiResponse<AppointmentDto>> register(
            @Valid @RequestBody AppointmentRequest request) {

        AppointmentDto created = appointmentService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(created,
                        "Appointment " + created.getAppointmentNumber() + " registered successfully."));
    }

    @GetMapping(ApiPaths.APPOINTMENT_BY_NUMBER)
    @Operation(summary = "Display appointment details",
               description = "Requirement 3. Searches by appointment number and returns the "
                       + "complete patient and appointment information.")
    public ResponseEntity<ApiResponse<AppointmentDto>> findByNumber(
            @Parameter(description = "e.g. APT-2026-000137", example = "APT-2026-000001")
            @PathVariable String appointmentNumber) {

        return ResponseEntity.ok(ApiResponse.ok(appointmentService.findByNumber(appointmentNumber)));
    }

    @GetMapping(ApiPaths.APPOINTMENTS)
    @Operation(summary = "Search appointments",
               description = "Filtered, paged list. Every filter is optional; omit them all for "
                       + "the most recent appointments.")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentDto>>> search(
            @Parameter(description = "Matches appointment number, patient name or telephone number")
            @RequestParam(required = false) String term,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) String dentistCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                appointmentService.search(term, status, dentistCode, fromDate, toDate, page, size)));
    }

    @GetMapping(ApiPaths.APPOINTMENTS + "/day/{date}")
    @Operation(summary = "One day's diary",
               description = "Every appointment on the given date, in time order.")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> forDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(ApiResponse.ok(appointmentService.listForDate(date)));
    }

    @GetMapping(ApiPaths.APPOINTMENTS + "/patient/{patientCode}")
    @Operation(summary = "A patient's visit history",
               description = "Every appointment for one patient, most recent first - the record "
                       + "the paper system could not produce.")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> patientHistory(
            @PathVariable String patientCode) {

        return ResponseEntity.ok(ApiResponse.ok(appointmentService.historyForPatient(patientCode)));
    }

    @GetMapping(ApiPaths.APPOINTMENT_AVAILABLE_SLOTS)
    @Operation(summary = "Free slots for a dentist on a date",
               description = "The primary defence against double booking: the receptionist picks "
                       + "from slots the server has already confirmed are free.")
    public ResponseEntity<ApiResponse<List<SlotDto>>> availableSlots(
            @RequestParam String dentistCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(ApiResponse.ok(appointmentService.availableSlots(dentistCode, date)));
    }

    @PatchMapping(ApiPaths.APPOINTMENT_STATUS)
    @Operation(summary = "Change an appointment's status",
               description = "Confirm, complete, cancel or record a no-show. Illegal moves - "
                       + "completing a cancelled appointment, for example - are refused.")
    public ResponseEntity<ApiResponse<AppointmentDto>> updateStatus(
            @PathVariable String appointmentNumber,
            @Valid @RequestBody StatusUpdateRequest request) {

        AppointmentDto updated = appointmentService.updateStatus(appointmentNumber, request);
        return ResponseEntity.ok(ApiResponse.ok(updated,
                "Appointment " + appointmentNumber + " is now "
                        + updated.getStatus().getDisplayName() + "."));
    }

    @PatchMapping(ApiPaths.APPOINTMENT_BY_NUMBER + "/reschedule")
    @Operation(summary = "Move an appointment",
               description = "Re-runs every booking rule against the new date and time, excluding "
                       + "this appointment from the clash check.")
    public ResponseEntity<ApiResponse<AppointmentDto>> reschedule(
            @PathVariable String appointmentNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newTime) {

        AppointmentDto updated = appointmentService.reschedule(appointmentNumber, newDate, newTime);
        return ResponseEntity.ok(ApiResponse.ok(updated,
                "Appointment " + appointmentNumber + " moved to " + newDate + " at " + newTime + "."));
    }
}
