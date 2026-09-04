package lk.icbt.cis6003.dental.server.mapper;

import lk.icbt.cis6003.dental.common.dto.AppointmentDto;
import lk.icbt.cis6003.dental.server.domain.Appointment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Flattens the {@link Appointment} aggregate into the single row that both
 * user interfaces display.
 *
 * <p>The DTO carries {@code dentistName} and {@code treatmentName} rather than
 * nested objects. A search-by-appointment-number result is read as a form and
 * printed on a receipt; neither needs the dentist's SLMC number or the
 * treatment's pricing strategy, and sending them would be a small, permanent
 * leak of internal structure onto a published contract.</p>
 *
 * <p>Must be called inside the transaction that loaded the appointment - the
 * associations are LAZY, which is why the repository read methods declare an
 * {@code @EntityGraph}.</p>
 */
@Component
public class AppointmentMapper {

    public AppointmentDto toDto(Appointment entity) {
        return toDto(entity, null);
    }

    /**
     * @param invoiceNumber the bill already raised against this visit, or
     *                      {@code null}; supplied by the service so the UI can
     *                      disable "Generate bill" without a second query
     */
    public AppointmentDto toDto(Appointment entity, String invoiceNumber) {
        if (entity == null) {
            return null;
        }

        AppointmentDto dto = new AppointmentDto();
        dto.setId(entity.getId());
        dto.setAppointmentNumber(entity.getAppointmentNumber());

        if (entity.getPatient() != null) {
            dto.setPatientCode(entity.getPatient().getPatientCode());
            dto.setPatientName(entity.getPatient().getFullName());
            dto.setAddress(entity.getPatient().getAddress());
            dto.setContactNumber(entity.getPatient().getContactNumber());
            dto.setPatientEmail(entity.getPatient().getEmail());
        }

        if (entity.getDentist() != null) {
            dto.setDentistCode(entity.getDentist().getDentistCode());
            dto.setDentistName(entity.getDentist().getFullName());
            dto.setDentistSpecialization(entity.getDentist().getSpecialization());
        }

        if (entity.getTreatment() != null) {
            dto.setTreatmentCode(entity.getTreatment().getCode());
            dto.setTreatmentName(entity.getTreatment().getName());
            dto.setTreatmentPrice(entity.getTreatment().getBasePrice());
        }

        dto.setDurationMinutes(entity.getDurationMinutes());
        dto.setAppointmentDate(entity.getAppointmentDate());
        dto.setAppointmentTime(entity.getAppointmentTime());
        dto.setAppointmentEndTime(entity.getEndTime());
        dto.setStatus(entity.getStatus());
        dto.setNotes(entity.getNotes());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setInvoiceNumber(invoiceNumber);
        dto.setInvoiced(invoiceNumber != null);

        return dto;
    }

    public List<AppointmentDto> toDtoList(List<Appointment> entities) {
        return entities.stream().map(e -> toDto(e, null)).toList();
    }
}
