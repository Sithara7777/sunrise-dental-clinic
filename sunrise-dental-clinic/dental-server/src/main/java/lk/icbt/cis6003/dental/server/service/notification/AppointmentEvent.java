package lk.icbt.cis6003.dental.server.service.notification;

import lk.icbt.cis6003.dental.server.domain.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * An immutable snapshot of something that happened to an appointment.
 *
 * <p><b>Why a snapshot and not the entity?</b> Observers may run after the
 * transaction has committed and the persistence context has closed. Handing
 * them the JPA entity would mean the first observer to touch
 * {@code appointment.getPatient().getEmail()} hits a
 * {@code LazyInitializationException}. Copying the handful of fields the
 * observers actually need makes the event safe to pass anywhere, including to
 * a background thread or, later, onto a message queue.</p>
 */
public final class AppointmentEvent {

    private final AppointmentEventType type;
    private final String appointmentNumber;
    private final String patientCode;
    private final String patientName;
    private final String patientEmail;
    private final String patientContact;
    private final String dentistName;
    private final String treatmentName;
    private final LocalDate appointmentDate;
    private final LocalTime appointmentTime;
    private final String status;
    private final String actor;
    private final String reference;
    private final String detail;
    private final BigDecimal amount;
    private final LocalDateTime occurredAt;

    private AppointmentEvent(Builder builder) {
        this.type = builder.type;
        this.appointmentNumber = builder.appointmentNumber;
        this.patientCode = builder.patientCode;
        this.patientName = builder.patientName;
        this.patientEmail = builder.patientEmail;
        this.patientContact = builder.patientContact;
        this.dentistName = builder.dentistName;
        this.treatmentName = builder.treatmentName;
        this.appointmentDate = builder.appointmentDate;
        this.appointmentTime = builder.appointmentTime;
        this.status = builder.status;
        this.actor = builder.actor;
        this.reference = builder.reference;
        this.detail = builder.detail;
        this.amount = builder.amount;
        this.occurredAt = LocalDateTime.now();
    }

    /**
     * Copies the fields the observers need out of a still-attached entity.
     *
     * <p>Must be called inside the transaction that loaded the appointment.</p>
     */
    public static Builder from(Appointment appointment, AppointmentEventType type, String actor) {
        Builder builder = new Builder()
                .type(type)
                .actor(actor)
                .appointmentNumber(appointment.getAppointmentNumber())
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus() == null ? null : appointment.getStatus().name());

        if (appointment.getPatient() != null) {
            builder.patientCode(appointment.getPatient().getPatientCode())
                   .patientName(appointment.getPatient().getFullName())
                   .patientEmail(appointment.getPatient().getEmail())
                   .patientContact(appointment.getPatient().getContactNumber());
        }
        if (appointment.getDentist() != null) {
            builder.dentistName(appointment.getDentist().getFullName());
        }
        if (appointment.getTreatment() != null) {
            builder.treatmentName(appointment.getTreatment().getName());
        }
        return builder;
    }

    public AppointmentEventType getType() {
        return type;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public String getPatientContact() {
        return patientContact;
    }

    public String getDentistName() {
        return dentistName;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public String getStatus() {
        return status;
    }

    /** The staff username responsible for the change. */
    public String getActor() {
        return actor;
    }

    /** Related business key, e.g. the invoice number for a billing event. */
    public String getReference() {
        return reference;
    }

    /** Free text, e.g. a cancellation reason. */
    public String getDetail() {
        return detail;
    }

    /** Money involved, for billing and payment events. */
    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public boolean hasEmail() {
        return patientEmail != null && !patientEmail.isBlank();
    }

    public boolean hasMobile() {
        return patientContact != null && !patientContact.isBlank();
    }

    @Override
    public String toString() {
        return type + " " + appointmentNumber + " by " + actor;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link AppointmentEvent}. */
    public static final class Builder {

        private AppointmentEventType type;
        private String appointmentNumber;
        private String patientCode;
        private String patientName;
        private String patientEmail;
        private String patientContact;
        private String dentistName;
        private String treatmentName;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private String status;
        private String actor;
        private String reference;
        private String detail;
        private BigDecimal amount;

        public Builder type(AppointmentEventType value) {
            this.type = value;
            return this;
        }

        public Builder appointmentNumber(String value) {
            this.appointmentNumber = value;
            return this;
        }

        public Builder patientCode(String value) {
            this.patientCode = value;
            return this;
        }

        public Builder patientName(String value) {
            this.patientName = value;
            return this;
        }

        public Builder patientEmail(String value) {
            this.patientEmail = value;
            return this;
        }

        public Builder patientContact(String value) {
            this.patientContact = value;
            return this;
        }

        public Builder dentistName(String value) {
            this.dentistName = value;
            return this;
        }

        public Builder treatmentName(String value) {
            this.treatmentName = value;
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

        public Builder status(String value) {
            this.status = value;
            return this;
        }

        public Builder actor(String value) {
            this.actor = value;
            return this;
        }

        public Builder reference(String value) {
            this.reference = value;
            return this;
        }

        public Builder detail(String value) {
            this.detail = value;
            return this;
        }

        public Builder amount(BigDecimal value) {
            this.amount = value;
            return this;
        }

        public AppointmentEvent build() {
            return new AppointmentEvent(this);
        }
    }
}
