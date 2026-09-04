package lk.icbt.cis6003.dental.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the appointment lifecycle.
 *
 * <p>The rule "only a completed visit can be billed" and the rule "a cancelled
 * appointment releases the dentist's slot" both live on this enum, so they are
 * tested here once rather than being re-verified everywhere they are consumed.
 * That is the point of putting them on the enum in the first place.</p>
 */
@DisplayName("Appointment lifecycle")
class AppointmentStatusTest {

    @Test
    @DisplayName("a scheduled appointment may be confirmed, completed, cancelled or missed")
    void scheduledCanMoveAnywhereSensible() {
        assertThat(AppointmentStatus.SCHEDULED.allowedTransitions())
                .containsExactlyInAnyOrder(AppointmentStatus.CONFIRMED,
                                           AppointmentStatus.COMPLETED,
                                           AppointmentStatus.CANCELLED,
                                           AppointmentStatus.NO_SHOW);
    }

    @Test
    @DisplayName("a confirmed appointment may no longer go back to scheduled")
    void confirmedCannotRegress() {
        assertThat(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.SCHEDULED))
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class,
                names = { "COMPLETED", "CANCELLED", "NO_SHOW" })
    @DisplayName("completed, cancelled and no-show are final")
    void terminalStatusesAreFinal(AppointmentStatus terminal) {
        assertThat(terminal.isTerminal()).isTrue();
        assertThat(terminal.allowedTransitions()).isEmpty();

        for (AppointmentStatus target : AppointmentStatus.values()) {
            assertThat(terminal.canTransitionTo(target))
                    .as("%s must not be able to move to %s", terminal, target)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("a cancelled appointment can never be completed - it would bill a visit that never happened")
    void cancelledCannotBeCompleted() {
        assertThat(AppointmentStatus.CANCELLED.canTransitionTo(AppointmentStatus.COMPLETED))
                .isFalse();
    }

    @Test
    @DisplayName("only a completed appointment is billable")
    void onlyCompletedIsBillable() {
        assertThat(AppointmentStatus.COMPLETED.isBillable()).isTrue();

        assertThat(AppointmentStatus.SCHEDULED.isBillable()).isFalse();
        assertThat(AppointmentStatus.CONFIRMED.isBillable()).isFalse();
        assertThat(AppointmentStatus.CANCELLED.isBillable()).isFalse();
        assertThat(AppointmentStatus.NO_SHOW.isBillable()).isFalse();
    }

    @Test
    @DisplayName("scheduled, confirmed and completed hold the dentist's slot")
    void liveStatusesOccupyTheSlot() {
        assertThat(AppointmentStatus.SCHEDULED.occupiesSlot()).isTrue();
        assertThat(AppointmentStatus.CONFIRMED.occupiesSlot()).isTrue();
        assertThat(AppointmentStatus.COMPLETED.occupiesSlot()).isTrue();
    }

    @Test
    @DisplayName("cancelled and no-show release the slot, so it can be resold")
    void deadStatusesReleaseTheSlot() {
        assertThat(AppointmentStatus.CANCELLED.occupiesSlot()).isFalse();
        assertThat(AppointmentStatus.NO_SHOW.occupiesSlot()).isFalse();
    }

    @Test
    @DisplayName("a null target is never a legal transition")
    void nullTargetIsRejected() {
        assertThat(AppointmentStatus.SCHEDULED.canTransitionTo(null)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AppointmentStatus.class)
    @DisplayName("every status has a display name and a CSS class for the badge")
    void everyStatusIsPresentable(AppointmentStatus status) {
        assertThat(status.getDisplayName()).isNotBlank();
        assertThat(status.getCssClass()).isNotBlank().startsWith("badge-");
    }
}
