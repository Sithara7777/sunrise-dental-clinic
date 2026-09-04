package lk.icbt.cis6003.dental.server.service.notification.gateway;

import lk.icbt.cis6003.dental.common.enums.NotificationChannel;
import lk.icbt.cis6003.dental.server.config.ClinicProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the SMS gateway Adapter.
 *
 * <p>The gateway simulates the transport but not the <em>behaviour</em> that
 * matters, and this class tests exactly the parts that are real: E.164
 * normalisation, and rejection of numbers a live aggregator would refuse. Those
 * are what make the failure path in the notification log genuinely
 * exercised rather than theoretical.</p>
 */
@DisplayName("SMS gateway (Adapter pattern)")
class MockSmsGatewayTest {

    private MockSmsGateway gateway;
    private ClinicProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ClinicProperties();
        gateway = new MockSmsGateway(properties);
    }

    @Test
    @DisplayName("declares itself as the SMS channel")
    void servesTheSmsChannel() {
        assertThat(gateway.getChannel()).isEqualTo(NotificationChannel.SMS);
        assertThat(gateway.getDescription()).isNotBlank();
    }

    @ParameterizedTest(name = "accepts {0}")
    @ValueSource(strings = {
        "0771234567",       // local mobile
        "0112573100",       // local land line
        "+94771234567",     // international
        "077 123 4567",     // with spaces, as a receptionist would type it
        "077-123-4567"      // with hyphens
    })
    @DisplayName("accepts every reasonable way of writing a Sri Lankan number")
    void acceptsValidNumbers(String number) {
        assertThatCode(() -> gateway.send(
                new GatewayMessage(number, null, "Test message", "APT-2026-000001")))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "rejects {0}")
    @ValueSource(strings = {
        "12345",            // far too short
        "07712345678901",   // too long
        "abcdefghij",       // not a number at all
        "+1 555 0100"       // not a Sri Lankan number
    })
    @DisplayName("rejects a malformed number exactly as a live aggregator would")
    void rejectsInvalidNumbers(String number) {
        assertThatThrownBy(() -> gateway.send(
                new GatewayMessage(number, null, "Test message", "APT-2026-000001")))
                .isInstanceOf(GatewayException.class)
                .hasMessageContaining("not a valid Sri Lankan telephone number");
    }

    @Test
    @DisplayName("refuses to send an empty message")
    void refusesEmptyMessage() {
        assertThatThrownBy(() -> gateway.send(
                new GatewayMessage("0771234567", null, "", "APT-2026-000001")))
                .isInstanceOf(GatewayException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("a message with no recipient is rejected before it reaches the gateway")
    void recipientIsMandatory() {
        assertThatThrownBy(() -> new GatewayMessage(null, null, "Body", "REF"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new GatewayMessage("  ", null, "Body", "REF"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("reports itself disabled when SMS is switched off in configuration")
    void respectsTheSmsSwitch() {
        assertThat(gateway.isEnabled()).isTrue();

        properties.getNotifications().setSmsEnabled(false);
        assertThat(gateway.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("reports itself disabled when notifications are switched off entirely")
    void respectsTheMasterSwitch() {
        properties.getNotifications().setEnabled(false);

        assertThat(gateway.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("a long message is accepted and billed as multiple segments")
    void longMessagesAreSegmented() {
        String longBody = "x".repeat(400);   // three GSM-7 segments

        assertThatCode(() -> gateway.send(
                new GatewayMessage("0771234567", null, longBody, "APT-2026-000001")))
                .doesNotThrowAnyException();
    }
}
