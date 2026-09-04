package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.dto.AppointmentDto;
import lk.icbt.cis6003.dental.common.dto.AppointmentRequest;
import lk.icbt.cis6003.dental.common.dto.PageResponse;
import lk.icbt.cis6003.dental.common.dto.SlotDto;
import lk.icbt.cis6003.dental.common.dto.StatusUpdateRequest;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.server.domain.Appointment;
import lk.icbt.cis6003.dental.server.domain.Dentist;
import lk.icbt.cis6003.dental.server.domain.Patient;
import lk.icbt.cis6003.dental.server.domain.Treatment;
import lk.icbt.cis6003.dental.server.exception.ResourceNotFoundException;
import lk.icbt.cis6003.dental.server.exception.SlotUnavailableException;
import lk.icbt.cis6003.dental.server.mapper.AppointmentMapper;
import lk.icbt.cis6003.dental.server.repository.AppointmentRepository;
import lk.icbt.cis6003.dental.server.repository.InvoiceRepository;
import lk.icbt.cis6003.dental.server.security.SecurityUtils;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEvent;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEventPublisher;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEventType;
import lk.icbt.cis6003.dental.server.service.validation.BookingValidationChain;
import lk.icbt.cis6003.dental.server.service.validation.BookingValidationRequest;
import lk.icbt.cis6003.dental.server.service.validation.DentistDoubleBookingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The heart of the system: registering, retrieving and amending appointments.
 *
 * <p>Covers requirement 2 ("Register New Appointment") and requirement 3
 * ("Display Appointment Details - search using the appointment number").</p>
 *
 * <p><b>How this class stays small.</b> It orchestrates; it does not decide.
 * The booking rules live in the validation chain, the identifier rule lives in
 * the sequence service, the patient-matching rule lives in the patient service,
 * and everything that must happen <em>after</em> a booking lives in the
 * observers. What remains here is the sequence of steps, which is exactly what
 * a reader wants from a service class.</p>
 */
