package lk.icbt.cis6003.dental.server.service.notification.gateway;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.enums.NotificationChannel;
import lk.icbt.cis6003.dental.server.config.ClinicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <b>Adapter</b> onto an SMS aggregator, implemented as a simulator.
 *
 * <p>Sri Lankan SMS aggregators charge per message and require a commercial
 * account, so this gateway simulates the transport rather than buying credit
 * for a student project. What it does <em>not</em> simulate away is the
 * behaviour that matters for the design:</p>
 *
 * <ul>
 *   <li>it normalises numbers to E.164 ({@code 0771234567} to
 *       {@code +94771234567}), which every real aggregator requires;</li>
 *   <li>it rejects malformed numbers exactly as a real API would, so the
 *       failure path in the notification log is genuinely exercised;</li>
 *   <li>it enforces the 160-character GSM-7 limit and reports the segment
 *       count, because that is what the clinic is billed on.</li>
 * </ul>
 *
 * <p>Replacing it with a live aggregator means writing one class implementing
 * {@link MessageGateway} and swapping the {@code @Component}. Nothing above
 * this layer knows the difference.</p>
 */
@Component
public class MockSmsGateway implements MessageGateway {

    private static final Logger log = LoggerFactory.getLogger(MockSmsGateway.class);

    /** One GSM-7 SMS segment. Longer messages are billed per segment. */
    private static final int SMS_SEGMENT_LENGTH = 160;

    private final ClinicProperties properties;

    public MockSmsGateway(ClinicProperties properties) {
        this.properties = properties;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean isEnabled() {
        return properties.getNotifications().isEnabled()
                && properties.getNotifications().isSmsEnabled();
    }

    @Override
    public String getDescription() {
        return "Simulated SMS aggregator - normalises to E.164 and validates, but does not transmit";
    }

    @Override
    public void send(GatewayMessage message) throws GatewayException {
        String normalised = normaliseToE164(message.recipient());

        String body = message.body() == null ? "" : message.body();
        int segments = (int) Math.ceil((double) body.length() / SMS_SEGMENT_LENGTH);
        if (segments == 0) {
            throw new GatewayException("Refusing to send an empty SMS to " + normalised);
        }

        log.info("""

                ==================== SMS (simulated aggregator) ==================
                To       : {}
                Ref      : {}
                Segments : {} ({} characters)
                ------------------------------------------------------------------
                {}
                ==================================================================
                """,
                normalised, message.referenceKey(), segments, body.length(), body);
    }

    /**
     * Converts a Sri Lankan number to international format.
     *
     * @throws GatewayException if the number is not a valid Sri Lankan number -
     *         the same rejection a real aggregator would return
     */
    private String normaliseToE164(String rawNumber) throws GatewayException {
        if (rawNumber == null) {
            throw new GatewayException("SMS recipient number is missing");
        }

        String digits = rawNumber.replaceAll("[\\s()-]", "");
        if (!digits.matches(ClinicConstants.CONTACT_NUMBER_PATTERN)) {
            throw new GatewayException("'" + rawNumber
                    + "' is not a valid Sri Lankan telephone number and was rejected by the SMS gateway");
        }

        if (digits.startsWith("+94")) {
            return digits;
        }
        // Local format 0XXXXXXXXX becomes +94XXXXXXXXX
        return "+94" + digits.substring(1);
    }
}
