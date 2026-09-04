package lk.icbt.cis6003.dental.server.db.function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the bodies of the database stored functions.
 *
 * <p><b>An unusual and deliberate benefit of the H2 design.</b> In H2 a stored
 * function is a Java static method registered with {@code CREATE ALIAS}. That
 * means {@code FN_INVOICE_TOTAL} and {@code FN_AGEING_BUCKET} can be unit
 * tested here directly, with no database running at all - something that is
 * normally impossible for stored procedure logic and which usually leaves it
 * as the least-tested code in a system.</p>
 *
 * <p>{@code DatabaseFeaturesIT} then proves the same methods really are
 * reachable as SQL functions once the migrations have run, and
 * {@code BillingServiceTest} proves the Java pricing tier agrees with them.</p>
 */
@DisplayName("Database stored functions")
class ClinicFunctionsTest {

    @Test
    @DisplayName("FN_INVOICE_TOTAL: consultation + treatment + VAT")
    void simpleTotal() {
        // 1,500 + 6,500 = 8,000; VAT 18% = 1,440; total = 9,440
        BigDecimal total = ClinicFunctions.invoiceTotal(
                new BigDecimal("1500.00"), new BigDecimal("6500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0.18"));

        assertThat(total).isEqualByComparingTo("9440.00");
    }

    @Test
    @DisplayName("FN_INVOICE_TOTAL: includes a surcharge in the taxable amount")
    void includesSurcharge() {
        // 1,500 + 25,000 + 3,000 = 29,500; VAT 18% = 5,310; total = 34,810
        BigDecimal total = ClinicFunctions.invoiceTotal(
                new BigDecimal("1500.00"), new BigDecimal("25000.00"),
                new BigDecimal("3000.00"), BigDecimal.ZERO, new BigDecimal("0.18"));

        assertThat(total).isEqualByComparingTo("34810.00");
    }

    @Test
    @DisplayName("FN_INVOICE_TOTAL: applies the discount BEFORE VAT")
    void discountAppliedBeforeVat() {
        // 8,000 - 10% (800) = 7,200 taxable; VAT = 1,296; total = 8,496.
        // Were VAT charged first the total would be 8,496 too by coincidence
        // at this rate, so the taxable-amount assertion in PricingStrategyTest
        // is the one that pins the ordering; this pins the published total.
        BigDecimal total = ClinicFunctions.invoiceTotal(
                new BigDecimal("1500.00"), new BigDecimal("6500.00"),
                BigDecimal.ZERO, new BigDecimal("10"), new BigDecimal("0.18"));

        assertThat(total).isEqualByComparingTo("8496.00");
    }

    @Test
    @DisplayName("FN_INVOICE_TOTAL: treats every null argument as zero")
    void nullsAreTreatedAsZero() {
        assertThat(ClinicFunctions.invoiceTotal(null, null, null, null, null))
                .isEqualByComparingTo("0.00");

        assertThat(ClinicFunctions.invoiceTotal(
                new BigDecimal("1000.00"), null, null, null, new BigDecimal("0.18")))
                .isEqualByComparingTo("1180.00");
    }

    @Test
    @DisplayName("FN_INVOICE_TOTAL: always returns exactly two decimal places")
    void alwaysScaledToTwoPlaces() {
        BigDecimal total = ClinicFunctions.invoiceTotal(
                new BigDecimal("333.33"), new BigDecimal("999.99"),
                BigDecimal.ZERO, new BigDecimal("7.5"), new BigDecimal("0.18"));

        assertThat(total.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("FN_INVOICE_TOTAL: a 100% discount leaves nothing to pay")
    void fullDiscountLeavesZero() {
        assertThat(ClinicFunctions.invoiceTotal(
                new BigDecimal("1500.00"), new BigDecimal("6500.00"),
                BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("0.18")))
                .isEqualByComparingTo("0.00");
    }

    /* ------------------------------------------------------------------ */

    @ParameterizedTest(name = "{0} days outstanding falls in the {1} band")
    @CsvSource({
        "0,   0-30",
        "1,   0-30",
        "30,  0-30",
        "31,  31-60",
        "45,  31-60",
        "60,  31-60",
        "61,  61-90",
        "90,  61-90",
        "91,  90+",
        "365, 90+"
    })
    @DisplayName("FN_AGEING_BUCKET: bands the debt correctly, including at every boundary")
    void ageingBands(long days, String expected) {
        assertThat(ClinicFunctions.ageingBucket(days)).isEqualTo(expected);
    }

    @Test
    @DisplayName("FN_AGEING_BUCKET: a bill issued today is in the newest band, not an error")
    void issuedTodayIsNewest() {
        assertThat(ClinicFunctions.ageingBucket(0)).isEqualTo("0-30");
    }
}
