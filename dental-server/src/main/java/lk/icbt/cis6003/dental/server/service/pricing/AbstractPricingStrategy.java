package lk.icbt.cis6003.dental.server.service.pricing;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.server.domain.InvoiceLine;
import lk.icbt.cis6003.dental.server.util.MoneyUtils;

import java.math.BigDecimal;

/**
 * <b>Template Method pattern</b> holding the invariant half of every bill.
 *
 * <p>The sequence subtotal → discount → VAT → total is fixed by Sri Lankan tax
 * law and must be identical on every receipt the clinic issues. Only two steps
 * genuinely vary between treatment types, so those two are the abstract hooks:</p>
 *
 * <ol>
 *   <li>{@link #calculateSurcharge(PricingContext)} - any loading this rule adds</li>
 *   <li>{@link #resolveDiscount(PricingContext)} - which concession, if any, applies</li>
 * </ol>
 *
 * <p>{@link #calculate(PricingContext)} is deliberately {@code final}. A
 * subclass that "improved" the order - taxing before discounting, say - would
 * produce a bill that is quietly wrong by a few hundred rupees on every line.
 * Sealing the algorithm makes that impossible rather than merely discouraged.</p>
 */
public abstract class AbstractPricingStrategy implements PricingStrategy {

    /** Concession for patients aged 65 and over. */
    protected static final BigDecimal SENIOR_CONCESSION_PERCENTAGE = new BigDecimal("10.00");

    /** Concession for patients under 18. */
    protected static final BigDecimal CHILD_CONCESSION_PERCENTAGE = new BigDecimal("5.00");

    /** Ceiling on any single discount, matching the CHECK constraint on the invoice table. */
    protected static final BigDecimal MAX_DISCOUNT_PERCENTAGE = new BigDecimal("50.00");

    /**
     * The fixed billing algorithm. Subclasses supply the two variable steps.
     */
    @Override
    public final PricingResult calculate(PricingContext context) {
        BigDecimal consultationFee = MoneyUtils.scale(context.getConsultationFee());
        BigDecimal treatmentCost = MoneyUtils.scale(context.getTreatmentBasePrice());
        BigDecimal surcharge = MoneyUtils.scale(calculateSurcharge(context));

        BigDecimal subTotal = MoneyUtils.scale(consultationFee.add(treatmentCost).add(surcharge));

        DiscountDecision discount = resolveDiscount(context);
        BigDecimal discountPercentage = capDiscount(discount.percentage());
        BigDecimal discountAmount = MoneyUtils.percentageOf(subTotal, discountPercentage);

        BigDecimal taxableAmount = MoneyUtils.subtract(subTotal, discountAmount);
        BigDecimal taxRate = MoneyUtils.nullSafe(context.getTaxRate());
        BigDecimal taxAmount = MoneyUtils.rateOf(taxableAmount, taxRate);
        BigDecimal totalAmount = MoneyUtils.add(taxableAmount, taxAmount);

        PricingResult.Builder builder = PricingResult.builder()
                .strategyKey(getKey())
                .strategyName(getDisplayName())
                .consultationFee(consultationFee)
                .treatmentCost(treatmentCost)
                .surchargeAmount(surcharge)
                .subTotal(subTotal)
                .discountPercentage(discountPercentage)
                .discountAmount(discountAmount)
                .discountReason(discount.reason())
                .taxableAmount(taxableAmount)
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount);

        buildReceiptLines(builder, context, consultationFee, treatmentCost, surcharge,
                          discountPercentage, discountAmount, taxRate, taxAmount);

        builder.addExplanation("Pricing rule applied: " + getDisplayName() + " - " + getDescription());
        if (MoneyUtils.isPositive(surcharge)) {
            builder.addExplanation(surchargeDescription() + ": "
                    + MoneyUtils.formatWithCurrency(surcharge));
        }
        if (MoneyUtils.isPositive(discountAmount)) {
            builder.addExplanation(discount.reason() + " (" + discountPercentage + "%): -"
                    + MoneyUtils.formatWithCurrency(discountAmount));
        }
        builder.addExplanation("VAT at " + taxRate.multiply(MoneyUtils.HUNDRED).stripTrailingZeros().toPlainString()
                + "% on " + MoneyUtils.formatWithCurrency(taxableAmount)
                + " = " + MoneyUtils.formatWithCurrency(taxAmount));

