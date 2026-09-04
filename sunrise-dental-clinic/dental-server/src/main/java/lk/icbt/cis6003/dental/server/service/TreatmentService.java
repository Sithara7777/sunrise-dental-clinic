package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.common.dto.TreatmentDto;
import lk.icbt.cis6003.dental.server.domain.Treatment;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.exception.DuplicateResourceException;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import lk.icbt.cis6003.dental.server.exception.ResourceNotFoundException;
import lk.icbt.cis6003.dental.server.mapper.TreatmentMapper;
import lk.icbt.cis6003.dental.server.repository.TreatmentRepository;
import lk.icbt.cis6003.dental.server.service.pricing.PricingStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Maintenance of the treatment catalogue - the scenario's "treatment type".
 *
 * <p>Validates the pricing-strategy key against the strategies actually
 * registered. Saving {@code "SURGCAL"} would otherwise be accepted silently
 * and only reveal itself as a missing surcharge weeks later, on a bill that
 * nobody could explain.</p>
 */
@Service
@Transactional(readOnly = true)
public class TreatmentService {

    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper treatmentMapper;
    private final PricingStrategyFactory pricingStrategyFactory;

    public TreatmentService(TreatmentRepository treatmentRepository,
                            TreatmentMapper treatmentMapper,
                            PricingStrategyFactory pricingStrategyFactory) {
        this.treatmentRepository = treatmentRepository;
        this.treatmentMapper = treatmentMapper;
        this.pricingStrategyFactory = pricingStrategyFactory;
    }

    @Transactional
    public TreatmentDto create(TreatmentDto dto) {
        String code = normaliseCode(dto.getCode());
        if (treatmentRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Treatment", code);
        }
        requireKnownPricingStrategy(dto.getPricingStrategy());

        Treatment treatment = new Treatment();
        treatmentMapper.applyToEntity(dto, treatment);
        treatment.setCode(code);
        return treatmentMapper.toDto(treatmentRepository.save(treatment));
    }

    @Transactional
    public TreatmentDto update(String code, TreatmentDto dto) {
        Treatment treatment = requireByCode(code);
        requireKnownPricingStrategy(dto.getPricingStrategy());
        treatmentMapper.applyToEntity(dto, treatment);
        return treatmentMapper.toDto(treatmentRepository.save(treatment));
    }

    /**
     * Withdraws a treatment from the catalogue.
     *
     * <p>Deactivated, never deleted: historic appointments and bills reference
     * it, and a receipt that cannot name what the patient paid for is not a
     * receipt.</p>
     */
    @Transactional
    public TreatmentDto deactivate(String code) {
        Treatment treatment = requireByCode(code);
        treatment.setActive(false);
        return treatmentMapper.toDto(treatmentRepository.save(treatment));
    }

    @Transactional
    public TreatmentDto reactivate(String code) {
        Treatment treatment = requireByCode(code);
        treatment.setActive(true);
        return treatmentMapper.toDto(treatmentRepository.save(treatment));
    }

    public TreatmentDto findByCode(String code) {
        return treatmentMapper.toDto(requireByCode(code));
    }

    public Treatment requireByCode(String code) {
        return treatmentRepository.findByCode(normaliseCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", code));
    }

    /** Bookable treatments - what the booking form's drop-down shows. */
    public List<TreatmentDto> listActive() {
        return treatmentMapper.toDtoList(treatmentRepository.findByActiveTrueOrderByNameAsc());
    }

    public List<TreatmentDto> listAll() {
        return treatmentMapper.toDtoList(treatmentRepository.findAllByOrderByCategoryAscNameAsc());
    }

    public List<TreatmentDto> listByCategory(String category) {
        return treatmentMapper.toDtoList(
                treatmentRepository.findByCategoryAndActiveTrueOrderByNameAsc(category));
    }

    public long countActive() {
        return treatmentRepository.countByActiveTrue();
    }

    /** The keys a treatment may be saved with, for the maintenance drop-down. */
    public List<String> availablePricingStrategies() {
        return List.copyOf(pricingStrategyFactory.getSupportedKeys());
    }

    private void requireKnownPricingStrategy(String key) {
        if (!pricingStrategyFactory.isSupported(key)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "'" + key + "' is not a recognised pricing rule. Choose one of "
                            + pricingStrategyFactory.getSupportedKeys() + ".");
        }
    }

    private String normaliseCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
