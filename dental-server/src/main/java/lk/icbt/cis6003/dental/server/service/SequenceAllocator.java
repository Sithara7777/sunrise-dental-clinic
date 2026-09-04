package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.server.domain.NumberSequence;
import lk.icbt.cis6003.dental.server.repository.NumberSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allocates one value from a {@link NumberSequence} row, creating the row on
 * first use, in its own transaction.
 *
 * <p><b>Why this is a separate bean from {@link SequenceGeneratorService}
 * rather than a private method on it.</b> {@code SELECT ... FOR UPDATE}
 * (see {@link NumberSequenceRepository#lockForUpdate}) can only lock a row
 * that already exists - it cannot lock the ABSENCE of one. The very first
 * time a key is used (once per year, for the appointment and invoice
 * counters), two concurrent callers can both find no row, and both attempt to
 * {@code INSERT} it; exactly one wins and the other hits a primary-key
 * violation.</p>
 *
 * <p>The correct response to that violation is "retry - the row now exists,
 * so the lock will work properly this time" - but that retry must run in a
 * genuinely NEW transaction, because the failed {@code INSERT} has already
 * marked the current one for rollback. A retry loop written inside this same
 * transactional method, or in a plain private method on the caller, would
 * either reuse the doomed transaction (self-invocation bypasses Spring's
 * proxy, so {@code @Transactional} on a method called via {@code this} has no
 * effect) or - in earlier code here - the flush that surfaces the failure was
 * deferred to the commit of the {@code REQUIRES_NEW} transaction, which
 * happens in the caller's proxy, one level higher, than a try/catch inside
 * this method could ever see. Making this allocation its own Spring-proxied
 * bean, called through dependency injection, is what makes each attempt a
 * genuinely separate, independently committing transaction - so
 * {@link SequenceGeneratorService} catching a failure from a call to this
 * bean and simply calling it again is a real retry, not a no-op.</p>
 */
@Service
public class SequenceAllocator {

    private final NumberSequenceRepository sequenceRepository;

    public SequenceAllocator(NumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * @param key the sequence key, e.g. {@code APPOINTMENT-2026}
     * @return the allocated value
     * @throws org.springframework.dao.DataIntegrityViolationException if this
     *         call lost the race to create the key's row for the first time;
     *         the caller retries
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long allocate(String key) {
        NumberSequence sequence = sequenceRepository.lockForUpdate(key)
                // saveAndFlush, not save: the INSERT must happen HERE, inside
                // this transaction's own commit, so that a losing INSERT's
                // constraint violation is thrown to the caller of THIS method
                // rather than surfacing later at some other, unrelated flush.
                .orElseGet(() -> sequenceRepository.saveAndFlush(new NumberSequence(key, 1L)));

        long allocated = sequence.allocate();
        sequenceRepository.save(sequence);
        return allocated;
    }
}
