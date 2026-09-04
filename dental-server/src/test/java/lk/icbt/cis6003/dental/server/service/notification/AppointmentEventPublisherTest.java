package lk.icbt.cis6003.dental.server.service.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for the Observer pattern's publisher.
 *
 * <p><b>The failure-isolation test is the important one.</b> The whole reason
 * the notification pipeline is built as observers is so that a third party
 * being unreachable cannot prevent a patient being booked. This class proves
 * that a throwing observer neither stops its siblings nor propagates to the
 * caller - which is the difference between "the SMS did not send" and "the
 * clinic cannot take bookings this morning".</p>
 */
@DisplayName("Appointment event publisher (Observer pattern)")
class AppointmentEventPublisherTest {

    @Test
    @DisplayName("every interested observer receives the event")
    void allObserversAreNotified() {
        RecordingObserver first = new RecordingObserver("first");
        RecordingObserver second = new RecordingObserver("second");

        new AppointmentEventPublisher(List.of(first, second)).publish(event());

        assertThat(first.received).hasSize(1);
        assertThat(second.received).hasSize(1);
    }

    @Test
    @DisplayName("an observer that throws does NOT stop the others running")
    void oneFailingObserverDoesNotStopTheRest() {
        RecordingObserver before = new RecordingObserver("before");
        ExplodingObserver failing = new ExplodingObserver();
        RecordingObserver after = new RecordingObserver("after");

        new AppointmentEventPublisher(List.of(before, failing, after)).publish(event());

        assertThat(before.received).as("the observer before the failure ran").hasSize(1);
        assertThat(after.received).as("the observer AFTER the failure still ran").hasSize(1);
        assertThat(failing.invocations.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("an observer that throws does NOT propagate to the caller")
    void failureDoesNotReachTheCaller() {
        AppointmentEventPublisher publisher =
                new AppointmentEventPublisher(List.of(new ExplodingObserver()));

        // If this threw, the surrounding booking transaction would roll back
        // and the patient would not be booked - the exact outcome the pattern
        // exists to prevent.
        assertThatCode(() -> publisher.publish(event())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("an observer that opts out with supports() is not invoked")
    void observersCanOptOut() {
        RecordingObserver interested = new RecordingObserver("interested");
        RecordingObserver uninterested = new RecordingObserver("uninterested");
        uninterested.interested = false;

        new AppointmentEventPublisher(List.of(interested, uninterested)).publish(event());

        assertThat(interested.received).hasSize(1);
        assertThat(uninterested.received).isEmpty();
    }

    @Test
    @DisplayName("publishing null is ignored rather than throwing")
    void nullEventIsIgnored() {
        RecordingObserver observer = new RecordingObserver("only");
        AppointmentEventPublisher publisher = new AppointmentEventPublisher(List.of(observer));

        assertThatCode(() -> publisher.publish(null)).doesNotThrowAnyException();
        assertThat(observer.received).isEmpty();
    }

    @Test
    @DisplayName("the publisher can name its registered observers for the diagnostics screen")
    void describesItsObservers() {
        AppointmentEventPublisher publisher = new AppointmentEventPublisher(
                List.of(new RecordingObserver("alpha"), new RecordingObserver("beta")));

        assertThat(publisher.describeObservers()).containsExactly("alpha", "beta");
    }

    @Test
    @DisplayName("an event with no observers at all is harmless")
    void noObserversIsHarmless() {
        assertThatCode(() -> new AppointmentEventPublisher(List.of()).publish(event()))
                .doesNotThrowAnyException();
    }

    /* ------------------------------------------------------------------ */

    private AppointmentEvent event() {
        return AppointmentEvent.builder()
                .type(AppointmentEventType.BOOKED)
                .appointmentNumber("APT-2026-000001")
                .patientName("Kamala Perera")
                .patientEmail("kamala@example.lk")
                .patientContact("0771234567")
                .dentistName("Nimal Perera")
                .treatmentName("Scaling and Polishing")
                .appointmentDate(java.time.LocalDate.now().plusDays(1))
                .appointmentTime(java.time.LocalTime.of(10, 0))
                .actor("reception")
                .build();
    }

    /** Records what it was given. */
    private static class RecordingObserver implements AppointmentObserver {

        private final String name;
        private final List<AppointmentEvent> received = new ArrayList<>();
        private boolean interested = true;

        RecordingObserver(String name) {
            this.name = name;
        }

        @Override
        public void onAppointmentEvent(AppointmentEvent event) {
            received.add(event);
        }

        @Override
        public String getObserverName() {
            return name;
        }

        @Override
        public boolean supports(AppointmentEvent event) {
            return interested;
        }
    }

    /** Stands in for an SMS gateway whose vendor is having an outage. */
    private static class ExplodingObserver implements AppointmentObserver {

        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public void onAppointmentEvent(AppointmentEvent event) {
            invocations.incrementAndGet();
            throw new IllegalStateException("the SMS aggregator is unreachable");
        }

        @Override
        public String getObserverName() {
            return "exploding";
        }
    }
}
