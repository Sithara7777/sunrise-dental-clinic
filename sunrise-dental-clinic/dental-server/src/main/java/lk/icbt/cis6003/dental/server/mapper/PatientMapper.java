package lk.icbt.cis6003.dental.server.mapper;

import lk.icbt.cis6003.dental.common.dto.PatientDto;
import lk.icbt.cis6003.dental.common.enums.Gender;
import lk.icbt.cis6003.dental.server.domain.Patient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Translates between the {@link Patient} entity and the {@link PatientDto}
 * that crosses the network.
 *
 * <p>Mapping is explicit and hand-written rather than reflective. Two reasons:
 * the compiler catches a renamed field immediately, and - more importantly -
 * it is impossible for a field added to the entity later to start leaking onto
 * the wire by accident. With a reflective mapper, adding a
 * {@code creditRating} column would silently publish it.</p>
 */
@Component
public class PatientMapper {

    public PatientDto toDto(Patient entity) {
        if (entity == null) {
            return null;
        }
        PatientDto dto = new PatientDto();
        dto.setId(entity.getId());
        dto.setPatientCode(entity.getPatientCode());
        dto.setFullName(entity.getFullName());
        dto.setAddress(entity.getAddress());
        dto.setContactNumber(entity.getContactNumber());
        dto.setEmail(entity.getEmail());
        dto.setNic(entity.getNic());
        dto.setGender(entity.getGender());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setMedicalNotes(entity.getMedicalNotes());
        dto.setRegisteredAt(entity.getCreatedAt());
        return dto;
    }

    public List<PatientDto> toDtoList(List<Patient> entities) {
        return entities.stream().map(this::toDto).toList();
    }

    /**
     * Copies the editable fields onto an existing entity.
     *
     * <p>{@code patientCode}, {@code id} and the audit stamps are deliberately
     * not copied: they are the server's to assign, and honouring them from a
     * request body would let a client rewrite another patient's identity.</p>
     */
    public void applyToEntity(PatientDto dto, Patient entity) {
        entity.setFullName(trim(dto.getFullName()));
        entity.setAddress(trim(dto.getAddress()));
        entity.setContactNumber(normaliseContact(dto.getContactNumber()));
        entity.setEmail(blankToNull(dto.getEmail()));
        entity.setNic(blankToNull(dto.getNic()));
        entity.setGender(dto.getGender() == null ? Gender.UNSPECIFIED : dto.getGender());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setMedicalNotes(blankToNull(dto.getMedicalNotes()));
    }

    /**
     * Strips spaces, brackets and hyphens from a telephone number so that
     * "077 123 4567" and "0771234567" are stored - and therefore matched - as
     * the same value. Without this, patient lookup by phone number quietly
     * fails for anyone who typed it with spaces.
     */
    private String normaliseContact(String value) {
        return value == null ? null : value.replaceAll("[\\s()-]", "");
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
