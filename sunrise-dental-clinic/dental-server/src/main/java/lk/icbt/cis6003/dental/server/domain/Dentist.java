package lk.icbt.cis6003.dental.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * A dentist practising at the clinic.
 *
 * <p>Carries its own consultation fee and its own working hours. Working hours
 * per dentist (rather than one clinic-wide window) is what allows the booking
 * validation chain to reject "Dr Perera at 19:30" when Dr Perera finishes at
 * 17:00, instead of accepting it and producing a patient who waits for nobody.</p>
 */
@Entity
@Table(name = "dentist",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_dentist_code", columnNames = "dentist_code")
       })
public class Dentist extends BaseEntity {

    @Column(name = "dentist_code", nullable = false, length = 20)
    private String dentistCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "specialization", nullable = false, length = 80)
    private String specialization;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "consultation_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(name = "slmc_registration_no", length = 40)
    private String slmcRegistrationNo;

    @Column(name = "work_start_time", nullable = false)
    private LocalTime workStartTime = LocalTime.of(8, 0);

    @Column(name = "work_end_time", nullable = false)
    private LocalTime workEndTime = LocalTime.of(20, 0);

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Dentist() {
        // required by JPA
    }

    public Dentist(String dentistCode, String fullName, String specialization,
                   String contactNumber, BigDecimal consultationFee) {
        this.dentistCode = dentistCode;
        this.fullName = fullName;
        this.specialization = specialization;
        this.contactNumber = contactNumber;
        this.consultationFee = consultationFee;
    }

    /* ------------------------- behaviour ------------------------------- */

    /**
     * @param start start of the proposed appointment
     * @param end   end of the proposed appointment
     * @return true when the whole appointment fits inside this dentist's shift
     */
    public boolean isWithinWorkingHours(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return false;
        }
        return !start.isBefore(workStartTime) && !end.isAfter(workEndTime);
    }

    /** Total minutes this dentist is available in one day. */
    public long getDailyCapacityMinutes() {
        return java.time.Duration.between(workStartTime, workEndTime).toMinutes();
    }

    /* ------------------------- accessors ------------------------------- */

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

    public LocalTime getWorkStartTime() {
        return workStartTime;
    }

    public void setWorkStartTime(LocalTime workStartTime) {
        this.workStartTime = workStartTime;
    }

    public LocalTime getWorkEndTime() {
        return workEndTime;
    }

    public void setWorkEndTime(LocalTime workEndTime) {
        this.workEndTime = workEndTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return dentistCode + " - " + fullName + " (" + specialization + ")";
    }
}
