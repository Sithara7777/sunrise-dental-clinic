package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.common.dto.AppointmentRequest;
import lk.icbt.cis6003.dental.common.dto.PageResponse;
import lk.icbt.cis6003.dental.common.dto.PatientDto;
import lk.icbt.cis6003.dental.common.enums.Gender;
import lk.icbt.cis6003.dental.server.domain.Patient;
import lk.icbt.cis6003.dental.server.exception.DuplicateResourceException;
import lk.icbt.cis6003.dental.server.exception.ResourceNotFoundException;
import lk.icbt.cis6003.dental.server.mapper.PatientMapper;
import lk.icbt.cis6003.dental.server.repository.AppointmentRepository;
import lk.icbt.cis6003.dental.server.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Patient master-file management - "new patients must be registered in the
 * system".
 *
 * <p>The interesting method is {@link #resolveOrRegister(AppointmentRequest)},
 * which is what stops the new system recreating the old one's central defect.
 * The paper system wrote the patient's name and address on every visit slip, so
 * "Mrs Perera" existed dozens of times with no link between them and no history
 * anyone could retrieve. Here, a booking either attaches to an existing patient
 * or creates exactly one new record - never a duplicate.</p>
 */
@Service
@Transactional(readOnly = true)
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientMapper patientMapper;
    private final SequenceGeneratorService sequenceGenerator;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          PatientMapper patientMapper,
                          SequenceGeneratorService sequenceGenerator) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientMapper = patientMapper;
        this.sequenceGenerator = sequenceGenerator;
    }

    /* ------------------------------------------------------------------ */
    /* Registration                                                        */
    /* ------------------------------------------------------------------ */

    /**
     * Registers a brand-new patient and assigns their permanent code.
     *
     * @throws DuplicateResourceException when the NIC is already on file - the
     *         one identifier that is genuinely unique to a person
     */
    @Transactional
    public PatientDto register(PatientDto dto) {
        if (dto.getNic() != null && !dto.getNic().isBlank()
                && patientRepository.existsByNicIgnoreCase(dto.getNic().trim())) {
            throw new DuplicateResourceException(
                    "A patient with NIC '" + dto.getNic() + "' is already registered. "
                            + "Search for them by NIC rather than creating a second record.");
        }

        Patient patient = new Patient();
        patientMapper.applyToEntity(dto, patient);
        patient.setPatientCode(sequenceGenerator.nextPatientCode());

        Patient saved = patientRepository.save(patient);
        log.info("Registered new patient {} ({})", saved.getPatientCode(), saved.getFullName());
        return toDtoWithVisits(saved);
    }

    /**
     * Finds the patient a booking refers to, registering them if they are new.
     *
     * <p>Three cases, in order of confidence:</p>
     * <ol>
     *   <li>an explicit patient code - the receptionist picked from a list;</li>
     *   <li>an NIC match - the strongest evidence two records are one person;</li>
     *   <li>an exact name <em>and</em> telephone match - the practical check a
     *       receptionist can perform at the desk, since most patients do not
     *       carry their NIC to a dental appointment.</li>
     * </ol>
     *
     * <p>Name alone is deliberately never enough. Merging two different
     * patients' clinical histories because they share a common Sri Lankan
     * surname would be far worse than holding one duplicate record.</p>
     */
    @Transactional
    public Patient resolveOrRegister(AppointmentRequest request) {
        if (request.getPatientCode() != null && !request.getPatientCode().isBlank()) {
            return patientRepository.findByPatientCode(request.getPatientCode().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", request.getPatientCode()));
        }

        if (request.getNic() != null && !request.getNic().isBlank()) {
            var byNic = patientRepository.findByNicIgnoreCase(request.getNic().trim());
            if (byNic.isPresent()) {
                log.debug("Booking matched existing patient {} by NIC", byNic.get().getPatientCode());
                return byNic.get();
            }
        }

        String contact = request.getContactNumber() == null
                ? null : request.getContactNumber().replaceAll("[\\s()-]", "");
        if (request.getPatientName() != null && contact != null) {
            var byNameAndPhone = patientRepository
                    .findFirstByFullNameIgnoreCaseAndContactNumber(request.getPatientName().trim(), contact);
            if (byNameAndPhone.isPresent()) {
                log.debug("Booking matched existing patient {} by name and telephone",
                          byNameAndPhone.get().getPatientCode());
                return byNameAndPhone.get();
            }
        }

        Patient patient = new Patient();
        patient.setPatientCode(sequenceGenerator.nextPatientCode());
        patient.setFullName(request.getPatientName().trim());
        patient.setAddress(request.getAddress().trim());
        patient.setContactNumber(contact);
        patient.setEmail(blankToNull(request.getEmail()));
        patient.setNic(blankToNull(request.getNic()));
        patient.setGender(request.getGender() == null ? Gender.UNSPECIFIED : request.getGender());
        patient.setDateOfBirth(request.getDateOfBirth());

        Patient saved = patientRepository.save(patient);
        log.info("Registered new patient {} ({}) while booking an appointment",
                 saved.getPatientCode(), saved.getFullName());
        return saved;
    }

    /* ------------------------------------------------------------------ */
    /* Maintenance and lookup                                              */
    /* ------------------------------------------------------------------ */

    @Transactional
    public PatientDto update(String patientCode, PatientDto dto) {
        Patient patient = requireByCode(patientCode);

        if (dto.getNic() != null && !dto.getNic().isBlank()) {
            patientRepository.findByNicIgnoreCase(dto.getNic().trim())
                    .filter(other -> !other.getId().equals(patient.getId()))
                    .ifPresent(other -> {
                        throw new DuplicateResourceException(
                                "NIC '" + dto.getNic() + "' already belongs to patient "
                                        + other.getPatientCode());
                    });
        }

        patientMapper.applyToEntity(dto, patient);
        return toDtoWithVisits(patientRepository.save(patient));
    }

    public PatientDto findByCode(String patientCode) {
        return toDtoWithVisits(requireByCode(patientCode));
    }

    public Patient requireByCode(String patientCode) {
        return patientRepository.findByPatientCode(patientCode)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientCode));
    }

    /** Free-text search across name, code, telephone and NIC. */
    public PageResponse<PatientDto> search(String term, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<Patient> result = patientRepository.search(term == null ? "" : term.trim(), pageable);
        return new PageResponse<>(patientMapper.toDtoList(result.getContent()),
                                  result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public List<PatientDto> listAll() {
        return patientMapper.toDtoList(patientRepository.findAllByOrderByFullNameAsc());
    }

    public long countAll() {
        return patientRepository.count();
    }

    public long countRegisteredSince(LocalDateTime from) {
        return patientRepository.countByCreatedAtGreaterThanEqual(from);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    /** Adds the visit count, which the patient detail screen shows. */
    private PatientDto toDtoWithVisits(Patient patient) {
        PatientDto dto = patientMapper.toDto(patient);
        dto.setTotalVisits(appointmentRepository.countByPatientPatientCode(patient.getPatientCode()));
        return dto;
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        // A client asking for 100 000 rows is a mistake, not a requirement.
        return Math.min(size, 200);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
