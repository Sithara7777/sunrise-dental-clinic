package lk.icbt.cis6003.dental.server.mapper;

import lk.icbt.cis6003.dental.common.dto.TreatmentDto;
import lk.icbt.cis6003.dental.server.domain.Treatment;
import org.springframework.stereotype.Component;

import java.util.List;

/** Translates between the {@link Treatment} entity and its DTO. */
@Component
public class TreatmentMapper {

    public TreatmentDto toDto(Treatment entity) {
        if (entity == null) {
            return null;
        }
        TreatmentDto dto = new TreatmentDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setBasePrice(entity.getBasePrice());
        dto.setDurationMinutes(entity.getDurationMinutes());
        dto.setPricingStrategy(entity.getPricingStrategyKey());
        dto.setActive(entity.isActive());
        return dto;
    }

    public List<TreatmentDto> toDtoList(List<Treatment> entities) {
        return entities.stream().map(this::toDto).toList();
    }

    public void applyToEntity(TreatmentDto dto, Treatment entity) {
        entity.setName(dto.getName() == null ? null : dto.getName().trim());
        entity.setDescription(dto.getDescription());
        entity.setCategory(dto.getCategory());
        entity.setBasePrice(dto.getBasePrice());
        entity.setDurationMinutes(dto.getDurationMinutes());
        entity.setPricingStrategyKey(dto.getPricingStrategy());
        entity.setActive(dto.isActive());
    }
}
