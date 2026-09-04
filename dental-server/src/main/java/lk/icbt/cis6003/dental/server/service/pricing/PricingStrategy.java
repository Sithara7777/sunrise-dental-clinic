package lk.icbt.cis6003.dental.server.service.pricing;

/**
 * <b>Strategy pattern</b> - the interchangeable billing rule for a treatment.
 *
 * <p><b>The problem it solves.</b> The scenario says the bill is calculated
 * "based on treatment type and consultation fee", but different treatment
 * types genuinely price differently: a surgical extraction carries a
 * sterilisation and consumables surcharge, an elective cosmetic procedure is
 * excluded from the clinic's senior and child concessions, and an emergency
 * seen out of hours carries a loading. Expressing that as a
 * {@code switch (treatment.getCategory())} inside the billing service would
 * mean every new treatment category edits - and risks breaking - the one
 * method that prints every bill in the clinic.</p>
 *
 * <p><b>How this is better.</b> Each rule is a separate class with its own
 * unit tests. Adding "INSURANCE_SCHEME" pricing next year means writing one new
 * {@code @Component} and one migration row; {@code BillingService} is not
 * touched, which is the Open/Closed Principle applied to the part of the
 * system where a mistake is most expensive.</p>
 *
 * <p><b>Cost, honestly stated.</b> Four small classes plus a factory is more
 * code than one {@code switch}, and a developer must open two files to follow
 * a price. The trade is worth it here specifically because billing errors are
 * one of the four problems the clinic asked us to fix, so isolating and
 * testing each rule in isolation has direct value.</p>
 *
 * @see PricingStrategyFactory
 */
public interface PricingStrategy {

    /**
     * @return the discriminator stored in {@code treatment.pricing_strategy},
     *         e.g. {@code "SURGICAL"}
     */
    String getKey();

    /** @return a label for the receipt and the treatment maintenance screen */
    String getDisplayName();

    /** @return why this rule exists - shown in the admin UI and the report */
    String getDescription();

    /**
     * Prices one appointment.
     *
     * @param context the immutable inputs; never {@code null}
     * @return the full itemised breakdown, never {@code null}
     */
    PricingResult calculate(PricingContext context);
}
