package lk.icbt.cis6003.dental.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single, database-owned counter behind the human-readable business
 * identifiers ({@code APT-2026-000137}, {@code INV-2026-000137},
 * {@code PAT-000042}).
 *
 * <p>Why not simply use the primary key? Because the clinic needs numbers that
 * restart each year and read sensibly on a printed receipt. Why not compute
 * {@code MAX(appointment_number) + 1}? Because two receptionists clicking
 * "Save" in the same second would both read the same maximum and produce the
 * same number - exactly the duplicate the system exists to prevent.</p>
 *
 * <p>Instead, each key is one row and the allocator takes a
 * {@code PESSIMISTIC_WRITE} lock on it, so the second transaction waits for the
 * first. The whole concurrency problem is reduced to a single row lock.</p>
 */
@Entity
@Table(name = "number_sequence")
public class NumberSequence {

    /** e.g. {@code APPOINTMENT-2026}, {@code INVOICE-2026}, {@code PATIENT}. */
    @Id
    @Column(name = "sequence_key", nullable = false, length = 40)
    private String sequenceKey;

    @Column(name = "next_value", nullable = false)
    private long nextValue = 1L;

    public NumberSequence() {
        // required by JPA
    }

    public NumberSequence(String sequenceKey, long nextValue) {
        this.sequenceKey = sequenceKey;
        this.nextValue = nextValue;
    }

    /** Returns the current value and advances the counter. */
    public long allocate() {
        long allocated = this.nextValue;
        this.nextValue = allocated + 1;
        return allocated;
    }

    public String getSequenceKey() {
        return sequenceKey;
    }

    public void setSequenceKey(String sequenceKey) {
        this.sequenceKey = sequenceKey;
    }

    public long getNextValue() {
        return nextValue;
    }

    public void setNextValue(long nextValue) {
        this.nextValue = nextValue;
    }

    @Override
    public String toString() {
        return sequenceKey + " -> " + nextValue;
    }
}
