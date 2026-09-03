package lk.icbt.cis6003.dental.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lk.icbt.cis6003.dental.common.ClinicConstants;

import java.math.BigDecimal;

/**
 * A dentist practising at the clinic.
 *
 * <p>Consultation fee lives on the dentist rather than being a single clinic
 * constant: the scenario says the bill is "based on treatment type and
 * consultation fee", and a senior orthodontist does not charge a junior's
 * consultation rate. Treatment price + this dentist's consultation fee are the
 * two inputs to the billing calculation.</p>
 */
public class DentistDto {

    private Long id;

    private String dentistCode;

    @NotBlank(message = "Dentist name is required")
    @Pattern(regexp = ClinicConstants.PERSON_NAME_PATTERN,
             message = "Dentist name may only contain letters, spaces, apostrophes, dots and hyphens")
    @Size(max = 100, message = "Dentist name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Specialization is required")
    @Size(max = 80, message = "Specialization must not exceed 80 characters")
    private String specialization;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = ClinicConstants.CONTACT_NUMBER_PATTERN,
             message = "Contact number must be a valid Sri Lankan number")
    private String contactNumber;

    @Email(message = "E-mail address is not valid")
    @Size(max = 120, message = "E-mail must not exceed 120 characters")
    private String email;

    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "0.00", message = "Consultation fee cannot be negative")
    @DecimalMax(value = "100000.00", message = "Consultation fee looks unrealistic")
    private BigDecimal consultationFee;

    @Size(max = 40, message = "SLMC registration number must not exceed 40 characters")
    private String slmcRegistrationNo;

    private boolean active = true;

    public DentistDto() {
        // required by Jackson
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDentistCode() {
        return dentistCode;
    }

    public void setDentistCode(String dentistCode) {
        this.dentistCode = dentistCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getSlmcRegistrationNo() {
        return slmcRegistrationNo;
    }

    public void setSlmcRegistrationNo(String slmcRegistrationNo) {
        this.slmcRegistrationNo = slmcRegistrationNo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Label used by the combo boxes in both UIs. */
    public String getDisplayLabel() {
        return fullName + " (" + specialization + ")";
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}
