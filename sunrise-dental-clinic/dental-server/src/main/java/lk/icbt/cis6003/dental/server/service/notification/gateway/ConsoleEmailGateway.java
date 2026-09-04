package lk.icbt.cis6003.dental.server.service.notification.gateway;

import lk.icbt.cis6003.dental.common.enums.NotificationChannel;
import lk.icbt.cis6003.dental.server.config.ClinicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * <b>Adapter</b> that "delivers" e-mail by writing it to the application log.
 *
 * <p>This is the default e-mail transport. It exists so the complete
 * notification feature - templates, observers, delivery log, failure handling -
 * can be demonstrated and tested on a machine with no SMTP server, no
 * credentials and no internet connection. The message body written here is
 * byte-for-byte what {@link SmtpEmailGateway} would transmit.</p>
 *
 * <p>Switching to real delivery is one property:
 * {@code clinic.notifications.smtp-enabled=true}. No business code changes,
 * which is the practical payoff of the Adapter pattern rather than a
 * theoretical one.</p>
 */
@Component
@ConditionalOnProperty(prefix = "clinic.notifications", name = "smtp-enabled",
                       havingValue = "false", matchIfMissing = true)
public class ConsoleEmailGateway implements MessageGateway {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailGateway.class);

    private final ClinicProperties properties;

    public ConsoleEmailGateway(ClinicProperties properties) {
        this.properties = properties;
        log.info("Console e-mail gateway active - alerts are written to the log and the "
                 + "notification history screen, not to a real mailbox. "
                 + "Set clinic.notifications.smtp-enabled=true to send real e-mail.");
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        return properties.getNotifications().isEnabled()
                && properties.getNotifications().isEmailEnabled();
    }

    @Override
    public String getDescription() {
        return "Console e-mail (development mode) - messages are logged and stored, not transmitted";
    }

    @Override
    public void send(GatewayMessage message) {
        log.info("""

                ==================== E-MAIL (console gateway) ====================
                From    : {} <{}>
                To      : {}
                Subject : {}
                Ref     : {}
                ------------------------------------------------------------------
                {}
                ==================================================================
                """,
                properties.getNotifications().getFromName(),
                properties.getNotifications().getFromAddress(),
                message.recipient(),
                message.subject(),
                message.referenceKey(),
                message.body());
    }
}