@Service
@Transactional(readOnly = true)
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PatientService patientService;
    private final DentistService dentistService;
    private final TreatmentService treatmentService;
    private final SequenceGeneratorService sequenceGenerator;
    private final BookingValidationChain validationChain;
    private final AppointmentEventPublisher eventPublisher;
    private final AppointmentMapper appointmentMapper;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              InvoiceRepository invoiceRepository,
                              PatientService patientService,
                              DentistService dentistService,
                              TreatmentService treatmentService,
                              SequenceGeneratorService sequenceGenerator,
                              BookingValidationChain validationChain,
                              AppointmentEventPublisher eventPublisher,
                              AppointmentMapper appointmentMapper) {
        this.appointmentRepository = appointmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.patientService = patientService;
        this.dentistService = dentistService;
        this.treatmentService = treatmentService;
        this.sequenceGenerator = sequenceGenerator;
        this.validationChain = validationChain;
        this.eventPublisher = eventPublisher;
        this.appointmentMapper = appointmentMapper;
    }

    /* ================================================================== */
    /* Requirement 2 - Register New Appointment                            */
    /* ================================================================== */

    /**
     * Registers a new appointment.
     *
     * <p>The order of the steps matters:</p>
     * <ol>
     *   <li>resolve the dentist and treatment, so the validation chain has real
     *       objects and never queries for them itself;</li>
     *   <li>resolve or register the patient;</li>
     *   <li>run all six booking rules and abort on the first failure;</li>
     *   <li>allocate the unique appointment number;</li>
     *   <li>save - where the database's unique constraint has the final word on
     *       double booking;</li>
     *   <li>announce the event, so alerts and audit happen without this method
     *       knowing about them.</li>
     * </ol>
     *
     * @throws SlotUnavailableException if the slot is taken, including the rare
     *         case where a competing transaction wins the race between step 3
     *         and step 5
     */
    @Transactional
    public AppointmentDto register(AppointmentRequest request) {
        String actor = SecurityUtils.getCurrentUsernameOrSystem();

        Dentist dentist = dentistService.requireByCode(request.getDentistCode());
        Treatment treatment = treatmentService.requireByCode(request.getTreatmentCode());
        Patient patient = patientService.resolveOrRegister(request);

        validationChain.validateOrThrow(new BookingValidationRequest(
                dentist, treatment, patient.getPatientCode(),
                request.getAppointmentDate(), request.getAppointmentTime(), null));

        Appointment appointment = Appointment.builder()
                .appointmentNumber(sequenceGenerator.nextAppointmentNumber())
                .patient(patient)
                .dentist(dentist)
                .treatment(treatment)
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.getNotes())
                .createdBy(actor)
                .build();

        Appointment saved = persistGuardingAgainstSlotClash(appointment, dentist);

        log.info("Appointment {} registered for {} with Dr {} on {} at {}",
                 saved.getAppointmentNumber(), patient.getFullName(), dentist.getFullName(),
                 saved.getAppointmentDate(), saved.getAppointmentTime());

        eventPublisher.publish(
                AppointmentEvent.from(saved, AppointmentEventType.BOOKED, actor).build());

        return appointmentMapper.toDto(saved, null);
    }

    /**
     * Saves the appointment, translating a unique-constraint violation into the
     * clinic's own message.
     *
     * <p>This is the last line of the anti-double-booking defence. The
     * validation chain has already checked, but between that check and this
     * insert another transaction may have committed the same slot. The database
     * refuses it; this method turns the resulting
     * {@code DataIntegrityViolationException} into a sentence a receptionist can
     * act on rather than a stack trace.</p>
     */
    private Appointment persistGuardingAgainstSlotClash(Appointment appointment, Dentist dentist) {
        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Slot clash detected by the database for Dr {} on {} at {} - "
                     + "another user booked it first",
                     dentist.getFullName(), appointment.getAppointmentDate(),
                     appointment.getAppointmentTime(), ex);
            throw new SlotUnavailableException(dentist.getFullName(),
                    appointment.getAppointmentDate(), appointment.getAppointmentTime());
        }
    }

    /* ================================================================== */
    /* Requirement 3 - Display Appointment Details                         */
    /* ================================================================== */

    /**
     * Searches by appointment number - the primary lookup in the scenario.
     *
     * @throws ResourceNotFoundException when no such appointment exists
     */
    public AppointmentDto findByNumber(String appointmentNumber) {
        Appointment appointment = requireByNumber(appointmentNumber);
        String invoiceNumber = invoiceRepository
                .findByAppointmentAppointmentNumber(appointment.getAppointmentNumber())
                .map(inv -> inv.getInvoiceNumber())
                .orElse(null);
        return appointmentMapper.toDto(appointment, invoiceNumber);
    }

    public Appointment requireByNumber(String appointmentNumber) {
        String key = appointmentNumber == null ? null : appointmentNumber.trim().toUpperCase();
        return appointmentRepository.findByAppointmentNumber(key)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentNumber));
    }

    /** Filtered, paged list for the appointments screen. */
    public PageResponse<AppointmentDto> search(String term, AppointmentStatus status, String dentistCode,
                                               LocalDate fromDate, LocalDate toDate,
                                               int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
                Sort.by(Sort.Direction.DESC, "appointmentDate")
                    .and(Sort.by(Sort.Direction.ASC, "appointmentTime")));

        Page<Appointment> result = appointmentRepository.search(
                term == null ? "" : term.trim(), status, dentistCode, fromDate, toDate, pageable);

        return new PageResponse<>(appointmentMapper.toDtoList(result.getContent()),
                                  result.getNumber(), result.getSize(), result.getTotalElements());
    }

    /** One day's diary, in time order - the daily schedule screen. */
    public List<AppointmentDto> listForDate(LocalDate date) {
        return appointmentMapper.toDtoList(
                appointmentRepository.findByAppointmentDateOrderByAppointmentTimeAsc(date));
    }

    /** A patient's complete visit history - impossible under the paper system. */
    public List<AppointmentDto> historyForPatient(String patientCode) {
        return appointmentMapper.toDtoList(appointmentRepository
                .findByPatientPatientCodeOrderByAppointmentDateDescAppointmentTimeDesc(patientCode));
    }

    /* ================================================================== */
    /* Lifecycle                                                           */
    /* ================================================================== */

    /**
     * Moves an appointment to a new status.
     *
     * <p>The legality of the move is decided by the entity, using
     * {@link AppointmentStatus#canTransitionTo}. Completing an appointment that
     * was cancelled last week is refused here rather than producing a bill for
     * treatment that never happened.</p>
     */
    @Transactional
    public AppointmentDto updateStatus(String appointmentNumber, StatusUpdateRequest request) {
        String actor = SecurityUtils.getCurrentUsernameOrSystem();
        Appointment appointment = requireByNumber(appointmentNumber);

        AppointmentStatus previous = appointment.getStatus();
        appointment.changeStatus(request.getStatus(), request.getReason(), actor);
        Appointment saved = appointmentRepository.saveAndFlush(appointment);

        log.info("Appointment {} moved from {} to {} by {}",
                 appointmentNumber, previous, request.getStatus(), actor);

        eventPublisher.publish(AppointmentEvent
                .from(saved, toEventType(request.getStatus()), actor)
                .detail(request.getReason())
                .build());

        return appointmentMapper.toDto(saved, null);
    }

    /**
     * Moves an appointment to a different date or time.
     *
     * <p>The whole validation chain runs again, excluding this appointment from
     * the clash check so that it does not report itself as occupying the slot
     * it is trying to leave.</p>
     */
    @Transactional
    public AppointmentDto reschedule(String appointmentNumber, LocalDate newDate, LocalTime newTime) {
        String actor = SecurityUtils.getCurrentUsernameOrSystem();
        Appointment appointment = requireByNumber(appointmentNumber);

        if (appointment.getStatus().isTerminal()) {
            throw new lk.icbt.cis6003.dental.server.exception.InvalidStateTransitionException(
                    "Appointment " + appointmentNumber + " is " + appointment.getStatus().getDisplayName()
                            + " and can no longer be rescheduled. Please book a new appointment.");
        }

        validationChain.validateOrThrow(new BookingValidationRequest(
                appointment.getDentist(), appointment.getTreatment(),
                appointment.getPatient().getPatientCode(),
                newDate, newTime, appointment.getId()));

        appointment.reschedule(newDate, newTime, actor);
        Appointment saved = persistGuardingAgainstSlotClash(appointment, appointment.getDentist());

        eventPublisher.publish(
                AppointmentEvent.from(saved, AppointmentEventType.RESCHEDULED, actor).build());

        return appointmentMapper.toDto(saved, null);
    }

    /* ================================================================== */
    /* Availability                                                        */
    /* ================================================================== */

    /**
     * The free and taken slots in one dentist's diary for one day.
     *
     * <p>Offering this <em>before</em> the receptionist types a time is the
     * primary defence against double booking: a slot that is never offered is
     * never requested. The unique constraint then handles the rare simultaneous
     * request.</p>
     *
     * <p>The grid runs from the later of the clinic's opening time and the
     * dentist's shift start, to the earlier of the two ends - so a dentist's
     * own hours, not the clinic's, bound what is offered.</p>
     */
    public List<SlotDto> availableSlots(String dentistCode, LocalDate date) {
        Dentist dentist = dentistService.requireByCode(dentistCode);

        Map<LocalTime, String> occupied = new HashMap<>();
        for (Appointment booked : appointmentRepository.findDiary(
                dentistCode, date, DentistDoubleBookingHandler.occupyingStatuses())) {
            // A long treatment blocks every slot it spans, not only its first.
            LocalTime cursor = booked.getAppointmentTime();
            LocalTime finish = booked.getEndTime();
            while (cursor.isBefore(finish)) {
                occupied.put(cursor, booked.getAppointmentNumber());
                cursor = cursor.plusMinutes(ClinicConstants.SLOT_DURATION_MINUTES);
            }
        }

        LocalTime start = max(ClinicConstants.CLINIC_OPENING_TIME, dentist.getWorkStartTime());
        LocalTime end = min(ClinicConstants.CLINIC_CLOSING_TIME, dentist.getWorkEndTime());

        List<SlotDto> slots = new ArrayList<>();
        LocalTime cursor = start;
        while (cursor.plusMinutes(ClinicConstants.SLOT_DURATION_MINUTES).compareTo(end) <= 0) {
            String occupiedBy = occupied.get(cursor);
            boolean stillBookable = date.isAfter(LocalDate.now())
                    || (date.isEqual(LocalDate.now()) && cursor.isAfter(LocalTime.now()));

            slots.add(new SlotDto(cursor,
                                  cursor.plusMinutes(ClinicConstants.SLOT_DURATION_MINUTES),
                                  occupiedBy == null && stillBookable,
                                  occupiedBy));
            cursor = cursor.plusMinutes(ClinicConstants.SLOT_DURATION_MINUTES);
        }
        return slots;
    }

    /* ================================================================== */
    /* Counters for the dashboard                                          */
    /* ================================================================== */

    public long countOn(LocalDate date) {
        return appointmentRepository.countByAppointmentDate(date);
    }

    public long countOnWithStatus(LocalDate date, AppointmentStatus status) {
        return appointmentRepository.countByAppointmentDateAndStatus(date, status);
    }

    public long countBetween(LocalDate from, LocalDate to) {
        return appointmentRepository.countByAppointmentDateBetween(from, to);
    }

    public long countWithStatus(AppointmentStatus status) {
        return appointmentRepository.countByStatus(status);
    }

    public long bookedMinutesOn(LocalDate date) {
        return appointmentRepository.sumBookedMinutes(
                date, DentistDoubleBookingHandler.occupyingStatuses());
    }

    public List<Appointment> dueForReminder(LocalDate date) {
        return appointmentRepository.findForReminder(date,
                List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED));
    }

    /* ================================================================== */
    /* Helpers                                                             */
    /* ================================================================== */

    private AppointmentEventType toEventType(AppointmentStatus status) {
        switch (status) {
            case CONFIRMED:
                return AppointmentEventType.CONFIRMED;
            case COMPLETED:
                return AppointmentEventType.COMPLETED;
            case CANCELLED:
                return AppointmentEventType.CANCELLED;
            case NO_SHOW:
                return AppointmentEventType.NO_SHOW;
            default:
                return AppointmentEventType.BOOKED;
        }
    }

    private static LocalTime max(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime min(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }

    private int clampSize(int size) {
        return size <= 0 ? 20 : Math.min(size, 200);
    }
}
