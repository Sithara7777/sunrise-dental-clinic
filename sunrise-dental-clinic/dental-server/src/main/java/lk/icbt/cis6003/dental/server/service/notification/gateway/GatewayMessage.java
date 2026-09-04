package lk.icbt.cis6003.dental.server.service.notification.gateway;

/**
 * A message in the one shape every delivery channel understands.
 *
 * <p>The vocabulary is deliberately neutral - {@code recipient}, not
 * "emailAddress" or "mobileNumber"; {@code subject}, which SMS simply ignores.
 * That is what lets a single {@link MessageGateway} interface stand in front of
 * transports as different as SMTP and an HTTP SMS API.</p>
 *
 * @param recipient    e-mail address or telephone number
 * @param subject      title; ignored by channels that have no concept of one
 * @param body         the message text
 * @param referenceKey the appointment or invoice number this relates to,
 *                     recorded in the notification log
 */
public record GatewayMessage(String recipient, String subject, String body, String referenceKey) {

    public GatewayMessage {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("A notification must have a recipient");
        }
    }
}
