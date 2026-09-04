package lk.icbt.cis6003.dental.server.service.notification;

/**
 * <b>Observer pattern</b> - something that reacts to an appointment event.
 *
 * <p><b>The problem it solves.</b> Booking an appointment must, today, send a
 * confirmation e-mail, send an SMS and write an audit entry. Tomorrow it will
 * also need to update a waiting-list and push a calendar entry to the dentist's
 * phone. Calling each of those directly from {@code AppointmentService} makes
 * that one method the place where every future integration is bolted on, and it
 * couples the act of booking to the availability of an SMS vendor.</p>
 *
 * <p><b>How this is better.</b> The service announces what happened and stops
 * caring. Each reaction is a separate class that can be tested, disabled by
 * configuration, or removed entirely without the service changing. The
 * publisher isolates failures, so an SMS outage cannot prevent a patient being
 * booked.</p>
 *
 * <p><b>Cost, honestly stated.</b> Reading {@code AppointmentService} no longer
 * tells you everything that happens when a booking is saved - the call graph is
 * indirect. That is a real readability cost, paid down by logging every
 * registered observer at start-up and by naming each one after what it does.</p>
 */
public interface AppointmentObserver {

    /**
     * Reacts to one event.
     *
     * <p>Implementations must not throw. Anything that can fail belongs inside
     * the implementation's own try/catch, because a reaction failing must never
     * roll back the clinical action that caused it.</p>
     */
    void onAppointmentEvent(AppointmentEvent event);

    /** @return a name for the start-up log and the admin diagnostics screen */
    String getObserverName();

    /**
     * @return {@code true} if this observer should be invoked for this event.
     *         Lets an observer opt out cheaply rather than every observer
     *         repeating the same {@code if} on event type.
     */
    default boolean supports(AppointmentEvent event) {
        return true;
    }
}
