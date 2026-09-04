package lk.icbt.cis6003.dental.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Patient master record. Used both as a request body (create / update) and as
 * a response, because the field set is identical in both directions and the
 * server-managed fields ({@code patientCode}, {@code registeredAt},
 * {@code totalVisits}) are simply ignored on input.
 */
public class PatientDto {

    private Long id;

    /** Server generated, e.g. {@code PAT-000042}. Read-only for clients. */
    private String patientCode;

    @NotBlank(message = "Patient name is required")
    @Pattern(regexp = ClinicConstants.PERSON_NAME_PATTERN,
             message = "Patient name may only contain letters, spaces, apostrophes, dots and hyphens")
    @Size(max = 100, message = "Patient name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
    private String address;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = ClinicConstants.CONTACT_NUMBER_PATTERN,
             message = "Contact number must be a valid Sri Lankan number, e.g. 0771234567")
    private String contactNumber;

    @Email(message = "E-mail address is not valid")
    @Size(max = 120, message = "E-mail must not exceed 120 characters")
    private String email;

    @Pattern(regexp = "^$|" + ClinicConstants.NIC_PATTERN,
             message = "NIC must be 9 digits followed by V/X, or 12 digits")
    private String nic;

    private Gender gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Medical notes must not exceed 500 characters")
    private String medicalNotes;

    private LocalDateTime registeredAt;
    private long totalVisits;

    public PatientDto() {
        // required by Jackson
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getMedicalNotes() {
        return medicalNotes;
    }

    public void setMedicalNotes(String medicalNotes) {
        this.medicalNotes = medicalNotes;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public long getTotalVisits() {
        return totalVisits;
    }

    public void setTotalVisits(long totalVisits) {
        this.totalVisits = totalVisits;
    }

    @Override
    public String toString() {
        return patientCode + " - " + fullName;
    }
}
