package lk.icbt.cis6003.dental.server.bootstrap;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.common.enums.Gender;
import lk.icbt.cis6003.dental.common.enums.PaymentMethod;
import lk.icbt.cis6003.dental.server.config.ClinicProperties;
import lk.icbt.cis6003.dental.server.domain.Appointment;
import lk.icbt.cis6003.dental.server.domain.Dentist;
import lk.icbt.cis6003.dental.server.domain.Invoice;
import lk.icbt.cis6003.dental.server.domain.InvoiceLine;
import lk.icbt.cis6003.dental.server.domain.Patient;
import lk.icbt.cis6003.dental.server.domain.Treatment;
import lk.icbt.cis6003.dental.server.repository.AppointmentRepository;
import lk.icbt.cis6003.dental.server.repository.DentistRepository;
import lk.icbt.cis6003.dental.server.repository.InvoiceRepository;
import lk.icbt.cis6003.dental.server.repository.PatientRepository;
import lk.icbt.cis6003.dental.server.repository.TreatmentRepository;
import lk.icbt.cis6003.dental.server.service.SequenceGeneratorService;
import lk.icbt.cis6003.dental.server.service.pricing.PricingContext;
import lk.icbt.cis6003.dental.server.service.pricing.PricingResult;
import lk.icbt.cis6003.dental.server.service.pricing.PricingStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Populates a fresh database with a plausible trading history.
 *
 * <p><b>Why this exists.</b> Five management reports and a dashboard cannot be
 * demonstrated, or meaningfully tested, against an empty database - every chart
 * reads zero and every ageing band is blank. This runner creates roughly six
 * weeks of past visits and three weeks of forward bookings so that the reports
 * show real distributions the moment the application starts.</p>
 *
 * <p><b>It writes through the repositories, not the services, on purpose.</b>
 * The booking validation chain correctly refuses appointments in the past;
 * demo history needs exactly that. Bypassing the service tier here is a
 * deliberate, contained exception rather than a weakening of the rules - the
 * rules still apply to every real booking.</p>
 *
 * <p>The random generator is seeded with a fixed value so every developer,
 * every CI run and every marker sees the same data. Reproducibility matters
 * more than novelty for a demonstration.</p>
 *
 * <p>Disable with {@code clinic.demo.seed-enabled=false}. It is switched off
 * automatically in the test profile.</p>
 */
