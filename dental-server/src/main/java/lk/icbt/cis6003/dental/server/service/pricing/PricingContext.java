package lk.icbt.cis6003.dental.server.service.pricing;

import lk.icbt.cis6003.dental.server.domain.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Everything a {@link PricingStrategy} is allowed to see.
 *
 * <p>Passing a context object rather than the {@link Appointment} entity keeps
 * the strategies free of JPA: they can be unit tested with three lines of
 * setup and no database, no Spring and no lazy-loading proxies. It also makes
 * the inputs to a price explicit, which matters when a patient queries a
 * bill.</p>
 *
 * <p>The object is immutable - built once by the billing service and read by
 * whichever strategy the factory selects.</p>
 */
public final class PricingContext {

    private final BigDecimal treatmentBasePrice;
    private final BigDecimal consultationFee;
    private final BigDecimal requestedDiscountPercentage;
    private final BigDecimal taxRate;
    private final boolean patientIsMinor;
    private final boolean patientIsSeniorCitizen;
    private final LocalDate appointmentDate;
    private final LocalTime appointmentTime;
    private final String treatmentName;
    private final String treatmentCategory;

    private PricingContext(Builder builder) {
        this.treatmentBasePrice = builder.treatmentBasePrice;
        this.consultationFee = builder.consultationFee;
        this.requestedDiscountPercentage = builder.requestedDiscountPercentage;
        this.taxRate = builder.taxRate;
        this.patientIsMinor = builder.patientIsMinor;
        this.patientIsSeniorCitizen = builder.patientIsSeniorCitizen;
        this.appointmentDate = builder.appointmentDate;
        this.appointmentTime = builder.appointmentTime;
        this.treatmentName = builder.treatmentName;
        this.treatmentCategory = builder.treatmentCategory;
    }

    public BigDecimal getTreatmentBasePrice() {
        return treatmentBasePrice;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public BigDecimal getRequestedDiscountPercentage() {
        return requestedDiscountPercentage;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public boolean isPatientMinor() {
        return patientIsMinor;
    }

    public boolean isPatientSeniorCitizen() {
        return patientIsSeniorCitizen;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public String getTreatmentCategory() {
        return treatmentCategory;
    }

    /** True on a Saturday or Sunday. */
    public boolean isWeekend() {
        if (appointmentDate == null) {
            return false;
        }
        java.time.DayOfWeek day = appointmentDate.getDayOfWeek();
        return day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY;
    }

    /** Before 09:00 or from 17:00 onwards - the clinic's out-of-hours window. */
    public boolean isOutsideCoreHours() {
        if (appointmentTime == null) {
            return false;
        }
        return appointmentTime.isBefore(LocalTime.of(9, 0)) || !appointmentTime.isBefore(LocalTime.of(17, 0));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder - the context has ten fields and no sensible ordering. */
    public static final class Builder {

        private BigDecimal treatmentBasePrice = BigDecimal.ZERO;
        private BigDecimal consultationFee = BigDecimal.ZERO;
        private BigDecimal requestedDiscountPercentage = BigDecimal.ZERO;
        private BigDecimal taxRate = lk.icbt.cis6003.dental.common.ClinicConstants.VAT_RATE;
        private boolean patientIsMinor;
        private boolean patientIsSeniorCitizen;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private String treatmentName;
        private String treatmentCategory;

        private Builder() {
        }

        public Builder treatmentBasePrice(BigDecimal value) {
            this.treatmentBasePrice = value;
            return this;
        }

        public Builder consultationFee(BigDecimal value) {
            this.consultationFee = value;
            return this;
        }

        public Builder requestedDiscountPercentage(BigDecimal value) {
            this.requestedDiscountPercentage = value == null ? BigDecimal.ZERO : value;
            return this;
        }

        public Builder taxRate(BigDecimal value) {
            this.taxRate = value;
            return this;
        }

        public Builder patientIsMinor(boolean value) {
            this.patientIsMinor = value;
            return this;
        }

        public Builder patientIsSeniorCitizen(boolean value) {
            this.patientIsSeniorCitizen = value;
            return this;
        }

        public Builder appointmentDate(LocalDate value) {
            this.appointmentDate = value;
            return this;
        }

        public Builder appointmentTime(LocalTime value) {
            this.appointmentTime = value;
            return this;
        }

        public Builder treatmentName(String value) {
            this.treatmentName = value;
            return this;
        }

        public Builder treatmentCategory(String value) {
            this.treatmentCategory = value;
            return this;
        }

        public PricingContext build() {
            return new PricingContext(this);
        }
    }
}
