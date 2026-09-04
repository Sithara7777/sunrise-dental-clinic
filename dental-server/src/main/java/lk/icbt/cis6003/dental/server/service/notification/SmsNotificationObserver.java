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

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * <b>Observer</b> that sends the patient an SMS.
 *
 * <p>Every patient record carries a mobile number - it is mandatory - but only
 * a minority give an e-mail address, so SMS is the channel that actually
 * reaches people. It is also the one the clinic pays for per message, which is
 * why this observer is more selective than the e-mail one: only the four events
 * a patient genuinely needs to act on are texted.</p>
 */
@Component
public class SmsNotificationObserver implements AppointmentObserver {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationObserver.class);

    /**
     * The only events worth the cost of an SMS. Confirmations and completions
     * are deliberately excluded - a patient who has just left the chair does
     * not need a text telling them they were treated.
     */
    private static final Set<AppointmentEventType> SMS_WORTHY = EnumSet.of(
            AppointmentEventType.BOOKED,
            AppointmentEventType.RESCHEDULED,
            AppointmentEventType.CANCELLED,
            AppointmentEventType.REMINDER);

    private final NotificationComposer composer;
    private final NotificationLogRepository notificationLogRepository;
    private final MessageGateway smsGateway;

    public SmsNotificationObserver(NotificationComposer composer,
                                   NotificationLogRepository notificationLogRepository,
                                   List<MessageGateway> gateways) {
        this.composer = composer;
        this.notificationLogRepository = notificationLogRepository;

        Optional<MessageGateway> gateway = gateways.stream()
                .filter(g -> g.getChannel() == NotificationChannel.SMS)
                .findFirst();
        this.smsGateway = gateway.orElse(null);

        if (this.smsGateway == null) {
            log.warn("No SMS gateway is configured - patient SMS alerts are disabled");
        } else {
            log.info("SMS alerts will use: {}", this.smsGateway.getDescription());
        }
    }

    @Override
    public String getObserverName() {
        return "SmsNotificationObserver";
    }

    @Override
    public boolean supports(AppointmentEvent event) {
        return smsGateway != null
                && SMS_WORTHY.contains(event.getType())
                && event.hasMobile();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentEvent(AppointmentEvent event) {
        String body = composer.smsBody(event);

        NotificationLog entry = new NotificationLog(
                NotificationChannel.SMS, event.getPatientContact(), null, body,
                event.getReference() != null ? event.getReference() : event.getAppointmentNumber());

        if (!smsGateway.isEnabled()) {
            entry.markSuppressed("SMS notifications are switched off in configuration");
            notificationLogRepository.save(entry);
            return;
        }

        try {
            smsGateway.send(new GatewayMessage(event.getPatientContact(), null, body,
                                               entry.getReferenceKey()));
        } catch (GatewayException ex) {
            entry.markFailed(ex.getMessage());
            log.warn("SMS to {} for {} failed: {}",
                     event.getPatientContact(), event.getAppointmentNumber(), ex.getMessage());
        }

        notificationLogRepository.save(entry);
    }
}
