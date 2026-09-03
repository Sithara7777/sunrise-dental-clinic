package lk.icbt.cis6003.dental.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.server.exception.InvalidStateTransitionException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A single patient visit - the heart of the system.
 *
 * <p><b>How double booking is made impossible.</b> The scenario's first
 * complaint is double bookings, so the fix is enforced at the lowest possible
 * level rather than trusted to the UI. The {@code slot_lock} column holds
 * {@code "yyyy-MM-dd|HH:mm"} while the appointment actually occupies the
 * dentist's chair, and is set to {@code NULL} the moment it is cancelled or
 * marked a no-show. A unique constraint over
 * {@code (dentist_id, slot_lock)} therefore rejects a second live booking for
 * the same dentist and time, while allowing any number of cancelled ones -
 * because SQL unique indexes do not compare NULLs to each other.</p>
 *
 * <p>The field is maintained by {@link #syncSlotLock()}, which every mutator
 * that can change status, dentist, date or time calls. That keeps one rule in
 * one method instead of scattering it across the service tier.</p>
 */
@Entity
@Table(name = "appointment",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_appointment_number", columnNames = "appointment_number"),
           @UniqueConstraint(name = "uk_appointment_slot", columnNames = {"dentist_id", "slot_lock"})
       },
       indexes = {
           @Index(name = "ix_appointment_date", columnList = "appointment_date"),
           @Index(name = "ix_appointment_patient", columnList = "patient_id"),
           @Index(name = "ix_appointment_status", columnList = "status")
       })
public class Appointment extends BaseEntity {

    @Column(name = "appointment_number", nullable = false, length = 20)
    private String appointmentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_appointment_patient"))
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dentist_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_appointment_dentist"))
    private Dentist dentist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "treatment_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_appointment_treatment"))
    private Treatment treatment;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    /**
     * {@code "yyyy-MM-dd|HH:mm"} while the slot is occupied, {@code NULL}
     * otherwise. Never set by hand - see {@link #syncSlotLock()}.
     */
    @Column(name = "slot_lock", length = 30)
    private String slotLock;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @Column(name = "created_by", nullable = false, length = 30)
    private String createdBy;

    @Column(name = "updated_by", length = 30)
    private String updatedBy;

    public Appointment() {
        // required by JPA
    }

    /* ------------------------- behaviour ------------------------------- */

    /**
     * Recomputes {@link #slotLock} from the current date, time and status.
     * Called by every mutator that could invalidate it.
     */
    private void syncSlotLock() {
        if (status != null && status.occupiesSlot() && appointmentDate != null && appointmentTime != null) {
            this.slotLock = appointmentDate + "|" + appointmentTime;
        } else {
            this.slotLock = null;
        }
    }

    /**
     * Moves the appointment to a new status, refusing illegal moves.
     *
     * @throws InvalidStateTransitionException when the move is not permitted by
     *         {@link AppointmentStatus#canTransitionTo(AppointmentStatus)}
     */
    public void changeStatus(AppointmentStatus target, String reason, String actor) {
        if (target == null) {
            throw new InvalidStateTransitionException("A target status must be supplied");
        }
        if (this.status == target) {
            return;
        }
        if (!this.status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(
                    "Appointment " + appointmentNumber + " cannot move from "
                            + status.getDisplayName() + " to " + target.getDisplayName());
        }
        this.status = target;
        if (target == AppointmentStatus.CANCELLED) {
            this.cancellationReason = reason;
        }
        this.updatedBy = actor;
        syncSlotLock();
    }

    /** Reschedules the visit, which necessarily re-derives the slot lock. */
    public void reschedule(LocalDate newDate, LocalTime newTime, String actor) {
        this.appointmentDate = newDate;
        this.appointmentTime = newTime;
        this.updatedBy = actor;
        syncSlotLock();
    }

    /** End of the visit, derived from the treatment's duration. */
    public LocalTime getEndTime() {
        return appointmentTime == null ? null : appointmentTime.plusMinutes(durationMinutes);
    }

    public LocalDateTime getStartsAt() {
        return (appointmentDate == null || appointmentTime == null)
                ? null : LocalDateTime.of(appointmentDate, appointmentTime);
    }

    /** True when the visit has not happened yet. */
    public boolean isUpcoming() {
        LocalDateTime start = getStartsAt();
        return start != null && start.isAfter(LocalDateTime.now());
    }

    public boolean isBillable() {
        return status != null && status.isBillable();
    }

    /* ------------------------- accessors ------------------------------- */

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Dentist getDentist() {
        return dentist;
    }

    public void setDentist(Dentist dentist) {
        this.dentist = dentist;
        syncSlotLock();
    }

    public Treatment getTreatment() {
        return treatment;
    }

    public void setTreatment(Treatment treatment) {
        this.treatment = treatment;
        if (treatment != null && treatment.getDurationMinutes() != null) {
            this.durationMinutes = treatment.getDurationMinutes();
        }
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
        syncSlotLock();
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
        syncSlotLock();
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    /**
     * Direct setter kept package-visible in spirit: prefer
     * {@link #changeStatus(AppointmentStatus, String, String)}, which enforces
     * the lifecycle. JPA needs a setter, so it stays public but still keeps the
     * slot lock consistent.
     */
    public void setStatus(AppointmentStatus status) {
        this.status = status;
        syncSlotLock();
    }

    public String getSlotLock() {
        return slotLock;
    }

    public void setSlotLock(String slotLock) {
        this.slotLock = slotLock;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return appointmentNumber + " @ " + appointmentDate + " " + appointmentTime + " [" + status + "]";
    }

    /* ------------------------------------------------------------------ */
    /* Builder pattern                                                     */
    /* ------------------------------------------------------------------ */

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Assembles a valid {@link Appointment}.
     *
     * <p>The entity has three mandatory associations plus a date, a time and a
     * generated number. A constructor taking all of them would be a six
     * argument call in which two {@code LocalDate}-ish parameters sit next to
     * each other - precisely the shape that produces silent argument-order
     * bugs. The builder names every value at the call site instead.</p>
     */
    public static final class Builder {

        private final Appointment target = new Appointment();

        private Builder() {
        }

        public Builder appointmentNumber(String appointmentNumber) {
            target.appointmentNumber = appointmentNumber;
            return this;
        }

        public Builder patient(Patient patient) {
            target.patient = patient;
            return this;
        }

        public Builder dentist(Dentist dentist) {
            target.dentist = dentist;
            return this;
        }

        public Builder treatment(Treatment treatment) {
            target.treatment = treatment;
            if (treatment != null && treatment.getDurationMinutes() != null) {
                target.durationMinutes = treatment.getDurationMinutes();
            }
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

        public Builder status(AppointmentStatus status) {
            target.status = status;
            return this;
        }

        public Builder notes(String notes) {
            target.notes = notes;
            return this;
        }

        public Builder createdBy(String createdBy) {
            target.createdBy = createdBy;
            return this;
        }

        /** Finishes the object and derives the slot lock exactly once. */
        public Appointment build() {
            target.syncSlotLock();
            return target;
        }
    }
}
