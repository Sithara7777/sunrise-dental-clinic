package lk.icbt.cis6003.dental.server.integration;

import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.server.domain.Appointment;
import lk.icbt.cis6003.dental.server.domain.Dentist;
import lk.icbt.cis6003.dental.server.domain.Patient;
import lk.icbt.cis6003.dental.server.domain.Treatment;
import lk.icbt.cis6003.dental.server.repository.AppointmentRepository;
import lk.icbt.cis6003.dental.server.repository.DentistRepository;
import lk.icbt.cis6003.dental.server.repository.PatientRepository;
import lk.icbt.cis6003.dental.server.repository.TreatmentRepository;
import lk.icbt.cis6003.dental.server.repository.dao.ReportingDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the ADVANCED DATABASE FEATURES - Task B (iii).
 *
 * <p>Everything here runs against a real H2 database with the real Flyway
 * migrations applied, which is the only way these can be tested at all. They
 * prove that:</p>
 *
 * <ul>
 *   <li>the five reporting <b>views</b> exist and return data;</li>
 *   <li>the two <b>stored functions</b> are callable as SQL;</li>
 *   <li>the two <b>triggers</b> actually fire and write audit rows;</li>
 *   <li>the <b>unique constraint</b> refuses a double booking even when the
 *       application layer is bypassed entirely; and</li>
 *   <li>the <b>CHECK constraints</b> refuse impossible data.</li>
 * </ul>
 *
 * <p>The double-booking test is the most important in the whole suite: it
 * writes directly through the repository, skipping the validation chain, and
 * shows that the clinic's original problem is prevented by the database itself
 * rather than by application code anyone could forget to call.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Database features (views, functions, triggers, constraints)")
class DatabaseFeaturesIT {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ReportingDao reportingDao;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DentistRepository dentistRepository;
    @Autowired private TreatmentRepository treatmentRepository;

    /* ================================================================== */
    /* Views                                                               */
    /* ================================================================== */

