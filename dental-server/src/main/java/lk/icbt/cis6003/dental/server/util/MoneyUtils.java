package lk.icbt.cis6003.dental.server.util;

import lk.icbt.cis6003.dental.common.ClinicConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Money arithmetic for the clinic.
 *
 * <p>Every amount in the system is a {@link BigDecimal} scaled to 2 decimal
 * places and rounded {@code HALF_UP}. Using {@code double} for currency is the
 * textbook defect - {@code 0.1 + 0.2 != 0.3} in binary floating point - and it
 * produces receipts whose lines do not add up to their total. Centralising the
 * rounding here means no service can accidentally use a different rule.</p>
 */
public final class MoneyUtils {

    private MoneyUtils() {
        throw new AssertionError("MoneyUtils is a utility class and must not be instantiated");
    }

    private static final DecimalFormat DISPLAY_FORMAT = new DecimalFormat("#,##0.00");

    public static final BigDecimal HUNDRED = new BigDecimal("100");

    /** Rounds to the clinic's money scale. {@code null} becomes zero. */
    public static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(ClinicConstants.MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(ClinicConstants.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(ClinicConstants.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return scale(nullSafe(a).add(nullSafe(b)));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return scale(nullSafe(a).subtract(nullSafe(b)));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return scale(nullSafe(a).multiply(nullSafe(b)));
    }

    /**
     * @param amount     the base amount
     * @param percentage a percentage, e.g. {@code 10} for 10%
     * @return {@code amount x percentage / 100}, rounded
     */
    public static BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage) {
        return scale(nullSafe(amount)
                .multiply(nullSafe(percentage))
                .divide(HUNDRED, ClinicConstants.MONEY_SCALE, RoundingMode.HALF_UP));
    }

    /**
     * @param amount   the base amount
     * @param rate     a fractional rate, e.g. {@code 0.18} for 18%
     */
    public static BigDecimal rateOf(BigDecimal amount, BigDecimal rate) {
        return scale(nullSafe(amount).multiply(nullSafe(rate)));
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return nullSafe(a).compareTo(nullSafe(b)) >= 0 ? nullSafe(a) : nullSafe(b);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    /** {@code 12345.5} becomes {@code "12,345.50"} - for receipts and screens. */
    public static String format(BigDecimal value) {
        synchronized (DISPLAY_FORMAT) {
            return DISPLAY_FORMAT.format(nullSafe(value));
        }
    }

    /** {@code 12345.5} becomes {@code "Rs. 12,345.50"}. */
    public static String formatWithCurrency(BigDecimal value) {
        return ClinicConstants.CURRENCY_SYMBOL + " " + format(value);
    }
}
