package lk.icbt.cis6003.dental.server.mapper;

import lk.icbt.cis6003.dental.common.dto.InvoiceDto;
import lk.icbt.cis6003.dental.common.dto.InvoiceLineDto;
import lk.icbt.cis6003.dental.server.domain.Invoice;
import lk.icbt.cis6003.dental.server.domain.InvoiceLine;
import org.springframework.stereotype.Component;

import java.util.List;

/** Translates an {@link Invoice} aggregate into the printable bill DTO. */
@Component
public class InvoiceMapper {

    public InvoiceDto toDto(Invoice entity) {
        if (entity == null) {
            return null;
        }

        InvoiceDto dto = new InvoiceDto();
        dto.setId(entity.getId());
        dto.setInvoiceNumber(entity.getInvoiceNumber());

        if (entity.getAppointment() != null) {
            dto.setAppointmentNumber(entity.getAppointment().getAppointmentNumber());
            dto.setAppointmentDate(entity.getAppointment().getAppointmentDate());
            dto.setAppointmentTime(entity.getAppointment().getAppointmentTime());
            if (entity.getAppointment().getPatient() != null) {
                dto.setPatientCode(entity.getAppointment().getPatient().getPatientCode());
            }
        }

        // Taken from the invoice's own copy, not from the live patient record:
        // a historic bill must show the details as they were on the day.
        dto.setPatientName(entity.getPatientName());
        dto.setPatientAddress(entity.getPatientAddress());
        dto.setPatientContact(entity.getPatientContact());
        dto.setDentistName(entity.getDentistName());
        dto.setTreatmentName(entity.getTreatmentName());

        dto.setConsultationFee(entity.getConsultationFee());
        dto.setTreatmentCost(entity.getTreatmentCost());
        dto.setSurchargeAmount(entity.getSurchargeAmount());
        dto.setSubTotal(entity.getSubTotal());
        dto.setDiscountPercentage(entity.getDiscountPercentage());
        dto.setDiscountAmount(entity.getDiscountAmount());
        dto.setDiscountReason(entity.getDiscountReason());
        dto.setTaxableAmount(entity.getTaxableAmount());
        dto.setTaxRate(entity.getTaxRate());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setAmountPaid(entity.getAmountPaid());
        dto.setBalanceDue(entity.getBalanceDue());
        dto.setPricingStrategyApplied(entity.getPricingStrategyApplied());

        dto.setPaymentStatus(entity.getPaymentStatus());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setIssuedBy(entity.getIssuedBy());
        dto.setIssuedAt(entity.getCreatedAt());
        dto.setPaidAt(entity.getPaidAt());
        dto.setRemarks(entity.getRemarks());

        for (InvoiceLine line : entity.getLines()) {
            dto.addLine(toLineDto(line));
        }
        return dto;
    }

    public InvoiceLineDto toLineDto(InvoiceLine line) {
        return new InvoiceLineDto(line.getLineNumber(), line.getDescription(), line.getQuantity(),
                                  line.getUnitPrice(), line.getLineTotal(), line.getLineType());
    }

    public List<InvoiceDto> toDtoList(List<Invoice> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}