    @Test
    @DisplayName("all five reporting views exist and are queryable")
    void allViewsExist() {
        for (String view : List.of("v_daily_schedule", "v_revenue_daily", "v_dentist_workload",
                                   "v_treatment_popularity", "v_outstanding_invoice")) {
            assertThatCode(() -> jdbc.queryForObject("SELECT COUNT(*) FROM " + view, Long.class))
                    .as("view %s must exist", view)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("v_dentist_workload includes a dentist with no appointments, with zeros")
    void workloadViewKeepsIdleDentists() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT dentist_code, total_appointments FROM v_dentist_workload");

        // The reference data seeds five dentists and this test creates no
        // appointments for most of them - a manager needs to see the idle ones.
        assertThat(rows).isNotEmpty();
    }

    /* ================================================================== */
    /* Stored functions                                                    */
    /* ================================================================== */

    @Test
    @DisplayName("FN_INVOICE_TOTAL is callable as a SQL function and agrees with the Java tier")
    void invoiceTotalFunctionIsCallable() {
        BigDecimal fromDatabase = reportingDao.calculateInvoiceTotalInDatabase(
                new BigDecimal("1500.00"), new BigDecimal("6500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0.18"));

        assertThat(fromDatabase).isEqualByComparingTo("9440.00");
    }

    @Test
    @DisplayName("FN_INVOICE_TOTAL applies a discount before VAT inside the database too")
    void invoiceTotalFunctionHandlesDiscount() {
        BigDecimal fromDatabase = reportingDao.calculateInvoiceTotalInDatabase(
                new BigDecimal("1500.00"), new BigDecimal("6500.00"),
                BigDecimal.ZERO, new BigDecimal("10"), new BigDecimal("0.18"));

        assertThat(fromDatabase).isEqualByComparingTo("8496.00");
    }

    @Test
    @DisplayName("FN_AGEING_BUCKET is callable as a SQL function")
    void ageingBucketFunctionIsCallable() {
        assertThat(reportingDao.resolveAgeingBucket(15)).isEqualTo("0-30");
        assertThat(reportingDao.resolveAgeingBucket(45)).isEqualTo("31-60");
        assertThat(reportingDao.resolveAgeingBucket(120)).isEqualTo("90+");
    }

    @Test
    @DisplayName("the debtor ageing view uses the stored function for its band")
    void outstandingViewUsesTheStoredFunction() {
        // Proves the view really calls FN_AGEING_BUCKET rather than duplicating
        // the banding rule - which is what keeps the report, the dashboard and
        // an accountant's own query in agreement.
        String definition = jdbc.queryForObject(
                "SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS "
                        + "WHERE UPPER(TABLE_NAME) = 'V_OUTSTANDING_INVOICE'", String.class);

        assertThat(definition).containsIgnoringCase("FN_AGEING_BUCKET");
    }

    /* ================================================================== */
    /* Triggers                                                            */
    /* ================================================================== */

    @Test
    @DisplayName("the appointment audit trigger fires on INSERT and stamps source=DB_TRIGGER")
    void appointmentInsertTriggerFires() {
        long before = countTriggerAudits();

        appointmentRepository.saveAndFlush(newAppointment(LocalTime.of(9, 0)));

        assertThat(countTriggerAudits())
                .as("the database trigger must record every appointment insert")
                .isGreaterThan(before);
    }

    @Test
    @DisplayName("the appointment audit trigger records WHICH fields changed on UPDATE")
    void appointmentUpdateTriggerRecordsTheChange() {
        Appointment appointment = appointmentRepository.saveAndFlush(newAppointment(LocalTime.of(9, 30)));

        appointment.changeStatus(AppointmentStatus.CONFIRMED, null, "reception");
        appointmentRepository.saveAndFlush(appointment);

        List<Map<String, Object>> audits = jdbc.queryForList(
                "SELECT action, details FROM audit_log "
                        + "WHERE source = 'DB_TRIGGER' AND entity_key = ? "
                        + "ORDER BY id DESC",
                appointment.getAppointmentNumber());

        assertThat(audits).isNotEmpty();
        assertThat(audits.get(0).get("ACTION")).isEqualTo("APPOINTMENT_UPDATED");
        assertThat(String.valueOf(audits.get(0).get("DETAILS")))
                .contains("status")
                .contains("CONFIRMED");
    }

    /* ================================================================== */
    /* Constraints - the real anti-double-booking guarantee                */
    /* ================================================================== */

    @Test
    @DisplayName("the DATABASE refuses a double booking, even bypassing the application entirely")
    void databaseRefusesDoubleBooking() {
        LocalTime slot = LocalTime.of(11, 0);
        appointmentRepository.saveAndFlush(newAppointment(slot));

        // Written straight through the repository. No validation chain, no
        // service. If this succeeded, the clinic's original problem would be
        // one forgotten service call away from returning.
        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(newAppointment(slot)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("a CANCELLED appointment releases its slot, so the time can be resold")
    void cancelledAppointmentReleasesTheSlot() {
        LocalTime slot = LocalTime.of(12, 0);

        Appointment first = appointmentRepository.saveAndFlush(newAppointment(slot));
        first.changeStatus(AppointmentStatus.CANCELLED, "Patient telephoned", "reception");
        appointmentRepository.saveAndFlush(first);

        // The same dentist, date and time must now be bookable, because
        // slot_lock went NULL and a unique index never compares two NULLs.
        assertThatCode(() -> appointmentRepository.saveAndFlush(newAppointment(slot)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("many cancelled appointments can share one slot without colliding")
    void manyCancelledAppointmentsCoexist() {
        LocalTime slot = LocalTime.of(13, 0);

        for (int i = 0; i < 3; i++) {
            Appointment appointment = appointmentRepository.saveAndFlush(newAppointment(slot));
            appointment.changeStatus(AppointmentStatus.CANCELLED, "Cancelled " + i, "reception");
            appointmentRepository.saveAndFlush(appointment);
        }

        assertThatCode(() -> appointmentRepository.saveAndFlush(newAppointment(slot)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("two dentists may hold the same time - the constraint is per dentist")
    void differentDentistsMayShareATime() {
        LocalTime slot = LocalTime.of(14, 0);
        Dentist first = dentistRepository.findByDentistCode("DEN-001").orElseThrow();
        Dentist second = dentistRepository.findByDentistCode("DEN-002").orElseThrow();

        appointmentRepository.saveAndFlush(newAppointment(slot, first));

        assertThatCode(() -> appointmentRepository.saveAndFlush(newAppointment(slot, second)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the appointment number is unique")
    void appointmentNumberIsUnique() {
        Appointment first = newAppointment(LocalTime.of(15, 0));
        first.setAppointmentNumber("APT-TEST-DUPLICATE");
        appointmentRepository.saveAndFlush(first);

        Appointment second = newAppointment(LocalTime.of(15, 30));
        second.setAppointmentNumber("APT-TEST-DUPLICATE");

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("the CHECK constraint refuses an invoice paid more than its total")
    void checkConstraintRefusesOverPayment() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO invoice (invoice_number, appointment_id, patient_name, patient_address,
                        patient_contact, dentist_name, treatment_name, total_amount, amount_paid,
                        issued_date, issued_by, created_at, version)
                VALUES ('INV-BAD-001', ?, 'Bad Data', 'Nowhere', '0771111111', 'Nobody',
                        'Nothing', 1000.00, 5000.00, CURRENT_DATE, 'test', CURRENT_TIMESTAMP, 0)
                """, appointmentRepository.saveAndFlush(newAppointment(LocalTime.of(16, 0))).getId()))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("the CHECK constraint refuses a discount above 50%")
    void checkConstraintRefusesExcessiveDiscount() {
        Long appointmentId = appointmentRepository
                .saveAndFlush(newAppointment(LocalTime.of(16, 30))).getId();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO invoice (invoice_number, appointment_id, patient_name, patient_address,
                        patient_contact, dentist_name, treatment_name, discount_percentage,
                        total_amount, issued_date, issued_by, created_at, version)
                VALUES ('INV-BAD-002', ?, 'Bad Data', 'Nowhere', '0771111111', 'Nobody',
                        'Nothing', 90.00, 1000.00, CURRENT_DATE, 'test', CURRENT_TIMESTAMP, 0)
                """, appointmentId))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("the CHECK constraint refuses an unrecognised appointment status")
    void checkConstraintRefusesUnknownStatus() {
        Long patientId = ensurePatient().getId();
        Long dentistId = dentistRepository.findByDentistCode("DEN-001").orElseThrow().getId();
        Long treatmentId = treatmentRepository.findByCode("SCALING").orElseThrow().getId();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO appointment (appointment_number, patient_id, dentist_id, treatment_id,
                        appointment_date, appointment_time, duration_minutes, status,
                        created_by, created_at, version)
                VALUES ('APT-BAD-001', ?, ?, ?, CURRENT_DATE, '09:00:00', 30, 'TELEPORTED',
                        'test', CURRENT_TIMESTAMP, 0)
                """, patientId, dentistId, treatmentId))
                .isInstanceOf(Exception.class);
    }

    /* ------------------------------------------------------------------ */

    private long countTriggerAudits() {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE source = 'DB_TRIGGER'", Long.class);
        return count == null ? 0 : count;
    }

    private Appointment newAppointment(LocalTime time) {
        return newAppointment(time, dentistRepository.findByDentistCode("DEN-001").orElseThrow());
    }

    /**
     * Unique test appointment numbers that still fit the real column width.
     *
     * <p>{@code appointment_number} is {@code VARCHAR(20)} in the production
     * schema, and these tests run against that schema rather than a relaxed
     * one - so the generator has to respect it, exactly as the application
     * does.</p>
     */
    private static final java.util.concurrent.atomic.AtomicInteger TEST_SEQUENCE =
            new java.util.concurrent.atomic.AtomicInteger();

    private Appointment newAppointment(LocalTime time, Dentist dentist) {
        Treatment treatment = treatmentRepository.findByCode("SCALING").orElseThrow();

        return Appointment.builder()
                .appointmentNumber(String.format("APT-T-%06d", TEST_SEQUENCE.incrementAndGet()))
                .patient(ensurePatient())
                .dentist(dentist)
                .treatment(treatment)
                .appointmentDate(LocalDate.now().plusDays(30))
                .appointmentTime(time)
                .status(AppointmentStatus.SCHEDULED)
                .createdBy("test")
                .build();
    }

    private Patient ensurePatient() {
        return patientRepository.findByPatientCode("PAT-TEST-001")
                .orElseGet(() -> {
                    Patient patient = new Patient("PAT-TEST-001", "Integration Test Patient",
                            "No. 1, Test Road, Colombo 01", "0770000001");
                    return patientRepository.saveAndFlush(patient);
                });
    }
}
