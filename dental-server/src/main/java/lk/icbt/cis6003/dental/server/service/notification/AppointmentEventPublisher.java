package lk.icbt.cis6003.dental.server.service.notification;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The <b>Subject</b> half of the Observer pattern.
 *
 * <p>Spring injects every {@link AppointmentObserver} bean, so registration is
 * automatic - there is no {@code addObserver} call anyone can forget to make.
 * The registered set is logged at start-up, which is the mitigation for the
 * pattern's indirection: the application states exactly which reactions are
 * live.</p>
 *
 * <p><b>Failure isolation is the important behaviour here.</b> Each observer is
 * invoked inside its own try/catch. If the SMS observer throws because the
 * aggregator is unreachable, the e-mail and audit observers still run and the
 * booking transaction still commits. The clinic's core job - recording that a
 * patient is coming in on Tuesday - does not depend on a third party being
 * up.</p>
 */
@Component
public class AppointmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventPublisher.class);

    private final List<AppointmentObserver> observers;

    public AppointmentEventPublisher(List<AppointmentObserver> observers) {
        this.observers = Collections.unmodifiableList(new ArrayList<>(observers));
    }

    @PostConstruct
    void logRegisteredObservers() {
        log.info("Appointment observers registered ({}): {}",
                 observers.size(),
                 observers.stream()
                          .map(AppointmentObserver::getObserverName)
                          .collect(Collectors.joining(", ")));
    }

    /**
     * Announces an event to every interested observer.
     *
     * @param event the event; ignored when {@code null}
     */
    public void publish(AppointmentEvent event) {
        if (event == null) {
            return;
        }

        for (AppointmentObserver observer : observers) {
            try {
                if (observer.supports(event)) {
                    observer.onAppointmentEvent(event);
                }
            } catch (RuntimeException ex) {
                // Deliberately swallowed. See the class comment: a reaction
                // must never undo the clinical action that triggered it.
                log.error("Observer '{}' failed handling {} for appointment {} - "
                          + "the appointment itself is unaffected",
                          observer.getObserverName(), event.getType(),
                          event.getAppointmentNumber(), ex);
            }
        }
    }

    /** Observer names, for the admin diagnostics screen. */
    public List<String> describeObservers() {
        return observers.stream()
                .map(AppointmentObserver::getObserverName)
                .collect(Collectors.toList());
    }
}
