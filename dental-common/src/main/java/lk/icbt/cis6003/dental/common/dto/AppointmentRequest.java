package lk.icbt.cis6003.dental.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.enums.Gender;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * "Register New Appointment" - the single form described in the scenario.
 *
 * <p>The scenario says the system must collect appointment number, patient
 * name, address, contact number, dentist name, treatment type, appointment
 * date and time. Two design decisions follow from that wording:</p>
 *
 * <ol>
 *   <li><b>The appointment number is not an input.</b> Letting a receptionist
 *       type it is exactly how the paper system produced duplicates. The
 *       server generates {@code APT-yyyy-nnnnnn} and returns it, so uniqueness
 *       is guaranteed by the database rather than by human care.</li>
 *   <li><b>{@code patientCode} is optional.</b> Leave it blank and the inline
 *       name/address/contact fields register a brand new patient in the same
 *       transaction; supply it and the existing patient record is reused. This
 *       covers "new patients must be registered" without creating a duplicate
 *       record every time a returning patient books.</li>
 * </ol>
 *
 * <p>The nested {@link Builder} is a worked example of the Builder pattern:
 * eight-plus optional fields make a telescoping constructor unreadable, and
 * the client code becomes self-documenting at the call site.</p>
 */
public class AppointmentRequest {

    /* --------------------------- patient ------------------------------- */

    /** Existing patient. When blank, a new patient is registered from the fields below. */
    @Size(max = 20, message = "Patient code must not exceed 20 characters")
    private String patientCode;

    @NotBlank(message = "Patient name is required")
    @Pattern(regexp = ClinicConstants.PERSON_NAME_PATTERN,
             message = "Patient name may only contain letters, spaces, apostrophes, dots and hyphens")
    @Size(max = 100, message = "Patient name must not exceed 100 characters")
    private String patientName;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
    private String address;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = ClinicConstants.CONTACT_NUMBER_PATTERN,
             message = "Contact number must be a valid Sri Lankan number, e.g. 0771234567 or +94771234567")
    private String contactNumber;

    /** Optional - only used to send the confirmation e-mail. */
    @Email(message = "E-mail address is not valid")
    @Size(max = 120, message = "E-mail must not exceed 120 characters")
    private String email;

    @Pattern(regexp = "^$|" + ClinicConstants.NIC_PATTERN,
             message = "NIC must be 9 digits followed by V/X, or 12 digits")
    private String nic;

    private Gender gender;

    private LocalDate dateOfBirth;

    /* --------------------------- booking ------------------------------- */

    @NotBlank(message = "Dentist must be selected")
    private String dentistCode;

    @NotBlank(message = "Treatment type must be selected")
    private String treatmentCode;

    @NotNull(message = "Appointment date is required")
    @Future(message = "Appointment date must be in the future")
    private LocalDate appointmentDate;

    @NotNull(message = "Appointment time is required")
    private LocalTime appointmentTime;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    public AppointmentRequest() {
        // required by Jackson
    }

    /* --------------------------- accessors ----------------------------- */

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
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

    public String getDentistCode() {
        return dentistCode;
    }

    public void setDentistCode(String dentistCode) {
        this.dentistCode = dentistCode;
    }

    public String getTreatmentCode() {
        return treatmentCode;
    }

    public void setTreatmentCode(String treatmentCode) {
        this.treatmentCode = treatmentCode;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /** True when the request should also create a new patient record. */
    public boolean isNewPatient() {
        return patientCode == null || patientCode.isBlank();
    }

    @Override
    public String toString() {
        return "AppointmentRequest{patient='" + patientName + "', dentist='" + dentistCode
                + "', treatment='" + treatmentCode + "', when=" + appointmentDate + " " + appointmentTime + "}";
    }

    /* ------------------------------------------------------------------ */
    /* Builder pattern                                                     */
    /* ------------------------------------------------------------------ */

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link AppointmentRequest}.
     *
     * <p>Used by the Swing client and heavily by the unit tests, where being
     * able to state only the two or three fields a test actually cares about
     * keeps the test's intent visible.</p>
     */
    public static final class Builder {

        private final AppointmentRequest target = new AppointmentRequest();

        private Builder() {
        }

        public Builder patientCode(String patientCode) {
            target.patientCode = patientCode;
            return this;
        }

        public Builder patientName(String patientName) {
            target.patientName = patientName;
            return this;
        }

        public Builder address(String address) {
            target.address = address;
            return this;
        }

        public Builder contactNumber(String contactNumber) {
            target.contactNumber = contactNumber;
            return this;
        }

        public Builder email(String email) {
            target.email = email;
            return this;
        }

        public Builder nic(String nic) {
            target.nic = nic;
            return this;
        }

        public Builder gender(Gender gender) {
            target.gender = gender;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            target.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder dentistCode(String dentistCode) {
            target.dentistCode = dentistCode;
            return this;
        }

        public Builder treatmentCode(String treatmentCode) {
            target.treatmentCode = treatmentCode;
            return this;
        }

        public Builder appointmentDate(LocalDate appointmentDate) {
            target.appointmentDate = appointmentDate;
            return this;
        }

        public Builder appointmentTime(LocalTime appointmentTime) {
            target.appointmentTime = appointmentTime;
            return this;
        }

        public Builder notes(String notes) {
            target.notes = notes;
            return this;
        }

        public AppointmentRequest build() {
            return target;
        }
    }
}
