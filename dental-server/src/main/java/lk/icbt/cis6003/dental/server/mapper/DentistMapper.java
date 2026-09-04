package lk.icbt.cis6003.dental.server.mapper;

import lk.icbt.cis6003.dental.common.dto.DentistDto;
import lk.icbt.cis6003.dental.server.domain.Dentist;
import org.springframework.stereotype.Component;

import java.util.List;

/** Translates between the {@link Dentist} entity and its DTO. */
@Component
public class DentistMapper {

    public DentistDto toDto(Dentist entity) {
        if (entity == null) {
            return null;
        }
        DentistDto dto = new DentistDto();
        dto.setId(entity.getId());
        dto.setDentistCode(entity.getDentistCode());
        dto.setFullName(entity.getFullName());
        dto.setSpecialization(entity.getSpecialization());
        dto.setContactNumber(entity.getContactNumber());
        dto.setEmail(entity.getEmail());
        dto.setConsultationFee(entity.getConsultationFee());
        dto.setSlmcRegistrationNo(entity.getSlmcRegistrationNo());
        dto.setActive(entity.isActive());
        return dto;
    }

    public List<DentistDto> toDtoList(List<Dentist> entities) {
        return entities.stream().map(this::toDto).toList();
    }

    /** Working hours are managed on the dedicated roster screen, not here. */
    public void applyToEntity(DentistDto dto, Dentist entity) {
        entity.setFullName(dto.getFullName() == null ? null : dto.getFullName().trim());
        entity.setSpecialization(dto.getSpecialization());
        entity.setContactNumber(dto.getContactNumber() == null
                ? null : dto.getContactNumber().replaceAll("[\\s()-]", ""));
        entity.setEmail(dto.getEmail());
        entity.setConsultationFee(dto.getConsultationFee());
        entity.setSlmcRegistrationNo(dto.getSlmcRegistrationNo());
        entity.setActive(dto.isActive());
    }
}
