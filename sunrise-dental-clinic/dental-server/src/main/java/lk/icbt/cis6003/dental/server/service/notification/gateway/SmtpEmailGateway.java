package lk.icbt.cis6003.dental.server.service.notification.gateway;

import lk.icbt.cis6003.dental.common.enums.NotificationChannel;
import lk.icbt.cis6003.dental.server.config.ClinicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * <b>Adapter</b> from the clinic's {@link MessageGateway} interface onto
 * Spring's {@code JavaMailSender}.
 *
 * <p>Active only when {@code clinic.notifications.smtp-enabled=true}, because
 * the default configuration must run on a marker's laptop with no mail server.
 * When it is inactive, {@link ConsoleEmailGateway} takes its place and the
 * business tier cannot tell the difference - which is the whole point of
 * putting an interface here.</p>
 */
@Component
@ConditionalOnProperty(prefix = "clinic.notifications", name = "smtp-enabled", havingValue = "true")
public class SmtpEmailGateway implements MessageGateway {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailGateway.class);

    private final JavaMailSender mailSender;
    private final ClinicProperties properties;

    public SmtpEmailGateway(JavaMailSender mailSender, ClinicProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
        log.info("SMTP e-mail gateway active - alerts will be delivered to real mailboxes");
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
        return "SMTP e-mail (JavaMailSender), from " + properties.getNotifications().getFromAddress();
    }

    @Override
    public void send(GatewayMessage message) throws GatewayException {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(properties.getNotifications().getFromAddress());
            mail.setTo(message.recipient());
            mail.setSubject(message.subject());
            mail.setText(message.body());
            mailSender.send(mail);
            log.debug("E-mail sent to {} regarding {}", message.recipient(), message.referenceKey());
        } catch (MailException ex) {
            // Wrapped, not propagated: the caller records the failure and the
            // clinical workflow continues.
            throw new GatewayException("SMTP delivery to " + message.recipient() + " failed: "
                    + ex.getMessage(), ex);
        }
    }
}
