package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Issues the clinic's human-readable business identifiers.
 *
 * <p>The scenario opens with "each patient visit will be assigned a unique
 * appointment number", and the manual system's failure to guarantee that is why
 * the clinic is replacing it. This service is where that guarantee is
 * implemented.</p>
 *
 * <p><b>Why not {@code MAX(appointment_number) + 1}?</b> Because two
 * receptionists pressing Save in the same second would both read the same
 * maximum and both produce {@code APT-2026-000137}. The unique constraint would
 * reject the second, so no corrupt data results - but one receptionist gets an
 * error in front of a waiting patient, which is a bad system.</p>
 *
 * <p><b>What happens instead.</b> Each key is one row in
 * {@code number_sequence}, allocated by {@link SequenceAllocator} under a
 * {@code PESSIMISTIC_WRITE} lock ({@code SELECT ... FOR UPDATE}). The second
 * transaction waits microseconds and receives the next number.</p>
 *
 * <p><b>The one case a row lock cannot cover, and how it is handled.</b> A
 * lock can only be taken on a row that exists. The very first time a key is
 * used - once per year, when the first appointment or invoice of a new year is
 * numbered - two concurrent callers can both find no row and both attempt to
 * create it; exactly one wins. This class catches that specific, expected
 * failure and retries the allocation exactly once, by which point the row
 * exists and behaves like any other allocation. See {@link SequenceAllocator}
 * for why the retry has to be a call to a separate Spring-managed bean rather
 * than a loop written inline here.</p>
 *
 * <p><b>Why each allocation commits immediately, independent of the booking
 * it is for.</b> If the surrounding booking then fails validation, the
 * allocated number is simply never used - the sequence has a gap. A gap is
 * harmless; a reused number is not, and holding the lock for the whole booking
 * transaction would serialise every booking in the clinic behind one
 * another.</p>
 */
@Service
public class SequenceGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SequenceGeneratorService.class);

    private static final String KEY_PATIENT = "PATIENT";
    private static final String KEY_APPOINTMENT_PREFIX = "APPOINTMENT-";
    private static final String KEY_INVOICE_PREFIX = "INVOICE-";
    private static final String KEY_DENTIST = "DENTIST";

    private final SequenceAllocator sequenceAllocator;

    public SequenceGeneratorService(SequenceAllocator sequenceAllocator) {
        this.sequenceAllocator = sequenceAllocator;
    }

    /**
     * @return the next appointment number, e.g. {@code APT-2026-000137}.
     *         Numbering restarts each January so the year is readable at a
     *         glance and the running count never becomes unwieldy.
     */
    public String nextAppointmentNumber() {
        int year = LocalDate.now().getYear();
        long value = allocateWithRetry(KEY_APPOINTMENT_PREFIX + year);
        return String.format("%s-%d-%06d", ClinicConstants.APPOINTMENT_NUMBER_PREFIX, year, value);
    }

    /** @return the next invoice number, e.g. {@code INV-2026-000137} */
    public String nextInvoiceNumber() {
        int year = LocalDate.now().getYear();
        long value = allocateWithRetry(KEY_INVOICE_PREFIX + year);
        return String.format("%s-%d-%06d", ClinicConstants.INVOICE_NUMBER_PREFIX, year, value);
    }

    /**
     * @return the next patient code, e.g. {@code PAT-000042}. Not year-scoped:
     *         a patient belongs to the clinic, not to the year they first
     *         attended.
     */
    public String nextPatientCode() {
        long value = allocateWithRetry(KEY_PATIENT);
        return String.format("%s-%06d", ClinicConstants.PATIENT_CODE_PREFIX, value);
    }

    /** @return the next dentist code, e.g. {@code DEN-007} */
    public String nextDentistCode() {
        long value = allocateWithRetry(KEY_DENTIST);
        return String.format("%s-%03d", ClinicConstants.DENTIST_CODE_PREFIX, value);
    }

    /**
     * Allocates one value, retrying exactly once if this call lost the race to
     * create the key's row for the very first time.
     *
     * <p>One retry is always sufficient: a {@code number_sequence} row is
     * never deleted once created, so after any single successful creation -
     * whether by this call or by the concurrent one that beat it - every
     * subsequent attempt finds the row and the pessimistic lock behaves
     * exactly as intended.</p>
     */
    private long allocateWithRetry(String key) {
        try {
            return sequenceAllocator.allocate(key);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Sequence '{}' was created by a concurrent request at the same instant - "
                      + "retrying now that the row exists", key);
            return sequenceAllocator.allocate(key);
        }
    }
}
