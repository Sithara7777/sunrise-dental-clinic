package lk.icbt.cis6003.dental.server.testsupport;

import lk.icbt.cis6003.dental.common.dto.AppointmentRequest;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.common.enums.Gender;
import lk.icbt.cis6003.dental.server.domain.Appointment;
import lk.icbt.cis6003.dental.server.domain.Dentist;
import lk.icbt.cis6003.dental.server.domain.Patient;
import lk.icbt.cis6003.dental.server.domain.Treatment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The project's test data, in one place - Task C, "devise your test data".
 *
 * <p><b>Why a factory rather than fixtures inside each test.</b> Building a
 * valid {@link Appointment} needs a patient, a dentist and a treatment, each
 * with mandatory fields. Repeating that in forty tests buries the one line
 * that actually matters under fifteen lines of scaffolding, and a test whose
 * intent is invisible is a test nobody maintains.</p>
 *
 * <p>Every method returns a <em>valid</em> object with sensible defaults; a
 * test then changes only the single field it is about. When a test reads
 * {@code dentist.setWorkEndTime(LocalTime.of(14, 0))}, the reader knows
 * immediately that shift hours are the subject.</p>
 *
 * <p>The named ages are deliberate: {@link #seniorPatient()} and
 * {@link #childPatient()} exist so the concession branches in the pricing
 * strategies are exercised by data that is obviously chosen for that purpose.</p>
 */
public final class TestDataFactory {

    private TestDataFactory() {
        throw new AssertionError("TestDataFactory is a static factory and must not be instantiated");
    }

    /* ------------------------------------------------------------------ */
    /* Dentists                                                            */
    /* ------------------------------------------------------------------ */

    /** Works 08:00-16:00, consultation Rs. 1,500. The everyday case. */
    public static Dentist dentist() {
        Dentist dentist = new Dentist("DEN-001", "Nimal Perera", "General Dentistry",
                                      "0112573101", new BigDecimal("1500.00"));
        dentist.setId(1L);
        dentist.setWorkStartTime(LocalTime.of(8, 0));
        dentist.setWorkEndTime(LocalTime.of(16, 0));
        dentist.setActive(true);
        return dentist;
    }

    /** Works 08:00-14:00 - the short shift, for the working-hours rule. */
    public static Dentist partTimeDentist() {
        Dentist dentist = dentist();
        dentist.setDentistCode("DEN-004");
        dentist.setFullName("Dilini Jayawardena");
        dentist.setWorkEndTime(LocalTime.of(14, 0));
        return dentist;
    }

    /** No longer practising - bookings against them must be refused. */
    public static Dentist retiredDentist() {
        Dentist dentist = dentist();
        dentist.setDentistCode("DEN-099");
        dentist.setFullName("Retired Dentist");
        dentist.setActive(false);
        return dentist;
    }

    /* ------------------------------------------------------------------ */
    /* Treatments - one per pricing rule                                   */
    /* ------------------------------------------------------------------ */

    /** Rs. 6,500, 45 minutes, STANDARD rule. */
    public static Treatment standardTreatment() {
        Treatment treatment = new Treatment("SCALING", "Scaling and Polishing", "Preventive",
                                            new BigDecimal("6500.00"), 45, "STANDARD");
        treatment.setId(1L);
        treatment.setActive(true);
        return treatment;
    }

    /** Rs. 25,000, 90 minutes, SURGICAL rule - carries the sterilisation surcharge. */
    public static Treatment surgicalTreatment() {
        Treatment treatment = new Treatment("SURGEXT", "Surgical Extraction", "Surgical",
                                            new BigDecimal("25000.00"), 90, "SURGICAL");
        treatment.setId(2L);
        treatment.setActive(true);
        return treatment;
    }

    /** Rs. 28,000, 60 minutes, COSMETIC rule - excluded from age concessions. */
    public static Treatment cosmeticTreatment() {
        Treatment treatment = new Treatment("WHITEN", "Teeth Whitening", "Cosmetic",
                                            new BigDecimal("28000.00"), 60, "COSMETIC");
        treatment.setId(3L);
        treatment.setActive(true);
        return treatment;
    }

    /** Rs. 5,000, 30 minutes, EMERGENCY rule - out-of-hours loading. */
    public static Treatment emergencyTreatment() {
        Treatment treatment = new Treatment("EMERG", "Emergency Pain Relief", "Emergency",
                                            new BigDecimal("5000.00"), 30, "EMERGENCY");
        treatment.setId(4L);
        treatment.setActive(true);
        return treatment;
    }

    /** A treatment whose pricing_strategy value is nonsense - the fallback case. */
    public static Treatment treatmentWithUnknownRule() {
        Treatment treatment = standardTreatment();
        treatment.setPricingStrategyKey("NOT_A_REAL_RULE");
        return treatment;
    }

    /* ------------------------------------------------------------------ */
    /* Patients - ages chosen to exercise the concession branches          */
    /* ------------------------------------------------------------------ */

    /** 35 years old: no age concession. */
    public static Patient adultPatient() {
        Patient patient = new Patient("PAT-000001", "Kamala Perera",
                                      "No. 45, Galle Road, Colombo 03", "0771234567");
        patient.setId(1L);
        patient.setEmail("kamala.perera@example.lk");
        patient.setGender(Gender.FEMALE);
        patient.setDateOfBirth(LocalDate.now().minusYears(35));
        return patient;
    }

    /** 70 years old: qualifies for the 10% senior concession. */
    public static Patient seniorPatient() {
        Patient patient = adultPatient();
        patient.setId(2L);
        patient.setPatientCode("PAT-000002");
        patient.setFullName("Sunil Fernando");
        patient.setDateOfBirth(LocalDate.now().minusYears(70));
        return patient;
    }

    /** 10 years old: qualifies for the 5% child concession. */
    public static Patient childPatient() {
        Patient patient = adultPatient();
        patient.setId(3L);
        patient.setPatientCode("PAT-000003");
        patient.setFullName("Amaya Silva");
        patient.setDateOfBirth(LocalDate.now().minusYears(10));
        return patient;
    }

    /** No date of birth recorded - neither concession may be assumed. */
    public static Patient patientWithUnknownAge() {
        Patient patient = adultPatient();
        patient.setId(4L);
        patient.setPatientCode("PAT-000004");
        patient.setDateOfBirth(null);
        return patient;
    }

    /* ------------------------------------------------------------------ */
    /* Appointments                                                        */
    /* ------------------------------------------------------------------ */

    /**
     * A valid future appointment: tomorrow at 10:00, SCHEDULED.
     *
     * <p>The id is set because these fixtures stand in for <em>persisted</em>
     * appointments. It matters for rescheduling: the service passes the id to
     * the validation chain so the appointment being moved does not report
     * itself as occupying the slot it is trying to leave, and a fixture with a
     * null id would make that rule untestable.</p>
     */
    public static Appointment appointment() {
        Appointment appointment = Appointment.builder()
                .appointmentNumber("APT-2026-000001")
                .patient(adultPatient())
                .dentist(dentist())
                .treatment(standardTreatment())
                .appointmentDate(LocalDate.now().plusDays(1))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.SCHEDULED)
                .createdBy("reception")
                .build();
        appointment.setId(1L);
        return appointment;
    }

    /** A completed visit - the only state that may be billed. */
    public static Appointment completedAppointment() {
        Appointment appointment = appointment();
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointment;
    }

    /**
     * A valid booking request. Callers override the one field under test:
     * {@code request().appointmentTime(LocalTime.of(21, 0)).build()}.
     */
    public static AppointmentRequest.Builder request() {
        return AppointmentRequest.builder()
                .patientName("Kamala Perera")
                .address("No. 45, Galle Road, Colombo 03")
                .contactNumber("0771234567")
                .email("kamala.perera@example.lk")
                .dentistCode("DEN-001")
                .treatmentCode("SCALING")
                .appointmentDate(LocalDate.now().plusDays(7))
                .appointmentTime(LocalTime.of(10, 0));
    }

    /**
     * A weekday, so tests of the out-of-hours emergency loading are not made
     * to pass or fail by whichever day of the week the suite happens to run.
     */
    public static LocalDate nextWeekday() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    /** A Saturday, for the weekend branch of the emergency rule. */
    public static LocalDate nextSaturday() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
