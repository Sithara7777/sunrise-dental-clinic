package lk.icbt.cis6003.dental.server.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lk.icbt.cis6003.dental.common.enums.Gender;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * The patient master record - "new patients must be registered in the system".
 *
 * <p>Separating the patient from the appointment is the single most important
 * modelling decision in the solution. The paper system recorded the patient's
 * name and address on every visit slip, which is exactly why records were
 * "lost": there was no one place that held a patient. Here a patient exists
 * once and every visit points at it, so a patient's full history is a single
 * query.</p>
 */
@Entity
@Table(name = "patient",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_patient_code", columnNames = "patient_code")
       })
public class Patient extends BaseEntity {

    @Column(name = "patient_code", nullable = false, length = 20)
    private String patientCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "nic", length = 20)
    private String nic;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender = Gender.UNSPECIFIED;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "medical_notes", length = 500)
    private String medicalNotes;

    /**
     * Bidirectional so that {@code patient.getAppointments()} reads naturally
     * in the business tier. LAZY because the patient list screen must not drag
     * every visit of every patient into memory.
     */
    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<Appointment> appointments = new ArrayList<>();

    public Patient() {
        // required by JPA
    }

    public Patient(String patientCode, String fullName, String address, String contactNumber) {
        this.patientCode = patientCode;
        this.fullName = fullName;
        this.address = address;
        this.contactNumber = contactNumber;
    }

    /* ------------------------- behaviour ------------------------------- */

    /** @return the patient's age in whole years, or {@code null} if unknown. */
    public Integer getAge() {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Under-18 patients attract the paediatric discount, so the pricing tier
     * asks the patient rather than recomputing ages itself.
     */
    public boolean isMinor() {
        Integer age = getAge();
        return age != null && age < 18;
    }

    /** Senior citizens (65+) attract the senior discount. */
    public boolean isSeniorCitizen() {
        Integer age = getAge();
        return age != null && age >= 65;
    }

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }

    /** Keeps both sides of the association consistent. */
    public void addAppointment(Appointment appointment) {
        this.appointments.add(appointment);
        appointment.setPatient(this);
    }

    /* ------------------------- accessors ------------------------------- */

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

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments == null ? new ArrayList<>() : appointments;
    }

    @Override
    public String toString() {
        return patientCode + " - " + fullName;
    }
}
