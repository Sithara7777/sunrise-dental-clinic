package lk.icbt.cis6003.dental.server.service.notification;

import lk.icbt.cis6003.dental.server.domain.AuditLog;
import lk.icbt.cis6003.dental.server.repository.AuditLogRepository;
import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Observer</b> that writes an application-level audit entry for every event.
 *
 * <p>This is the counterpart to the database triggers. The trigger knows which
 * <em>row</em> changed; this observer knows which <em>business action</em>
 * occurred and, crucially, <b>which member of staff performed it</b> - the
 * database has no notion of the logged-in user. Together they answer both "what
 * changed in the data?" and "who did what, and why?".</p>
 *
 * <p>Unlike the two notification observers, this one reacts to every event
 * type, including internal ones the patient never sees.</p>
 */
@Component
public class AuditTrailObserver implements AppointmentObserver {

    private final AuditLogRepository auditLogRepository;

    public AuditTrailObserver(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public String getObserverName() {
        return "AuditTrailObserver";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentEvent(AppointmentEvent event) {
        AuditLog entry = new AuditLog(
                event.getActor() == null ? "system" : event.getActor(),
                "APPOINTMENT_" + event.getType().name(),
                "APPOINTMENT",
                event.getAppointmentNumber(),
                describe(event));
        auditLogRepository.save(entry);
    }

    private String describe(AppointmentEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("patient=").append(event.getPatientName());
        if (event.getDentistName() != null) {
            sb.append("; dentist=").append(event.getDentistName());
        }
        if (event.getTreatmentName() != null) {
            sb.append("; treatment=").append(event.getTreatmentName());
        }
        if (event.getAppointmentDate() != null) {
            sb.append("; when=").append(event.getAppointmentDate())
              .append(' ').append(event.getAppointmentTime());
        }
        if (event.getStatus() != null) {
            sb.append("; status=").append(event.getStatus());
        }
        if (event.getReference() != null) {
            sb.append("; ref=").append(event.getReference());
        }
        if (event.getAmount() != null) {
            sb.append("; amount=").append(MoneyUtils.format(event.getAmount()));
        }
        if (event.getDetail() != null && !event.getDetail().isBlank()) {
            sb.append("; note=").append(event.getDetail());
        }

        String details = sb.toString();
        return details.length() > 1000 ? details.substring(0, 1000) : details;
    }
}
