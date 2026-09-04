package lk.icbt.cis6003.dental.common.dto.report;

import java.math.BigDecimal;

/**
 * One dentist's line in the Workload &amp; Utilisation report.
 *
 * <p>Decision it supports: whether to recruit, and how to re-balance the
 * diary. {@code utilisationPercentage} compares booked minutes against the
 * minutes the dentist was actually available, so a dentist at 95% is a
 * bottleneck and one at 30% is spare capacity.</p>
 */
public class DentistWorkloadRow {

    private String dentistCode;
    private String dentistName;
    private String specialization;
    private long totalAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
    private long noShowAppointments;
    private long bookedMinutes;
    private BigDecimal utilisationPercentage = BigDecimal.ZERO;
    private BigDecimal revenueGenerated = BigDecimal.ZERO;

    public DentistWorkloadRow() {
        // required by Jackson
    }

    public String getDentistCode() {
        return dentistCode;
    }

    public void setDentistCode(String dentistCode) {
        this.dentistCode = dentistCode;
    }

    public String getDentistName() {
        return dentistName;
    }

    public void setDentistName(String dentistName) {
        this.dentistName = dentistName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public long getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(long totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public long getCompletedAppointments() {
        return completedAppointments;
    }

    public void setCompletedAppointments(long completedAppointments) {
        this.completedAppointments = completedAppointments;
    }

    public long getCancelledAppointments() {
        return cancelledAppointments;
    }

    public void setCancelledAppointments(long cancelledAppointments) {
        this.cancelledAppointments = cancelledAppointments;
    }

    public long getNoShowAppointments() {
        return noShowAppointments;
    }

    public void setNoShowAppointments(long noShowAppointments) {
        this.noShowAppointments = noShowAppointments;
    }

    public long getBookedMinutes() {
        return bookedMinutes;
    }

    public void setBookedMinutes(long bookedMinutes) {
        this.bookedMinutes = bookedMinutes;
    }

    public BigDecimal getUtilisationPercentage() {
        return utilisationPercentage;
    }

    public void setUtilisationPercentage(BigDecimal utilisationPercentage) {
        this.utilisationPercentage = utilisationPercentage;
    }

    public BigDecimal getRevenueGenerated() {
        return revenueGenerated;
    }

    public void setRevenueGenerated(BigDecimal revenueGenerated) {
        this.revenueGenerated = revenueGenerated;
    }
}