@Component
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** Fixed seed: the demonstration must be identical on every machine. */
    private static final long RANDOM_SEED = 20260808L;

    private static final String[] FIRST_NAMES = {
        "Nimal", "Kamala", "Sunil", "Priyanka", "Ruwan", "Chamari", "Dilshan", "Nadeeka",
        "Sanjeewa", "Ishara", "Thilina", "Malsha", "Roshan", "Gayani", "Buddhika", "Sewwandi",
        "Kasun", "Hiruni", "Lahiru", "Amaya", "Chathura", "Nethmi", "Dinesh", "Sachini",
        "Pradeep", "Tharushi", "Asela", "Nilanka", "Mahesh", "Upeksha", "Janaka", "Shanika",
        "Rasika", "Kavindi", "Suresh", "Dinusha", "Anura", "Piyumi", "Charith", "Oshadi"
    };

    private static final String[] LAST_NAMES = {
        "Perera", "Fernando", "Silva", "Jayawardena", "Wickramasinghe", "Bandara", "Rajapaksa",
        "Gunasekara", "Dissanayake", "Herath", "Senanayake", "Ratnayake", "Weerasinghe",
        "Amarasinghe", "Karunaratne", "Ekanayake", "Abeywardena", "Samaraweera"
    };

    private static final String[] STREETS = {
        "Galle Road", "Duplication Road", "Marine Drive", "Havelock Road", "Baseline Road",
        "Nawala Road", "Kandy Road", "High Level Road", "Old Moor Street", "Bauddhaloka Mawatha"
    };

    private static final String[] CITIES = {
        "Colombo 03", "Colombo 05", "Colombo 06", "Dehiwala", "Mount Lavinia",
        "Nugegoda", "Rajagiriya", "Battaramulla", "Maharagama", "Kotte"
    };

    private final PatientRepository patientRepository;
    private final DentistRepository dentistRepository;
    private final TreatmentRepository treatmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final SequenceGeneratorService sequenceGenerator;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final ClinicProperties properties;

    public DemoDataSeeder(PatientRepository patientRepository,
                          DentistRepository dentistRepository,
                          TreatmentRepository treatmentRepository,
                          AppointmentRepository appointmentRepository,
                          InvoiceRepository invoiceRepository,
                          SequenceGeneratorService sequenceGenerator,
                          PricingStrategyFactory pricingStrategyFactory,
                          ClinicProperties properties) {
        this.patientRepository = patientRepository;
        this.dentistRepository = dentistRepository;
        this.treatmentRepository = treatmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.sequenceGenerator = sequenceGenerator;
        this.pricingStrategyFactory = pricingStrategyFactory;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.getDemo().isSeedEnabled()) {
            log.info("Demonstration data seeding is disabled (clinic.demo.seed-enabled=false)");
            return;
        }
        if (appointmentRepository.count() > 0) {
            log.info("Appointments already exist - skipping demonstration data seeding");
            return;
        }

        List<Dentist> dentists = dentistRepository.findByActiveTrueOrderByFullNameAsc();
        List<Treatment> treatments = treatmentRepository.findByActiveTrueOrderByNameAsc();
        if (dentists.isEmpty() || treatments.isEmpty()) {
            log.warn("Reference data is missing - cannot seed demonstration appointments");
            return;
        }

        Random random = new Random(RANDOM_SEED);
        List<Patient> patients = seedPatients(random, properties.getDemo().getPatientCount());

        int appointments = seedAppointments(random, patients, dentists, treatments);

        log.info("Demonstration data seeded: {} patients, {} appointments, {} bills",
                 patients.size(), appointments, invoiceRepository.count());
    }

    /* ------------------------------------------------------------------ */
    /* Patients                                                            */
    /* ------------------------------------------------------------------ */

    private List<Patient> seedPatients(Random random, int count) {
        List<Patient> patients = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String last = LAST_NAMES[random.nextInt(LAST_NAMES.length)];

            Patient patient = new Patient();
            patient.setPatientCode(sequenceGenerator.nextPatientCode());
            patient.setFullName(first + " " + last);
            patient.setAddress(String.format("No. %d, %s, %s",
                    10 + random.nextInt(300),
                    STREETS[random.nextInt(STREETS.length)],
                    CITIES[random.nextInt(CITIES.length)]));
            patient.setContactNumber(String.format("07%d%07d",
                    random.nextInt(8), random.nextInt(10_000_000)));

            // Roughly one patient in three supplies an e-mail address, which is
            // realistic and makes the notification log show both channels.
            if (random.nextInt(3) == 0) {
                patient.setEmail((first + "." + last).toLowerCase() + i + "@example.lk");
            }

            patient.setGender(random.nextBoolean() ? Gender.MALE : Gender.FEMALE);

            // A deliberate age spread: about 15% children and 15% seniors, so the
            // concession rules in the pricing strategies are actually exercised.
            int age = switch (random.nextInt(100) / 15) {
                case 0 -> 4 + random.nextInt(14);      // child, under 18
                case 1 -> 66 + random.nextInt(20);     // senior, 65+
                default -> 19 + random.nextInt(45);    // adult
            };
            patient.setDateOfBirth(LocalDate.now().minusYears(age).minusDays(random.nextInt(365)));
            patient.setNic(String.format("%09d%s", random.nextInt(1_000_000_000),
                    random.nextBoolean() ? "V" : "X"));

            patients.add(patientRepository.save(patient));
        }
        return patients;
    }

    /* ------------------------------------------------------------------ */
    /* Appointments and bills                                              */
    /* ------------------------------------------------------------------ */

    private int seedAppointments(Random random, List<Patient> patients,
                                 List<Dentist> dentists, List<Treatment> treatments) {

        LocalDate start = LocalDate.now().minusDays(properties.getDemo().getPastDays());
        LocalDate end = LocalDate.now().plusDays(properties.getDemo().getFutureDays());
        int created = 0;

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            // Sunday is quiet; the clinic runs an emergency service only.
            int appointmentsToday = day.getDayOfWeek() == java.time.DayOfWeek.SUNDAY
                    ? 1 + random.nextInt(3)
                    : 4 + random.nextInt(7);

            for (int i = 0; i < appointmentsToday; i++) {
                Dentist dentist = dentists.get(random.nextInt(dentists.size()));
                Treatment treatment = treatments.get(random.nextInt(treatments.size()));
                Patient patient = patients.get(random.nextInt(patients.size()));

                LocalTime time = pickSlot(random, dentist, treatment);
                if (time == null) {
                    continue;
                }

                // The database refuses a clash; skipping is cheaper than
                // tracking every allocated slot in memory.
                if (appointmentRepository.isSlotTaken(dentist.getDentistCode(), day, time,
                        List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED,
                                AppointmentStatus.COMPLETED), null)) {
                    continue;
                }

                AppointmentStatus status = pickStatus(random, day);

                Appointment appointment = Appointment.builder()
                        .appointmentNumber(sequenceGenerator.nextAppointmentNumber())
                        .patient(patient)
                        .dentist(dentist)
                        .treatment(treatment)
                        .appointmentDate(day)
                        .appointmentTime(time)
                        .status(status)
                        .createdBy("reception")
                        .build();

                Appointment saved = appointmentRepository.save(appointment);
                created++;

                if (status == AppointmentStatus.COMPLETED) {
                    seedInvoice(random, saved);
                }
            }
        }
        return created;
    }

    /** A slot inside the dentist's shift that the whole treatment fits into. */
    private LocalTime pickSlot(Random random, Dentist dentist, Treatment treatment) {
        LocalTime earliest = dentist.getWorkStartTime();
        LocalTime latest = dentist.getWorkEndTime().minusMinutes(treatment.getDurationMinutes());
        if (latest.isBefore(earliest)) {
            return null;
        }

        long slots = java.time.Duration.between(earliest, latest).toMinutes()
                / ClinicConstants.SLOT_DURATION_MINUTES;
        if (slots <= 0) {
            return earliest;
        }
        return earliest.plusMinutes(random.nextInt((int) slots + 1)
                * ClinicConstants.SLOT_DURATION_MINUTES);
    }

    /**
     * Past days get a realistic mix of outcomes; future days are still open.
     * The ratios (about 8% cancelled, 5% no-show) mean the no-show rate on the
     * dashboard and the cancellation columns in the workload report are all
     * populated with plausible figures.
     */
    private AppointmentStatus pickStatus(Random random, LocalDate day) {
        if (day.isAfter(LocalDate.now())) {
            return random.nextInt(10) < 6 ? AppointmentStatus.CONFIRMED : AppointmentStatus.SCHEDULED;
        }
        if (day.isEqual(LocalDate.now())) {
            int roll = random.nextInt(10);
            if (roll < 4) {
                return AppointmentStatus.COMPLETED;
            }
            return roll < 8 ? AppointmentStatus.CONFIRMED : AppointmentStatus.SCHEDULED;
        }

        int roll = random.nextInt(100);
        if (roll < 8) {
            return AppointmentStatus.CANCELLED;
        }
        if (roll < 13) {
            return AppointmentStatus.NO_SHOW;
        }
        return AppointmentStatus.COMPLETED;
    }

    /**
     * Bills a completed visit using the real pricing strategies, so the demo
     * figures are the ones the live system would have produced.
     */
    private void seedInvoice(Random random, Appointment appointment) {
        var strategy = pricingStrategyFactory.resolve(appointment.getTreatment().getPricingStrategyKey());

        // A discount is approved on roughly one bill in six.
        BigDecimal requestedDiscount = random.nextInt(6) == 0
                ? BigDecimal.valueOf(5L * (1 + random.nextInt(3)))
                : BigDecimal.ZERO;

        PricingResult pricing = strategy.calculate(PricingContext.builder()
                .treatmentBasePrice(appointment.getTreatment().getBasePrice())
                .consultationFee(appointment.getDentist().getConsultationFee())
                .requestedDiscountPercentage(requestedDiscount)
                .taxRate(ClinicConstants.VAT_RATE)
                .patientIsMinor(appointment.getPatient().isMinor())
                .patientIsSeniorCitizen(appointment.getPatient().isSeniorCitizen())
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .treatmentName(appointment.getTreatment().getName())
                .treatmentCategory(appointment.getTreatment().getCategory())
                .build());

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(sequenceGenerator.nextInvoiceNumber());
        invoice.setAppointment(appointment);
        invoice.setPatientName(appointment.getPatient().getFullName());
        invoice.setPatientAddress(appointment.getPatient().getAddress());
        invoice.setPatientContact(appointment.getPatient().getContactNumber());
        invoice.setDentistName(appointment.getDentist().getFullName());
        invoice.setTreatmentName(appointment.getTreatment().getName());

        invoice.setConsultationFee(pricing.getConsultationFee());
        invoice.setTreatmentCost(pricing.getTreatmentCost());
        invoice.setSurchargeAmount(pricing.getSurchargeAmount());
        invoice.setSubTotal(pricing.getSubTotal());
        invoice.setDiscountPercentage(pricing.getDiscountPercentage());
        invoice.setDiscountAmount(pricing.getDiscountAmount());
        invoice.setDiscountReason(pricing.getDiscountReason());
        invoice.setTaxableAmount(pricing.getTaxableAmount());
        invoice.setTaxRate(pricing.getTaxRate());
        invoice.setTaxAmount(pricing.getTaxAmount());
        invoice.setTotalAmount(pricing.getTotalAmount());
        invoice.setPricingStrategyApplied(pricing.getStrategyKey());
        invoice.setIssuedDate(appointment.getAppointmentDate());
        invoice.setIssuedBy("reception");

        for (InvoiceLine line : pricing.getLines()) {
            invoice.addLine(new InvoiceLine(line.getDescription(), line.getQuantity(),
                                            line.getUnitPrice(), line.getLineType()));
        }

        // About 78% settled in full, 10% part paid, 12% still outstanding - so
        // the debtor ageing report has entries in every band.
        int roll = random.nextInt(100);
        if (roll < 78) {
            invoice.applyPayment(invoice.getTotalAmount(), randomMethod(random), null);
        } else if (roll < 88) {
            BigDecimal half = invoice.getTotalAmount()
                    .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
            if (half.compareTo(BigDecimal.ZERO) > 0) {
                invoice.applyPayment(half, randomMethod(random), null);
            }
        }

        invoiceRepository.save(invoice);
    }

    private PaymentMethod randomMethod(Random random) {
        PaymentMethod[] methods = PaymentMethod.values();
        return methods[random.nextInt(methods.length)];
    }
}
