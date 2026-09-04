package lk.icbt.cis6003.dental.server.repository;

import jakarta.persistence.LockModeType;
import lk.icbt.cis6003.dental.server.domain.NumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for the business-identifier counters.
 *
 * <p>{@link #lockForUpdate(String)} takes a {@code PESSIMISTIC_WRITE} lock -
 * SQL {@code SELECT ... FOR UPDATE}. It is the one place in the system where a
 * pessimistic lock is the right tool: the critical section is a single row
 * held for microseconds, and the cost of getting it wrong is a duplicate
 * appointment number on a printed receipt.</p>
 */
@Repository
public interface NumberSequenceRepository extends JpaRepository<NumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM NumberSequence s WHERE s.sequenceKey = :key")
    Optional<NumberSequence> lockForUpdate(@Param("key") String key);
}
