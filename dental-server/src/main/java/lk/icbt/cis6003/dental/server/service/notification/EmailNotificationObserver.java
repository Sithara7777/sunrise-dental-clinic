package lk.icbt.cis6003.dental.server.service.notification;

import lk.icbt.cis6003.dental.common.enums.NotificationChannel;
import lk.icbt.cis6003.dental.server.domain.NotificationLog;
import lk.icbt.cis6003.dental.server.repository.NotificationLogRepository;
import lk.icbt.cis6003.dental.server.service.notification.gateway.GatewayException;
import lk.icbt.cis6003.dental.server.service.notification.gateway.GatewayMessage;
import lk.icbt.cis6003.dental.server.service.notification.gateway.MessageGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <b>Observer</b> that e-mails the patient.
 *
 * <p>Chooses its transport by asking the injected gateways which one serves
 * {@link NotificationChannel#EMAIL} - it never names {@code SmtpEmailGateway}
 * or {@code ConsoleEmailGateway}, so the deployment decides.</p>
 *
 * <p><b>Why {@code REQUIRES_NEW}.</b> The delivery log must survive even if the
 * surrounding business transaction later rolls back. "We told the patient their
 * appointment was cancelled" is true whether or not a subsequent step failed,
 * and a support enquiry needs to see that.</p>
 */
@Component
public class EmailNotificationObserver implements AppointmentObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationObserver.class);

    private final NotificationComposer composer;
    private final NotificationLogRepository notificationLogRepository;
    private final MessageGateway emailGateway;

    public EmailNotificationObserver(NotificationComposer composer,
                                     NotificationLogRepository notificationLogRepository,
                                     List<MessageGateway> gateways) {
        this.composer = composer;
        this.notificationLogRepository = notificationLogRepository;

        Optional<MessageGateway> gateway = gateways.stream()
                .filter(g -> g.getChannel() == NotificationChannel.EMAIL)
                .findFirst();
        this.emailGateway = gateway.orElse(null);

        if (this.emailGateway == null) {
            log.warn("No e-mail gateway is configured - patient e-mail alerts are disabled");
        } else {
            log.info("E-mail alerts will use: {}", this.emailGateway.getDescription());
        }
    }

    @Override
    public String getObserverName() {
        return "EmailNotificationObserver";
    }

    /** Only patient-facing events, and only when we hold an e-mail address. */
    @Override
    public boolean supports(AppointmentEvent event) {
        return emailGateway != null
                && event.getType().isNotifyPatient()
                && event.hasEmail();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentEvent(AppointmentEvent event) {
        String subject = composer.emailSubject(event);
        String body = composer.emailBody(event);

        NotificationLog entry = new NotificationLog(
                NotificationChannel.EMAIL, event.getPatientEmail(), subject, body,
                event.getReference() != null ? event.getReference() : event.getAppointmentNumber());

        if (!emailGateway.isEnabled()) {
            entry.markSuppressed("E-mail notifications are switched off in configuration");
            notificationLogRepository.save(entry);
            return;
        }

        try {
            emailGateway.send(new GatewayMessage(event.getPatientEmail(), subject, body,
                                                 entry.getReferenceKey()));
        } catch (GatewayException ex) {
            // Recorded, not rethrown: a failed e-mail must not undo a booking.
            entry.markFailed(ex.getMessage());
            log.warn("E-mail to {} for {} failed: {}",
                     event.getPatientEmail(), event.getAppointmentNumber(), ex.getMessage());
        }

        notificationLogRepository.save(entry);
    }
}
