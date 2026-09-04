package lk.icbt.cis6003.dental.common.dto.report;

import java.time.LocalTime;

/**
 * One line of the Daily Appointment Schedule.
 *
 * <p>Decision it supports: the practice manager prints this at 07:45 and knows
 * immediately who is expected, with which dentist, and which slots are still
 * sellable to walk-in patients.</p>
 */
public class DailyScheduleRow {

    private LocalTime appointmentTime;
    private String appointmentNumber;
    private String patientName;
    private String contactNumber;
    private String dentistName;
    private String treatmentName;
    private String status;

    public DailyScheduleRow() {
        // required by Jackson
    }

    public DailyScheduleRow(LocalTime appointmentTime, String appointmentNumber, String patientName,
                            String contactNumber, String dentistName, String treatmentName, String status) {
        this.appointmentTime = appointmentTime;
        this.appointmentNumber = appointmentNumber;
        this.patientName = patientName;
        this.contactNumber = contactNumber;
        this.dentistName = dentistName;
        this.treatmentName = treatmentName;
        this.status = status;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getDentistName() {
        return dentistName;
    }

    public void setDentistName(String dentistName) {
        this.dentistName = dentistName;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