        return builder.build();
    }

    /** Produces the printed lines in a fixed, auditable order. */
    private void buildReceiptLines(PricingResult.Builder builder, PricingContext context,
                                   BigDecimal consultationFee, BigDecimal treatmentCost,
                                   BigDecimal surcharge, BigDecimal discountPercentage,
                                   BigDecimal discountAmount, BigDecimal taxRate, BigDecimal taxAmount) {

        if (MoneyUtils.isPositive(consultationFee)) {
            builder.addLine(new InvoiceLine("Consultation fee", 1, consultationFee, InvoiceLine.TYPE_CHARGE));
        }

        String treatmentLabel = context.getTreatmentName() == null
                ? "Treatment" : context.getTreatmentName();
        builder.addLine(new InvoiceLine(treatmentLabel, 1, treatmentCost, InvoiceLine.TYPE_CHARGE));

        if (MoneyUtils.isPositive(surcharge)) {
            builder.addLine(new InvoiceLine(surchargeDescription(), 1, surcharge, InvoiceLine.TYPE_SURCHARGE));
        }

        if (MoneyUtils.isPositive(discountAmount)) {
            InvoiceLine discountLine = new InvoiceLine(
                    "Discount (" + discountPercentage.stripTrailingZeros().toPlainString() + "%)",
                    1, discountAmount.negate(), InvoiceLine.TYPE_DISCOUNT);
            builder.addLine(discountLine);
        }

        if (MoneyUtils.isPositive(taxAmount)) {
            String vatLabel = "VAT @ "
                    + taxRate.multiply(MoneyUtils.HUNDRED).stripTrailingZeros().toPlainString() + "%";
            builder.addLine(new InvoiceLine(vatLabel, 1, taxAmount, InvoiceLine.TYPE_TAX));
        }
    }

    /* ------------------------------------------------------------------ */
    /* Hooks for subclasses                                                */
    /* ------------------------------------------------------------------ */

    /**
     * @return the loading this rule adds to the bill; {@link BigDecimal#ZERO}
     *         when the rule adds none
     */
    protected abstract BigDecimal calculateSurcharge(PricingContext context);

    /** @return the discount this rule grants, together with its printed reason */
    protected abstract DiscountDecision resolveDiscount(PricingContext context);

    /** @return the wording of the surcharge line on the receipt */
    protected String surchargeDescription() {
        return "Surcharge";
    }

    /* ------------------------------------------------------------------ */
    /* Shared helpers                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * The clinic's standard concession policy, shared by every rule that
     * grants concessions at all.
     *
     * <p>Concessions and the manual discount are <em>not</em> added together -
     * the larger of the two applies. Stacking a 10% senior concession on a 40%
     * goodwill discount would breach the 50% ceiling the clinic set, and
     * "highest single discount wins" is the policy staff can explain at the
     * desk.</p>
     */
    protected DiscountDecision standardConcession(PricingContext context) {
        BigDecimal manual = MoneyUtils.nullSafe(context.getRequestedDiscountPercentage());

        BigDecimal concession = BigDecimal.ZERO;
        String concessionReason = null;
        if (context.isPatientSeniorCitizen()) {
            concession = SENIOR_CONCESSION_PERCENTAGE;
            concessionReason = "Senior citizen concession";
        } else if (context.isPatientMinor()) {
            concession = CHILD_CONCESSION_PERCENTAGE;
            concessionReason = "Child patient concession";
        }

        if (concession.compareTo(manual) >= 0 && MoneyUtils.isPositive(concession)) {
            return new DiscountDecision(concession, concessionReason);
        }
        if (MoneyUtils.isPositive(manual)) {
            return new DiscountDecision(manual, "Discount approved at reception");
        }
        return DiscountDecision.none();
    }

    private BigDecimal capDiscount(BigDecimal percentage) {
        BigDecimal value = MoneyUtils.nullSafe(percentage);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value.min(MAX_DISCOUNT_PERCENTAGE)
                    .setScale(ClinicConstants.MONEY_SCALE, java.math.RoundingMode.HALF_UP);
    }

    /**
     * A discount together with the reason printed next to it.
     *
     * @param percentage the discount as a percentage, e.g. {@code 10} for 10%
     * @param reason     the wording shown to the patient
     */
    public record DiscountDecision(BigDecimal percentage, String reason) {

        public static DiscountDecision none() {
            return new DiscountDecision(BigDecimal.ZERO, null);
        }
    }
}
