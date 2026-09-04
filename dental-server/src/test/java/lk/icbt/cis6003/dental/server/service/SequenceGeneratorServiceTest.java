package lk.icbt.cis6003.dental.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SequenceGeneratorService}.
 *
 * <p>The case that matters here is not the happy path - it is what a genuine
 * concurrency test against a running server found: {@code SELECT ... FOR
 * UPDATE} cannot lock a row that does not exist yet, so the very first
 * allocation of any sequence key can race, and the loser must retry rather
 * than fail the booking it was allocating a number for. These tests pin that
 * retry, and that a SECOND, genuine failure (not just a lost race) is still
 * reported rather than silently retried forever.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Sequence number generation")
class SequenceGeneratorServiceTest {

    @Mock
    private SequenceAllocator sequenceAllocator;

    private SequenceGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new SequenceGeneratorService(sequenceAllocator);
    }

    @Test
    @DisplayName("a normal allocation calls the allocator once and formats the number")
    void normalAllocationFormatsTheNumber() {
        when(sequenceAllocator.allocate("PATIENT")).thenReturn(42L);

        String code = service.nextPatientCode();

        assertThat(code).isEqualTo("PAT-000042");
        verify(sequenceAllocator, times(1)).allocate("PATIENT");
    }

    @Test
    @DisplayName("appointment numbers carry the current year and are zero-padded to six digits")
    void appointmentNumberFollowsThePublishedFormat() {
        when(sequenceAllocator.allocate(org.mockito.ArgumentMatchers.startsWith("APPOINTMENT-")))
                .thenReturn(137L);

        String number = service.nextAppointmentNumber();

        assertThat(number).matches("APT-\\d{4}-000137");
    }

    @Test
    @DisplayName("losing the race to create a sequence row for the first time is retried ONCE, "
            + "and the retry's result is returned")
    void firstAllocationRaceIsRetriedOnce() {
        // First call: this caller lost the race to INSERT the row - another
        // concurrent request's transaction won and committed it first.
        // Second call: the row now exists, so the retry succeeds normally.
        when(sequenceAllocator.allocate("DENTIST"))
                .thenThrow(new DataIntegrityViolationException(
                        "Unique index or primary key violation on NUMBER_SEQUENCE"))
                .thenReturn(6L);

        String code = service.nextDentistCode();

        assertThat(code).isEqualTo("DEN-006");
        verify(sequenceAllocator, times(2)).allocate("DENTIST");
    }

    @Test
    @DisplayName("a SECOND consecutive failure is NOT silently retried again - it propagates")
    void aSecondFailureIsNotSwallowed() {
        // Retrying forever on a genuine, persistent failure would hang the
        // request; one retry covers the only case that is actually expected
        // (a lost race on first creation), and anything beyond that is a real
        // problem the caller - and ultimately RestExceptionHandler - must see.
        when(sequenceAllocator.allocate("PATIENT"))
                .thenThrow(new DataIntegrityViolationException("first failure"))
                .thenThrow(new DataIntegrityViolationException("second failure"));

        assertThatThrownBy(() -> service.nextPatientCode())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("second failure");

        verify(sequenceAllocator, times(2)).allocate("PATIENT");
    }

    @Test
    @DisplayName("invoice numbers use their own year-scoped key, distinct from appointments")
    void invoiceAndAppointmentSequencesAreIndependent() {
        when(sequenceAllocator.allocate(org.mockito.ArgumentMatchers.startsWith("INVOICE-")))
                .thenReturn(258L);

        String number = service.nextInvoiceNumber();

        assertThat(number).matches("INV-\\d{4}-000258");
        verify(sequenceAllocator).allocate(
                org.mockito.ArgumentMatchers.argThat(key -> key.startsWith("INVOICE-")));
    }
}
