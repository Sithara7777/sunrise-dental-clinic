package lk.icbt.cis6003.dental.server.service.notification.gateway;

import lk.icbt.cis6003.dental.common.enums.NotificationChannel;

/**
 * <b>Adapter pattern</b> - the clinic's own outbound-messaging interface.
 *
 * <p><b>The problem it solves.</b> Sending an e-mail through Spring's
 * {@code JavaMailSender} and sending an SMS through a Sri Lankan aggregator's
 * HTTP API have nothing in common: different method names, different parameter
 * shapes, different failure modes. If the business tier called each one
 * directly it would be coupled to both vendors, and swapping SMS provider - a
 * routine commercial event - would mean editing the appointment service.</p>
 *
 * <p><b>How this is better.</b> This interface is the shape the clinic wants.
 * Each concrete gateway adapts one vendor to it. The observers know only
 * {@code send(GatewayMessage)}, so replacing the SMS vendor is one new class
 * and one configuration property, with no change above the adapter layer.</p>
 *
 * <p><b>Cost, honestly stated.</b> The common shape is a lowest common
 * denominator: it cannot express e-mail attachments or SMS delivery receipts.
 * That is an accepted trade for this system, whose alerts are short plain-text
 * notices. A future requirement for attachments would justify widening the
 * interface, not abandoning it.</p>
 */
public interface MessageGateway {

    /** @return the channel this gateway serves */
    NotificationChannel getChannel();

    /**
     * @return {@code false} when the channel is switched off by configuration,
     *         in which case the notification is logged as {@code SUPPRESSED}
     *         rather than silently dropped
     */
    boolean isEnabled();

    /** @return a short description of the transport, shown on the admin screen */
    String getDescription();

    /**
     * Delivers one message.
     *
     * @throws GatewayException when the transport fails; the caller records the
     *         failure in the notification log and carries on. A dental
     *         appointment must never fail to be booked because an SMS gateway
     *         is down.
     */
    void send(GatewayMessage message) throws GatewayException;
}
