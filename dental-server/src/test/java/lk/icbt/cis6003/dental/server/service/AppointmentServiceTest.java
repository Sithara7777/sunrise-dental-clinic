package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.common.dto.AppointmentDto;
import lk.icbt.cis6003.dental.common.dto.SlotDto;
import lk.icbt.cis6003.dental.common.dto.StatusUpdateRequest;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.server.domain.Appointment;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.exception.ResourceNotFoundException;
import lk.icbt.cis6003.dental.server.exception.SlotUnavailableException;
import lk.icbt.cis6003.dental.server.mapper.AppointmentMapper;
import lk.icbt.cis6003.dental.server.repository.AppointmentRepository;
import lk.icbt.cis6003.dental.server.repository.InvoiceRepository;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEvent;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEventPublisher;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEventType;
import lk.icbt.cis6003.dental.server.service.validation.BookingValidationChain;
import lk.icbt.cis6003.dental.server.testsupport.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the appointment service, with every collaborator mocked.
 *
 * <p>These are the tests that verify <em>orchestration</em>: that the service
 * runs the validation chain before saving, that it allocates a number from the
 * sequence rather than inventing one, that it announces the booking so alerts
 * and audit happen, and - the important one - that it converts a database
 * constraint violation into a sentence a receptionist can act on.</p>
 *
 * <p>Mocking the collaborators is what makes the last case testable at all: a
 * genuine race between two transactions is almost impossible to reproduce on
 * demand, but a mock can be told to throw exactly what the database would.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Appointment service")
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PatientService patientService;
    @Mock private DentistService dentistService;
    @Mock private TreatmentService treatmentService;
    @Mock private SequenceGeneratorService sequenceGenerator;
    @Mock private BookingValidationChain validationChain;
    @Mock private AppointmentEventPublisher eventPublisher;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, invoiceRepository,
                patientService, dentistService, treatmentService, sequenceGenerator,
                validationChain, eventPublisher, new AppointmentMapper());

        when(dentistService.requireByCode("DEN-001")).thenReturn(TestDataFactory.dentist());
        when(treatmentService.requireByCode("SCALING")).thenReturn(TestDataFactory.standardTreatment());
        when(patientService.resolveOrRegister(any())).thenReturn(TestDataFactory.adultPatient());
        when(sequenceGenerator.nextAppointmentNumber()).thenReturn("APT-2026-000042");
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.findByAppointmentAppointmentNumber(anyString()))
                .thenReturn(Optional.empty());
    }

    /* ================================================================== */
    /* Requirement 2 - registering                                         */
    /* ================================================================== */

    @Test
    @DisplayName("registers an appointment and returns the server-issued number")
    void registersAnAppointment() {
        AppointmentDto created = service.register(TestDataFactory.request().build());

        assertThat(created.getAppointmentNumber()).isEqualTo("APT-2026-000042");
        assertThat(created.getPatientName()).isEqualTo("Kamala Perera");
        assertThat(created.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("takes the appointment number from the sequence, ignoring anything a client sends")
    void numberComesFromTheSequence() {
        service.register(TestDataFactory.request().build());

        verify(sequenceGenerator).nextAppointmentNumber();
    }

    @Test
    @DisplayName("runs the validation chain BEFORE saving")
    void validatesBeforeSaving() {
        doThrow(new SlotUnavailableException("Dr Perera is already booked"))
                .when(validationChain).validateOrThrow(any());

        assertThatThrownBy(() -> service.register(TestDataFactory.request().build()))
                .isInstanceOf(SlotUnavailableException.class);

        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("announces the booking so alerts and audit happen without it knowing about them")
    void publishesTheBookingEvent() {
        service.register(TestDataFactory.request().build());

        ArgumentCaptor<AppointmentEvent> captor = ArgumentCaptor.forClass(AppointmentEvent.class);
        verify(eventPublisher).publish(captor.capture());

        AppointmentEvent event = captor.getValue();
        assertThat(event.getType()).isEqualTo(AppointmentEventType.BOOKED);
        assertThat(event.getAppointmentNumber()).isEqualTo("APT-2026-000042");
        assertThat(event.getPatientName()).isEqualTo("Kamala Perera");
    }

    @Test
    @DisplayName("a database slot clash becomes a clear message, not a stack trace")
    void databaseClashBecomesAReadableMessage() {
        // Simulates the genuine race: validation passed, then a competing
        // transaction committed the same slot before this INSERT reached the
        // database. The unique constraint is the real guarantee; this is how
        // the user finds out about it.
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Unique index or primary key violation: UK_APPOINTMENT_SLOT"));

        assertThatThrownBy(() -> service.register(TestDataFactory.request().build()))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("Nimal Perera")
                .hasMessageContaining("another time slot");
    }

    @Test
    @DisplayName("no event is published when the save fails")
    void noEventWhenSaveFails() {
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("clash"));

        assertThatThrownBy(() -> service.register(TestDataFactory.request().build()))
                .isInstanceOf(SlotUnavailableException.class);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("the treatment's duration is copied onto the appointment")
    void durationComesFromTheTreatment() {
        AppointmentDto created = service.register(TestDataFactory.request().build());

        assertThat(created.getDurationMinutes()).isEqualTo(45);
        assertThat(created.getAppointmentEndTime()).isEqualTo(LocalTime.of(10, 45));
    }

    /* ================================================================== */
    /* Requirement 3 - retrieving                                          */
    /* ================================================================== */

    @Test
    @DisplayName("finds an appointment by its number")
    void findsByNumber() {
        when(appointmentRepository.findByAppointmentNumber("APT-2026-000001"))
                .thenReturn(Optional.of(TestDataFactory.appointment()));

        AppointmentDto found = service.findByNumber("APT-2026-000001");

        assertThat(found.getAppointmentNumber()).isEqualTo("APT-2026-000001");
        assertThat(found.getPatientName()).isEqualTo("Kamala Perera");
        assertThat(found.getAddress()).isEqualTo("No. 45, Galle Road, Colombo 03");
    }

    @Test
    @DisplayName("an unknown appointment number is reported as not found, naming the number")
    void unknownNumberIsReported() {
        when(appointmentRepository.findByAppointmentNumber(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByNumber("APT-2026-999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("APT-2026-999999");
    }

    @Test
    @DisplayName("the appointment number is matched case-insensitively and trimmed")
    void numberLookupIsForgiving() {
        when(appointmentRepository.findByAppointmentNumber("APT-2026-000001"))
                .thenReturn(Optional.of(TestDataFactory.appointment()));

        assertThat(service.findByNumber("  apt-2026-000001  ").getAppointmentNumber())
                .isEqualTo("APT-2026-000001");
    }

    /* ================================================================== */
    /* Lifecycle                                                           */
    /* ================================================================== */

    @Test
    @DisplayName("a legal status change is applied and announced")
    void statusChangeIsAppliedAndAnnounced() {
        when(appointmentRepository.findByAppointmentNumber("APT-2026-000001"))
                .thenReturn(Optional.of(TestDataFactory.appointment()));

        AppointmentDto updated = service.updateStatus("APT-2026-000001",
                new StatusUpdateRequest(AppointmentStatus.COMPLETED));

        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(eventPublisher).publish(any(AppointmentEvent.class));
    }

    @Test
    @DisplayName("an illegal status change is refused before anything is saved")
    void illegalStatusChangeIsRefused() {
        Appointment cancelled = TestDataFactory.appointment();
        cancelled.changeStatus(AppointmentStatus.CANCELLED, "Patient telephoned", "reception");
        when(appointmentRepository.findByAppointmentNumber("APT-2026-000001"))
                .thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.updateStatus("APT-2026-000001",
                new StatusUpdateRequest(AppointmentStatus.COMPLETED)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("a terminal appointment cannot be rescheduled")
    void terminalAppointmentCannotBeRescheduled() {
        Appointment completed = TestDataFactory.completedAppointment();
        when(appointmentRepository.findByAppointmentNumber("APT-2026-000001"))
                .thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service.reschedule("APT-2026-000001",
                LocalDate.now().plusDays(3), LocalTime.of(11, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("can no longer be rescheduled");
    }

    @Test
    @DisplayName("rescheduling excludes the appointment itself from the clash check")
    void reschedulingExcludesItself() {
        when(appointmentRepository.findByAppointmentNumber("APT-2026-000001"))
                .thenReturn(Optional.of(TestDataFactory.appointment()));

        service.reschedule("APT-2026-000001", LocalDate.now().plusDays(3), LocalTime.of(11, 0));

        ArgumentCaptor<lk.icbt.cis6003.dental.server.service.validation.BookingValidationRequest> captor =
                ArgumentCaptor.forClass(
                        lk.icbt.cis6003.dental.server.service.validation.BookingValidationRequest.class);
        verify(validationChain).validateOrThrow(captor.capture());

        assertThat(captor.getValue().isReschedule())
                .as("the appointment being moved must not report itself as a clash")
                .isTrue();
    }

    /* ================================================================== */
    /* Availability                                                        */
    /* ================================================================== */

    @Test
    @DisplayName("offers only slots inside the dentist's own shift")
    void slotsRespectTheDentistShift() {
        when(dentistService.requireByCode("DEN-004"))
                .thenReturn(TestDataFactory.partTimeDentist());   // 08:00-14:00
        when(appointmentRepository.findDiary(anyString(), any(), any())).thenReturn(List.of());

        List<SlotDto> slots = service.availableSlots("DEN-004", LocalDate.now().plusDays(3));

        // 08:00 to 14:00 in 30-minute slots is 12 slots.
        assertThat(slots).hasSize(12);
        assertThat(slots.get(0).getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(slots.get(slots.size() - 1).getEndTime()).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    @DisplayName("a long treatment blocks EVERY slot it spans, not just its first")
    void longTreatmentBlocksEverySlotItSpans() {
        Appointment booked = Appointment.builder()
                .appointmentNumber("APT-2026-000100")
                .patient(TestDataFactory.adultPatient())
                .dentist(TestDataFactory.dentist())
                .treatment(TestDataFactory.surgicalTreatment())    // 90 minutes
                .appointmentDate(LocalDate.now().plusDays(3))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.SCHEDULED)
                .createdBy("reception")
                .build();

        when(appointmentRepository.findDiary(anyString(), any(), any())).thenReturn(List.of(booked));

        List<SlotDto> slots = service.availableSlots("DEN-001", LocalDate.now().plusDays(3));

        // 10:00, 10:30 and 11:00 must all be shown as taken.
        assertThat(slots).filteredOn(s -> !s.isAvailable())
                .extracting(SlotDto::getStartTime)
                .contains(LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 0));

        assertThat(slots).filteredOn(s -> !s.isAvailable())
                .allMatch(s -> "APT-2026-000100".equals(s.getOccupiedBy()));
    }

    @Test
    @DisplayName("a slot that has already passed today is not offered")
    void pastSlotsTodayAreNotOffered() {
        when(appointmentRepository.findDiary(anyString(), any(), any())).thenReturn(List.of());

        List<SlotDto> slots = service.availableSlots("DEN-001", LocalDate.now());

        assertThat(slots)
                .filteredOn(s -> s.getStartTime().isBefore(LocalTime.now()))
                .allMatch(s -> !s.isAvailable());
    }
}
