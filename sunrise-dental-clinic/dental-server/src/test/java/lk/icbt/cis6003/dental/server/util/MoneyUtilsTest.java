package lk.icbt.cis6003.dental.server.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the money arithmetic every bill depends on.
 *
 * <p>These look trivial and are not. Every rounding rule in the system funnels
 * through this class, so a defect here would appear as a receipt whose lines do
 * not add up to its total - the single most damaging kind of bug in a billing
 * system, because it destroys the patient's trust in every other figure on the
 * page.</p>
 */
@DisplayName("Money arithmetic")
class MoneyUtilsTest {

    @Test
    @DisplayName("scales every amount to exactly two decimal places")
    void scalesToTwoPlaces() {
        assertThat(MoneyUtils.scale(new BigDecimal("100"))).isEqualByComparingTo("100.00");
        assertThat(MoneyUtils.scale(new BigDecimal("100"))).hasToString("100.00");
        assertThat(MoneyUtils.scale(new BigDecimal("99.999"))).hasToString("100.00");
    }

    @Test
    @DisplayName("rounds half up, the convention Sri Lankan invoicing expects")
    void roundsHalfUp() {
        assertThat(MoneyUtils.scale(new BigDecimal("10.005"))).hasToString("10.01");
        assertThat(MoneyUtils.scale(new BigDecimal("10.004"))).hasToString("10.00");
    }

    @Test
    @DisplayName("treats null as zero rather than throwing")
    void nullIsZero() {
        assertThat(MoneyUtils.scale(null)).isEqualByComparingTo("0.00");
        assertThat(MoneyUtils.nullSafe(null)).isEqualByComparingTo("0");
        assertThat(MoneyUtils.add(null, null)).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest(name = "{1}% of {0} is {2}")
    @CsvSource({
        "8000.00, 10,   800.00",
        "8000.00, 5,    400.00",
        "8000.00, 12.5, 1000.00",
        "6500.00, 0,    0.00",
        "999.99,  33,   330.00"
    })
    @DisplayName("percentageOf computes a percentage correctly")
    void percentageOf(BigDecimal amount, BigDecimal percentage, BigDecimal expected) {
        assertThat(MoneyUtils.percentageOf(amount, percentage)).isEqualByComparingTo(expected);
    }

    @ParameterizedTest(name = "{0} at a rate of {1} is {2}")
    @CsvSource({
        "8000.00, 0.18, 1440.00",
        "7200.00, 0.18, 1296.00",
        "1000.00, 0.15, 150.00"
    })
    @DisplayName("rateOf computes VAT from a fractional rate")
    void rateOf(BigDecimal amount, BigDecimal rate, BigDecimal expected) {
        assertThat(MoneyUtils.rateOf(amount, rate)).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("add and subtract keep the two-place scale")
    void addAndSubtractKeepScale() {
        assertThat(MoneyUtils.add(new BigDecimal("1500"), new BigDecimal("6500")))
                .hasToString("8000.00");
        assertThat(MoneyUtils.subtract(new BigDecimal("8000"), new BigDecimal("800")))
                .hasToString("7200.00");
    }

    @Test
    @DisplayName("max returns the larger amount - used for the surgical surcharge floor")
    void maxPicksTheLarger() {
        assertThat(MoneyUtils.max(new BigDecimal("900.00"), new BigDecimal("1500.00")))
                .isEqualByComparingTo("1500.00");
        assertThat(MoneyUtils.max(new BigDecimal("3000.00"), new BigDecimal("1500.00")))
                .isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("isPositive and isZero read correctly, including for a scaled zero")
    void positiveAndZeroChecks() {
        assertThat(MoneyUtils.isPositive(new BigDecimal("0.01"))).isTrue();
        assertThat(MoneyUtils.isPositive(BigDecimal.ZERO)).isFalse();
        assertThat(MoneyUtils.isPositive(null)).isFalse();

        assertThat(MoneyUtils.isZero(new BigDecimal("0.00"))).isTrue();
        assertThat(MoneyUtils.isZero(null)).isTrue();
        assertThat(MoneyUtils.isZero(new BigDecimal("0.01"))).isFalse();
    }

    @Test
    @DisplayName("formats with thousands separators for the receipt")
    void formatsForDisplay() {
        assertThat(MoneyUtils.format(new BigDecimal("12345.5"))).isEqualTo("12,345.50");
        assertThat(MoneyUtils.format(new BigDecimal("0"))).isEqualTo("0.00");
        assertThat(MoneyUtils.format(null)).isEqualTo("0.00");
    }

    @Test
    @DisplayName("formats with the currency symbol")
    void formatsWithCurrency() {
        assertThat(MoneyUtils.formatWithCurrency(new BigDecimal("9440.00")))
                .isEqualTo("Rs. 9,440.00");
    }

    @Test
    @DisplayName("the classic floating-point defect does not occur")
    void avoidsTheFloatingPointDefect() {
        // 0.1 + 0.2 is 0.30000000000000004 in binary floating point. With
        // BigDecimal it is exactly 0.30, which is why every amount in this
        // system is a BigDecimal and never a double.
        BigDecimal sum = MoneyUtils.add(new BigDecimal("0.1"), new BigDecimal("0.2"));

        assertThat(sum).isEqualByComparingTo("0.30");
        assertThat(sum).hasToString("0.30");
    }

    @Test
    @DisplayName("a hundred separate 0.01 additions still total exactly 1.00")
    void repeatedAdditionDoesNotDrift() {
        BigDecimal total = MoneyUtils.zero();
        for (int i = 0; i < 100; i++) {
            total = MoneyUtils.add(total, new BigDecimal("0.01"));
        }

        assertThat(total).isEqualByComparingTo("1.00");
    }
}
