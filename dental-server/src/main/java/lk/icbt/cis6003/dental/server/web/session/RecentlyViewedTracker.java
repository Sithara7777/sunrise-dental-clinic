package lk.icbt.cis6003.dental.server.web.session;

import lk.icbt.cis6003.dental.common.dto.AppointmentDto;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A per-user, <b>session-scoped</b> list of the appointments this member of
 * staff has looked at.
 *
 * <p><b>Why this belongs in the session rather than the database.</b> "The last
 * five records I opened" is true only of one person, at one desk, for the
 * length of one shift. Persisting it would add a table, a write on every page
 * view and a cleanup job, to store information that is worthless tomorrow
 * morning. Session state is the correct tool, and this is a concrete use of it
 * beyond simply holding a login.</p>
 *
 * <p><b>Why the scoped proxy.</b> The bean is injected into singleton
 * controllers. {@code ScopedProxyMode.TARGET_CLASS} inserts a proxy that
 * resolves to the current request's session at call time, so two receptionists
 * on the same server never see each other's history.</p>
 *
 * <p>Bounded at five entries: a trail is a convenience, not an audit log, and
 * an unbounded list in every session is a slow memory leak.</p>
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RecentlyViewedTracker implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_ENTRIES = 5;

    private final Deque<Entry> entries = new ArrayDeque<>();

    /** Records a view, moving an already-present appointment back to the top. */
    public void record(AppointmentDto appointment) {
        if (appointment == null || appointment.getAppointmentNumber() == null) {
            return;
        }

        entries.removeIf(e -> e.appointmentNumber().equals(appointment.getAppointmentNumber()));
        entries.addFirst(new Entry(
                appointment.getAppointmentNumber(),
                appointment.getPatientName(),
                appointment.getAppointmentDate() == null ? "" : appointment.getAppointmentDate().toString(),
                appointment.getStatus() == null ? "" : appointment.getStatus().getDisplayName()));

        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    /** Most recently viewed first. */
    public List<Entry> list() {
        return List.copyOf(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }

    /** One entry in the trail. */
    public record Entry(String appointmentNumber, String patientName, String date, String status)
            implements Serializable {
    }
}
