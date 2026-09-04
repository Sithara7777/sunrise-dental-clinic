package lk.icbt.cis6003.dental.server.db.function;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Bodies of the clinic's database <b>stored functions</b>.
 *
 * <p>H2 implements a user-defined function by aliasing a public static Java
 * method, so these methods <em>are</em> the stored functions once
 * {@code V3__functions_and_views.sql} has run:</p>
 *
 * <pre>
 *   CREATE ALIAS FN_INVOICE_TOTAL  DETERMINISTIC FOR '...ClinicFunctions.invoiceTotal';
 *   CREATE ALIAS FN_AGEING_BUCKET  DETERMINISTIC FOR '...ClinicFunctions.ageingBucket';
 * </pre>
 *
 * <p>The MySQL profile declares the same two functions natively in
 * {@code V3__functions_and_views.sql} under {@code db/migration/mysql}, with
 * identical names and semantics, so {@link
 * lk.icbt.cis6003.dental.server.repository.dao.JdbcReportingDao} runs unchanged
 * against either engine.</p>
 *
 * <p><b>Why put the money formula in the database at all?</b> Because the
 * clinic's accountant will eventually run a SQL query over this schema
 * directly. If the VAT and discount rules lived only in Java, that query would
 * quietly produce a different number from the printed receipt. Defining it once
 * as {@code FN_INVOICE_TOTAL} means the application and any ad-hoc query agree
 * by construction - and the billing service asserts that agreement on every
 * bill it issues.</p>
 */
public final class ClinicFunctions {

    private ClinicFunctions() {
        throw new AssertionError("ClinicFunctions holds stored-function bodies and must not be instantiated");
    }

    private static final int MONEY_SCALE = 2;

    /**
     * The clinic's billing formula, in one authoritative place.
     *
     * <pre>
     *   subTotal      = consultationFee + treatmentCost + surcharge
     *   discount      = subTotal x discountPercentage / 100
     *   taxableAmount = subTotal - discount
     *   tax           = taxableAmount x taxRate
     *   total         = taxableAmount + tax
     * </pre>
     *
     * <p>Discount is applied <em>before</em> VAT because Sri Lankan VAT is
     * charged on the consideration actually received, not on the list price.</p>
     *
     * @param consultationFee    the dentist's consultation charge
     * @param treatmentCost      the treatment charge after strategy adjustment
     * @param surcharge          any strategy-driven loading (may be null)
     * @param discountPercentage discount as a percentage, e.g. 10 for 10%
     * @param taxRate            VAT as a fraction, e.g. 0.18 for 18%
     * @return the amount payable, rounded half-up to 2 decimal places
     */
    public static BigDecimal invoiceTotal(BigDecimal consultationFee,
                                          BigDecimal treatmentCost,
                                          BigDecimal surcharge,
                                          BigDecimal discountPercentage,
                                          BigDecimal taxRate) {

        BigDecimal fee = nullSafe(consultationFee);
        BigDecimal cost = nullSafe(treatmentCost);
        BigDecimal load = nullSafe(surcharge);
        BigDecimal discountPct = nullSafe(discountPercentage);
        BigDecimal vat = nullSafe(taxRate);

        BigDecimal subTotal = fee.add(cost).add(load);

        BigDecimal discount = subTotal
                .multiply(discountPct)
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal taxable = subTotal.subtract(discount);
        BigDecimal tax = taxable.multiply(vat).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return taxable.add(tax).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Groups a debt into the clinic's standard ageing bands.
     *
     * <p>Used by the {@code v_outstanding_invoice} view, so the debtor report
     * and any manual query bucket money identically.</p>
     *
     * @param daysOutstanding whole days since the bill was issued
     * @return {@code "0-30"}, {@code "31-60"}, {@code "61-90"} or {@code "90+"}
     */
    public static String ageingBucket(long daysOutstanding) {
        if (daysOutstanding <= 30) {
            return "0-30";
        }
        if (daysOutstanding <= 60) {
            return "31-60";
        }
        if (daysOutstanding <= 90) {
            return "61-90";
        }
        return "90+";
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
