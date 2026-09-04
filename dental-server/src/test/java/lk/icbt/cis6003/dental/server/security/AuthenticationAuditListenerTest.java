package lk.icbt.cis6003.dental.server.security;

import lk.icbt.cis6003.dental.server.domain.AuditLog;
import lk.icbt.cis6003.dental.server.domain.User;
import lk.icbt.cis6003.dental.server.repository.AuditLogRepository;
import lk.icbt.cis6003.dental.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AuthenticationAuditListener}.
 *
 * <p>The case that matters here is not the happy path - it is what happens
 * when {@link UserLoginBookkeepingService} loses an optimistic-locking race,
 * which is exactly what a real concurrency test against a running server
 * exposed: two requests authenticating as the same account in the same
 * instant. Before the fix, that collision surfaced as an uncaught
 * {@code ObjectOptimisticLockingFailureException} thrown from inside Spring
 * Security's filter chain - outside the REST exception handler's reach - which
 * corrupted the HTTP response for BOTH requests and silently discarded the
 * audit entry for the one that lost the race. These tests pin the fix: the
 * listener must swallow that specific exception and still write the audit
 * entry unconditionally.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication audit listener")
class AuthenticationAuditListenerTest {

    @Mock
    private UserLoginBookkeepingService loginBookkeepingService;

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuthenticationAuditListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuthenticationAuditListener(loginBookkeepingService, auditLogRepository);
    }

    @Test
    @DisplayName("a successful sign-in records bookkeeping and an audit entry")
    void successfulSignInIsRecorded() {
        listener.onSuccess(successEvent("reception"));

        verify(loginBookkeepingService).recordSuccessfulLogin("reception");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog entry = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(entry.getAction()).isEqualTo("LOGIN_SUCCESS");
        org.assertj.core.api.Assertions.assertThat(entry.getUsername()).isEqualTo("reception");
    }

    @Test
    @DisplayName("a concurrent bookkeeping conflict on success does NOT propagate, "
            + "and the audit entry is STILL written")
    void concurrentConflictOnSuccessIsSwallowed() {
        doThrow(new OptimisticLockingFailureException("concurrent update"))
                .when(loginBookkeepingService).recordSuccessfulLogin("reception");

        // This is the exact defect the race test found: the exception must
        // never reach the caller of onSuccess() - Spring Security's filter
        // chain, in the real system - because there is no exception handler
        // downstream of it that can turn it into a clean response.
        assertThatCode(() -> listener.onSuccess(successEvent("reception")))
                .doesNotThrowAnyException();

        // Losing the bookkeeping update must not also lose the audit trail -
        // it is the one part of this listener that genuinely cannot be
        // allowed to silently disappear.
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getAction())
                .isEqualTo("LOGIN_SUCCESS");
    }

    @Test
    @DisplayName("a failed sign-in against a known user records the remaining attempts")
    void failedSignInRecordsRemainingAttempts() {
        User user = new User("reception", "hash", "Chamari Gunasekara", Role.RECEPTIONIST);
        user.registerFailedLogin();     // 1 of 5

        when(loginBookkeepingService.recordFailedLogin("reception"))
                .thenReturn(java.util.Optional.of(user));

        listener.onFailure(failureEvent("reception"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getDetails())
                .contains("4 attempt(s) remaining");
    }

    @Test
    @DisplayName("a concurrent bookkeeping conflict on failure does NOT propagate, "
            + "and the audit entry is still written with a safe generic message")
    void concurrentConflictOnFailureIsSwallowed() {
        doThrow(new OptimisticLockingFailureException("concurrent update"))
                .when(loginBookkeepingService).recordFailedLogin("reception");

        assertThatCode(() -> listener.onFailure(failureEvent("reception")))
                .doesNotThrowAnyException();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getAction())
                .isEqualTo("LOGIN_FAILURE");
    }

    @Test
    @DisplayName("a failed sign-in against an unknown username is still recorded")
    void unknownUsernameIsStillRecorded() {
        when(loginBookkeepingService.recordFailedLogin("no-such-user"))
                .thenReturn(java.util.Optional.empty());

        listener.onFailure(failureEvent("no-such-user"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getDetails())
                .contains("unrecognised username");
    }

    /* ------------------------------------------------------------------ */

    private AuthenticationSuccessEvent successEvent(String username) {
        return new AuthenticationSuccessEvent(
                new UsernamePasswordAuthenticationToken(username, "password"));
    }

    private AuthenticationFailureBadCredentialsEvent failureEvent(String username) {
        return new AuthenticationFailureBadCredentialsEvent(
                new UsernamePasswordAuthenticationToken(username, "password"),
                new org.springframework.security.authentication.BadCredentialsException("bad"));
    }
}
