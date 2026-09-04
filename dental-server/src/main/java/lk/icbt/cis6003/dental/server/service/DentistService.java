package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.common.dto.DentistDto;
import lk.icbt.cis6003.dental.server.domain.Dentist;
import lk.icbt.cis6003.dental.server.exception.ResourceNotFoundException;
import lk.icbt.cis6003.dental.server.mapper.DentistMapper;
import lk.icbt.cis6003.dental.server.repository.DentistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

/** Maintenance of the clinic's dentists. */
@Service
@Transactional(readOnly = true)
public class DentistService {

    private final DentistRepository dentistRepository;
    private final DentistMapper dentistMapper;
    private final SequenceGeneratorService sequenceGenerator;

    public DentistService(DentistRepository dentistRepository,
                          DentistMapper dentistMapper,
                          SequenceGeneratorService sequenceGenerator) {
        this.dentistRepository = dentistRepository;
        this.dentistMapper = dentistMapper;
        this.sequenceGenerator = sequenceGenerator;
    }

    @Transactional
    public DentistDto create(DentistDto dto) {
        Dentist dentist = new Dentist();
        dentistMapper.applyToEntity(dto, dentist);
        dentist.setDentistCode(sequenceGenerator.nextDentistCode());
        return dentistMapper.toDto(dentistRepository.save(dentist));
    }

    @Transactional
    public DentistDto update(String dentistCode, DentistDto dto) {
        Dentist dentist = requireByCode(dentistCode);
        dentistMapper.applyToEntity(dto, dentist);
        return dentistMapper.toDto(dentistRepository.save(dentist));
    }

    /** Sets a dentist's shift, which the booking validation chain enforces. */
    @Transactional
    public DentistDto updateWorkingHours(String dentistCode, LocalTime start, LocalTime end) {
        Dentist dentist = requireByCode(dentistCode);
        if (start == null || end == null || !end.isAfter(start)) {
            throw new lk.icbt.cis6003.dental.server.exception.BusinessException(
                    lk.icbt.cis6003.dental.server.exception.ErrorCode.VALIDATION_ERROR,
                    "The end of a shift must be later than its start.");
        }
        dentist.setWorkStartTime(start);
        dentist.setWorkEndTime(end);
        return dentistMapper.toDto(dentistRepository.save(dentist));
    }

    /**
     * Retires a dentist without deleting them.
     *
     * <p>Deleting would orphan every appointment they ever treated, and a
     * patient's clinical history must record who performed the work.</p>
     */
    @Transactional
    public DentistDto deactivate(String dentistCode) {
        Dentist dentist = requireByCode(dentistCode);
        dentist.setActive(false);
        return dentistMapper.toDto(dentistRepository.save(dentist));
    }

    @Transactional
    public DentistDto reactivate(String dentistCode) {
        Dentist dentist = requireByCode(dentistCode);
        dentist.setActive(true);
        return dentistMapper.toDto(dentistRepository.save(dentist));
    }

    public DentistDto findByCode(String dentistCode) {
        return dentistMapper.toDto(requireByCode(dentistCode));
    }

    public Dentist requireByCode(String dentistCode) {
        return dentistRepository.findByDentistCode(dentistCode)
                .orElseThrow(() -> new ResourceNotFoundException("Dentist", dentistCode));
    }

    /** Only practising dentists - what the booking form's drop-down shows. */
    public List<DentistDto> listActive() {
        return dentistMapper.toDtoList(dentistRepository.findByActiveTrueOrderByFullNameAsc());
    }

    /** Including retired ones - what the maintenance screen shows. */
    public List<DentistDto> listAll() {
        return dentistMapper.toDtoList(dentistRepository.findAllByOrderByFullNameAsc());
    }

    public long countActive() {
        return dentistRepository.countByActiveTrue();
    }
}
