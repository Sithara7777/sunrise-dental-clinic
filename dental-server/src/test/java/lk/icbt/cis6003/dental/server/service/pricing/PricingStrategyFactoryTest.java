package lk.icbt.cis6003.dental.server.service.pricing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the pricing Factory.
 *
 * <p>The behaviour that matters most is the last one: an unrecognised key in
 * the {@code treatment.pricing_strategy} column must produce a correct
 * list-price bill and a warning, not an exception. A patient standing at the
 * desk should never be told the system cannot bill them because somebody
 * mistyped a reference-data value.</p>
 */
@DisplayName("Pricing strategy factory (Factory pattern)")
class PricingStrategyFactoryTest {

    private PricingStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PricingStrategyFactory(List.of(
                new StandardPricingStrategy(),
                new SurgicalPricingStrategy(),
                new CosmeticPricingStrategy(),
                new EmergencyPricingStrategy()));
    }

    @Test
    @DisplayName("resolves each key to its own rule")
    void resolvesEachKey() {
        assertThat(factory.resolve("STANDARD")).isInstanceOf(StandardPricingStrategy.class);
        assertThat(factory.resolve("SURGICAL")).isInstanceOf(SurgicalPricingStrategy.class);
        assertThat(factory.resolve("COSMETIC")).isInstanceOf(CosmeticPricingStrategy.class);
        assertThat(factory.resolve("EMERGENCY")).isInstanceOf(EmergencyPricingStrategy.class);
    }

    @Test
    @DisplayName("key matching ignores case and surrounding whitespace")
    void keyMatchingIsForgiving() {
        assertThat(factory.resolve("  surgical  ")).isInstanceOf(SurgicalPricingStrategy.class);
        assertThat(factory.resolve("Cosmetic")).isInstanceOf(CosmeticPricingStrategy.class);
    }

    @Test
    @DisplayName("an unknown key falls back to STANDARD rather than failing to bill")
    void unknownKeyFallsBackToStandard() {
        PricingStrategy resolved = factory.resolve("NOT_A_REAL_RULE");

        assertThat(resolved).isInstanceOf(StandardPricingStrategy.class);
    }

    @Test
    @DisplayName("a null or blank key also falls back to STANDARD")
    void nullKeyFallsBackToStandard() {
        assertThat(factory.resolve(null)).isInstanceOf(StandardPricingStrategy.class);
        assertThat(factory.resolve("   ")).isInstanceOf(StandardPricingStrategy.class);
    }

    @Test
    @DisplayName("the fallback still produces a correct, complete bill")
    void fallbackStillBillsCorrectly() {
        PricingResult result = factory.resolve("TYPO").calculate(PricingContext.builder()
                .treatmentBasePrice(new BigDecimal("6500.00"))
                .consultationFee(new BigDecimal("1500.00"))
                .taxRate(new BigDecimal("0.18"))
                .appointmentDate(java.time.LocalDate.now().plusDays(1))
                .appointmentTime(java.time.LocalTime.of(10, 0))
                .build());

        assertThat(result.getTotalAmount()).isEqualByComparingTo("9440.00");
    }

    @Test
    @DisplayName("isSupported() distinguishes a real key from a typo, for reference-data validation")
    void isSupportedGuardsReferenceData() {
        assertThat(factory.isSupported("SURGICAL")).isTrue();
        assertThat(factory.isSupported("SURGCAL")).isFalse();
        assertThat(factory.isSupported(null)).isFalse();
    }

    @Test
    @DisplayName("every registered rule is advertised for the maintenance drop-down")
    void advertisesEveryRule() {
        assertThat(factory.getSupportedKeys())
                .containsExactlyInAnyOrder("STANDARD", "SURGICAL", "COSMETIC", "EMERGENCY");
    }

    @Test
    @DisplayName("two rules claiming the same key is a start-up failure, not a silent overwrite")
    void duplicateKeysFailFast() {
        assertThatThrownBy(() -> new PricingStrategyFactory(List.of(
                new StandardPricingStrategy(),
                new StandardPricingStrategy())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("claim the key");
    }

    @Test
    @DisplayName("a missing STANDARD rule is a start-up failure - there would be no fallback")
    void missingFallbackFailsFast() {
        assertThatThrownBy(() -> new PricingStrategyFactory(List.of(new SurgicalPricingStrategy())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mandatory");
    }
}
